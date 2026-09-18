import { useCountdown } from "@/hooks/useCountdown";
import { formatClock, formatCountdownParts, formatDateTime } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Đếm ngược tới giờ mở bán (dạng lớn: ngày / giờ / phút / giây).
 * aria-live="off" cho khối số vì đọc từng giây sẽ làm phiền người dùng
 * trình đọc màn hình; thay vào đó có một dòng tóm tắt riêng đọc theo phút.
 */
export function SaleCountdown({
  opensAt, label, onOpen,
}: { opensAt: string; label: string; onOpen?: () => void }) {
  const { remaining, expired } = useCountdown(opensAt, { onExpire: onOpen });
  const { days, hours, minutes, seconds } = formatCountdownParts(remaining);

  if (expired) {
    return (
      <div className="flex items-center gap-2 rounded-xl bg-white/15 px-4 py-3 font-semibold text-white">
        <span aria-hidden className="size-2.5 animate-pulse rounded-full bg-mai-200" />
        Đang mở bán — {label}
      </div>
    );
  }

  const cells = [
    { value: days, unit: "ngày" },
    { value: hours, unit: "giờ" },
    { value: minutes, unit: "phút" },
    { value: seconds, unit: "giây" },
  ];

  return (
    <div>
      <p className="text-sm font-medium text-mai-100">{label} mở bán sau</p>
      <div className="mt-2 flex items-center gap-1.5" aria-hidden>
        {cells.map((c, i) => (
          <div key={c.unit} className="flex items-center gap-1.5">
            <div className="rounded-xl bg-white/15 px-2.5 py-1.5 text-center backdrop-blur-sm">
              <div className="tnum text-2xl font-bold text-white sm:text-3xl">
                {String(c.value).padStart(2, "0")}
              </div>
              <div className="text-[11px] font-medium text-mai-100">{c.unit}</div>
            </div>
            {i < cells.length - 1 && <span className="pb-4 text-xl font-bold text-white/50">:</span>}
          </div>
        ))}
      </div>
      <p className="sr-only" aria-live="polite">
        Còn {days} ngày {hours} giờ {minutes} phút nữa tới giờ mở bán, lúc {formatDateTime(opensAt)}.
      </p>
      <p className="mt-2 text-sm text-mai-100">Mở bán lúc {formatDateTime(opensAt)}</p>
    </div>
  );
}

/**
 * Đồng hồ giữ chỗ (mm:ss). Đổi màu theo mức khẩn cấp:
 *  > 3 phút : trung tính — không hối thúc, tránh gây hoảng khi đang nhập CCCD
 *  1–3 phút : vàng cảnh báo
 *  < 1 phút : đỏ + nhấp nháy nhẹ
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
        !expired && urgent && "border-son-300 bg-son-50 text-son-700",
        !expired && !urgent && warning && "border-mai-300 bg-mai-50 text-mai-700",
        !expired && !warning && "border-ink-200 bg-white text-ink-800",
        className,
      )}
    >
      <span aria-hidden className={cn("text-base", urgent && !expired && "animate-pulse")}>⏱</span>
      <div className="leading-tight">
        <div className="text-[11px] font-medium opacity-80">Giữ chỗ còn</div>
        <div className="tnum text-lg font-bold">{formatClock(seconds)}</div>
      </div>
      {/* Đọc mốc 5, 3, 1 phút thay vì đọc từng giây */}
      <span className="sr-only" aria-live="polite">
        {[300, 180, 60].includes(seconds) && `Còn ${seconds / 60} phút để hoàn tất đặt vé.`}
        {expired && "Đã hết thời gian giữ chỗ."}
      </span>
    </div>
  );
}
