/**
 * Âm lịch Việt Nam (múi giờ +7) — thuật toán Hồ Ngọc Đức.
 * Dùng để hiển thị ngày âm trên lịch chọn ngày đi và đánh dấu cao điểm Tết.
 */
const PI = Math.PI;
const TZ = 7;
const INT = (d: number) => Math.floor(d);

function jdFromDate(dd: number, mm: number, yy: number): number {
  const a = INT((14 - mm) / 12);
  const y = yy + 4800 - a;
  const m = mm + 12 * a - 3;
  let jd = dd + INT((153 * m + 2) / 5) + 365 * y + INT(y / 4) - INT(y / 100) + INT(y / 400) - 32045;
  if (jd < 2299161) jd = dd + INT((153 * m + 2) / 5) + 365 * y + INT(y / 4) - 32083;
  return jd;
}

function newMoon(k: number): number {
  const T = k / 1236.85;
  const T2 = T * T;
  const T3 = T2 * T;
  const dr = PI / 180;
  let jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3;
  jd1 += 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
  const M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
  const Mpr = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
  const F = 21.2964 + 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
  let c1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M);
  c1 = c1 - 0.4068 * Math.sin(Mpr * dr) + 0.0161 * Math.sin(dr * 2 * Mpr);
  c1 = c1 - 0.0004 * Math.sin(dr * 3 * Mpr);
  c1 = c1 + 0.0104 * Math.sin(dr * 2 * F) - 0.0051 * Math.sin(dr * (M + Mpr));
  c1 = c1 - 0.0074 * Math.sin(dr * (M - Mpr)) + 0.0004 * Math.sin(dr * (2 * F + M));
  c1 = c1 - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mpr));
  c1 = c1 + 0.001 * Math.sin(dr * (2 * F - Mpr)) + 0.0005 * Math.sin(dr * (2 * Mpr + M));
  const deltat =
    T < -11
      ? 0.001 + 0.000839 * T + 0.0002261 * T2 - 0.00000845 * T3 - 0.000000081 * T * T3
      : -0.000278 + 0.000265 * T + 0.000262 * T2;
  return jd1 + c1 - deltat;
}

function sunLongitude(jdn: number): number {
  const T = (jdn - 2451545.0) / 36525;
  const T2 = T * T;
  const dr = PI / 180;
  const M = 357.5291 + 35999.0503 * T - 0.0001559 * T2 - 0.00000048 * T * T2;
  const L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
  let DL = (1.9146 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M);
  DL += (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.00029 * Math.sin(dr * 3 * M);
  let L = (L0 + DL) * dr;
  L = L - PI * 2 * INT(L / (PI * 2));
  return L;
}

const getSunLongitude = (dayNumber: number) => INT((sunLongitude(dayNumber - 0.5 - TZ / 24) / PI) * 6);
const getNewMoonDay = (k: number) => INT(newMoon(k) + 0.5 + TZ / 24);

function getLunarMonth11(yy: number): number {
  const off = jdFromDate(31, 12, yy) - 2415021;
  const k = INT(off / 29.530588853);
  let nm = getNewMoonDay(k);
  if (getSunLongitude(nm) >= 9) nm = getNewMoonDay(k - 1);
  return nm;
}

function getLeapMonthOffset(a11: number): number {
  const k = INT((a11 - 2415021.076998695) / 29.530588853 + 0.5);
  let last: number;
  let i = 1;
  let arc = getSunLongitude(getNewMoonDay(k + i));
  do {
    last = arc;
    i++;
    arc = getSunLongitude(getNewMoonDay(k + i));
  } while (arc !== last && i < 14);
  return i - 1;
}

export type LunarDate = { day: number; month: number; year: number; leap: boolean };

export function solarToLunar(date: Date): LunarDate {
  const dd = date.getDate();
  const mm = date.getMonth() + 1;
  const yy = date.getFullYear();
  const dayNumber = jdFromDate(dd, mm, yy);
  const k = INT((dayNumber - 2415021.076998695) / 29.530588853);
  let monthStart = getNewMoonDay(k + 1);
  if (monthStart > dayNumber) monthStart = getNewMoonDay(k);

  let a11 = getLunarMonth11(yy);
  let b11 = a11;
  let lunarYear: number;
  if (a11 >= monthStart) {
    lunarYear = yy;
    a11 = getLunarMonth11(yy - 1);
  } else {
    lunarYear = yy + 1;
    b11 = getLunarMonth11(yy + 1);
  }

  const lunarDay = dayNumber - monthStart + 1;
  const diff = INT((monthStart - a11) / 29);
  let leap = false;
  let lunarMonth = diff + 11;
  if (b11 - a11 > 365) {
    const leapOffset = getLeapMonthOffset(a11);
    if (diff >= leapOffset) {
      lunarMonth = diff + 10;
      if (diff === leapOffset) leap = true;
    }
  }
  if (lunarMonth > 12) lunarMonth -= 12;
  if (lunarMonth >= 11 && diff < 4) lunarYear -= 1;
  return { day: lunarDay, month: lunarMonth, year: lunarYear, leap };
}

/** "28/12 âm" hoặc "Mùng 3 Tết" */
export function lunarLabel(l: LunarDate): string {
  if (l.month === 1 && l.day <= 10) return `Mùng ${l.day}`;
  return `${l.day}/${l.month}${l.leap ? " N" : ""}`;
}

export type PeakLevel = "none" | "high" | "peak";

export type DayInfo = {
  date: Date;
  lunar: LunarDate;
  lunarText: string;
  peak: PeakLevel;
  /** Nhãn ngắn hiện trên ô lịch, ví dụ "29 Tết", "Mùng 4" */
  peakLabel?: string;
};

/**
 * Phân loại cao điểm Tết theo ngày âm, không hard-code ngày dương:
 *  - peak : 25 tháng Chạp → mùng 1  (sóng về quê) và mùng 3 → mùng 6 (sóng quay lại)
 *  - high : 20–24 tháng Chạp và mùng 7 → mùng 12
 */
export function getDayInfo(date: Date): DayInfo {
  const lunar = solarToLunar(date);
  const { day, month } = lunar;
  let peak: PeakLevel = "none";
  let peakLabel: string | undefined;

  if (month === 12) {
    if (day >= 25) {
      peak = "peak";
      peakLabel = `${day} Tết`;
    } else if (day >= 20) {
      peak = "high";
    }
  } else if (month === 1) {
    if (day <= 1) {
      peak = "peak";
      peakLabel = "Mùng 1";
    } else if (day >= 3 && day <= 6) {
      peak = "peak";
      peakLabel = `Mùng ${day}`;
    } else if (day === 2) {
      peak = "high";
      peakLabel = "Mùng 2";
    } else if (day <= 12) {
      peak = "high";
    }
  }
  return { date, lunar, lunarText: lunarLabel(lunar), peak, peakLabel };
}

/** Hệ số giá cao điểm — dùng chung với mock để giá hiển thị nhất quán */
export const peakSurcharge: Record<PeakLevel, number> = { none: 1, high: 1.15, peak: 1.35 };

export const peakText: Record<PeakLevel, string> = {
  none: "Ngày thường",
  high: "Đông khách",
  peak: "Cao điểm Tết",
};
