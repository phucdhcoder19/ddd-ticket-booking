import type { QueueTicket } from "@/api/types";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Shows the position in the queue.
 *
 * The progress bar measures the DISTANCE COVERED, not what is left: people
 * waiting need to see themselves moving forward. The starting point
 * (positionAtEntry) stays fixed so the bar only moves one way and never goes
 * backwards when the server re-estimates.
 */
export function QueueStatus({
  queue, positionAtEntry,
}: { queue: QueueTicket; positionAtEntry: number }) {
  const progress = positionAtEntry > 0
    ? Math.min(100, Math.max(2, ((positionAtEntry - queue.position) / positionAtEntry) * 100))
    : 100;
  const minutes = Math.ceil(queue.estimatedWaitSeconds / 60);

  return (
    <div className="rounded-3xl border border-ink-200 bg-white p-6 shadow-[var(--shadow-soft)]">
      <div className="text-center">
        <p className="text-sm font-medium text-ink-600">Your position in the queue</p>
        <p className="tnum mt-1 text-5xl font-bold text-brand-600" aria-live="polite">
          {formatNumber(queue.position)}
        </p>
        <p className="mt-1 text-ink-600">
          out of <span className="tnum font-semibold">{formatNumber(queue.total)}</span> people waiting
        </p>
      </div>

      <div className="mt-5">
        <div
          role="progressbar"
          aria-valuenow={Math.round(progress)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Queue progress"
          className="h-3 w-full overflow-hidden rounded-full bg-ink-200"
        >
          <div
            className="h-full rounded-full bg-gradient-to-r from-brand-600 to-accent-400 transition-[width] duration-700 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
        <div className="mt-2 flex items-center justify-between text-sm text-ink-600">
          <span>{Math.round(progress)}% done</span>
          <span className="font-semibold text-ink-800">
            {queue.position === 0
              ? "It's your turn!"
              : minutes <= 1
                ? "Less than 1 minute left"
                : `About ${minutes} minutes left`}
          </span>
        </div>
      </div>
    </div>
  );
}

/**
 * Warning not to reload the page.
 * This is the most common mistake during a holiday ticket rush: the screen
 * looks frozen, people press F5, and lose their place. The warning must be big,
 * right under the queue number, and spell out the consequence instead of just
 * "please do not reload".
 */
export function NoRefreshWarning({ className }: { className?: string }) {
  return (
    <div
      className={cn("flex items-start gap-3 rounded-2xl border-2 border-accent-300 bg-accent-50 p-4", className)}
      role="note"
    >
      <span aria-hidden className="text-2xl">⚠️</span>
      <div>
        <p className="font-bold text-ink-900">Do not reload the page or close this tab</p>
        <p className="mt-1 text-sm text-ink-700">
          If you press F5, close the tab or press back, you will <strong>lose your place</strong> and have to queue
          again from the start. This page updates by itself, you only need to wait.
        </p>
      </div>
    </div>
  );
}
