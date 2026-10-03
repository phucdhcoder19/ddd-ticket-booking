import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type Tone = "neutral" | "brand" | "accent" | "ok" | "warn" | "info" | "muted";

const TONES: Record<Tone, string> = {
  neutral: "bg-ink-100 text-ink-800 border-ink-200",
  brand: "bg-brand-50 text-brand-700 border-brand-200",
  accent: "bg-accent-50 text-accent-700 border-accent-200",
  ok: "bg-ok-50 text-ok-600 border-ok-600/20",
  warn: "bg-warn-50 text-warn-600 border-warn-600/25",
  info: "bg-info-50 text-info-600 border-info-600/20",
  muted: "bg-ink-100 text-ink-500 border-ink-200",
};

export function Badge({
  tone = "neutral", children, className, icon,
}: { tone?: Tone; children: ReactNode; className?: string; icon?: ReactNode }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-semibold whitespace-nowrap",
        TONES[tone],
        className,
      )}
    >
      {icon && <span aria-hidden>{icon}</span>}
      {children}
    </span>
  );
}

/**
 * Badge for the number of seats left.
 * The warning thresholds are absolute numbers, not percentages: for a
 * traveller, "3 seats left" is what matters, not "2% left".
 */
export function AvailabilityBadge({ available, className }: { available: number; className?: string }) {
  if (available <= 0) {
    return <Badge tone="muted" className={className} icon="✕">Sold out</Badge>;
  }
  if (available <= 10) {
    return <Badge tone="brand" className={className} icon="🔥">Almost gone · {available} left</Badge>;
  }
  if (available <= 30) {
    return <Badge tone="warn" className={className}>{available} seats left</Badge>;
  }
  return <Badge tone="ok" className={className}>{available} seats left</Badge>;
}
