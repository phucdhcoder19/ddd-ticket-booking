export type Station = { code: string; name: string; region: "North" | "Central" | "South" };

export type SeatClassCode = "SOFT_SEAT" | "BERTH_4" | "BERTH_6";

export const SEAT_CLASS_LABEL: Record<SeatClassCode, string> = {
  SOFT_SEAT: "Air-conditioned soft seat",
  BERTH_4: "4-berth sleeper",
  BERTH_6: "6-berth sleeper",
};

export const SEAT_CLASS_SHORT: Record<SeatClassCode, string> = {
  SOFT_SEAT: "Soft seat",
  BERTH_4: "4-berth",
  BERTH_6: "6-berth",
};

export type SeatClassOffer = {
  code: SeatClassCode;
  price: number;
  available: number;
  total: number;
};

export type Trip = {
  id: string;
  trainCode: string;          // SE1, SE7, TN3...
  fromStation: Station;
  toStation: Station;
  departAt: string;           // ISO
  arriveAt: string;           // ISO
  durationMinutes: number;
  classes: SeatClassOffer[];
  /** Free places on the whole trip — used for the "Almost gone"/"Sold out" badges */
  availableTotal: number;
};

export type SeatStatus = "available" | "held" | "sold" | "selected";

export type Seat = {
  id: string;                 // "C3-12"
  label: string;              // "12"
  row: number;
  col: number;
  /** Sleeper carriage: compartment number (1..n); seating carriage: undefined */
  compartment?: number;
  /** Berth level: 1 = lower (more expensive), 2, 3 */
  berthLevel?: number;
  status: Exclude<SeatStatus, "selected">;
  price: number;
};

export type Carriage = {
  id: string;
  number: number;             // Carriage 3
  seatClass: SeatClassCode;
  layout: "seat-2-2" | "berth-4" | "berth-6";
  rows: number;
  seats: Seat[];
  available: number;
};

export type Discount = "NONE" | "STUDENT" | "CHILD" | "SENIOR";

export const DISCOUNT_LABEL: Record<Discount, string> = {
  NONE: "No discount",
  STUDENT: "Student (−10%)",
  CHILD: "Child under 10 (−25%)",
  SENIOR: "Senior, 60 and over (−15%)",
};

export const DISCOUNT_RATE: Record<Discount, number> = {
  NONE: 0,
  STUDENT: 0.1,
  CHILD: 0.25,
  SENIOR: 0.15,
};

export type Passenger = {
  seatId: string;
  fullName: string;
  idNumber: string;
  phone: string;
  discount: Discount;
};

export type HoldItem = {
  seatId: string;
  seatLabel: string;
  carriageNumber: number;
  seatClass: SeatClassCode;
  price: number;
};

export type Hold = {
  holdId: string;
  tripId: string;
  items: HoldItem[];
  /** When the hold expires (ISO) — the server is the source of truth, the client only counts down */
  expiresAt: string;
  serverNow: string;
};

export type PaymentMethod = "VNPAY" | "MOMO" | "BANK_CARD";

export const PAYMENT_LABEL: Record<PaymentMethod, string> = {
  VNPAY: "VNPay",
  MOMO: "MoMo wallet",
  BANK_CARD: "Bank card (ATM/Visa)",
};

export type OrderStatus = "PENDING" | "PAID" | "FAILED" | "EXPIRED";

export type Order = {
  orderId: string;
  code: string;               // VT2702061234
  status: OrderStatus;
  totalAmount: number;
  paidAt?: string;
  failureReason?: string;
};

export type TicketStatus = "VALID" | "USED" | "REFUNDED" | "EXCHANGING";

export type Ticket = {
  id: string;
  code: string;
  orderCode: string;
  trainCode: string;
  fromStation: string;
  toStation: string;
  departAt: string;
  carriageNumber: number;
  seatLabel: string;
  seatClass: SeatClassCode;
  passengerName: string;
  passengerId: string;
  price: number;
  status: TicketStatus;
  qrPayload: string;
};

export type QueueTicket = {
  token: string;
  position: number;
  total: number;
  estimatedWaitSeconds: number;
  status: "WAITING" | "ADMITTED";
  /** How long there is to buy once admitted */
  admissionExpiresAt?: string;
};

export type SaleWindow = {
  /** When the current sale opens (ISO). If already open, opensAt <= now */
  opensAt: string;
  label: string;              // "Sale 2 — Lunar New Year 2027 tickets"
  isOpen: boolean;
  serverNow: string;
};

export type SearchQuery = {
  from: string;
  to: string;
  date: string;               // yyyy-MM-dd
  passengers: number;
};
