import { useRef } from "react";
import type { Carriage, Seat } from "@/api/types";
import { formatVnd } from "@/lib/format";
import { cn } from "@/lib/cn";
import { Skeleton } from "./ui/StateView";

/**
 * Carriage seat map.
 *
 * Four key design decisions:
 *
 * 1. NOT COLOUR ALONE to tell states apart. Each state also has its own mark
 *    (✓ selected, ✕ sold, ⏳ held by someone else) and a different border, so
 *    colour-blind people — about 8% of men — can still use it.
 *
 * 2. Seats are real <button>s, not <div onClick>. So Tab/Enter/Space work out
 *    of the box, and each seat has a full aria-label: "Seat 12, level 1, ₫1,250,000, available".
 *
 * 3. Seat cells are at least 44×44px even on small screens; long carriages
 *    scroll sideways instead of shrinking seats — tapping the wrong seat while
 *    the network is busy wastes a lot of time.
 *
 * 4. Seats just snatched by someone else are marked separately
 *    (justTakenSeatIds), and suggested replacements get a pulsing ring so the
 *    eye finds them right away.
 */
export type SeatMapProps = {
  carriage: Carriage;
  selectedSeatIds: string[];
  onToggleSeat: (seat: Seat) => void;
  /** Max number of seats that can be selected (= number of passengers) */
  maxSelectable: number;
  /** Seats lost in the latest failed hold attempt */
  justTakenSeatIds?: string[];
  /** Seats the system suggests instead */
  suggestedSeatIds?: string[];
};

