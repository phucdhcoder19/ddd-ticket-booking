import { AvailabilityBadge, Badge } from "./ui/Badge";
import { Skeleton } from "./ui/StateView";
import { SEAT_CLASS_LABEL, SEAT_CLASS_SHORT, type SeatClassCode, type Trip } from "@/api/types";
import { formatDuration, formatTime, formatVnd } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Card for one trip.
 *
 * The layout follows the order in which travellers decide:
 *   departure → arrival → journey time → lowest price → seats left.
 * Each seat class is its own button, so users tap straight into the class they want, saving a step.
 */
export function TrainCard({
  trip, onSelectClass, highlightClass,
}: {
  trip: Trip;
  onSelectClass: (seatClass: SeatClassCode) => void;
  highlightClass?: SeatClassCode | null;
}) {
  const soldOut = trip.availableTotal <= 0;
  const cheapest = Math.min(...trip.classes.filter((c) => c.available > 0).map((c) => c.price));

  return (
    <article
      className={cn(
        "overflow-hidden rounded-2xl border bg-white shadow-[var(--shadow-soft)] transition-shadow",
        soldOut ? "border-ink-200 opacity-75" : "border-ink-200 hover:shadow-[var(--shadow-lift)]",
      )}
      aria-label={`Train ${trip.trainCode}, departs ${formatTime(trip.departAt)}`}
    >
      <div className="flex items-start justify-between gap-3 px-4 pt-4">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-brand-600 px-2.5 py-1 text-sm font-bold text-white">{trip.trainCode}</span>
          <span className="text-sm text-ink-600">{formatDuration(trip.durationMinutes)}</span>
        </div>
        {soldOut ? <Badge tone="muted">Sold out</Badge> : <AvailabilityBadge available={trip.availableTotal} />}
      </div>

      {/* Journey line: departure ─ duration ─ arrival */}
      <div className="flex items-center gap-3 px-4 py-3">
        <div className="text-left">
          <div className="tnum text-2xl font-bold text-ink-900">{formatTime(trip.departAt)}</div>
          <div className="text-sm text-ink-600">{trip.fromStation.name}</div>
        </div>
        <div className="flex flex-1 flex-col items-center gap-1" aria-hidden>
          <div className="flex w-full items-center gap-1">
            <span className="size-2 rounded-full bg-brand-600" />
            <span className="h-0.5 flex-1 bg-gradient-to-r from-brand-600 via-accent-300 to-brand-600" />
            <span className="text-brand-600">🚆</span>
            <span className="h-0.5 flex-1 bg-gradient-to-r from-brand-600 via-accent-300 to-brand-600" />
            <span className="size-2 rounded-full bg-brand-600" />
          </div>
        </div>
        <div className="text-right">
          <div className="tnum text-2xl font-bold text-ink-900">{formatTime(trip.arriveAt)}</div>
          <div className="text-sm text-ink-600">{trip.toStation.name}</div>
        </div>
      </div>

      {/* Seat classes */}
      <div className="border-t border-ink-100 bg-ink-50/60 px-3 py-3">
        <ul className="flex flex-col gap-2">
          {trip.classes.map((c) => {
            const out = c.available <= 0;
            return (
              <li key={c.code}>
                <button
                  type="button"
                  disabled={out}
                  onClick={() => onSelectClass(c.code)}
                  aria-label={`Choose ${SEAT_CLASS_LABEL[c.code]}, price ${formatVnd(c.price)}, ${out ? "sold out" : `${c.available} seats left`}`}
                  className={cn(
                    "flex w-full min-h-14 items-center gap-3 rounded-xl border-2 bg-white px-3 py-2 text-left transition-colors",
                    out
                      ? "cursor-not-allowed border-ink-200 opacity-60"
                      : "border-ink-200 hover:border-brand-400 hover:bg-brand-50/50 active:bg-brand-50",
                    highlightClass === c.code && "border-brand-500 bg-brand-50",
                  )}
                >
                  <div className="min-w-0 flex-1">
                    <div className="font-semibold text-ink-900">{SEAT_CLASS_SHORT[c.code]}</div>
                    <div className="text-xs text-ink-500">{SEAT_CLASS_LABEL[c.code]}</div>
                  </div>
                  <div className="text-right">
                    <div className={cn("tnum font-bold", out ? "text-ink-400 line-through" : "text-brand-700")}>
                      {formatVnd(c.price)}
                    </div>
                    <div className="mt-0.5">
                      <AvailabilityBadge available={c.available} />
                    </div>
                  </div>
                  <span aria-hidden className={cn("text-xl", out ? "text-ink-300" : "text-brand-600")}>›</span>
                </button>
              </li>
            );
          })}
        </ul>
        {!soldOut && (
          <p className="mt-2 px-1 text-xs text-ink-500">
            From <span className="tnum font-semibold text-ink-700">{formatVnd(cheapest)}</span> / seat
          </p>
        )}
      </div>
    </article>
  );
}

export function TrainCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-ink-200 bg-white p-4">
      <div className="flex justify-between">
        <Skeleton className="h-7 w-20" />
        <Skeleton className="h-6 w-28" />
      </div>
      <div className="mt-4 flex items-center justify-between gap-4">
        <Skeleton className="h-12 w-24" />
        <Skeleton className="h-2 flex-1" />
        <Skeleton className="h-12 w-24" />
      </div>
      <div className="mt-4 flex flex-col gap-2">
        <Skeleton className="h-14 w-full" />
        <Skeleton className="h-14 w-full" />
      </div>
    </div>
  );
}

/** Shared hold button at the bottom of the seat selection screen */
export function StickyActionBar({
  children, note,
}: { children: React.ReactNode; note?: React.ReactNode }) {
  return (
    <div className="sticky bottom-0 -mx-4 mt-6 border-t border-ink-200 bg-white/95 px-4 py-3 backdrop-blur-sm sm:-mx-6 sm:px-6">
      {note && <div className="mb-2">{note}</div>}
      {children}
    </div>
  );
}
