import { useState } from "react";
import { DISCOUNT_LABEL, DISCOUNT_RATE, type Discount, type HoldItem, type Passenger } from "@/api/types";
import { SelectInput, TextInput } from "./ui/Field";
import { Badge } from "./ui/Badge";
import { DISCOUNT_PROOF, validateFullName, validateIdNumber, validatePhone } from "@/lib/validate";
import { formatVnd } from "@/lib/format";

/**
 * Form một hành khách.
 *
 * Chi tiết nhỏ nhưng quan trọng trên điện thoại:
 *  - inputMode="numeric" cho CCCD và số điện thoại để bật bàn phím số.
 *  - autoComplete đúng chuẩn, để trình duyệt điền hộ — người lớn tuổi gõ CCCD
 *    12 số trên màn hình cảm ứng là việc rất cực.
 *  - Ô CCCD chỉ nhận chữ số: lọc ngay lúc gõ thay vì để người dùng gõ xong mới
 *    báo lỗi "không được có dấu cách".
 */
export function PassengerForm({
  index, item, value, onChange, showErrors,
}: {
  index: number;
  item: HoldItem;
  value: Passenger;
  onChange: (p: Passenger) => void;
  /** Bật khi người dùng đã bấm "Tiếp tục" — lúc đó hiện lỗi của cả ô chưa chạm tới */
  showErrors: boolean;
}) {
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const set = <K extends keyof Passenger>(key: K, v: Passenger[K]) => onChange({ ...value, [key]: v });
  const show = (field: string) => showErrors || touched[field];

  const nameError = show("fullName") ? validateFullName(value.fullName) : null;
  const idError = show("idNumber") ? validateIdNumber(value.idNumber) : null;
  const phoneError = show("phone") ? validatePhone(value.phone) : null;

  const discountRate = DISCOUNT_RATE[value.discount];
  const finalPrice = Math.round((item.price * (1 - discountRate)) / 1000) * 1000;

  return (
    <fieldset className="rounded-2xl border border-ink-200 bg-white p-4">
      <legend className="sr-only">Thông tin hành khách thứ {index + 1}</legend>

      <div className="mb-3 flex items-center justify-between gap-3">
        <h3 className="font-bold text-ink-900">Hành khách {index + 1}</h3>
        <Badge tone="son">Toa {item.carriageNumber} · chỗ {item.seatLabel}</Badge>
      </div>

      <div className="flex flex-col gap-1">
        <TextInput
          label="Họ và tên"
          placeholder="NGUYỄN VĂN AN"
          hint="Ghi đúng như trên CCCD, không viết tắt."
          autoComplete="name"
          autoCapitalize="characters"
          required
          value={value.fullName}
          error={nameError}
          onChange={(e) => set("fullName", e.target.value)}
          onBlur={() => setTouched((t) => ({ ...t, fullName: true }))}
        />

        <TextInput
          label="Số CCCD"
          placeholder="001203001234"
          hint="12 chữ số trên căn cước công dân."
          inputMode="numeric"
          autoComplete="off"
          maxLength={12}
          required
          value={value.idNumber}
          error={idError}
          onChange={(e) => set("idNumber", e.target.value.replace(/\D/g, ""))}
          onBlur={() => setTouched((t) => ({ ...t, idNumber: true }))}
        />

        <TextInput
          label="Số điện thoại"
          placeholder="0912345678"
          hint="Dùng để báo tin khi tàu đổi giờ."
          inputMode="tel"
          type="tel"
          autoComplete="tel"
          maxLength={15}
          required
          value={value.phone}
          error={phoneError}
          onChange={(e) => set("phone", e.target.value.replace(/[^\d+]/g, ""))}
          onBlur={() => setTouched((t) => ({ ...t, phone: true }))}
        />

        <SelectInput
          label="Đối tượng giảm giá"
          hint={DISCOUNT_PROOF[value.discount] ?? "Chọn nếu bạn thuộc diện được giảm giá vé."}
          value={value.discount}
          onChange={(e) => set("discount", e.target.value as Discount)}
          options={(Object.keys(DISCOUNT_LABEL) as Discount[]).map((d) => ({
            value: d,
            label: DISCOUNT_LABEL[d],
          }))}
        />
      </div>

      <div className="mt-2 flex items-center justify-between border-t border-ink-100 pt-3">
        <span className="text-sm text-ink-600">Giá vé</span>
        <span className="flex items-baseline gap-2">
          {discountRate > 0 && (
            <span className="tnum text-sm text-ink-400 line-through">{formatVnd(item.price)}</span>
          )}
          <span className="tnum font-bold text-son-700">{formatVnd(finalPrice)}</span>
        </span>
      </div>
    </fieldset>
  );
}

/** Giá cuối của một vé sau khi trừ giảm giá — dùng chung giữa form và màn thanh toán */
export const finalPriceOf = (item: HoldItem, discount: Discount) =>
  Math.round((item.price * (1 - DISCOUNT_RATE[discount])) / 1000) * 1000;
