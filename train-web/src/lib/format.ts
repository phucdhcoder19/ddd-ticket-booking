/** Format VND: 1250000 -> "₫1,250,000" */
export function formatVnd(amount: number): string {
  return new Intl.NumberFormat("en-GB", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

/** "1,250,000" (no symbol, used when a separate "₫" label is already shown) */
export function formatNumber(n: number): string {
  return new Intl.NumberFormat("en-GB").format(n);
}

const pad = (n: number) => String(n).padStart(2, "0");

/** Date | ISO -> "06/02/2027" */
export function formatDate(d: Date | string): string {
  const date = typeof d === "string" ? parseIso(d) : d;
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}

/** Date | ISO -> "19:30" */
export function formatTime(d: Date | string): string {
  const date = typeof d === "string" ? parseIso(d) : d;
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** Date | ISO -> "19:30 06/02/2027" */
export function formatDateTime(d: Date | string): string {
  return `${formatTime(d)} ${formatDate(d)}`;
}

/** "2027-02-06" (key used for the API & date comparison, always in local time) */
export function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function fromDateKey(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y, m - 1, d);
}

/** ISO string parsing that is safe on iOS Safari (does not rely on Date.parse with odd strings) */
export function parseIso(s: string): Date {
  const d = new Date(s);
  if (!Number.isNaN(d.getTime())) return d;
  const m = s.match(/(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/);
  if (!m) return new Date(NaN);
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]);
}

/** 1050 minutes -> "17 h 30 min" */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  if (m === 0) return `${h} h`;
  return `${h} h ${m} min`;
}

/** seconds -> "09:58" (hold timer) */
export function formatClock(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  return `${pad(Math.floor(s / 60))}:${pad(s % 60)}`;
}

/** seconds -> "2 days 05:12:40" (sale opening countdown) */
export function formatCountdownParts(totalSeconds: number) {
  const s = Math.max(0, Math.floor(totalSeconds));
  return {
    days: Math.floor(s / 86400),
    hours: Math.floor((s % 86400) / 3600),
    minutes: Math.floor((s % 3600) / 60),
    seconds: s % 60,
  };
}

const WEEKDAYS = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
export const weekdayLabel = (d: Date) => WEEKDAYS[d.getDay()];
export const weekdayShort = (d: Date) => ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d.getDay()];

/** Mask the ID number when showing it again: 001203001234 -> 001•••••1234 */
export function maskIdNumber(id: string): string {
  if (id.length < 7) return id;
  return `${id.slice(0, 3)}${"•".repeat(id.length - 7)}${id.slice(-4)}`;
}
