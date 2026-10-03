import type { ReactNode } from "react";
import { Button } from "./Button";
import { friendlyMessage } from "@/api/errors";
import { cn } from "@/lib/cn";

/** Rounded grey block used to build skeletons */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cn("animate-pulse rounded-lg bg-ink-200/70", className)} />;
}

/**
 * Loading region. Always includes hidden text for screen readers, because a
 * shapes-only skeleton cannot tell a blind user whether it is loading or empty.
 */
export function LoadingRegion({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div role="status" aria-busy="true" aria-live="polite">
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

export function EmptyState({
  icon = "🔍", title, detail, action,
}: { icon?: string; title: string; detail?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border-2 border-dashed border-ink-200 bg-white/60 px-6 py-12 text-center">
      <span aria-hidden className="text-4xl">{icon}</span>
      <h3 className="text-lg font-bold text-ink-900">{title}</h3>
      {detail && <p className="max-w-sm text-ink-600">{detail}</p>}
      {action}
    </div>
  );
}

/**
 * Shows errors in the user's language, with an action to fix them.
 * Every screen uses this component, so errors always look the same.
 */
export function ErrorState({
  error, onRetry, retryLabel, compact = false,
}: { error: unknown; onRetry?: () => void; retryLabel?: string; compact?: boolean }) {
  const { title, detail, action } = friendlyMessage(error);
  return (
    <div
      role="alert"
      className={cn(
        "rounded-2xl border-2 border-brand-200 bg-brand-50 text-center",
        compact ? "px-4 py-4" : "px-6 py-10",
      )}
    >
      <span aria-hidden className="text-3xl">😔</span>
      <h3 className="mt-2 text-lg font-bold text-ink-900">{title}</h3>
      <p className="mx-auto mt-1 max-w-sm text-ink-700">{detail}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          {retryLabel ?? action ?? "Try again"}
        </Button>
      )}
    </div>
  );
}

/**
 * Banner shown while the http layer retries automatically (429 / timeout).
 * Users need to know the system is STILL working, otherwise they keep clicking again.
 */
export function RetryBanner({ attempt, maxAttempts }: { attempt: number; maxAttempts: number }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center gap-3 rounded-xl border border-accent-300 bg-accent-50 px-4 py-3 text-sm font-medium text-ink-800"
    >
      <span aria-hidden className="size-2.5 shrink-0 animate-ping rounded-full bg-accent-400" />
      <span>
        The system is very busy, we are retrying for you automatically (attempt {attempt}/{maxAttempts}). Please keep this page open.
      </span>
    </div>
  );
}
