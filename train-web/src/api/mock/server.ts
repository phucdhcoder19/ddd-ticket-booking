/**
 * In-browser mock server — simulates the real backend's behaviour:
 * atomic stock deduction (Lua/Redis), holds with a TTL, 409 when a seat is
 * taken, 429 under overload, and network latency. All of it sits behind
 * `client.ts`, so switching to the real API only means flipping
 * VITE_USE_MOCK, without touching the UI layer.
 */
import { ApiError } from "@/api/errors";
import type {
  Carriage, Hold, HoldItem, Order, Passenger, PaymentMethod, QueueTicket,
  SaleWindow, SearchQuery, Seat, SeatClassCode, Station, Ticket, Trip,
} from "@/api/types";
import { STATIONS, buildCarriages, buildTrips } from "./db";
import { formatDate, formatTime } from "@/lib/format";

/** Chaos control panel — tweak it from the console: `__mock.overloadRate = 0.8` */
export const chaos = {
  /** Share of requests answered with 429 */
  overloadRate: 0.12,
  /** Share of requests that hang too long (simulated slow network) */
  timeoutRate: 0.03,
  /** Share of seats snatched by someone else right when holding */
  seatStealRate: 0.18,
  /** Share of failed payments */
  paymentFailRate: 0.15,
  latencyMs: [180, 650] as [number, number],
  /** Enable the waiting room: simulates a traffic spike */
  queueEnabled: true,
  holdSeconds: 10 * 60,
};
if (typeof window !== "undefined") (window as unknown as Record<string, unknown>).__mock = chaos;

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
const rnd = (a: number, b: number) => a + Math.random() * (b - a);
const uid = (p: string) => `${p}_${Math.random().toString(36).slice(2, 10)}`;

/** Requests already handled per Idempotency-Key — guards against double-clicks / duplicate retries */
const idempotencyStore = new Map<string, unknown>();

async function gate(opts: { canOverload?: boolean } = {}) {
  await sleep(rnd(chaos.latencyMs[0], chaos.latencyMs[1]));
  if (opts.canOverload !== false && Math.random() < chaos.overloadRate) {
    throw new ApiError("OVERLOADED", "The system is overloaded", { status: 429, retryAfter: 1 });
  }
  if (Math.random() < chaos.timeoutRate) {
    await sleep(20_000); // let the http layer hit its own timeout
  }
}

// ── In-memory stock state ───────────────────────────────────────────────────
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
 * The "heartbeat" of the sale: every few seconds someone else holds or buys seats.
 * So the seat map and the remaining counts really change while the user is looking.
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
      // Some held seats are released when the other person gives up
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

// ── Waiting room ────────────────────────────────────────────────────────────
let queueState: { token: string; position: number; total: number; enteredAt: number } | null = null;

// ── API ─────────────────────────────────────────────────────────────────────
export const mockApi = {
  async getStations(): Promise<Station[]> {
    await sleep(rnd(120, 300));
    return STATIONS;
  },

  async getSaleWindow(): Promise<SaleWindow> {
    await sleep(rnd(100, 250));
    // Next sale: on the hour, a little more than a day from now
    const opensAt = new Date(Date.now() + 26 * 3600_000);
    opensAt.setMinutes(0, 0, 0);
    return {
      opensAt: opensAt.toISOString(),
      label: "Sale 2 — Lunar New Year 2027 tickets",
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
    if (!trip) throw new ApiError("NOT_FOUND", "Trip not found");
    return trip;
  },

  async getCarriages(tripId: string, seatClass: SeatClassCode): Promise<Carriage[]> {
    await gate();
    return carriagesFor(tripId, seatClass).map((c) => ({ ...c, seats: c.seats.map((s) => ({ ...s })) }));
  },

  /**
   * Hold seats — the counterpart of the backend's atomic stock deduction Lua script.
   * Either ALL seats are held, or none are (no half-done holds).
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
      if (!entry) throw new ApiError("NOT_FOUND", `Seat ${id} not found`);
      if (entry.seat.status !== "available" || Math.random() < chaos.seatStealRate) taken.push(id);
    }

    if (taken.length > 0) {
      // Mark the lost seats, then suggest the nearest free seats in the same carriage
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
      throw new ApiError("SEAT_TAKEN", "Another passenger just took this seat", {
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
    if (!hold) throw new ApiError("HOLD_EXPIRED", "This hold is no longer valid", { status: 410 });
    if (new Date(hold.expiresAt).getTime() < Date.now()) {
      throw new ApiError("HOLD_EXPIRED", "The hold has expired", { status: 410 });
    }
    return { ...hold, serverNow: new Date().toISOString() };
  },

  async releaseHold(holdId: string): Promise<void> {
    await sleep(rnd(80, 200));
    const hold = holds.get(holdId);
    if (!hold) return;
    // Put the seats back to free so others can buy them right away
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
      throw new ApiError("HOLD_EXPIRED", "The hold has expired", { status: 410 });
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
      throw new ApiError("HOLD_EXPIRED", "The hold has expired", { status: 410 });
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

  /** Poll the payment status — to be replaced by the payment gateway's webhook later */
  async getOrderStatus(orderId: string): Promise<Order> {
    await sleep(rnd(400, 900));
    const order = orders.get(orderId);
    if (!order) throw new ApiError("NOT_FOUND", "Order not found");
    if (order.status !== "PENDING") return order;

    const hold = holds.get(order.holdId);
    if (!hold || new Date(hold.expiresAt).getTime() < Date.now()) {
      order.status = "EXPIRED";
      return order;
    }
    // The payment gateway needs a few seconds to return a result
    if (Math.random() < 0.45) return order;

    if (Math.random() < chaos.paymentFailRate) {
      order.status = "FAILED";
      order.failureReason = "The bank declined the transaction (insufficient balance or wrong OTP)";
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
        fromStation: trip?.fromStation.name ?? "Hanoi",
        toStation: trip?.toStation.name ?? "Saigon",
        departAt: trip?.departAt ?? new Date().toISOString(),
        carriageNumber: item.carriageNumber,
        seatLabel: item.seatLabel,
        seatClass: item.seatClass,
        passengerName: p?.fullName ?? "Passenger",
        passengerId: p?.idNumber ?? "",
        price: item.price,
        status: "VALID",
        qrPayload: [
          order.code,
          trip?.trainCode,
          `Carriage ${item.carriageNumber}`,
          `Seat ${item.seatLabel}`,
          p?.idNumber,
          trip ? `${formatTime(trip.departAt)} ${formatDate(trip.departAt)}` : "",
        ].join("|"),
      };
      tickets.unshift(ticket);
      // The seat becomes sold for good
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
    if (!t) throw new ApiError("NOT_FOUND", "Ticket not found");
    if (t.status !== "VALID") throw new ApiError("VALIDATION", "This ticket cannot be refunded");
    t.status = "REFUNDED";
    return t;
  },

  // ── Waiting room ──────────────────────────────────────────────────────────
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
      throw new ApiError("NOT_FOUND", "This place in the queue is no longer valid");
    }
    // The queue moves forward at about 35 people per second
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
