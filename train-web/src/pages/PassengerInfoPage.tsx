import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import { ApiError } from "@/api/errors";
import type { Passenger } from "@/api/types";
import { HoldBar, HoldExpiredDialog } from "@/components/HoldBar";
import { PassengerForm, finalPriceOf } from "@/components/PassengerForm";
import { StickyActionBar } from "@/components/TrainCard";
import { Button } from "@/components/ui/Button";
import { Stepper } from "@/components/ui/Stepper";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useBooking } from "@/store/BookingContext";
import { useToast } from "@/store/ToastContext";
import { isPassengerValid } from "@/lib/validate";
import { formatTime, formatVnd } from "@/lib/format";

/**
 * Màn 5 — Giữ chỗ và nhập thông tin hành khách.
 *
 * Quyết định thiết kế quan trọng nhất: KHÔNG chặn người dùng nhập khi còn lỗi.
 * Form vẫn cho gõ thoải mái, chỉ nút "Tiếp tục" mới kiểm tra toàn bộ và cuộn
 * tới ô sai đầu tiên. Ép sửa lỗi ngay tại chỗ trong khi đồng hồ đang chạy là
 * cách chắc chắn khiến người dùng hoảng và mất chỗ.
 */
export function PassengerInfoPage() {
  const navigate = useNavigate();
  const { trip, hold, passengers, setPassengers, clearHold } = useBooking();
  const toast = useToast();

  const [showErrors, setShowErrors] = useState(false);
  const [expired, setExpired] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (!hold || !trip) navigate("/chuyen-tau", { replace: true });
  }, [hold, trip, navigate]);

  const total = useMemo(
    () =>
      (hold?.items ?? []).reduce(
        (sum, item, i) => sum + finalPriceOf(item, passengers[i]?.discount ?? "NONE"),
        0,
      ),
    [hold, passengers],
  );

  const allValid = passengers.length > 0 && passengers.every(isPassengerValid);

  const handleExpire = () => {
    setExpired(true);
    clearHold();
  };

  const { submit: goToPayment, pending } = useSubmitLock(async () => {
    if (!hold) return;
    if (!allValid) {
      setShowErrors(true);
      // Đưa người dùng tới đúng ô đang sai thay vì để họ tự dò tìm
      requestAnimationFrame(() => {
        const firstInvalid = formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]');
        firstInvalid?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstInvalid?.focus({ preventScroll: true });
      });
      toast.show({
        tone: "warning",
        title: "Còn thông tin chưa hợp lệ",
        detail: "Bạn kiểm tra lại các ô được đánh dấu đỏ giúp nhé.",
      });
      return;
    }
    try {
      await api.savePassengers(hold.holdId, passengers);
      navigate("/thanh-toan");
    } catch (e) {
      if (e instanceof ApiError && e.kind === "HOLD_EXPIRED") {
        handleExpire();
        return;
      }
      toast.show({
        tone: "error",
        title: "Chưa lưu được thông tin",
        detail: "Bạn thử bấm lại giúp chúng tôi nhé, chỗ vẫn đang được giữ.",
      });
      throw e;
    }
  });

  const updatePassenger = (index: number, p: Passenger) => {
    const next = [...passengers];
    next[index] = p;
    setPassengers(next);
  };

  /** Chép nhanh số điện thoại của hành khách đầu cho cả đoàn — cả nhà thường dùng chung một số */
  const copyPhoneToAll = () => {
    const phone = passengers[0]?.phone;
    if (!phone) return;
    setPassengers(passengers.map((p) => ({ ...p, phone })));
    toast.show({ tone: "success", title: "Đã dùng chung số điện thoại cho tất cả hành khách" });
  };

  if (!hold || !trip) return <HoldExpiredDialog open={expired} tripId={trip?.id} />;

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={2} />

      <HoldBar hold={hold} total={total} onExpire={handleExpire} />

      <div className="rounded-2xl border border-ink-200 bg-white px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-son-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
          <span className="font-semibold text-ink-900">
            {trip.fromStation.name} → {trip.toStation.name}
          </span>
        </div>
        <p className="mt-1 text-sm text-ink-600">Khởi hành {formatTime(trip.departAt)}</p>
      </div>

      <div>
        <h1 className="text-xl font-bold">Thông tin hành khách</h1>
        <p className="mt-1 text-sm text-ink-600">
          Họ tên và số CCCD phải khớp với giấy tờ mang theo khi lên tàu, nếu sai sẽ không được đi.
        </p>
      </div>

      <form ref={formRef} className="flex flex-col gap-3" onSubmit={(e) => e.preventDefault()} noValidate>
        {hold.items.map((item, i) => (
          <div key={item.seatId}>
            <PassengerForm
              index={i}
              item={item}
              value={passengers[i] ?? { seatId: item.seatId, fullName: "", idNumber: "", phone: "", discount: "NONE" }}
              onChange={(p) => updatePassenger(i, p)}
              showErrors={showErrors}
            />
            {i === 0 && hold.items.length > 1 && passengers[0]?.phone && (
              <button
                type="button"
                onClick={copyPhoneToAll}
                className="mt-1.5 min-h-11 w-full rounded-xl border-2 border-dashed border-ink-300 px-3 text-sm font-semibold text-ink-700 hover:border-son-300 hover:text-son-700"
              >
                Dùng số điện thoại này cho tất cả hành khách
              </button>
            )}
          </div>
        ))}
      </form>

      <StickyActionBar
        note={
          <div className="flex items-center justify-between">
            <span className="text-sm text-ink-600">Tổng cộng {hold.items.length} vé</span>
            <span className="tnum text-lg font-bold text-son-700">{formatVnd(total)}</span>
          </div>
        }
      >
        <Button
          size="lg"
          fullWidth
          loading={pending}
          loadingText="Đang lưu thông tin…"
          onClick={() => void goToPayment().catch(() => {})}
        >
          Tiếp tục thanh toán
        </Button>
        {!allValid && showErrors && (
          <p role="status" className="mt-2 text-center text-sm font-medium text-son-700">
            Vui lòng hoàn tất các ô được đánh dấu phía trên.
          </p>
        )}
      </StickyActionBar>

      <HoldExpiredDialog open={expired} tripId={trip.id} />
    </div>
  );
}
