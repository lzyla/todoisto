// Logika przekazania zadania do Notion — bez zależności od Deno, żeby dało się
// ją testować w Node (`node --test`). Wejście/wyjście czysto danymi + wstrzykiwany fetch.

/** Wersja API Notion PRZYPIĘTA jawnie (CLAUDE.md). Zmiana tylko z testami. */
export const NOTION_VERSION = "2025-09-03";
const NOTION_API = "https://api.notion.com/v1";

export type TaskIn = {
  id: string;               // id zadania jako tekst (bez utraty precyzji)
  title: string;
  notes?: string;
  dueDate?: number | null;  // epoch day
  dueTimeMinutes?: number | null;
  priority?: string;        // P1..P4
};

export type Destination = {
  id: string;
  notion_database_id: string;
  status_property: string | null;
  status_value: string;
  date_property: string | null;
  id_property: string;
};

type PropSchema = { id?: string; name?: string; type: string; status?: { options?: { name: string }[] }; select?: { options?: { name: string }[] } };
export type Schema = Record<string, PropSchema>;

export class RouteError extends Error {
  readonly retryable: boolean;
  readonly status: number;
  constructor(message: string, retryable: boolean, status = 0) {
    super(message);
    this.retryable = retryable;
    this.status = status;
  }
}

/** Data z epoch day (+ opcjonalnie godzina) w formacie Notion: 'YYYY-MM-DD' albo lokalny datetime ze strefą. */
export function notionDate(epochDay: number, minutes?: number | null, timeZone = "Europe/Warsaw"): Record<string, string> {
  const d = new Date(epochDay * 86_400_000).toISOString().slice(0, 10);
  if (minutes == null) return { start: d };
  const hh = String(Math.floor(minutes / 60)).padStart(2, "0");
  const mm = String(minutes % 60).padStart(2, "0");
  return { start: `${d}T${hh}:${mm}:00`, time_zone: timeZone };
}

function findProp(schema: Schema, type: string | string[], preferred: string | null, nameHints: RegExp): string | null {
  const types = Array.isArray(type) ? type : [type];
  if (preferred) {
    const p = schema[preferred];
    if (!p) throw new RouteError(`Brak kolumny „${preferred}” w bazie Notion.`, false);
    if (!types.includes(p.type)) throw new RouteError(`Kolumna „${preferred}” ma typ ${p.type}, oczekiwano ${types.join("/")}.`, false);
    return preferred;
  }
  const candidates = Object.entries(schema).filter(([, p]) => types.includes(p.type)).map(([n]) => n);
  return candidates.find((n) => nameHints.test(n)) ?? candidates[0] ?? null;
}

/** Buduje `properties` nowej strony na podstawie schematu bazy (data source). */
export function buildProperties(schema: Schema, task: TaskIn, dest: Destination): Record<string, unknown> {
  const titleProp = Object.entries(schema).find(([, p]) => p.type === "title")?.[0];
  if (!titleProp) throw new RouteError("Baza Notion nie ma kolumny tytułu.", false);
  const props: Record<string, unknown> = {
    [titleProp]: { title: [{ type: "text", text: { content: task.title.slice(0, 2000) } }] },
  };

  const idProp = schema[dest.id_property];
  if (!idProp || idProp.type !== "rich_text") {
    throw new RouteError(`Dodaj w bazie Notion kolumnę tekstową „${dest.id_property}” (służy do unikania duplikatów).`, false);
  }
  props[dest.id_property] = { rich_text: [{ type: "text", text: { content: task.id } }] };

  const statusProp = findProp(schema, ["status", "select"], dest.status_property, /status|stan|etap/i);
  if (statusProp) {
    const p = schema[statusProp];
    const options = (p.type === "status" ? p.status?.options : p.select?.options) ?? [];
    if (options.length && !options.some((o) => o.name === dest.status_value)) {
      throw new RouteError(`Kolumna „${statusProp}” nie ma wartości „${dest.status_value}”. Dostępne: ${options.map((o) => o.name).join(", ")}.`, false);
    }
    props[statusProp] = p.type === "status" ? { status: { name: dest.status_value } } : { select: { name: dest.status_value } };
  }

  // Kolumnę daty rozwiązujemy zawsze — błędna konfiguracja ma wyjść od razu, nie dopiero przy zadaniu z terminem.
  const dateProp = findProp(schema, "date", dest.date_property, /termin|due|data|date|deadline/i);
  if (dateProp && task.dueDate != null) props[dateProp] = { date: notionDate(task.dueDate, task.dueTimeMinutes) };
  return props;
}

