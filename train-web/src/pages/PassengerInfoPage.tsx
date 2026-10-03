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
 * Screen 5 — Hold and passenger details.
 *
 * The most important design decision: DO NOT block typing while there are
 * errors. The form lets users type freely, and only the "Continue" button
 * checks everything and scrolls to the first invalid field. Forcing fixes on
 * the spot while the timer runs is a sure way to make people panic and lose
 * their seats.
 */
export function PassengerInfoPage() {
  const navigate = useNavigate();
  const { trip, hold, passengers, setPassengers, clearHold } = useBooking();
  const toast = useToast();

  const [showErrors, setShowErrors] = useState(false);
  const [expired, setExpired] = useState(false);
  const formRef = useRef<HTMLFormElement>(null);

  useEffect(() => {
    if (!hold || !trip) navigate("/trips", { replace: true });
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
      // Take the user to the invalid field instead of making them hunt for it
      requestAnimationFrame(() => {
        const firstInvalid = formRef.current?.querySelector<HTMLElement>('[aria-invalid="true"]');
        firstInvalid?.scrollIntoView({ behavior: "smooth", block: "center" });
        firstInvalid?.focus({ preventScroll: true });
      });
      toast.show({
        tone: "warning",
        title: "Some details are not valid yet",
        detail: "Please check the fields marked in red.",
      });
      return;
    }
    try {
      await api.savePassengers(hold.holdId, passengers);
      navigate("/payment");
    } catch (e) {
      if (e instanceof ApiError && e.kind === "HOLD_EXPIRED") {
        handleExpire();
        return;
      }
      toast.show({
        tone: "error",
        title: "Your details were not saved",
        detail: "Please press the button again, your seats are still held.",
      });
      throw e;
    }
  });

  const updatePassenger = (index: number, p: Passenger) => {
    const next = [...passengers];
    next[index] = p;
    setPassengers(next);
  };

  /** Copy the first passenger's phone to the whole group — families often share one number */
  const copyPhoneToAll = () => {
    const phone = passengers[0]?.phone;
    if (!phone) return;
    setPassengers(passengers.map((p) => ({ ...p, phone })));
    toast.show({ tone: "success", title: "The same phone number is now used for all passengers" });
  };

  if (!hold || !trip) return <HoldExpiredDialog open={expired} tripId={trip?.id} />;

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={2} />

      <HoldBar hold={hold} total={total} onExpire={handleExpire} />

      <div className="rounded-2xl border border-ink-200 bg-white px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-brand-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
          <span className="font-semibold text-ink-900">
            {trip.fromStation.name} → {trip.toStation.name}
          </span>
        </div>
        <p className="mt-1 text-sm text-ink-600">Departs {formatTime(trip.departAt)}</p>
      </div>

      <div>
        <h1 className="text-xl font-bold">Passenger details</h1>
        <p className="mt-1 text-sm text-ink-600">
          Names and ID numbers must match the documents carried on board, otherwise the passenger cannot travel.
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
                className="mt-1.5 min-h-11 w-full rounded-xl border-2 border-dashed border-ink-300 px-3 text-sm font-semibold text-ink-700 hover:border-brand-300 hover:text-brand-700"
              >
                Use this phone number for all passengers
              </button>
            )}
          </div>
        ))}
      </form>

      <StickyActionBar
        note={
          <div className="flex items-center justify-between">
            <span className="text-sm text-ink-600">Total for {hold.items.length} {hold.items.length === 1 ? "ticket" : "tickets"}</span>
            <span className="tnum text-lg font-bold text-brand-700">{formatVnd(total)}</span>
          </div>
        }
      >
        <Button
          size="lg"
          fullWidth
          loading={pending}
          loadingText="Saving your details…"
          onClick={() => void goToPayment().catch(() => {})}
        >
          Continue to payment
        </Button>
        {!allValid && showErrors && (
          <p role="status" className="mt-2 text-center text-sm font-medium text-brand-700">
            Please complete the fields marked above.
          </p>
        )}
      </StickyActionBar>

      <HoldExpiredDialog open={expired} tripId={trip.id} />
    </div>
  );
}
