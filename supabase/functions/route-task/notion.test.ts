// Testy logiki przekazania (bez sieci). Uruchom: node --test supabase/functions/route-task/notion.test.ts
import { test } from "node:test";
import assert from "node:assert/strict";
import { resolveStatus, buildChildren, buildProperties, NotionClient, notionDate, parseDatabaseId, routeToNotion, RouteError, NOTION_VERSION, type Destination, type Schema } from "./notion.ts";

const dest: Destination = {
  id: "d1", notion_database_id: "0123456789abcdef0123456789abcdef",
  status_property: null, status_value: "To do", date_property: null, id_property: "Todoisto ID",
};
const schema: Schema = {
  "Nazwa": { type: "title" },
  "Status": { type: "status", status: { options: [{ name: "To do" }, { name: "Done" }] } },
  "Termin": { type: "date" },
  "Todoisto ID": { type: "rich_text" },
};

test("wersja API Notion przypięta", () => assert.equal(NOTION_VERSION, "2025-09-03"));

test("mapowanie: tytuł, status kanbanu, termin z godziną, id do deduplikacji", () => {
  const p: any = buildProperties(schema, { id: "2199023255553", title: "Napisać rozdział", dueDate: 20760, dueTimeMinutes: 9 * 60 + 5 }, dest);
  assert.equal(p["Nazwa"].title[0].text.content, "Napisać rozdział");
  assert.deepEqual(p["Status"], { status: { name: "To do" } });
  assert.deepEqual(p["Termin"].date, { start: "2026-11-03T09:05:00", time_zone: "Europe/Warsaw" });
  assert.equal(p["Todoisto ID"].rich_text[0].text.content, "2199023255553");
});

test("termin bez godziny = sama data", () => assert.deepEqual(notionDate(20760), { start: "2026-11-03" }));

test("select zamiast status też działa", () => {
  const s: Schema = { ...schema, "Status": { type: "select", select: { options: [{ name: "To do" }] } } };
  assert.deepEqual((buildProperties(s, { id: "1", title: "x" }, dest) as any)["Status"], { select: { name: "To do" } });
});

test("błędy konfiguracji są nie-ponawialne i mówią co poprawić", () => {
  const noId: Schema = { "Nazwa": { type: "title" } };
  assert.throws(() => buildProperties(noId, { id: "1", title: "x" }, dest), (e: any) => e instanceof RouteError && !e.retryable && /Todoisto ID/.test(e.message));
  assert.throws(() => buildProperties(schema, { id: "1", title: "x" }, { ...dest, status_value: "Backlog" }), /Dostępne: To do, Done/);
  assert.throws(() => buildProperties(schema, { id: "1", title: "x" }, { ...dest, date_property: "Brak" }), /Brak kolumny/);
});

test("notatki → akapity; ślad aktywności pomijany", () => {
  assert.equal(buildChildren({ id: "1", title: "x", notes: "activity:12" }).length, 0);
  assert.equal(buildChildren({ id: "1", title: "x", notes: "a".repeat(4500) }).length, 3);
});

test("id bazy z różnych postaci linku", () => {
  const id = "0123456789abcdef0123456789abcdef";
  assert.equal(parseDatabaseId(`https://www.notion.so/ws/Kanban-Vocative-${id}?v=ffffffffffffffffffffffffffffffff`), id);
  assert.equal(parseDatabaseId(`https://www.notion.so/${id}`), id);
  assert.equal(parseDatabaseId("01234567-89ab-cdef-0123-456789abcdef"), id);
  assert.equal(parseDatabaseId(`https://www.notion.so/ws/Bad-${id}`), id); // „Bad” kończy się hex — nie może przesunąć wyniku
  assert.equal(parseDatabaseId("https://www.notion.so/nic"), null);
});

function fakeNotion(opts: { existing?: boolean; status?: number } = {}) {
  const calls: { method: string; url: string; body?: any; headers: any }[] = [];
  const f = async (url: string, init: any = {}) => {
    calls.push({ method: init.method, url, body: init.body ? JSON.parse(init.body) : undefined, headers: init.headers });
    if (opts.status) return new Response(JSON.stringify({ message: "boom" }), { status: opts.status });
    if (url.endsWith("/databases/" + dest.notion_database_id)) return Response.json({ data_sources: [{ id: "ds1" }] });
    if (url.endsWith("/data_sources/ds1")) return Response.json({ properties: schema });
    if (url.endsWith("/data_sources/ds1/query")) return Response.json({ results: opts.existing ? [{ id: "p0", url: "https://notion.so/p0" }] : [] });
    if (url.endsWith("/pages")) return Response.json({ id: "p1", url: "https://notion.so/p1" });
    return new Response("{}", { status: 404 });
  };
  return { calls, client: new NotionClient("secret", f as any) };
}