export function SeatMap({
  carriage, selectedSeatIds, onToggleSeat, maxSelectable, justTakenSeatIds = [], suggestedSeatIds = [],
}: SeatMapProps) {
  const isBerth = carriage.layout !== "seat-2-2";
  const gridRef = useRef<HTMLDivElement>(null);

  // Group seats by row (seating carriage) or by compartment (sleeper carriage)
  const groups = new Map<number, Seat[]>();
  for (const s of carriage.seats) {
    const key = s.row;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(s);
  }

  return (
    <div>
      <SeatLegend />
      <div className="mt-3 overflow-x-auto pb-2">
        <div
          ref={gridRef}
          role="group"
          aria-label={`Map of carriage ${carriage.number}, ${carriage.available} seats available`}
          className="mx-auto w-fit rounded-3xl border-2 border-ink-300 bg-ink-50 p-3"
        >
          {/* Carriage ends — helps users tell the front from the back */}
          <div className="mb-2 flex items-center justify-between px-2 text-xs font-semibold text-ink-500">
            <span aria-hidden>◄ Front of train</span>
            <span>Carriage {carriage.number}</span>
            <span aria-hidden>Rear of train ►</span>
          </div>

          <div className="flex flex-col gap-1.5">
            {[...groups.entries()].map(([rowKey, seats]) => (
              <div key={rowKey} className="flex items-center gap-2">
                <span className="w-10 shrink-0 text-right text-xs font-medium text-ink-500" aria-hidden>
                  {isBerth ? `C.${rowKey}` : `R.${rowKey}`}
                </span>
                <div className={cn("flex gap-1.5", isBerth && "rounded-xl bg-white/70 p-1.5 ring-1 ring-ink-200")}>
                  {seats.map((seat, i) => (
                    <span key={seat.id} className="flex items-center gap-1.5">
                      <SeatButton
                        seat={seat}
                        selected={selectedSeatIds.includes(seat.id)}
                        justTaken={justTakenSeatIds.includes(seat.id)}
                        suggested={suggestedSeatIds.includes(seat.id)}
                        disabledByLimit={
                          selectedSeatIds.length >= maxSelectable && !selectedSeatIds.includes(seat.id)
                        }
                        onToggle={onToggleSeat}
                      />
                      {/* Aisle: between the two seat pairs (seating) or between the two berth stacks */}
                      {i === seats.length / 2 - 1 && (
                        <span aria-hidden className="mx-0.5 h-10 w-px bg-ink-300" />
                      )}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      {isBerth && (
        <p className="mt-2 text-center text-xs text-ink-500">
          Each frame is one compartment. Lower berths are more convenient, so they cost more than middle and upper ones.
        </p>
      )}
    </div>
  );
}

function SeatButton({
  seat, selected, justTaken, suggested, disabledByLimit, onToggle,
}: {
  seat: Seat;
  selected: boolean;
  justTaken: boolean;
  suggested: boolean;
  disabledByLimit: boolean;
  onToggle: (seat: Seat) => void;
}) {
  const unavailable = seat.status !== "available";
  const disabled = unavailable || disabledByLimit;

  const statusText = selected
    ? "selected by you"
    : seat.status === "sold"
      ? "sold"
      : seat.status === "held"
        ? justTaken
          ? "just taken by another passenger"
          : "held by another passenger"
        : disabledByLimit
          ? "available, but you have already picked enough seats"
          : "available";

  const mark = selected ? "✓" : seat.status === "sold" ? "✕" : seat.status === "held" ? "⏳" : null;

  return (
    <button
      type="button"
      disabled={disabled}
      aria-pressed={selected}
      onClick={() => onToggle(seat)}
      title={`Seat ${seat.label} — ${formatVnd(seat.price)}`}
      aria-label={[
        `Seat ${seat.label}`,
        seat.berthLevel ? `level ${seat.berthLevel}` : null,
        formatVnd(seat.price),
        statusText,
      ].filter(Boolean).join(", ")}
      className={cn(
        "relative grid size-11 shrink-0 place-items-center rounded-lg border-2 text-sm font-bold transition-all",
        // available
        !unavailable && !selected && !disabledByLimit &&
          "border-ok-600/40 bg-white text-ink-900 hover:border-brand-500 hover:bg-brand-50 active:scale-95",
        // selected — inverted colours so it stands out most on the map
        selected && "border-brand-700 bg-brand-600 text-white shadow-[var(--shadow-soft)]",
        // held by someone else
        seat.status === "held" && !selected && "cursor-not-allowed border-accent-300 bg-accent-50 text-accent-700",
        // sold
        seat.status === "sold" && "cursor-not-allowed border-ink-200 bg-ink-200 text-ink-500",
        // free, but enough seats are already picked
        !unavailable && disabledByLimit && !selected && "cursor-not-allowed border-ink-200 bg-white text-ink-400",
        justTaken && "animate-pulse ring-3 ring-brand-400",
        suggested && "ring-3 ring-accent-400 ring-offset-1",
      )}
    >
      <span className="leading-none">{seat.label}</span>
      {mark && (
        <span aria-hidden className="absolute -right-0.5 -top-0.5 text-[10px] leading-none">
          {mark}
        </span>
      )}
      {seat.berthLevel && (
        <span aria-hidden className="absolute bottom-0.5 text-[9px] font-medium opacity-70">
          L{seat.berthLevel}
        </span>
      )}
    </button>
  );
}

export function SeatLegend() {
  const items = [
    { cls: "border-ok-600/40 bg-white", mark: "", label: "Available" },
    { cls: "border-brand-700 bg-brand-600 text-white", mark: "✓", label: "Your selection" },
    { cls: "border-accent-300 bg-accent-50 text-accent-700", mark: "⏳", label: "Held by someone else" },
    { cls: "border-ink-200 bg-ink-200 text-ink-500", mark: "✕", label: "Sold" },
  ];
  return (
    <ul className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2">
      {items.map((it) => (
        <li key={it.label} className="flex items-center gap-1.5 text-sm text-ink-700">
          <span aria-hidden className={cn("grid size-6 place-items-center rounded-md border-2 text-[10px] font-bold", it.cls)}>
            {it.mark}
          </span>
          {it.label}
        </li>
      ))}
    </ul>
  );
}

export function SeatMapSkeleton() {
  return (
    <div className="mx-auto w-fit rounded-3xl border-2 border-ink-200 bg-ink-50 p-3">
      <div className="flex flex-col gap-1.5">
        {Array.from({ length: 8 }).map((_, r) => (
          <div key={r} className="flex gap-1.5">
            {Array.from({ length: 5 }).map((_, c) => (
              <Skeleton key={c} className="size-11 rounded-lg" />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
