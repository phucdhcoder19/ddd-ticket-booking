import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import { ApiError } from "@/api/errors";
import { PAYMENT_LABEL, SEAT_CLASS_SHORT, type Order, type PaymentMethod } from "@/api/types";
import { HoldBar, HoldExpiredDialog } from "@/components/HoldBar";
import { finalPriceOf } from "@/components/PassengerForm";
import { StickyActionBar } from "@/components/TrainCard";
import { Button, Spinner } from "@/components/ui/Button";
import { Stepper } from "@/components/ui/Stepper";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useBooking } from "@/store/BookingContext";
import { useToast } from "@/store/ToastContext";
import { formatDate, formatTime, formatVnd, maskIdNumber } from "@/lib/format";
import { friendlyMessage } from "@/api/errors";
import { cn } from "@/lib/cn";

type Phase = "choose" | "processing" | "success" | "failed" | "expired";

const METHODS: Array<{ code: PaymentMethod; icon: string; note: string }> = [
  { code: "VNPAY", icon: "🏦", note: "Quét QR hoặc mở ứng dụng ngân hàng" },
  { code: "MOMO", icon: "💗", note: "Thanh toán bằng số dư ví MoMo" },
  { code: "BANK_CARD", icon: "💳", note: "Thẻ ATM nội địa, Visa, Mastercard" },
];

/**
 * Màn 6 — Thanh toán.
 *
 * Trạng thái "đang xử lý" là chỗ dễ hỏng nhất: người dùng thấy màn hình đứng
 * yên sẽ bấm lại hoặc F5, dẫn tới trừ tiền hai lần. Cách xử lý:
 *  - Nút bấm khoá bằng useSubmitLock + Idempotency-Key, gửi trùng cũng vô hại.
 *  - Trong lúc chờ, màn hình CHIẾM TRỌN và giải thích rõ đang chờ ngân hàng,
 *    kèm cảnh báo không tải lại trang; không còn nút nào để bấm nhầm.
 *  - Trạng thái đơn được hỏi lại theo chu kỳ (poll), vì kết quả thật đến từ
 *    webhook của cổng thanh toán chứ không từ phản hồi của nút bấm.
 */
