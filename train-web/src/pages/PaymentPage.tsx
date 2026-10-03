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
import { clearQueuePass } from "@/lib/queuePass";

type Phase = "choose" | "processing" | "success" | "failed" | "expired";

const METHODS: Array<{ code: PaymentMethod; icon: string; note: string }> = [
  { code: "VNPAY", icon: "🏦", note: "Scan a QR code or open your banking app" },
  { code: "MOMO", icon: "💗", note: "Pay with your MoMo wallet balance" },
  { code: "BANK_CARD", icon: "💳", note: "Domestic ATM card, Visa, Mastercard" },
];

/**
 * Screen 6 — Payment.
 *
 * The "processing" state is where things break most easily: users who see a
 * frozen screen press again or F5, and get charged twice. How it is handled:
 *  - The button is locked with useSubmitLock + Idempotency-Key, so duplicates are harmless.
 *  - While waiting, the screen TAKES OVER and clearly says we are waiting for
 *    the bank, with a warning not to reload; there are no buttons left to misclick.
 *  - The order status is polled, because the real result comes from the
 *    payment gateway's webhook, not from the button's response.
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
    if (!hold || !trip) navigate("/trips", { replace: true });
  }, [hold, trip, navigate]);

  // Block leaving the page while waiting for the bank's result
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
    if (phase === "success") return; // once paid, the timer no longer matters
    setPhase("expired");
    clearHold();
  };

  /** Poll the order status until there is a final result */
  const pollOrder = (id: string, attempt = 0) => {
    pollRef.current = window.setTimeout(async () => {
      try {
        const next = await api.getOrderStatus(id);
        setOrder(next);
        if (next.status === "PAID") {
          setPhase("success");
          setOrderId(null);
          // The server freed the waiting room slot when the order was created. Drop the
          // pass in the browser too: buying more tickets means queueing again, which is fair to those waiting.
          clearQueuePass();
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
        // After 60 attempts (~2 minutes), stop polling and ask the user to check
        if (attempt > 60) {
          setPhase("failed");
          setOrder({ ...next, failureReason: "The bank has not responded yet. If your account was charged, the money will be refunded within 3 business days." });
          return;
        }
        pollOrder(id, attempt + 1);
      } catch {
        // Network error while waiting: keep polling, do NOT report failure, the order may have succeeded
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
    resetKey();          // a new attempt is a new transaction
    setOrder(null);
    setPhase("choose");
  };

  if (!hold || !trip) {
    return <HoldExpiredDialog open={phase === "expired"} tripId={trip?.id} />;
  }

  if (phase === "expired") return <HoldExpiredDialog open tripId={trip.id} />;

  // ── Processing ────────────────────────────────────────────────────────────
  if (phase === "processing") {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 text-center" role="status" aria-live="polite">
        <Spinner className="size-12 text-brand-600" />
        <h1 className="text-xl font-bold">Processing your payment</h1>
        <p className="max-w-sm text-ink-700">
          We are waiting for {PAYMENT_LABEL[method]} to confirm the transaction. This usually takes a few seconds.
        </p>
        <div className="mt-2 w-full max-w-sm rounded-2xl border-2 border-accent-300 bg-accent-50 px-4 py-3 text-left">
          <p className="font-bold text-ink-900">Do not reload the page</p>
          <p className="mt-1 text-sm text-ink-700">
            If you reload or press pay again, the transaction is still recorded only once, but you will not
            see the result. Please wait a moment longer.
          </p>
        </div>
        {order && <p className="tnum text-sm text-ink-500">Order code: {order.code}</p>}
      </div>
    );
  }

  // ── Success ───────────────────────────────────────────────────────────────
  if (phase === "success" && order) {
    return (
      <div className="flex flex-col items-center gap-4 py-8 text-center">
        <span aria-hidden className="grid size-20 place-items-center rounded-full bg-ok-50 text-4xl">✓</span>
        <div>
          <h1 className="text-2xl font-bold text-ink-900">Booking confirmed</h1>
          <p className="mt-1 text-ink-700">
            {hold.items.length} {hold.items.length === 1 ? "ticket" : "tickets"} for train {trip.trainCode} {hold.items.length === 1 ? "has" : "have"} been issued. Have a safe trip home for the holidays!
          </p>
        </div>
        <dl className="w-full rounded-2xl border border-ink-200 bg-white p-4 text-left">
          <Row label="Order code" value={<span className="tnum font-bold">{order.code}</span>} />
          <Row label="Amount paid" value={<span className="tnum font-bold text-brand-700">{formatVnd(order.totalAmount)}</span>} />
          <Row label="Payment method" value={PAYMENT_LABEL[method]} />
          <Row label="Train" value={`${trip.trainCode} · ${formatTime(trip.departAt)} ${formatDate(trip.departAt)}`} />
        </dl>
        <p className="text-sm text-ink-600">
          The QR code of each ticket is under <strong>My tickets</strong>. Remember to take a screenshot in case there is no signal at the station.
        </p>
        <div className="flex w-full flex-col gap-2">
          <Button size="lg" fullWidth onClick={() => navigate("/my-tickets")}>
            View my tickets
          </Button>
          <Button variant="secondary" fullWidth onClick={() => navigate("/")}>
            Book more tickets
          </Button>
        </div>
      </div>
    );
  }

  // ── Failed ────────────────────────────────────────────────────────────────
  if (phase === "failed") {
    const holdStillAlive = new Date(hold.expiresAt).getTime() > Date.now();
    return (
      <div className="flex flex-col gap-4">
        <HoldBar hold={hold} total={total} onExpire={handleExpire} />
        <div className="flex flex-col items-center gap-3 rounded-2xl border-2 border-brand-200 bg-brand-50 px-5 py-8 text-center">
          <span aria-hidden className="text-4xl">😔</span>
          <h1 className="text-xl font-bold text-ink-900">Payment did not go through</h1>
          <p className="max-w-sm text-ink-700">
            {order?.failureReason ?? "The transaction was not completed. Your account has not been charged."}
          </p>
          {holdStillAlive && (
            <p className="rounded-xl bg-white px-3 py-2 text-sm font-medium text-ok-600">
              Good news: your seats are still held. Try again right away.
            </p>
          )}
        </div>
        <div className="flex flex-col gap-2">
          <Button size="lg" fullWidth onClick={retryPayment}>
            Choose a payment method and try again
          </Button>
          <Button variant="secondary" fullWidth onClick={() => navigate("/trips")}>
            Cancel and choose another trip
          </Button>
        </div>
      </div>
    );
  }

  // ── Choose a method ───────────────────────────────────────────────────────
  return (
    <div className="flex flex-col gap-4">
      <Stepper current={3} />
      <HoldBar hold={hold} total={total} onExpire={handleExpire} />

      <h1 className="text-xl font-bold">Review and pay</h1>

      {/* Order summary */}
      <section aria-labelledby="order-summary" className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 id="order-summary" className="font-bold text-ink-900">Order summary</h2>
        <div className="mt-2 flex items-center gap-2 border-b border-ink-100 pb-3">
          <span className="rounded-lg bg-brand-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
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
                  <p className="truncate font-semibold text-ink-900">{p?.fullName || `Passenger ${i + 1}`}</p>
                  <p className="text-sm text-ink-600">
                    Carriage {item.carriageNumber} · seat {item.seatLabel} · {SEAT_CLASS_SHORT[item.seatClass]}
                  </p>
                  {p?.idNumber && <p className="tnum text-xs text-ink-500">ID {maskIdNumber(p.idNumber)}</p>}
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
          <span className="font-bold text-ink-900">Total to pay</span>
          <span className="tnum text-xl font-bold text-brand-700">{formatVnd(total)}</span>
        </div>
      </section>

      {/* Payment method */}
      <section aria-labelledby="payment-method" className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 id="payment-method" className="font-bold text-ink-900">Payment method</h2>
        <fieldset className="mt-2">
          <legend className="sr-only">Choose a payment method</legend>
          <div className="flex flex-col gap-2">
            {METHODS.map((m) => {
              const active = method === m.code;
              return (
                <label
                  key={m.code}
                  className={cn(
                    "flex min-h-16 cursor-pointer items-center gap-3 rounded-xl border-2 px-3 py-2 transition-colors",
                    active ? "border-brand-600 bg-brand-50" : "border-ink-200 hover:border-ink-300",
                  )}
                >
                  <input
                    type="radio"
                    name="payment-method"
                    value={m.code}
                    checked={active}
                    onChange={() => { setMethod(m.code); resetKey(); }}
                    className="size-5 accent-brand-600"
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
          loadingText="Redirecting to the payment gateway…"
          onClick={() => void pay().catch(() => {})}
        >
          Pay {formatVnd(total)}
        </Button>
        <p className="mt-2 text-center text-xs text-ink-500">
          By paying you agree to the railway's conditions of carriage.
        </p>
      </StickyActionBar>

      {orderId && <span className="sr-only">Order being processed {orderId}</span>}
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
