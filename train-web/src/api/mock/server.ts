/**
 * Mock server chạy trong trình duyệt — mô phỏng đúng hành vi của backend thật:
 * trừ tồn kho nguyên tử (Lua/Redis), giữ chỗ có TTL, 409 khi ghế bị tranh mất,
 * 429 khi quá tải, và độ trễ mạng. Toàn bộ nằm sau `client.ts` nên khi có API
 * thật chỉ cần đổi cờ VITE_USE_MOCK, không đụng vào tầng UI.
 */
import { ApiError } from "@/api/errors";
import type {
  Carriage, Hold, HoldItem, Order, Passenger, PaymentMethod, QueueTicket,
  SaleWindow, SearchQuery, Seat, SeatClassCode, Station, Ticket, Trip,
} from "@/api/types";
import { STATIONS, buildCarriages, buildTrips } from "./db";
import { formatDate, formatTime } from "@/lib/format";

/** Bảng điều khiển hỗn loạn — chỉnh từ console: `__mock.overloadRate = 0.8` */
export const chaos = {
  /** Tỉ lệ trả 429 cho mỗi request */
  overloadRate: 0.12,
  /** Tỉ lệ request bị treo quá lâu (giả lập mạng chậm) */
  timeoutRate: 0.03,
  /** Tỉ lệ ghế bị người khác giật mất ngay khi bấm giữ chỗ */
  seatStealRate: 0.18,
  /** Tỉ lệ thanh toán thất bại */
  paymentFailRate: 0.15,
  latencyMs: [180, 650] as [number, number],
  /** Bật phòng chờ: mô phỏng lượng truy cập đột biến */
  queueEnabled: true,
  holdSeconds: 10 * 60,
};
if (typeof window !== "undefined") (window as unknown as Record<string, unknown>).__mock = chaos;

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
const rnd = (a: number, b: number) => a + Math.random() * (b - a);
const uid = (p: string) => `${p}_${Math.random().toString(36).slice(2, 10)}`;

/** Các request đã xử lý theo Idempotency-Key — chống double-click / retry gửi trùng */
const idempotencyStore = new Map<string, unknown>();

async function gate(opts: { canOverload?: boolean } = {}) {
  await sleep(rnd(chaos.latencyMs[0], chaos.latencyMs[1]));
  if (opts.canOverload !== false && Math.random() < chaos.overloadRate) {
    throw new ApiError("OVERLOADED", "Hệ thống đang quá tải", { status: 429, retryAfter: 1 });
  }
  if (Math.random() < chaos.timeoutRate) {
    await sleep(20_000); // để tầng http tự bắt timeout
  }
}

// ── Trạng thái tồn kho trong bộ nhớ ─────────────────────────────────────────
const carriageCache = new Map<string, Carriage[]>();
const tripCache = new Map<string, Trip[]>();
const holds = new Map<string, Hold & { passengers?: Passenger[] }>();
const orders = new Map<string, Order & { holdId: string }>();
const tickets: Ticket[] = [];

function tripsFor(q: SearchQuery): Trip[] {
  const key = `${q.from}|${q.to}|${q.date}`;
  if (!tripCache.has(key)) tripCache.set(key, buildTrips(q.from, q.to, q.date));
  return tripCache.get(key)!;
}

function findTrip(tripId: string): Trip | undefined {
  for (const list of tripCache.values()) {
    const t = list.find((x) => x.id === tripId);
    if (t) return t;
  }
  return undefined;
}

function carriagesFor(tripId: string, seatClass: SeatClassCode): Carriage[] {
  const key = `${tripId}|${seatClass}`;
  if (!carriageCache.has(key)) {
    const trip = findTrip(tripId);
    const price = trip?.classes.find((c) => c.code === seatClass)?.price ?? 500_000;
    carriageCache.set(key, buildCarriages(tripId, seatClass, price));
  }
  return carriageCache.get(key)!;
}

/**
 * "Nhịp đập" của phiên mở bán: cứ vài giây lại có người khác giữ/mua chỗ.
 * Nhờ đó sơ đồ ghế và số chỗ còn lại thay đổi thật sự khi người dùng đang xem.
 */
