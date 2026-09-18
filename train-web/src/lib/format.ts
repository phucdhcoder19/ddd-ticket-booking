/** Định dạng tiền VND: 1250000 -> "1.250.000 ₫" */
export function formatVnd(amount: number): string {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    maximumFractionDigits: 0,
  }).format(amount);
}

/** "1.250.000" (không kèm ký hiệu, dùng khi đã có nhãn "₫" riêng) */
export function formatNumber(n: number): string {
  return new Intl.NumberFormat("vi-VN").format(n);
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

/** "2027-02-06" (khoá dùng cho API & so sánh ngày, luôn theo giờ địa phương) */
export function toDateKey(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function fromDateKey(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y, m - 1, d);
}

/** ISO string an toàn trên iOS Safari (không dựa vào Date.parse với chuỗi lạ) */
export function parseIso(s: string): Date {
  const d = new Date(s);
  if (!Number.isNaN(d.getTime())) return d;
  const m = s.match(/(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/);
  if (!m) return new Date(NaN);
  return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]);
}

/** 1050 phút -> "17 giờ 30 phút" */
export function formatDuration(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} phút`;
  if (m === 0) return `${h} giờ`;
  return `${h} giờ ${m} phút`;
}

/** giây -> "09:58" (đồng hồ giữ chỗ) */
export function formatClock(totalSeconds: number): string {
  const s = Math.max(0, Math.floor(totalSeconds));
  return `${pad(Math.floor(s / 60))}:${pad(s % 60)}`;
}

/** giây -> "2 ngày 05:12:40" (đếm ngược mở bán) */
export function formatCountdownParts(totalSeconds: number) {
  const s = Math.max(0, Math.floor(totalSeconds));
  return {
    days: Math.floor(s / 86400),
    hours: Math.floor((s % 86400) / 3600),
    minutes: Math.floor((s % 3600) / 60),
    seconds: s % 60,
  };
}

const WEEKDAYS = ["Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"];
export const weekdayLabel = (d: Date) => WEEKDAYS[d.getDay()];
export const weekdayShort = (d: Date) => ["CN", "T2", "T3", "T4", "T5", "T6", "T7"][d.getDay()];

/** Che bớt CCCD khi hiển thị lại: 001203001234 -> 001•••••1234 */
export function maskIdNumber(id: string): string {
  if (id.length < 7) return id;
  return `${id.slice(0, 3)}${"•".repeat(id.length - 7)}${id.slice(-4)}`;
}
