import type { Carriage, Seat, SeatClassCode, Station, Trip } from "@/api/types";
import { getDayInfo, peakSurcharge } from "@/lib/lunar";
import { fromDateKey } from "@/lib/format";

export const STATIONS: Station[] = [
  { code: "HNO", name: "Hà Nội", region: "Bắc" },
  { code: "NDI", name: "Nam Định", region: "Bắc" },
  { code: "THA", name: "Thanh Hoá", region: "Bắc" },
  { code: "VIN", name: "Vinh", region: "Trung" },
  { code: "DHO", name: "Đồng Hới", region: "Trung" },
  { code: "HUE", name: "Huế", region: "Trung" },
  { code: "DNA", name: "Đà Nẵng", region: "Trung" },
  { code: "QNG", name: "Quảng Ngãi", region: "Trung" },
  { code: "DTH", name: "Diêu Trì (Quy Nhơn)", region: "Trung" },
  { code: "TUY", name: "Tuy Hoà", region: "Trung" },
  { code: "NTR", name: "Nha Trang", region: "Trung" },
  { code: "THC", name: "Tháp Chàm (Phan Rang)", region: "Nam" },
  { code: "BTH", name: "Bình Thuận (Phan Thiết)", region: "Nam" },
  { code: "BHO", name: "Biên Hoà", region: "Nam" },
  { code: "SGO", name: "Sài Gòn", region: "Nam" },
];

/** Thứ tự ga trên trục Bắc – Nam, dùng để tính quãng đường và thời gian chạy */
const ORDER = STATIONS.map((s) => s.code);
const KM: Record<string, number> = {
  HNO: 0, NDI: 87, THA: 175, VIN: 319, DHO: 522, HUE: 688, DNA: 791,
  QNG: 928, DTH: 1096, TUY: 1198, NTR: 1315, THC: 1408, BTH: 1551, BHO: 1675, SGO: 1726,
};

const TRAINS = [
  { code: "SE1", departHour: 20, departMinute: 25, speed: 60, classes: ["SOFT_SEAT", "BERTH_6", "BERTH_4"] },
  { code: "SE3", departHour: 19, departMinute: 20, speed: 62, classes: ["SOFT_SEAT", "BERTH_6", "BERTH_4"] },
  { code: "SE5", departHour: 9, departMinute: 0, speed: 54, classes: ["SOFT_SEAT", "BERTH_6", "BERTH_4"] },
  { code: "SE7", departHour: 6, departMinute: 0, speed: 52, classes: ["SOFT_SEAT", "BERTH_6"] },
  { code: "SE9", departHour: 14, departMinute: 30, speed: 50, classes: ["SOFT_SEAT", "BERTH_6", "BERTH_4"] },
  { code: "TN3", departHour: 22, departMinute: 15, speed: 45, classes: ["SOFT_SEAT", "BERTH_6"] },
  { code: "SE22", departHour: 11, departMinute: 45, speed: 58, classes: ["SOFT_SEAT", "BERTH_4"] },
] as const;

const BASE_PRICE_PER_KM: Record<SeatClassCode, number> = {
  SOFT_SEAT: 620,
  BERTH_6: 900,
  BERTH_4: 1180,
};

/** Hash ổn định để cùng một truy vấn luôn ra cùng dữ liệu (tránh nhảy số khi quay lại màn hình) */
function hash(str: string): number {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return Math.abs(h);
}
const rand = (seed: string, max: number) => hash(seed) % max;

const roundPrice = (p: number) => Math.round(p / 1000) * 1000;

export function findStation(code: string): Station | undefined {
  return STATIONS.find((s) => s.code === code);
}