export function PaymentPage() {
  const navigate = useNavigate();
  const { trip, hold, passengers, orderId, setOrderId, clearHold } = useBooking();
  const toast = useToast();

  const [method, setMethod] = useState<PaymentMethod>("VNPAY");
  const [phase, setPhase] = useState<Phase>("choose");
  const [order, setOrder] = useState<Order | null>(null);
  const pollRef = useRef<number | null>(null);

  useEffect(() => {
    if (!hold || !trip) navigate("/chuyen-tau", { replace: true });
  }, [hold, trip, navigate]);

  // Chặn rời trang khi đang chờ ngân hàng trả kết quả
  useEffect(() => {
    const handler = (e: BeforeUnloadEvent) => {
      if (phase === "processing") e.preventDefault();
    };
    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [phase]);

  const total = useMemo(
    () =>
      (hold?.items ?? []).reduce(
        (sum, item, i) => sum + finalPriceOf(item, passengers[i]?.discount ?? "NONE"),
        0,
      ),
    [hold, passengers],
  );

  const handleExpire = () => {
    if (phase === "success") return; // đã thanh toán xong thì đồng hồ không còn ý nghĩa
    setPhase("expired");
    clearHold();
  };

  /** Hỏi lại trạng thái đơn cho tới khi có kết quả cuối cùng */
  const pollOrder = (id: string, attempt = 0) => {
    pollRef.current = window.setTimeout(async () => {
      try {
        const next = await api.getOrderStatus(id);
        setOrder(next);
        if (next.status === "PAID") {
          setPhase("success");
          setOrderId(null);
          return;
        }
        if (next.status === "FAILED") {
          setPhase("failed");
          return;
        }
        if (next.status === "EXPIRED") {
          handleExpire();
          return;
        }
        // Quá 60 lần (~2 phút) thì dừng hỏi và mời người dùng kiểm tra lại
        if (attempt > 60) {
          setPhase("failed");
          setOrder({ ...next, failureReason: "Ngân hàng chưa phản hồi. Nếu tài khoản đã bị trừ tiền, tiền sẽ được hoàn trong 3 ngày làm việc." });
          return;
        }
        pollOrder(id, attempt + 1);
      } catch {
        // Lỗi mạng khi đang chờ: cứ hỏi tiếp, KHÔNG báo thất bại vì đơn có thể đã thành công
        pollOrder(id, attempt + 1);
      }
    }, 2000);
  };

  useEffect(() => () => { if (pollRef.current) clearTimeout(pollRef.current); }, []);

  const { submit: pay, pending, resetKey } = useSubmitLock(async ({ idempotencyKey }) => {
    if (!hold) return;
    setPhase("processing");
    try {
      const created = await api.createOrder(hold.holdId, method, { idempotencyKey });
      setOrder(created);
      setOrderId(created.orderId);
      pollOrder(created.orderId);
    } catch (e) {
      if (e instanceof ApiError && e.kind === "HOLD_EXPIRED") {
        handleExpire();
        return;
      }
      setPhase("failed");
      setOrder(null);
      toast.show({ tone: "error", ...pick(friendlyMessage(e)) });
      throw e;
    }
  });

  const retryPayment = () => {
    resetKey();          // lần thử mới là một giao dịch mới
    setOrder(null);
    setPhase("choose");
  };

  if (!hold || !trip) {
    return <HoldExpiredDialog open={phase === "expired"} tripId={trip?.id} />;
  }

  if (phase === "expired") return <HoldExpiredDialog open tripId={trip.id} />;

  // ── Đang xử lý ────────────────────────────────────────────────────────────
  if (phase === "processing") {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 text-center" role="status" aria-live="polite">
        <Spinner className="size-12 text-son-600" />
        <h1 className="text-xl font-bold">Đang xử lý thanh toán</h1>
        <p className="max-w-sm text-ink-700">
          Chúng tôi đang chờ {PAYMENT_LABEL[method]} xác nhận giao dịch. Việc này thường mất vài giây.
        </p>
        <div className="mt-2 w-full max-w-sm rounded-2xl border-2 border-mai-300 bg-mai-50 px-4 py-3 text-left">
          <p className="font-bold text-ink-900">Đừng tải lại trang</p>
          <p className="mt-1 text-sm text-ink-700">
            Nếu bạn tải lại hoặc bấm thanh toán lần nữa, giao dịch vẫn chỉ được ghi nhận một lần, nhưng bạn sẽ
            không thấy được kết quả. Xin chờ thêm một chút.
          </p>
        </div>
        {order && <p className="tnum text-sm text-ink-500">Mã đơn: {order.code}</p>}
      </div>
    );
  }

  // ── Thành công ────────────────────────────────────────────────────────────
  if (phase === "success" && order) {
    return (
      <div className="flex flex-col items-center gap-4 py-8 text-center">
        <span aria-hidden className="grid size-20 place-items-center rounded-full bg-ok-50 text-4xl">✓</span>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Đặt vé thành công</h1>
          <p className="mt-1 text-ink-700">
            {hold.items.length} vé tàu {trip.trainCode} đã được xuất. Chúc bạn về quê ăn Tết bình an!
          </p>
        </div>
        <dl className="w-full rounded-2xl border border-ink-200 bg-white p-4 text-left">
          <Row label="Mã đơn hàng" value={<span className="tnum font-bold">{order.code}</span>} />
          <Row label="Số tiền đã thanh toán" value={<span className="tnum font-bold text-son-700">{formatVnd(order.totalAmount)}</span>} />
          <Row label="Phương thức" value={PAYMENT_LABEL[method]} />
          <Row label="Chuyến tàu" value={`${trip.trainCode} · ${formatTime(trip.departAt)} ${formatDate(trip.departAt)}`} />
        </dl>
        <p className="text-sm text-ink-600">
          Mã QR của từng vé nằm trong mục <strong>Vé của tôi</strong>. Bạn nhớ chụp màn hình phòng khi ga không có sóng.
        </p>
        <div className="flex w-full flex-col gap-2">
          <Button size="lg" fullWidth onClick={() => navigate("/ve-cua-toi")}>
            Xem vé của tôi
          </Button>
          <Button variant="secondary" fullWidth onClick={() => navigate("/")}>
            Đặt thêm vé khác
          </Button>
        </div>
      </div>
    );
  }

  // ── Thất bại ──────────────────────────────────────────────────────────────
  if (phase === "failed") {
    const holdStillAlive = new Date(hold.expiresAt).getTime() > Date.now();
    return (
      <div className="flex flex-col gap-4">
        <HoldBar hold={hold} total={total} onExpire={handleExpire} />
        <div className="flex flex-col items-center gap-3 rounded-2xl border-2 border-son-200 bg-son-50 px-5 py-8 text-center">
          <span aria-hidden className="text-4xl">😔</span>
          <h1 className="text-xl font-bold text-ink-900">Thanh toán chưa thành công</h1>
          <p className="max-w-sm text-ink-700">
            {order?.failureReason ?? "Giao dịch không hoàn tất. Tài khoản của bạn chưa bị trừ tiền."}
          </p>
          {holdStillAlive && (
            <p className="rounded-xl bg-white px-3 py-2 text-sm font-medium text-ok-600">
              Tin tốt: chỗ của bạn vẫn đang được giữ. Bạn thử lại ngay nhé.
            </p>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <Button size="lg" fullWidth onClick={retryPayment}>
            Chọn lại phương thức và thử lại
          </Button>
          <Button variant="secondary" fullWidth onClick={() => navigate("/chuyen-tau")}>
            Huỷ và chọn chuyến khác
          </Button>
        </div>
      </div>
    );
  }

  // ── Chọn phương thức ──────────────────────────────────────────────────────
  return (
    <div className="flex flex-col gap-4">
      <Stepper current={3} />
      <HoldBar hold={hold} total={total} onExpire={handleExpire} />

      <h1 className="text-xl font-bold">Xác nhận và thanh toán</h1>

      {/* Tóm tắt đơn */}
      <section aria-labelledby="tom-tat" className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 id="tom-tat" className="font-bold text-ink-900">Tóm tắt đơn hàng</h2>
        <div className="mt-2 flex items-center gap-2 border-b border-ink-100 pb-3">
          <span className="rounded-lg bg-son-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
          <div className="text-sm">
            <div className="font-semibold text-ink-900">{trip.fromStation.name} → {trip.toStation.name}</div>
            <div className="text-ink-600">
              {formatTime(trip.departAt)} · {formatDate(trip.departAt)}
            </div>
          </div>
        </div>

        <ul className="flex flex-col divide-y divide-ink-100">
          {hold.items.map((item, i) => {
            const p = passengers[i];
            const price = finalPriceOf(item, p?.discount ?? "NONE");
            return (
              <li key={item.seatId} className="flex items-start justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="truncate font-semibold text-ink-900">{p?.fullName || `Hành khách ${i + 1}`}</p>
                  <p className="text-sm text-ink-600">
                    Toa {item.carriageNumber} · chỗ {item.seatLabel} · {SEAT_CLASS_SHORT[item.seatClass]}
                  </p>
                  {p?.idNumber && <p className="tnum text-xs text-ink-500">CCCD {maskIdNumber(p.idNumber)}</p>}
                </div>
                <div className="shrink-0 text-right">
                  {price !== item.price && (
                    <div className="tnum text-xs text-ink-400 line-through">{formatVnd(item.price)}</div>
                  )}
                  <div className="tnum font-semibold text-ink-900">{formatVnd(price)}</div>
                </div>
              </li>
            );
          })}
        </ul>

        <div className="flex items-center justify-between border-t-2 border-ink-200 pt-3">
          <span className="font-bold text-ink-900">Tổng thanh toán</span>
          <span className="tnum text-xl font-bold text-son-700">{formatVnd(total)}</span>
        </div>
      </section>

      {/* Phương thức thanh toán */}
      <section aria-labelledby="pttt" className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 id="pttt" className="font-bold text-ink-900">Phương thức thanh toán</h2>
        <fieldset className="mt-2">
          <legend className="sr-only">Chọn phương thức thanh toán</legend>
          <div className="flex flex-col gap-2">
            {METHODS.map((m) => {
              const active = method === m.code;
              return (
                <label
                  key={m.code}
                  className={cn(
                    "flex min-h-16 cursor-pointer items-center gap-3 rounded-xl border-2 px-3 py-2 transition-colors",
                    active ? "border-son-600 bg-son-50" : "border-ink-200 hover:border-ink-300",
                  )}
                >
                  <input
                    type="radio"
                    name="payment-method"
                    value={m.code}
                    checked={active}
                    onChange={() => { setMethod(m.code); resetKey(); }}
                    className="size-5 accent-son-600"
                  />
                  <span aria-hidden className="text-2xl">{m.icon}</span>
                  <span className="min-w-0 flex-1">
                    <span className="block font-semibold text-ink-900">{PAYMENT_LABEL[m.code]}</span>
                    <span className="block text-sm text-ink-600">{m.note}</span>
                  </span>
                </label>
              );
            })}
          </div>
        </fieldset>
      </section>

      <StickyActionBar>
        <Button
          size="lg"
          fullWidth
          loading={pending}
          loadingText="Đang chuyển tới cổng thanh toán…"
          onClick={() => void pay().catch(() => {})}
        >
          Thanh toán {formatVnd(total)}
        </Button>
        <p className="mt-2 text-center text-xs text-ink-500">
          Bấm thanh toán nghĩa là bạn đồng ý với điều kiện vận chuyển của ngành đường sắt.
        </p>
      </StickyActionBar>

      {orderId && <span className="sr-only">Mã đơn đang xử lý {orderId}</span>}
    </div>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-ink-100 py-2 last:border-0">
      <dt className="text-sm text-ink-600">{label}</dt>
      <dd className="text-right text-sm text-ink-900">{value}</dd>
    </div>
  );
}

const pick = (m: { title: string; detail: string }) => ({ title: m.title, detail: m.detail });
