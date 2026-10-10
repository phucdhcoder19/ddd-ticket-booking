import { useNavigate } from "react-router-dom";
import { HoldCountdown } from "./CountdownTimer";
import { Button } from "./ui/Button";
import { Modal } from "./ui/Modal";
import { api } from "@/api/client";
import { ApiError } from "@/api/errors";
import type { Hold } from "@/api/types";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useToast } from "@/store/ToastContext";
import { formatVnd } from "@/lib/format";

/**
 * Hold bar pinned to the top of screens 5 and 6, right under the two-row
 * header (108px, HEADER_HEIGHT in AppLayout) so the header never covers it.
 *
 * The timer must ALWAYS be visible while filling in details, so this bar is
 * sticky right under the header. It also repeats which seats are held — a
 * user typing ID numbers for 3 people easily forgets which carriage and seats
 * they are on.
 */
export function HoldBar({
  hold, total, onExpire, onCancel,
}: { hold: Hold; total: number; onExpire: () => void; onCancel?: () => void }) {
  return (
    <div className="sticky top-[108px] z-20 -mx-4 border-b border-ink-200 bg-canvas/95 px-4 py-2.5 backdrop-blur-sm sm:-mx-6 sm:px-6">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ink-900">
            Holding {hold.items.length} {hold.items.length === 1 ? "seat" : "seats"}
          </p>
          <p className="truncate text-xs text-ink-600">
            {hold.items.map((i) => `Carriage ${i.carriageNumber} · seat ${i.seatLabel}`).join(" · ")}
          </p>
          <p className="tnum text-xs font-semibold text-brand-700">{formatVnd(total)}</p>
        </div>
        <div className="flex shrink-0 flex-col items-end gap-1">
          <HoldCountdown expiresAt={hold.expiresAt} onExpire={onExpire} />
          {onCancel && (
            <button
              type="button"
              onClick={onCancel}
              className="min-h-8 rounded-lg px-2 text-xs font-semibold text-brand-700 underline underline-offset-2 hover:bg-brand-50"
            >
              Cancel hold
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

/**
 * Shown on the trip list and the seat map while the user still holds seats.
 *
 * Going back with the browser button must NOT release the seats: people press
 * back to double-check the trip or the carriage, not to give up. Without this
 * banner their own seats simply look "taken" on the map and the hold is
 * forgotten by the UI while it keeps blocking those seats for 10 minutes.
 * Releasing is always an explicit choice, behind a confirmation.
 */
export function ActiveHoldBanner({
  hold, onContinue, onCancel, onExpire,
}: { hold: Hold; onContinue: () => void; onCancel: () => void; onExpire: () => void }) {
  const count = hold.items.length;
  return (
    <div role="status" className="rounded-2xl border-2 border-accent-300 bg-accent-50 px-4 py-3">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="font-bold text-ink-900">
            You have {count} {count === 1 ? "seat" : "seats"} on hold
          </p>
          <p className="mt-0.5 text-sm text-ink-700">
            {hold.items.map((i) => `Carriage ${i.carriageNumber} · seat ${i.seatLabel}`).join(" · ")}
          </p>
          <p className="mt-1 text-sm text-ink-600">
            Enter the passenger details and pay before the timer runs out to keep {count === 1 ? "it" : "them"}.
          </p>
        </div>
        <HoldCountdown expiresAt={hold.expiresAt} onExpire={onExpire} className="shrink-0" />
      </div>
      <div className="mt-3 flex flex-col gap-2 sm:flex-row">
        <Button onClick={onContinue} className="sm:flex-1">Enter passenger details</Button>
        <Button variant="danger" onClick={onCancel} className="sm:flex-1">Cancel hold</Button>
      </div>
    </div>
  );
}

/**
 * Confirmation before releasing a hold. A hold that is already gone on the
 * server (released by the job, 404/410) counts as cancelled: the end state the
 * user asked for is the same.
 */
export function CancelHoldDialog({
  open, hold, onClose, onCancelled,
}: { open: boolean; hold: Hold; onClose: () => void; onCancelled: () => void }) {
  const toast = useToast();
  const count = hold.items.length;

  const { submit, pending } = useSubmitLock(async () => {
    try {
      await api.releaseHold(hold.holdId);
    } catch (e) {
      const alreadyGone = e instanceof ApiError && (e.kind === "NOT_FOUND" || e.kind === "HOLD_EXPIRED");
      if (!alreadyGone) {
        toast.show({
          tone: "error",
          title: "Could not cancel your hold",
          detail: "Your seats are still held. Please try again.",
        });
        throw e;
      }
    }
    toast.show({
      tone: "success",
      title: "Hold cancelled",
      detail: "You can pick seats again.",
    });
    onCancelled();
  });

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Cancel this hold?"
      description={`The ${count === 1 ? "seat goes" : `${count} seats go`} back on sale right away, and someone else may take ${count === 1 ? "it" : "them"}. Passenger details you entered will be cleared.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Keep my seats</Button>
          <Button
            variant="danger"
            loading={pending}
            loadingText="Cancelling…"
            onClick={() => void submit().catch(() => {})}
          >
            Cancel hold
          </Button>
        </>
      }
    >
      <p className="rounded-xl border border-ink-200 bg-ink-50 p-3 text-sm text-ink-800">
        {hold.items.map((i) => `Carriage ${i.carriageNumber} · seat ${i.seatLabel}`).join(" · ")}
      </p>
    </Modal>
  );
}

/**
 * Dialog shown when the hold expires.
 *
 * Cannot be closed with Esc or by clicking outside: the user must choose a
 * way forward, because whatever was half-entered is no longer valid. The tone
 * takes the blame on the system's side rather than blaming the user for being slow.
 */
export function HoldExpiredDialog({ open, tripId }: { open: boolean; tripId?: string | null }) {
  const navigate = useNavigate();
  return (
    <Modal
      open={open}
      onClose={() => {}}
      dismissible={false}
      title="Your hold has expired"
      description="After 10 minutes, seats are released to other passengers so everyone gets a chance to buy. The details you entered are kept for now, you only need to pick your seats again."
      footer={
        <>
          <Button variant="secondary" onClick={() => navigate("/trips")}>
            See other trips
          </Button>
          <Button
            onClick={() => navigate(tripId ? `/seats/${encodeURIComponent(tripId)}` : "/trips")}
          >
            Pick seats on this trip again
          </Button>
        </>
      }
    />
  );
}
