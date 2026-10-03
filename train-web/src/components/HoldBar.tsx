import { useNavigate } from "react-router-dom";
import { HoldCountdown } from "./CountdownTimer";
import { Button } from "./ui/Button";
import { Modal } from "./ui/Modal";
import type { Hold } from "@/api/types";
import { formatVnd } from "@/lib/format";

/**
 * Hold bar pinned to the top of screens 5 and 6.
 *
 * The timer must ALWAYS be visible while filling in details, so this bar is
 * sticky right under the header. It also repeats which seats are held — a
 * user typing ID numbers for 3 people easily forgets which carriage and seats
 * they are on.
 */
export function HoldBar({
  hold, total, onExpire,
}: { hold: Hold; total: number; onExpire: () => void }) {
  return (
    <div className="sticky top-16 z-20 -mx-4 border-b border-ink-200 bg-canvas/95 px-4 py-2.5 backdrop-blur-sm sm:-mx-6 sm:px-6">
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
        <HoldCountdown expiresAt={hold.expiresAt} onExpire={onExpire} />
      </div>
    </div>
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
