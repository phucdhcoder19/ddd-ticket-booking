import { cn } from "@/lib/cn";

export const BOOKING_STEPS = ["Find trip", "Pick seats", "Passengers", "Payment"] as const;

/**
 * Booking progress bar. On phones only the step numbers + the current step
 * name are shown so precious height is not wasted; from sm screens up all 4 steps show.
 */
export function Stepper({ current }: { current: number }) {
  return (
    <nav aria-label="Booking progress" className="w-full">
      <ol className="flex items-center gap-1.5 sm:gap-2">
        {BOOKING_STEPS.map((label, i) => {
          const state = i < current ? "done" : i === current ? "current" : "todo";
          return (
            <li key={label} className={cn("flex items-center gap-1.5", i === current ? "flex-1" : "shrink-0 sm:flex-1")}>
              <span
                aria-hidden
                className={cn(
                  "grid size-7 shrink-0 place-items-center rounded-full text-sm font-bold",
                  state === "done" && "bg-brand-600 text-white",
                  state === "current" && "bg-brand-600 text-white ring-4 ring-brand-200",
                  state === "todo" && "bg-ink-200 text-ink-600",
                )}
              >
                {state === "done" ? "✓" : i + 1}
              </span>
              <span
                className={cn(
                  "truncate text-sm font-semibold",
                  state === "current" ? "text-ink-900" : "hidden text-ink-500 sm:inline",
                )}
              >
                {label}
              </span>
              <span className="sr-only">
                {state === "done" ? "(done)" : state === "current" ? "(current step)" : "(not yet)"}
              </span>
              {i < BOOKING_STEPS.length - 1 && (
                <span aria-hidden className={cn("hidden h-0.5 flex-1 rounded-full sm:block", i < current ? "bg-brand-600" : "bg-ink-200")} />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