test("pełny przebieg: tworzy stronę w źródle danych z nagłówkiem wersji", async () => {
  const { calls, client } = fakeNotion();
  const r = await routeToNotion(client, dest, { id: "7", title: "x" });
  assert.deepEqual(r, { id: "p1", url: "https://notion.so/p1", existed: false });
  const create = calls.find((c) => c.url.endsWith("/pages"))!;
  assert.deepEqual(create.body.parent, { type: "data_source_id", data_source_id: "ds1" });
  assert.ok(calls.every((c) => c.headers["Notion-Version"] === NOTION_VERSION));
});

test("idempotencja: strona z tym id już jest → bez duplikatu", async () => {
  const { calls, client } = fakeNotion({ existing: true });
  const r = await routeToNotion(client, dest, { id: "7", title: "x" });
  assert.equal(r.existed, true);
  assert.equal(calls.filter((c) => c.url.endsWith("/pages")).length, 0);
});

test("429/5xx ponawialne, 404 nie (z podpowiedzią o udostępnieniu bazy)", async () => {
  for (const [status, retry] of [[429, true], [503, true], [404, false], [400, false]] as const) {
    const { client } = fakeNotion({ status });
    await assert.rejects(routeToNotion(client, dest, { id: "7", title: "x" }), (e: any) => e.retryable === retry);
  }
  const { client } = fakeNotion({ status: 404 });
  await assert.rejects(routeToNotion(client, dest, { id: "7", title: "x" }), /Connections/);
});

// Realny schemat kanbanu „Vocative do ogarnięcia moje” (stan z 3.11.2026).
const vocative: Schema = {
  "Feature": { type: "title" },
  "Priority": { type: "select", select: { options: [{ name: "High" }, { name: "Medium" }, { name: "Low" }] } },
  "Sprint": { type: "select", select: { options: [{ name: "Sprint 1" }, { name: "Sprint 2" }] } },
  "Status": { type: "status", status: {
    options: [{ id: "a", name: "To Do" }, { id: "b", name: "In Progress" }, { id: "c", name: "In Review" }, { id: "d", name: "Done" }],
    groups: [{ name: "To-do", option_ids: ["a"] }, { name: "In progress", option_ids: ["b", "c"] }, { name: "Complete", option_ids: ["d"] }],
  } },
  "Summary": { type: "rich_text" },
  "Target Date": { type: "date" },
  "Text": { type: "rich_text" },
  "Todoisto ID": { type: "rich_text" },
};

test("Vocative: domyślne „To do” trafia do kolumny „To Do”, termin do Target Date, P1 → High", () => {
  const p: any = buildProperties(vocative, { id: "9", title: "Demo dla klienta", dueDate: 20760, priority: "P1" }, dest);
  assert.equal(p["Feature"].title[0].text.content, "Demo dla klienta");
  assert.deepEqual(p["Status"], { status: { name: "To Do" } });
  assert.deepEqual(p["Target Date"], { date: { start: "2026-11-03" } });
  assert.deepEqual(p["Priority"], { select: { name: "High" } });
  assert.equal(p["Sprint"], undefined, "sprint zostaje pusty");
});

test("priorytety: P2 → Medium, P3 → Low, P4 → pusto", () => {
  const pr = (priority: string) => (buildProperties(vocative, { id: "9", title: "x", priority }, dest) as any)["Priority"];
  assert.deepEqual(pr("P2"), { select: { name: "Medium" } });
  assert.deepEqual(pr("P3"), { select: { name: "Low" } });
  assert.equal(pr("P4"), undefined);
});

test("status: dopasowanie bez wielkości liter i z grupy To-do", () => {
  assert.equal(resolveStatus(vocative["Status"], "to do"), "To Do");
  assert.equal(resolveStatus(vocative["Status"], "In review"), "In Review");
  assert.equal(resolveStatus({ ...vocative["Status"], status: { ...vocative["Status"].status!, options: [{ id: "a", name: "Backlog" }] } }, "To do"), "Backlog");
  assert.equal(resolveStatus(vocative["Status"], "Zrobione"), null);
});
