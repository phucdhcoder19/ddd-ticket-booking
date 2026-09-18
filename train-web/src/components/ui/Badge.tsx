import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type Tone = "neutral" | "son" | "mai" | "ok" | "warn" | "info" | "muted";

const TONES: Record<Tone, string> = {
  neutral: "bg-ink-100 text-ink-800 border-ink-200",
  son: "bg-son-50 text-son-700 border-son-200",
  mai: "bg-mai-50 text-mai-700 border-mai-200",
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
 * Badge số chỗ còn lại.
 * Ngưỡng cảnh báo tính theo số tuyệt đối chứ không theo phần trăm: với hành
 * khách, "còn 3 chỗ" mới là thông tin quyết định, chứ không phải "còn 2%".
 */
export function AvailabilityBadge({ available, className }: { available: number; className?: string }) {
  if (available <= 0) {
    return <Badge tone="muted" className={className} icon="✕">Hết vé</Badge>;
  }
  if (available <= 10) {
    return <Badge tone="son" className={className} icon="🔥">Sắp hết · còn {available} chỗ</Badge>;
  }
  if (available <= 30) {
    return <Badge tone="warn" className={className}>Còn {available} chỗ</Badge>;
  }
  return <Badge tone="ok" className={className}>Còn {available} chỗ</Badge>;
}
