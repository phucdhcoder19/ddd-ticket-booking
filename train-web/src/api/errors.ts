export type ApiErrorKind =
  | "SOLD_OUT"        // 409 — hết vé
  | "SEAT_TAKEN"      // 409 — ghế vừa bị người khác giữ
  | "HOLD_EXPIRED"    // 410 — hết thời gian giữ chỗ
  | "OVERLOADED"      // 429 — hệ thống quá tải
  | "TIMEOUT"         // mạng chậm / không phản hồi
  | "OFFLINE"
  | "VALIDATION"      // 400
  | "UNAUTHORIZED"    // 401/403
  | "NOT_FOUND"       // 404
  | "SERVER"          // 5xx
  | "UNKNOWN";

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  /** Server gợi ý chờ bao lâu rồi thử lại (giây) — từ header Retry-After */
  readonly retryAfter?: number;
  /** Dữ liệu kèm theo, ví dụ danh sách ghế vừa bị lấy mất */
  readonly details?: unknown;

  constructor(kind: ApiErrorKind, message: string, opts: { status?: number; retryAfter?: number; details?: unknown } = {}) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = opts.status;
    this.retryAfter = opts.retryAfter;
    this.details = opts.details;
  }
}

/**
 * Thông điệp hiển thị cho người dùng — luôn nói RÕ chuyện gì xảy ra và
 * NÊN LÀM GÌ TIẾP, không bao giờ đổ mã lỗi kỹ thuật lên màn hình.
 */
export function friendlyMessage(err: unknown): { title: string; detail: string; action?: string } {
  if (!(err instanceof ApiError)) {
    return {
      title: "Đã có lỗi xảy ra",
      detail: "Rất tiếc, hệ thống gặp sự cố ngoài dự kiến. Bạn vui lòng thử lại.",
      action: "Thử lại",
    };
  }
  switch (err.kind) {
    case "SOLD_OUT":
      return {
        title: "Chuyến này đã hết vé",
        detail: "Vé vừa được bán hết trong lúc bạn đang chọn. Bạn thử chuyến khác hoặc ngày gần đó nhé.",
        action: "Xem chuyến khác",
      };
    case "SEAT_TAKEN":
      return {
        title: "Chỗ vừa có người giữ mất",
        detail: "Có hành khách khác nhanh tay hơn một chút. Chúng tôi đã gợi ý những chỗ trống gần đó cho bạn.",
        action: "Chọn chỗ khác",
      };
    case "HOLD_EXPIRED":
      return {
        title: "Đã hết thời gian giữ chỗ",
        detail: "Chỗ của bạn đã được trả lại cho hành khách khác. Bạn cần chọn lại chỗ để tiếp tục.",
        action: "Chọn lại chỗ",
      };
    case "OVERLOADED":
      return {
        title: "Hệ thống đang rất đông",
        detail: "Quá nhiều người cùng đặt vé lúc này. Chúng tôi đang tự động thử lại giúp bạn, xin đừng tắt trang.",
      };
    case "TIMEOUT":
      return {
        title: "Mạng phản hồi chậm",
        detail: "Yêu cầu của bạn chờ quá lâu chưa có kết quả. Kiểm tra kết nối rồi thử lại giúp chúng tôi nhé.",
        action: "Thử lại",
      };
    case "OFFLINE":
      return {
        title: "Không có kết nối mạng",
        detail: "Thiết bị của bạn đang ngoại tuyến. Vé và thao tác dở dang vẫn được giữ nguyên.",
        action: "Thử lại",
      };
    case "VALIDATION":
      return { title: "Thông tin chưa hợp lệ", detail: err.message, action: "Kiểm tra lại" };
    case "NOT_FOUND":
      return { title: "Không tìm thấy", detail: "Nội dung bạn tìm không còn tồn tại hoặc đã bị xoá." };
    case "UNAUTHORIZED":
      return { title: "Phiên đăng nhập đã hết", detail: "Bạn vui lòng đăng nhập lại để tiếp tục.", action: "Đăng nhập" };
    default:
      return {
        title: "Máy chủ đang bận",
        detail: "Hệ thống tạm thời chưa xử lý được yêu cầu. Bạn thử lại sau ít phút nhé.",
        action: "Thử lại",
      };
  }
}
