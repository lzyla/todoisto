// Edge Function: Google Health API → sygnały energii dla Todoisto (sen, tętno spoczynkowe, HRV).
//
//   POST {action:"start"}   (JWT użytkownika) → { url }  — adres zgody Google (otwierany w przeglądarce)
//   GET  /callback?code&state                 — powrót z Google; zapisuje token, pokazuje „wróć do aplikacji"
//   POST {action:"sync"}    (JWT użytkownika) → { connected, signals? , reason? }
//   POST {action:"disconnect"} (JWT)          → { connected:false }
//
// Wdrażać z --no-verify-jwt (callback z Google nie ma JWT) — sesję weryfikujemy sami (/auth/v1/user).
// Sekrety: GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET. Token odświeżania leży w tabeli bez dostępu dla
// anon/authenticated; tu czytamy go rolą serwisową WYŁĄCZNIE po zweryfikowaniu sesji.
// Zakresy: tylko odczyt (sen, metryki zdrowotne, aktywność).

import { buildSignals, type DayValue, type SleepPoint } from "./signals.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const ANON = Deno.env.get("SUPABASE_ANON_KEY")!;
const SERVICE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const CLIENT_ID = Deno.env.get("GOOGLE_CLIENT_ID") ?? "";
const CLIENT_SECRET = Deno.env.get("GOOGLE_CLIENT_SECRET") ?? "";
const REDIRECT = `${SUPABASE_URL}/functions/v1/health-sync/callback`;
const API = "https://health.googleapis.com/v4/users/me/dataTypes";
const TZ = "Europe/Warsaw";
const SCOPES = ["sleep", "health_metrics_and_measurements", "activity_and_fitness"]
  .map((s) => `https://www.googleapis.com/auth/googlehealth.${s}.readonly`);

const json = (status: number, body: unknown) =>
  new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
// Supabase wymusza text/plain dla odpowiedzi funkcji (HTML nie jest renderowany) — zwracamy czysty tekst z UTF-8.
const html = (msg: string, ok: boolean) =>
  new Response(
    `${ok ? "OK" : "BŁĄD"}: ${msg}\n\n${ok ? "Możesz zamknąć tę kartę i wrócić do Todoisto." : "Wróć do Todoisto i spróbuj ponownie."}\n`,
    { status: ok ? 200 : 400, headers: { "Content-Type": "text/plain; charset=utf-8" } },
  );

// ---- stan OAuth: podpisany (HMAC), żeby callback wiedział, czyj to token ----
const b64 = (buf: ArrayBuffer) => btoa(String.fromCharCode(...new Uint8Array(buf))).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
async function hmac(data: string): Promise<string> {
  const key = await crypto.subtle.importKey("raw", new TextEncoder().encode(CLIENT_SECRET), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return b64(await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(data)));
}
async function makeState(userId: string): Promise<string> {
  const payload = `${userId}.${Date.now() + 10 * 60_000}`;
  return `${payload}.${await hmac(payload)}`;
}
async function readState(state: string): Promise<string | null> {
  const i = state.lastIndexOf(".");
  if (i < 0) return null;
  const payload = state.slice(0, i), sig = state.slice(i + 1);
  if ((await hmac(payload)) !== sig) return null;
  const [userId, exp] = payload.split(".");
  return Number(exp) > Date.now() && /^[0-9a-f-]{36}$/.test(userId) ? userId : null;
}

// ---- baza (rola serwisowa, tylko tabela tokenów) ----
async function db(path: string, init: RequestInit = {}) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    ...init,
    headers: { apikey: SERVICE, Authorization: `Bearer ${SERVICE}`, "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`Baza ${res.status}: ${text}`);
  return text ? JSON.parse(text) : null;
}
const saveToken = (userId: string, refresh: string, scope: string) =>
  db("todoisto_health_tokens?on_conflict=user_id", {
    method: "POST", headers: { Prefer: "resolution=merge-duplicates" },
    body: JSON.stringify({ user_id: userId, refresh_token: refresh, scope, updated_at: new Date().toISOString() }),
  });
const loadToken = async (userId: string): Promise<string | null> =>
  (await db(`todoisto_health_tokens?select=refresh_token&user_id=eq.${userId}&limit=1`))?.[0]?.refresh_token ?? null;
const dropToken = (userId: string) => db(`todoisto_health_tokens?user_id=eq.${userId}`, { method: "DELETE" });

// ---- Google ----
async function tokenRequest(params: Record<string, string>) {
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ client_id: CLIENT_ID, client_secret: CLIENT_SECRET, ...params }),
  });
  const body = await res.json().catch(() => ({}));
  return { ok: res.ok, status: res.status, body };
}

const civil = (d: { year: number; month: number; day: number }) =>
  `${d.year}-${String(d.month).padStart(2, "0")}-${String(d.day).padStart(2, "0")}`;
const dayStr = (t: Date) => new Intl.DateTimeFormat("sv-SE", { timeZone: TZ }).format(t); // YYYY-MM-DD

async function listAll(access: string, type: string, filter: string, pageSize = 100): Promise<any[]> {
  const out: any[] = [];
  let token = "";
  for (let page = 0; page < 6; page++) {
    const qs = new URLSearchParams({ filter, pageSize: String(pageSize) });
    if (token) qs.set("pageToken", token);
    const res = await fetch(`${API}/${type}/dataPoints?${qs}`, { headers: { Authorization: `Bearer ${access}` } });
    if (!res.ok) throw new Error(`Google Health ${type}: ${res.status} ${(await res.text()).slice(0, 300)}`);
    const body = await res.json();
    out.push(...(body.dataPoints ?? []));
    token = body.nextPageToken ?? "";
    if (!token) break;
  }
  return out;
}

