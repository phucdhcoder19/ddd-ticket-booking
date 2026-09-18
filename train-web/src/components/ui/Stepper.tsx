import { cn } from "@/lib/cn";

export const BOOKING_STEPS = ["Tìm chuyến", "Chọn chỗ", "Hành khách", "Thanh toán"] as const;

/**
 * Thanh tiến trình đặt vé. Trên điện thoại chỉ hiện số bước + tên bước hiện tại
 * để không chiếm mất chiều cao quý giá; từ màn hình sm trở lên mới hiện đủ 4 bước.
 */
export function Stepper({ current }: { current: number }) {
  return (
    <nav aria-label="Tiến trình đặt vé" className="w-full">
      <ol className="flex items-center gap-1.5 sm:gap-2">
        {BOOKING_STEPS.map((label, i) => {
          const state = i < current ? "done" : i === current ? "current" : "todo";
          return (
            <li key={label} className={cn("flex items-center gap-1.5", i === current ? "flex-1" : "shrink-0 sm:flex-1")}>
              <span
                aria-hidden
                className={cn(
                  "grid size-7 shrink-0 place-items-center rounded-full text-sm font-bold",
                  state === "done" && "bg-son-600 text-white",
                  state === "current" && "bg-son-600 text-white ring-4 ring-son-200",
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
                {state === "done" ? "(đã xong)" : state === "current" ? "(bước hiện tại)" : "(chưa tới)"}
              </span>
              {i < BOOKING_STEPS.length - 1 && (
                <span aria-hidden className={cn("hidden h-0.5 flex-1 rounded-full sm:block", i < current ? "bg-son-600" : "bg-ink-200")} />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