export function buildTrips(from: string, to: string, dateKey: string): Trip[] {
  const a = ORDER.indexOf(from);
  const b = ORDER.indexOf(to);
  if (a < 0 || b < 0 || a === b) return [];
  const distance = Math.abs(KM[to] - KM[from]);
  const date = fromDateKey(dateKey);
  const info = getDayInfo(date);
  const surcharge = peakSurcharge[info.peak];

  return TRAINS.map((t) => {
    const seed = `${t.code}|${from}|${to}|${dateKey}`;
    const durationMinutes = Math.round((distance / t.speed) * 60 + 25 + rand(seed + "d", 40));
    const departAt = new Date(date);
    departAt.setHours(t.departHour, t.departMinute, 0, 0);
    const arriveAt = new Date(departAt.getTime() + durationMinutes * 60_000);

    const classes = t.classes.map((code) => {
      const c = code as SeatClassCode;
      const total = c === "SOFT_SEAT" ? 128 : c === "BERTH_6" ? 84 : 56;
      // Cao điểm thì chỗ còn rất ít — đúng cảm giác mở bán vé Tết
      const scarcity = info.peak === "peak" ? 0.06 : info.peak === "high" ? 0.22 : 0.65;
      const noise = rand(seed + c, 100) / 100;
      let available = Math.round(total * scarcity * noise * 1.8);
      if (rand(seed + c + "z", 10) === 0) available = 0; // thỉnh thoảng có hạng hết sạch
      return {
        code: c,
        price: roundPrice(distance * BASE_PRICE_PER_KM[c] * surcharge + 20_000),
        available: Math.min(available, total),
        total,
      };
    });

    return {
      id: `${t.code}-${from}-${to}-${dateKey}`,
      trainCode: t.code,
      fromStation: findStation(from)!,
      toStation: findStation(to)!,
      departAt: departAt.toISOString(),
      arriveAt: arriveAt.toISOString(),
      durationMinutes,
      classes,
      availableTotal: classes.reduce((s, c) => s + c.available, 0),
    };
  }).sort((x, y) => x.departAt.localeCompare(y.departAt));
}

const LAYOUT: Record<SeatClassCode, { layout: Carriage["layout"]; rows: number; perRow: number }> = {
  SOFT_SEAT: { layout: "seat-2-2", rows: 16, perRow: 4 },
  BERTH_6: { layout: "berth-6", rows: 7, perRow: 6 },
  BERTH_4: { layout: "berth-4", rows: 7, perRow: 4 },
};

export function buildCarriages(tripId: string, seatClass: SeatClassCode, basePrice: number): Carriage[] {
  const cfg = LAYOUT[seatClass];
  const carriageNumbers = seatClass === "SOFT_SEAT" ? [3, 4, 5] : seatClass === "BERTH_6" ? [6, 7] : [8, 9];

  return carriageNumbers.map((number) => {
    const seats: Seat[] = [];
    for (let r = 1; r <= cfg.rows; r++) {
      for (let c = 1; c <= cfg.perRow; c++) {
        const index = (r - 1) * cfg.perRow + c;
        const id = `T${number}-${index}`;
        const seed = `${tripId}|${id}`;
        const roll = rand(seed, 100);
        const status: Seat["status"] = roll < 46 ? "sold" : roll < 58 ? "held" : "available";
        // Giường tầng 1 đắt hơn tầng 2, tầng 2 đắt hơn tầng 3
        const berthLevel = seatClass === "SOFT_SEAT" ? undefined : ((c - 1) % (cfg.perRow / 2)) + 1;
        const levelFactor = berthLevel ? [1.12, 1, 0.9][berthLevel - 1] : 1;
        seats.push({
          id,
          label: String(index),
          row: r,
          col: c,
          compartment: seatClass === "SOFT_SEAT" ? undefined : r,
          berthLevel,
          status,
          price: roundPrice(basePrice * levelFactor),
        });
      }
    }
    return {
      id: `${tripId}-T${number}`,
      number,
      seatClass,
      layout: cfg.layout,
      rows: cfg.rows,
      seats,
      available: seats.filter((s) => s.status === "available").length,
    };
  });
}