async function fetchSignals(access: string) {
  const now = new Date();
  const today = dayStr(now);
  const from = dayStr(new Date(now.getTime() - 35 * 86_400_000));
  const to = dayStr(new Date(now.getTime() + 86_400_000));

  const [sleepRaw, hrvRaw, rhrRaw] = await Promise.all([
    listAll(access, "sleep", `sleep.interval.civil_end_time >= "${from}T00:00:00" AND sleep.interval.civil_end_time < "${to}T00:00:00"`),
    listAll(access, "daily-heart-rate-variability", `daily_heart_rate_variability.date >= "${from}" AND daily_heart_rate_variability.date < "${to}"`),
    listAll(access, "daily-resting-heart-rate", `daily_resting_heart_rate.date >= "${from}" AND daily_resting_heart_rate.date < "${to}"`),
  ]);

  const sleep: SleepPoint[] = [];
  for (const p of sleepRaw) {
    const s = p.sleep, end = s?.interval?.endTime, mins = Number(s?.summary?.minutesAsleep);
    if (!end || !Number.isFinite(mins)) continue;
    sleep.push({ endDate: dayStr(new Date(end)), minutesAsleep: mins });
  }
  const hrv: DayValue[] = hrvRaw.flatMap((p) => {
    const d = p.dailyHeartRateVariability, v = Number(d?.averageHeartRateVariabilityMilliseconds);
    return d?.date && Number.isFinite(v) ? [{ date: civil(d.date), value: v }] : [];
  });
  const rhr: DayValue[] = rhrRaw.flatMap((p) => {
    const d = p.dailyRestingHeartRate, v = Number(d?.beatsPerMinute);
    return d?.date && Number.isFinite(v) ? [{ date: civil(d.date), value: v }] : [];
  });
  return buildSignals(today, sleep, rhr, hrv);
}

async function userOf(req: Request): Promise<string | null> {
  const jwt = (req.headers.get("Authorization") ?? "").replace(/^Bearer\s+/i, "");
  if (!jwt) return null;
  const res = await fetch(`${SUPABASE_URL}/auth/v1/user`, { headers: { apikey: ANON, Authorization: `Bearer ${jwt}` } });
  return res.ok ? (await res.json()).id ?? null : null;
}

Deno.serve(async (req) => {
  if (!CLIENT_ID || !CLIENT_SECRET || !SERVICE) return json(500, { error: "Brak sekretów GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET.", retryable: false });
  const url = new URL(req.url);

  try {
    if (req.method === "GET" && url.pathname.endsWith("/callback")) {
      if (url.searchParams.get("error")) return html("Odmówiono zgody w Google.", false);
      const userId = await readState(url.searchParams.get("state") ?? "");
      const code = url.searchParams.get("code");
      if (!userId || !code) return html("Nieprawidłowy lub wygasły link logowania.", false);
      const t = await tokenRequest({ code, grant_type: "authorization_code", redirect_uri: REDIRECT });
      if (!t.ok || !t.body.refresh_token) {
        // Tylko kod błędu Google (bez sekretów) — żeby dało się zdiagnozować np. invalid_client.
        console.error("token exchange failed", t.status, t.body?.error, t.body?.error_description);
        return html(`Google nie zwróciło tokenu: ${t.body?.error ?? t.status}${t.body?.error_description ? " — " + t.body.error_description : ""}`, false);
      }
      await saveToken(userId, t.body.refresh_token, t.body.scope ?? "");
      return html("Połączono z Google Health.", true);
    }

    if (req.method !== "POST") return json(405, { error: "Tylko POST.", retryable: false });
    const userId = await userOf(req);
    if (!userId) return json(401, { error: "Sesja nieważna — zaloguj się ponownie.", retryable: false });
    const action = (await req.json().catch(() => ({})))?.action;

    if (action === "start") {
      const q = new URLSearchParams({
        client_id: CLIENT_ID, redirect_uri: REDIRECT, response_type: "code", scope: SCOPES.join(" "),
        access_type: "offline", prompt: "consent", state: await makeState(userId),
      });
      return json(200, { url: `https://accounts.google.com/o/oauth2/v2/auth?${q}` });
    }

    if (action === "disconnect") {
      await dropToken(userId);
      return json(200, { connected: false });
    }

    if (action === "sync") {
      const refresh = await loadToken(userId);
      if (!refresh) return json(200, { connected: false, reason: "not_connected" });
      const t = await tokenRequest({ refresh_token: refresh, grant_type: "refresh_token" });
      if (!t.ok) {
        if (t.body?.error === "invalid_grant") { await dropToken(userId); return json(200, { connected: false, reason: "expired" }); }
        return json(503, { error: `Google OAuth: ${t.body?.error ?? t.status}`, retryable: true });
      }
      return json(200, { connected: true, signals: await fetchSignals(t.body.access_token) });
    }

    return json(400, { error: "Nieznana akcja.", retryable: false });
  } catch (e) {
    return json(503, { error: `Nieoczekiwany błąd: ${(e as Error).message}`, retryable: true });
  }
});
