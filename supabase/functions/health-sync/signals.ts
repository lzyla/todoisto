// Czysta logika (bez sieci): z surowych punktów Google Health robi sygnały dla EnergyModel.

export interface Signals {
  date: string;                       // dzień, którego dotyczy poranek (YYYY-MM-DD, strefa użytkownika)
  sleepMinutes: number | null;        // sen ostatniej nocy (minutesAsleep)
  sleepBaselineMinutes: number | null;
  restingHr: number | null;
  restingHrBaseline: number | null;
  hrv: number | null;                 // RMSSD, ms
  hrvBaseline: number | null;
}

export interface SleepPoint { endDate: string; minutesAsleep: number }
export interface DayValue { date: string; value: number }

export const MIN_BASELINE_DAYS = 5;

export function median(xs: number[]): number | null {
  if (!xs.length) return null;
  const s = [...xs].sort((a, b) => a - b), m = s.length >> 1;
  return s.length % 2 ? s[m] : (s[m - 1] + s[m]) / 2;
}

// Jedna noc = sesja z największym snem w danym dniu zakończenia (drzemki odpadają).
export function nightlySleep(points: SleepPoint[]): DayValue[] {
  const best = new Map<string, number>();
  for (const p of points) {
    if (!Number.isFinite(p.minutesAsleep)) continue;
    best.set(p.endDate, Math.max(best.get(p.endDate) ?? 0, p.minutesAsleep));
  }
  return [...best].map(([date, value]) => ({ date, value })).sort((a, b) => a.date.localeCompare(b.date));
}

// Ostatnia wartość nie późniejsza niż `today` + mediana wcześniejszych (do 28), min. MIN_BASELINE_DAYS.
export function latestAndBaseline(series: DayValue[], today: string): { latest: number | null; baseline: number | null } {
  const s = series.filter((d) => d.date <= today).sort((a, b) => a.date.localeCompare(b.date));
  const last = s[s.length - 1];
  if (!last || last.date < shiftDate(today, -1)) return { latest: null, baseline: median(s.slice(-29, -1).map((d) => d.value)) };
  const prev = s.slice(0, -1).slice(-28).map((d) => d.value);
  return { latest: last.value, baseline: prev.length >= MIN_BASELINE_DAYS ? median(prev) : null };
}

export function shiftDate(iso: string, days: number): string {
  const d = new Date(iso + "T00:00:00Z");
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

export function buildSignals(today: string, sleep: SleepPoint[], rhr: DayValue[], hrv: DayValue[]): Signals {
  const s = latestAndBaseline(nightlySleep(sleep), today);
  const r = latestAndBaseline(rhr, today);
  const h = latestAndBaseline(hrv, today);
  return {
    date: today,
    sleepMinutes: s.latest, sleepBaselineMinutes: s.baseline,
    restingHr: r.latest, restingHrBaseline: r.baseline,
    hrv: h.latest, hrvBaseline: h.baseline,
  };
}
