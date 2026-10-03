import { useCountdown } from "@/hooks/useCountdown";
import { formatClock, formatCountdownParts, formatDateTime } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Countdown to the sale opening (large format: days / hours / minutes / seconds).
 * aria-live="off" on the digits because announcing every second would annoy
 * screen reader users; instead a separate summary line is read per minute.
 */
export function SaleCountdown({
  opensAt, label, onOpen,
}: { opensAt: string; label: string; onOpen?: () => void }) {
  const { remaining, expired } = useCountdown(opensAt, { onExpire: onOpen });
  const { days, hours, minutes, seconds } = formatCountdownParts(remaining);

  if (expired) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-white/15 px-4 py-3 font-semibold text-white">
        <span aria-hidden className="size-2.5 animate-pulse rounded-full bg-accent-200" />
        On sale now — {label}
      </div>
    );
  }

  const cells = [
    { value: days, unit: "days" },
    { value: hours, unit: "hrs" },
    { value: minutes, unit: "min" },
    { value: seconds, unit: "sec" },
  ];

  return (
    <div className="inline-flex flex-wrap items-center gap-x-3 gap-y-1.5 rounded-lg bg-black/25 px-4 py-2.5 backdrop-blur-sm">
      <span className="text-sm font-semibold text-white">{label} opens in</span>

      {/* One compact row, instead of 4 big squares taking half the screen */}
      <span className="flex items-baseline gap-1.5" aria-hidden>
        {cells.map((c, i) => (
          <span key={c.unit} className="flex items-baseline gap-1.5">
            <span className="tnum text-xl font-bold text-white">
              {String(c.value).padStart(2, "0")}
            </span>
            <span className="text-[11px] font-medium text-white/70">{c.unit}</span>
            {i < cells.length - 1 && <span className="text-white/40">·</span>}
          </span>
        ))}
      </span>

      <span className="text-sm text-white/70">· Opens {formatDateTime(opensAt)}</span>

      <p className="sr-only" aria-live="polite">
        {days} days {hours} hours {minutes} minutes until the sale opens, at {formatDateTime(opensAt)}.
      </p>
    </div>
  );
}

/**
 * Hold timer (mm:ss). Changes colour with urgency:
 *  > 3 minutes : neutral — no rushing, so nobody panics while typing an ID number
 *  1–3 minutes : yellow warning
 *  < 1 minute  : red + a gentle pulse
 */
export function HoldCountdown({
  expiresAt, onExpire, className,
}: { expiresAt: string; onExpire?: () => void; className?: string }) {
  const { remaining, seconds, expired } = useCountdown(expiresAt, { onExpire });
  const urgent = remaining < 60;
  const warning = remaining < 180;

  return (
    <div
      className={cn(
        "flex items-center gap-2 rounded-xl border-2 px-3 py-1.5",
        expired && "border-ink-300 bg-ink-100 text-ink-600",
        !expired && urgent && "border-brand-300 bg-brand-50 text-brand-700",
        !expired && !urgent && warning && "border-accent-300 bg-accent-50 text-accent-700",
        !expired && !warning && "border-ink-200 bg-white text-ink-800",
        className,
      )}
    >
      <span aria-hidden className={cn("text-base", urgent && !expired && "animate-pulse")}>⏱</span>
      <div className="leading-tight">
        <div className="text-[11px] font-medium opacity-80">Hold time left</div>
        <div className="tnum text-lg font-bold">{formatClock(seconds)}</div>
      </div>
      {/* Announce the 5, 3 and 1 minute marks instead of every second */}
      <span className="sr-only" aria-live="polite">
        {[300, 180, 60].includes(seconds) && `${seconds / 60} minutes left to complete your booking.`}
        {expired && "Your hold has expired."}
      </span>
    </div>
  );
}