export function buildChildren(task: TaskIn): unknown[] {
  const notes = (task.notes ?? "").trim();
  if (!notes || /^activity:\d+$/.test(notes)) return [];
  // Notion: max 2000 znaków na fragment tekstu.
  const chunks = notes.match(/[\s\S]{1,2000}/g) ?? [];
  return chunks.slice(0, 50).map((c) => ({
    object: "block", type: "paragraph", paragraph: { rich_text: [{ type: "text", text: { content: c } }] },
  }));
}

type Fetch = (url: string, init?: RequestInit) => Promise<Response>;

export class NotionClient {
  private token: string;
  private fetchFn: Fetch;
  constructor(token: string, fetchFn: Fetch = fetch) {
    this.token = token;
    this.fetchFn = fetchFn;
  }

  private async call(method: string, path: string, body?: unknown): Promise<any> {
    const res = await this.fetchFn(`${NOTION_API}${path}`, {
      method,
      headers: {
        "Authorization": `Bearer ${this.token}`,
        "Notion-Version": NOTION_VERSION,
        "Content-Type": "application/json",
      },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    const text = await res.text();
    const json = text ? JSON.parse(text) : {};
    if (!res.ok) {
      const msg = json?.message ?? `HTTP ${res.status}`;
      // 429 i 5xx — spróbuj później; 4xx — błąd konfiguracji (nie powtarzaj w kółko).
      const retryable = res.status === 429 || res.status >= 500;
      const hint = res.status === 404 ? " (udostępnij bazę integracji: ••• → Connections)" : "";
      throw new RouteError(`Notion: ${msg}${hint}`, retryable, res.status);
    }
    return json;
  }

  async dataSourceId(databaseId: string): Promise<string> {
    const db = await this.call("GET", `/databases/${databaseId}`);
    const ds = db?.data_sources?.[0]?.id;
    if (!ds) throw new RouteError("Baza Notion nie ma źródła danych.", false);
    return ds;
  }

  async schema(dataSourceId: string): Promise<Schema> {
    const ds = await this.call("GET", `/data_sources/${dataSourceId}`);
    return ds?.properties ?? {};
  }

  /** Szuka strony już utworzonej dla tego zadania (gdyby zapis rejestru się nie udał). */
  async findByTaskId(dataSourceId: string, idProperty: string, taskId: string): Promise<{ id: string; url: string } | null> {
    const r = await this.call("POST", `/data_sources/${dataSourceId}/query`, {
      filter: { property: idProperty, rich_text: { equals: taskId } },
      page_size: 1,
    });
    const p = r?.results?.[0];
    return p?.id ? { id: p.id, url: p.url } : null;
  }

  async createPage(dataSourceId: string, properties: Record<string, unknown>, children: unknown[]): Promise<{ id: string; url: string }> {
    const p = await this.call("POST", "/pages", {
      parent: { type: "data_source_id", data_source_id: dataSourceId },
      properties,
      ...(children.length ? { children } : {}),
    });
    return { id: p.id, url: p.url };
  }
}

/** Pełne przekazanie: idempotentne względem Notion (dedup po kolumnie z id zadania). */
export async function routeToNotion(client: NotionClient, dest: Destination, task: TaskIn): Promise<{ id: string; url: string; existed: boolean }> {
  const ds = await client.dataSourceId(dest.notion_database_id);
  const schema = await client.schema(ds);
  const properties = buildProperties(schema, task, dest); // waliduje konfigurację przed jakimkolwiek zapisem
  const existing = await client.findByTaskId(ds, dest.id_property, task.id);
  if (existing) return { ...existing, existed: true };
  const created = await client.createPage(ds, properties, buildChildren(task));
  return { ...created, existed: false };
}

/** Id bazy z linku Notion (…/Nazwa-0123…cdef?v=…) albo z samego id (z myślnikami lub bez). */
export function parseDatabaseId(input: string): string | null {
  // Id to zawsze ostatnie 32 znaki hex ostatniego segmentu ścieżki (po usunięciu myślników).
  const segment = input.trim().split(/[?#]/)[0].replace(/\/+$/, "").split("/").pop() ?? "";
  const tail = segment.replace(/-/g, "").slice(-32);
  return /^[0-9a-f]{32}$/i.test(tail) ? tail.toLowerCase() : null;
}
