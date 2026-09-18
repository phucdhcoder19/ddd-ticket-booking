/**
 * Lớp gọi API duy nhất của ứng dụng. Tầng UI chỉ import từ đây.
 * VITE_USE_MOCK=true  -> dùng mock server trong trình duyệt
 * VITE_USE_MOCK=false -> gọi thật qua VITE_API_BASE_URL (mặc định http://localhost:8080/api)
 *
 * Chữ ký hàm hai bên giống hệt nhau nên khi backend sẵn sàng chỉ cần đổi biến môi trường.
 */
import { request, type RequestOptions } from "@/lib/http";
import { mockApi } from "./mock/server";
import type {
  Carriage, Hold, Order, Passenger, PaymentMethod, QueueTicket,
  SaleWindow, SearchQuery, SeatClassCode, Station, Ticket, Trip,
} from "./types";

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== "false";

/** Tuỳ chọn truyền xuống tầng http (retry, huỷ, báo đang thử lại) */
export type CallOptions = Pick<RequestOptions, "signal" | "onRetry" | "maxRetries" | "timeoutMs" | "idempotencyKey">;

const qs = (params: Record<string, string | number>) =>
  "?" + new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)])).toString();

export const api = {
  getStations: (o?: CallOptions) =>
    USE_MOCK ? mockApi.getStations() : request<Station[]>("/stations", o),

  getSaleWindow: (o?: CallOptions) =>
    USE_MOCK ? mockApi.getSaleWindow() : request<SaleWindow>("/sale-window", o),

  searchTrips: (q: SearchQuery, o?: CallOptions) =>
    USE_MOCK ? mockApi.searchTrips(q) : request<Trip[]>(`/trips${qs({ ...q })}`, o),

  getTrip: (tripId: string, o?: CallOptions) =>
    USE_MOCK ? mockApi.getTrip(tripId) : request<Trip>(`/trips/${tripId}`, o),

  getCarriages: (tripId: string, seatClass: SeatClassCode, o?: CallOptions) =>
    USE_MOCK
      ? mockApi.getCarriages(tripId, seatClass)
      : request<Carriage[]>(`/trips/${tripId}/carriages${qs({ seatClass })}`, o),

  /** Giữ chỗ — luôn kèm Idempotency-Key để double-click không tạo hai lượt giữ */
  holdSeats: (
    input: { tripId: string; seatClass: SeatClassCode; seatIds: string[] },
    o?: CallOptions,
  ) =>
    USE_MOCK
      ? mockApi.holdSeats(input.tripId, input.seatClass, input.seatIds, o?.idempotencyKey)
      : request<Hold>("/holds", { ...o, method: "POST", body: input, maxRetries: 0 }),

  getHold: (holdId: string, o?: CallOptions) =>
    USE_MOCK ? mockApi.getHold(holdId) : request<Hold>(`/holds/${holdId}`, o),

  releaseHold: (holdId: string, o?: CallOptions) =>
    USE_MOCK
      ? mockApi.releaseHold(holdId)
      : request<void>(`/holds/${holdId}`, { ...o, method: "DELETE" }),

  savePassengers: (holdId: string, passengers: Passenger[], o?: CallOptions) =>
    USE_MOCK
      ? mockApi.savePassengers(holdId, passengers)
      : request<void>(`/holds/${holdId}/passengers`, { ...o, method: "PUT", body: { passengers } }),

  createOrder: (holdId: string, method: PaymentMethod, o?: CallOptions) =>
    USE_MOCK
      ? mockApi.createOrder(holdId, method, o?.idempotencyKey)
      : request<Order>("/orders", { ...o, method: "POST", body: { holdId, paymentMethod: method }, maxRetries: 0 }),

  getOrderStatus: (orderId: string, o?: CallOptions) =>
    USE_MOCK ? mockApi.getOrderStatus(orderId) : request<Order>(`/orders/${orderId}`, o),

  getMyTickets: (o?: CallOptions) =>
    USE_MOCK ? mockApi.getMyTickets() : request<Ticket[]>("/me/tickets", o),

  refundTicket: (ticketId: string, o?: CallOptions) =>
    USE_MOCK
      ? mockApi.refundTicket(ticketId)
      : request<Ticket>(`/tickets/${ticketId}/refund`, { ...o, method: "POST", maxRetries: 0 }),

  joinQueue: (o?: CallOptions) =>
    USE_MOCK ? mockApi.joinQueue() : request<QueueTicket>("/queue", { ...o, method: "POST" }),

  getQueueStatus: (token: string, o?: CallOptions) =>
    USE_MOCK ? mockApi.getQueueStatus(token) : request<QueueTicket>(`/queue/${token}`, o),
};