function churn() {
  for (const list of carriageCache.values()) {
    for (const carriage of list) {
      const free = carriage.seats.filter((s) => s.status === "available");
      const take = Math.min(free.length, Math.floor(Math.random() * 3));
      for (let i = 0; i < take; i++) {
        const seat = free[Math.floor(Math.random() * free.length)];
        seat.status = Math.random() < 0.6 ? "held" : "sold";
      }
      // Một phần chỗ bị giữ sẽ được nhả ra khi người kia bỏ dở
      for (const s of carriage.seats) {
        if (s.status === "held" && Math.random() < 0.06) s.status = "available";
      }
      carriage.available = carriage.seats.filter((s) => s.status === "available").length;
    }
  }
  for (const list of tripCache.values()) {
    for (const trip of list) {
      for (const c of trip.classes) {
        if (c.available > 0 && Math.random() < 0.5) {
          c.available = Math.max(0, c.available - Math.floor(Math.random() * 3));
        }
      }
      trip.availableTotal = trip.classes.reduce((s, c) => s + c.available, 0);
    }
  }
}
if (typeof window !== "undefined") setInterval(churn, 5_000);

// ── Phòng chờ ───────────────────────────────────────────────────────────────
let queueState: { token: string; position: number; total: number; enteredAt: number } | null = null;

