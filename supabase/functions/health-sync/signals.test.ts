import { test } from "node:test";
import assert from "node:assert/strict";
import { buildSignals, median, nightlySleep } from "./signals.ts";

const days = (n: number, f: (i: number) => number, end = "2026-11-03") =>
  Array.from({ length: n }, (_, i) => {
    const d = new Date(end + "T00:00:00Z"); d.setUTCDate(d.getUTCDate() - (n - 1 - i));
    return { date: d.toISOString().slice(0, 10), value: f(i) };
  });

test("median", () => { assert.equal(median([3, 1, 2]), 2); assert.equal(median([1, 2, 3, 4]), 2.5); assert.equal(median([]), null); });

test("nightlySleep: drzemka nie zastępuje nocy", () => {
  const r = nightlySleep([{ endDate: "2026-11-03", minutesAsleep: 420 }, { endDate: "2026-11-03", minutesAsleep: 40 }]);
  assert.deepEqual(r, [{ date: "2026-11-03", value: 420 }]);
});

test("baseline z poprzednich dni, bez dzisiejszej", () => {
  const sleep = days(10, (i) => (i === 9 ? 300 : 450)).map((d) => ({ endDate: d.date, minutesAsleep: d.value }));
  const s = buildSignals("2026-11-03", sleep, [], []);
  assert.equal(s.sleepMinutes, 300);
  assert.equal(s.sleepBaselineMinutes, 450);
});

test("za mało historii → brak baseline", () => {
  const s = buildSignals("2026-11-03", [], days(3, () => 55), []);
  assert.equal(s.restingHr, 55);
  assert.equal(s.restingHrBaseline, null);
});

test("nieświeże dane (sprzed 2+ dni) nie są 'dzisiejsze'", () => {
  const s = buildSignals("2026-11-03", [], [], days(8, () => 40, "2026-10-30"));
  assert.equal(s.hrv, null);
});
