import type { ReactNode } from "react";
import { Button } from "./Button";
import { friendlyMessage } from "@/api/errors";
import { cn } from "@/lib/cn";

/** Khối xám bo góc dùng dựng skeleton */
export function Skeleton({ className }: { className?: string }) {
  return <div aria-hidden className={cn("animate-pulse rounded-lg bg-ink-200/70", className)} />;
}

/**
 * Vùng chờ tải. Luôn kèm text ẩn cho trình đọc màn hình, vì skeleton thuần
 * hình khối thì người dùng khiếm thị không biết là đang tải hay đã trống.
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
 * Hiển thị lỗi theo ngôn ngữ của người dùng, kèm hành động khắc phục.
 * Mọi màn hình dùng chung component này nên cách báo lỗi luôn nhất quán.
 */
export function ErrorState({
  error, onRetry, retryLabel, compact = false,
}: { error: unknown; onRetry?: () => void; retryLabel?: string; compact?: boolean }) {
  const { title, detail, action } = friendlyMessage(error);
  return (
    <div
      role="alert"
      className={cn(
        "rounded-2xl border-2 border-son-200 bg-son-50 text-center",
        compact ? "px-4 py-4" : "px-6 py-10",
      )}
    >
      <span aria-hidden className="text-3xl">😔</span>
      <h3 className="mt-2 text-lg font-bold text-ink-900">{title}</h3>
      <p className="mx-auto mt-1 max-w-sm text-ink-700">{detail}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          {retryLabel ?? action ?? "Thử lại"}
        </Button>
      )}
    </div>
  );
}

/**
 * Dải băng hiện khi tầng http đang tự động thử lại (429 / timeout).
 * Người dùng cần biết hệ thống VẪN đang làm việc, nếu không họ sẽ bấm lại liên tục.
 */
export function RetryBanner({ attempt, maxAttempts }: { attempt: number; maxAttempts: number }) {
  return (
    <div
      role="status"
      aria-live="polite"
      className="flex items-center gap-3 rounded-xl border border-mai-300 bg-mai-50 px-4 py-3 text-sm font-medium text-ink-800"
    >
      <span aria-hidden className="size-2.5 shrink-0 animate-ping rounded-full bg-mai-400" />
      <span>
        Hệ thống đang rất đông, chúng tôi tự động thử lại giúp bạn (lần {attempt}/{maxAttempts}). Xin đừng tắt trang.
      </span>
    </div>
  );
}
