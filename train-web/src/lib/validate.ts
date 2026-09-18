import type { Discount, Passenger } from "@/api/types";

export type FieldErrors = Partial<Record<keyof Passenger, string>>;

/**
 * Quy tắc kiểm tra dữ liệu hành khách.
 *
 * Ba nguyên tắc về cách báo lỗi:
 *  - Chỉ báo lỗi SAU khi người dùng rời khỏi ô (blur) hoặc bấm gửi; báo ngay
 *    từ ký tự đầu tiên là cách nhanh nhất làm người dùng bực.
 *  - Khi ô đang có lỗi thì chuyển sang kiểm tra ngay từng ký tự, để người dùng
 *    thấy lỗi biến mất lúc sửa xong — đó là phản hồi họ cần.
 *  - Lỗi nói rõ ĐỊNH DẠNG ĐÚNG là gì, không chỉ nói "không hợp lệ".
 */

/** CCCD Việt Nam: 12 chữ số. CMND cũ 9 số vẫn được chấp nhận cho người lớn tuổi. */
export function validateIdNumber(value: string): string | null {
  const v = value.trim();
  if (!v) return "Bạn chưa nhập số CCCD.";
  if (!/^\d+$/.test(v)) return "Số CCCD chỉ gồm chữ số, không có dấu cách hay dấu gạch.";
  if (v.length !== 12 && v.length !== 9) {
    return `Số CCCD phải có 12 chữ số (hoặc 9 chữ số với CMND cũ). Bạn đang nhập ${v.length} chữ số.`;
  }
  return null;
}

/** Số di động Việt Nam: 10 số bắt đầu bằng 0, hoặc dạng +84 */
export function validatePhone(value: string): string | null {
  const v = value.replace(/[\s.]/g, "");
  if (!v) return "Bạn chưa nhập số điện thoại.";
  const normalized = v.startsWith("+84") ? "0" + v.slice(3) : v;
  if (!/^0\d{9}$/.test(normalized)) {
    return "Số điện thoại phải có 10 chữ số, bắt đầu bằng số 0. Ví dụ: 0912345678.";
  }
  return null;
}

export function validateFullName(value: string): string | null {
  const v = value.trim().replace(/\s+/g, " ");
  if (!v) return "Bạn chưa nhập họ tên.";
  if (v.length < 3) return "Họ tên quá ngắn, bạn nhập đầy đủ họ và tên giúp nhé.";
  if (!v.includes(" ")) return "Bạn nhập cả họ và tên, ví dụ: Nguyễn Văn An.";
  if (/\d/.test(v)) return "Họ tên không chứa chữ số.";
  // Chỉ cho chữ cái Latin có dấu tiếng Việt, dấu cách và dấu nháy đơn
  if (!/^[\p{L}\s']+$/u.test(v)) return "Họ tên chỉ gồm chữ cái, đúng như trên CCCD.";
  return null;
}

export function validatePassenger(p: Passenger): FieldErrors {
  const errors: FieldErrors = {};
  const name = validateFullName(p.fullName);
  if (name) errors.fullName = name;
  const id = validateIdNumber(p.idNumber);
  if (id) errors.idNumber = id;
  const phone = validatePhone(p.phone);
  if (phone) errors.phone = phone;
  return errors;
}

/** Giấy tờ cần mang theo khi lên tàu, tuỳ đối tượng giảm giá */
export const DISCOUNT_PROOF: Record<Discount, string | null> = {
  NONE: null,
  STUDENT: "Khi lên tàu cần xuất trình thẻ sinh viên còn hạn.",
  CHILD: "Trẻ em cần giấy khai sinh hoặc hộ chiếu để đối chiếu tuổi.",
  SENIOR: "Cần CCCD thể hiện đủ 60 tuổi trở lên.",
};

export const isPassengerValid = (p: Passenger) => Object.keys(validatePassenger(p)).length === 0;
