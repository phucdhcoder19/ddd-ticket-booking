export type Station = { code: string; name: string; region: "Bắc" | "Trung" | "Nam" };

export type SeatClassCode = "SOFT_SEAT" | "BERTH_4" | "BERTH_6";

export const SEAT_CLASS_LABEL: Record<SeatClassCode, string> = {
  SOFT_SEAT: "Ngồi mềm điều hoà",
  BERTH_4: "Nằm khoang 4",
  BERTH_6: "Nằm khoang 6",
};

export const SEAT_CLASS_SHORT: Record<SeatClassCode, string> = {
  SOFT_SEAT: "Ngồi mềm",
  BERTH_4: "Khoang 4",
  BERTH_6: "Khoang 6",
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
  /** Tổng chỗ còn của cả chuyến — dùng cho badge "Sắp hết"/"Hết vé" */
  availableTotal: number;
};

export type SeatStatus = "available" | "held" | "sold" | "selected";

export type Seat = {
  id: string;                 // "C3-12"
  label: string;              // "12"
  row: number;
  col: number;
  /** Với toa nằm: số khoang (1..n); với toa ngồi: undefined */
  compartment?: number;
  /** Tầng giường: 1 = tầng 1 (đắt hơn), 2, 3 */
  berthLevel?: number;
  status: Exclude<SeatStatus, "selected">;
  price: number;
};

export type Carriage = {
  id: string;
  number: number;             // Toa số 3
  seatClass: SeatClassCode;
  layout: "seat-2-2" | "berth-4" | "berth-6";
  rows: number;
  seats: Seat[];
  available: number;
};

export type Discount = "NONE" | "STUDENT" | "CHILD" | "SENIOR";

export const DISCOUNT_LABEL: Record<Discount, string> = {
  NONE: "Không giảm giá",
  STUDENT: "Sinh viên (−10%)",
  CHILD: "Trẻ em dưới 10 tuổi (−25%)",
  SENIOR: "Người cao tuổi từ 60 (−15%)",
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
  /** Thời điểm hết hạn giữ chỗ (ISO) — nguồn sự thật là server, client chỉ đếm ngược */
  expiresAt: string;
  serverNow: string;
};

export type PaymentMethod = "VNPAY" | "MOMO" | "BANK_CARD";

export const PAYMENT_LABEL: Record<PaymentMethod, string> = {
  VNPAY: "VNPay",
  MOMO: "Ví MoMo",
  BANK_CARD: "Thẻ ngân hàng (ATM/Visa)",
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
  /** Thời hạn để vào mua sau khi được gọi */
  admissionExpiresAt?: string;
};

export type SaleWindow = {
  /** Thời điểm mở bán đợt hiện tại (ISO). Nếu đã mở thì opensAt <= now */
  opensAt: string;
  label: string;              // "Đợt 2 — vé Tết Đinh Mùi 2027"
  isOpen: boolean;
  serverNow: string;
};

export type SearchQuery = {
  from: string;
  to: string;
  date: string;               // yyyy-MM-dd
  passengers: number;
};