// ── API ─────────────────────────────────────────────────────────────────────
export const mockApi = {
  async getStations(): Promise<Station[]> {
    await sleep(rnd(120, 300));
    return STATIONS;
  },

  async getSaleWindow(): Promise<SaleWindow> {
    await sleep(rnd(100, 250));
    // Đợt mở bán tiếp theo: đúng giờ tròn, cách hiện tại hơn một ngày
    const opensAt = new Date(Date.now() + 26 * 3600_000);
    opensAt.setMinutes(0, 0, 0);
    return {
      opensAt: opensAt.toISOString(),
      label: "Đợt 2 — vé Tết Đinh Mùi 2027",
      isOpen: false,
      serverNow: new Date().toISOString(),
    };
  },

  async searchTrips(q: SearchQuery): Promise<Trip[]> {
    await gate();
    return tripsFor(q).map((t) => ({ ...t, classes: t.classes.map((c) => ({ ...c })) }));
  },

  async getTrip(tripId: string): Promise<Trip> {
    await gate({ canOverload: false });
    const trip = findTrip(tripId);
    if (!trip) throw new ApiError("NOT_FOUND", "Không tìm thấy chuyến tàu");
    return trip;
  },

  async getCarriages(tripId: string, seatClass: SeatClassCode): Promise<Carriage[]> {
    await gate();
    return carriagesFor(tripId, seatClass).map((c) => ({ ...c, seats: c.seats.map((s) => ({ ...s })) }));
  },

  /**
   * Giữ chỗ — tương ứng Lua script trừ tồn kho nguyên tử ở backend.
   * Hoặc giữ được TOÀN BỘ ghế, hoặc không giữ ghế nào (tránh giữ dở dang).
   */
  async holdSeats(
    tripId: string,
    seatClass: SeatClassCode,
    seatIds: string[],
    idempotencyKey?: string,
  ): Promise<Hold> {
    if (idempotencyKey && idempotencyStore.has(idempotencyKey)) {
      return idempotencyStore.get(idempotencyKey) as Hold;
    }
    await gate();

    const carriages = carriagesFor(tripId, seatClass);
    const all = new Map<string, { seat: Seat; carriage: Carriage }>();
    for (const c of carriages) for (const s of c.seats) all.set(s.id, { seat: s, carriage: c });

    const taken: string[] = [];
    for (const id of seatIds) {
      const entry = all.get(id);
      if (!entry) throw new ApiError("NOT_FOUND", `Không tìm thấy chỗ ${id}`);
      if (entry.seat.status !== "available" || Math.random() < chaos.seatStealRate) taken.push(id);
    }

    if (taken.length > 0) {
      // Đánh dấu ghế đã mất rồi gợi ý chỗ trống gần nhất trong cùng toa
      const suggestions: string[] = [];
      for (const id of taken) {
        const { seat, carriage } = all.get(id)!;
        seat.status = "held";
        const near = carriage.seats
          .filter((s) => s.status === "available")
          .sort((a, b) => Math.abs(a.row - seat.row) - Math.abs(b.row - seat.row))
          .slice(0, 3)
          .map((s) => s.id);
        suggestions.push(...near);
      }
      throw new ApiError("SEAT_TAKEN", "Chỗ vừa được hành khách khác giữ", {
        status: 409,
        details: { takenSeatIds: taken, suggestedSeatIds: [...new Set(suggestions)].slice(0, 4) },
      });
    }

    const items: HoldItem[] = seatIds.map((id) => {
      const { seat, carriage } = all.get(id)!;
      seat.status = "held";
      carriage.available--;
      return {
        seatId: id,
        seatLabel: seat.label,
        carriageNumber: carriage.number,
        seatClass,
        price: seat.price,
      };
    });

    const hold: Hold = {
      holdId: uid("hold"),
      tripId,
      items,
      expiresAt: new Date(Date.now() + chaos.holdSeconds * 1000).toISOString(),
      serverNow: new Date().toISOString(),
    };
    holds.set(hold.holdId, hold);
    if (idempotencyKey) idempotencyStore.set(idempotencyKey, hold);
    return hold;
  },

  async getHold(holdId: string): Promise<Hold> {
    await sleep(rnd(80, 200));
    const hold = holds.get(holdId);
    if (!hold) throw new ApiError("HOLD_EXPIRED", "Phiên giữ chỗ không còn hiệu lực", { status: 410 });
    if (new Date(hold.expiresAt).getTime() < Date.now()) {
      throw new ApiError("HOLD_EXPIRED", "Đã hết thời gian giữ chỗ", { status: 410 });
    }
    return { ...hold, serverNow: new Date().toISOString() };
  },

  async releaseHold(holdId: string): Promise<void> {
    await sleep(rnd(80, 200));
    const hold = holds.get(holdId);
    if (!hold) return;
    // Trả chỗ về trạng thái trống để người khác mua được ngay
    for (const item of hold.items) {
      for (const c of carriagesFor(hold.tripId, item.seatClass)) {
        const seat = c.seats.find((s) => s.id === item.seatId);
        if (seat && seat.status === "held") {
          seat.status = "available";
          c.available++;
        }
      }
    }
    holds.delete(holdId);
  },

  async savePassengers(holdId: string, passengers: Passenger[]): Promise<void> {
    await gate({ canOverload: false });
    const hold = holds.get(holdId);
    if (!hold || new Date(hold.expiresAt).getTime() < Date.now()) {
      throw new ApiError("HOLD_EXPIRED", "Đã hết thời gian giữ chỗ", { status: 410 });
    }
    hold.passengers = passengers;
  },

  async createOrder(holdId: string, method: PaymentMethod, idempotencyKey?: string): Promise<Order> {
    if (idempotencyKey && idempotencyStore.has(idempotencyKey)) {
      return idempotencyStore.get(idempotencyKey) as Order;
    }
    await gate();
    const hold = holds.get(holdId);
    if (!hold || new Date(hold.expiresAt).getTime() < Date.now()) {
      throw new ApiError("HOLD_EXPIRED", "Đã hết thời gian giữ chỗ", { status: 410 });
    }
    void method;
    const total = hold.items.reduce((s, i) => s + i.price, 0);
    const order: Order & { holdId: string } = {
      orderId: uid("ord"),
      code: `VT${Date.now().toString().slice(-10)}`,
      status: "PENDING",
      totalAmount: total,
      holdId,
    };
    orders.set(order.orderId, order);
    if (idempotencyKey) idempotencyStore.set(idempotencyKey, order);
    return order;
  },

  /** Poll trạng thái thanh toán — về sau thay bằng webhook của cổng thanh toán */
  async getOrderStatus(orderId: string): Promise<Order> {
    await sleep(rnd(400, 900));
    const order = orders.get(orderId);
    if (!order) throw new ApiError("NOT_FOUND", "Không tìm thấy đơn hàng");
    if (order.status !== "PENDING") return order;

    const hold = holds.get(order.holdId);
    if (!hold || new Date(hold.expiresAt).getTime() < Date.now()) {
      order.status = "EXPIRED";
      return order;
    }
    // Cổng thanh toán cần vài giây mới có kết quả
    if (Math.random() < 0.45) return order;

    if (Math.random() < chaos.paymentFailRate) {
      order.status = "FAILED";
      order.failureReason = "Ngân hàng từ chối giao dịch (số dư không đủ hoặc sai OTP)";
      return order;
    }

    order.status = "PAID";
    order.paidAt = new Date().toISOString();
    const trip = findTrip(hold.tripId);
    hold.items.forEach((item, i) => {
      const p = hold.passengers?.[i];
      const ticket: Ticket = {
        id: uid("tk"),
        code: `${order.code}-${i + 1}`,
        orderCode: order.code,
        trainCode: trip?.trainCode ?? "SE1",
        fromStation: trip?.fromStation.name ?? "Hà Nội",
        toStation: trip?.toStation.name ?? "Sài Gòn",
        departAt: trip?.departAt ?? new Date().toISOString(),
        carriageNumber: item.carriageNumber,
        seatLabel: item.seatLabel,
        seatClass: item.seatClass,
        passengerName: p?.fullName ?? "Hành khách",
        passengerId: p?.idNumber ?? "",
        price: item.price,
        status: "VALID",
        qrPayload: [
          order.code,
          trip?.trainCode,
          `Toa ${item.carriageNumber}`,
          `Cho ${item.seatLabel}`,
          p?.idNumber,
          trip ? `${formatTime(trip.departAt)} ${formatDate(trip.departAt)}` : "",
        ].join("|"),
      };
      tickets.unshift(ticket);
      // Ghế chuyển hẳn sang đã bán
      for (const c of carriagesFor(hold.tripId, item.seatClass)) {
        const seat = c.seats.find((s) => s.id === item.seatId);
        if (seat) seat.status = "sold";
      }
    });
    holds.delete(order.holdId);
    return order;
  },

  async getMyTickets(): Promise<Ticket[]> {
    await gate({ canOverload: false });
    return tickets;
  },

  async refundTicket(ticketId: string): Promise<Ticket> {
    await gate({ canOverload: false });
    const t = tickets.find((x) => x.id === ticketId);
    if (!t) throw new ApiError("NOT_FOUND", "Không tìm thấy vé");
    if (t.status !== "VALID") throw new ApiError("VALIDATION", "Vé này không thể trả");
    t.status = "REFUNDED";
    return t;
  },

  // ── Phòng chờ ─────────────────────────────────────────────────────────────
  async joinQueue(): Promise<QueueTicket> {
    await sleep(rnd(200, 500));
    if (!chaos.queueEnabled) {
      return { token: uid("q"), position: 0, total: 0, estimatedWaitSeconds: 0, status: "ADMITTED" };
    }
    if (!queueState) {
      const total = 8_000 + Math.floor(Math.random() * 20_000);
      queueState = {
        token: uid("q"),
        position: 400 + Math.floor(Math.random() * 1_800),
        total,
        enteredAt: Date.now(),
      };
    }
    return mockApi.getQueueStatus(queueState.token);
  },

  async getQueueStatus(token: string): Promise<QueueTicket> {
    await sleep(rnd(150, 400));
    if (!queueState || queueState.token !== token) {
      throw new ApiError("NOT_FOUND", "Lượt chờ không còn hiệu lực");
    }
    // Hàng đợi nhích dần: khoảng 35 người mỗi giây
    const elapsed = (Date.now() - queueState.enteredAt) / 1000;
    const position = Math.max(0, Math.round(queueState.position - elapsed * 35));
    const admitted = position === 0;
    return {
      token,
      position,
      total: queueState.total,
      estimatedWaitSeconds: Math.round(position / 35),
      status: admitted ? "ADMITTED" : "WAITING",
      admissionExpiresAt: admitted ? new Date(Date.now() + 8 * 60_000).toISOString() : undefined,
    };
  },

  resetQueue() {
    queueState = null;
  },
};
