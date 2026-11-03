// Supabase Edge Function: przekazuje jedno zadanie Todoisto do Notion.
//
// Wywołanie (z aplikacji, z tokenem sesji użytkownika):
//   POST /functions/v1/route-task  { "projectId": "…", "task": { "id": "…", "title": "…", … } }
// Odpowiedź 200: { remoteId, remoteUrl, existed }
// Błędy: 4xx { error, retryable:false } — konfiguracja (aplikacja pokazuje błąd i nie ponawia);
//        5xx/503 { error, retryable:true } — przejściowe (aplikacja ponawia z backoffem).
//
// Sekrety (NIGDY w aplikacji): NOTION_TOKEN — `supabase secrets set NOTION_TOKEN=secret_…`.
// SUPABASE_URL i SUPABASE_ANON_KEY są wstrzykiwane przez platformę.
// Wszystkie odczyty/zapisy w bazie idą z tokenem UŻYTKOWNIKA → obowiązuje RLS (bez service role).

import { NotionClient, RouteError, routeToNotion, type Destination, type TaskIn } from "./notion.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const NOTION_TOKEN = Deno.env.get("NOTION_TOKEN") ?? "";

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });

async function rest(path: string, jwt: string, init: RequestInit = {}) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    ...init,
    headers: { apikey: ANON, Authorization: `Bearer ${jwt}`, "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
  const text = await res.text();
  if (!res.ok) throw new RouteError(`Baza: ${text || res.status}`, res.status >= 500, res.status);
  return text ? JSON.parse(text) : null;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json(405, { error: "Tylko POST.", retryable: false });
  const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!jwt) return json(401, { error: "Brak sesji.", retryable: false });
  if (!NOTION_TOKEN) return json(500, { error: "Brak sekretu NOTION_TOKEN w Edge Function.", retryable: false });

  try {
    // Kim jest wywołujący (weryfikacja tokenu przez Supabase Auth).
    const ures = await fetch(`${SUPABASE_URL}/auth/v1/user`, { headers: { apikey: ANON, Authorization: `Bearer ${jwt}` } });
    if (!ures.ok) return json(401, { error: "Sesja nieważna — zaloguj się ponownie.", retryable: false });
    const user = await ures.json();

    const body = await req.json().catch(() => null);
    const task = body?.task as TaskIn | undefined;
    const projectId = String(body?.projectId ?? "");
    if (!task?.id || !task?.title || !/^\d+$/.test(projectId) || !/^\d+$/.test(String(task.id))) {
      return json(400, { error: "Nieprawidłowe dane zadania.", retryable: false });
    }
    task.id = String(task.id);

    const dests = await rest(
      `todoisto_destinations?select=*&project_id=eq.${projectId}&enabled=is.true&kind=eq.notion&limit=1`, jwt,
    ) as Destination[];
    const dest = dests?.[0];
    if (!dest) return json(404, { error: "Ten projekt nie ma skonfigurowanego przekazywania do Notion.", retryable: false });

    // 1) Rejestr: to zadanie już przekazane → zwróć tę samą stronę (idempotencja).
    const done = await rest(
      `todoisto_routed?select=remote_id,remote_url&task_id=eq.${task.id}&destination_id=eq.${dest.id}&limit=1`, jwt,
    );
    if (done?.[0]) return json(200, { remoteId: done[0].remote_id, remoteUrl: done[0].remote_url, existed: true });

    // 2) Notion: deduplikacja po kolumnie z id zadania, potem utworzenie strony.
    const page = await routeToNotion(new NotionClient(NOTION_TOKEN), dest, task);

    // 3) Zapis do rejestru (konflikt = równoległe wywołanie zdążyło pierwsze — OK).
    await rest("todoisto_routed", jwt, {
      method: "POST",
      headers: { Prefer: "resolution=ignore-duplicates" },
      body: JSON.stringify({ user_id: user.id, task_id: task.id, destination_id: dest.id, remote_id: page.id, remote_url: page.url }),
    });
    return json(200, { remoteId: page.id, remoteUrl: page.url, existed: page.existed });
  } catch (e) {
    if (e instanceof RouteError) return json(e.retryable ? 503 : 422, { error: e.message, retryable: e.retryable });
    return json(503, { error: `Nieoczekiwany błąd: ${(e as Error).message}`, retryable: true });
  }
});
