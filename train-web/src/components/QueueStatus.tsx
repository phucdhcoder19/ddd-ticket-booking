import type { QueueTicket } from "@/api/types";
import { formatNumber } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Hiển thị vị trí trong hàng đợi.
 *
 * Thanh tiến trình đo QUÃNG ĐƯỜNG ĐÃ ĐI được, không phải phần còn lại: người
 * chờ cần thấy mình đang tiến lên. Mốc ban đầu (positionAtEntry) được giữ cố
 * định để thanh chỉ chạy một chiều, không bị lùi khi server ước tính lại.
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
        <p className="text-sm font-medium text-ink-600">Vị trí của bạn trong hàng chờ</p>
        <p className="tnum mt-1 text-5xl font-bold text-son-600" aria-live="polite">
          {formatNumber(queue.position)}
        </p>
        <p className="mt-1 text-ink-600">
          trên tổng số <span className="tnum font-semibold">{formatNumber(queue.total)}</span> người đang chờ
        </p>
      </div>

      <div className="mt-5">
        <div
          role="progressbar"
          aria-valuenow={Math.round(progress)}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Tiến độ hàng chờ"
          className="h-3 w-full overflow-hidden rounded-full bg-ink-200"
        >
          <div
            className="h-full rounded-full bg-gradient-to-r from-son-600 to-mai-400 transition-[width] duration-700 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
        <div className="mt-2 flex items-center justify-between text-sm text-ink-600">
          <span>Đã qua {Math.round(progress)}%</span>
          <span className="font-semibold text-ink-800">
            {queue.position === 0
              ? "Tới lượt bạn rồi!"
              : minutes <= 1
                ? "Còn dưới 1 phút"
                : `Còn khoảng ${minutes} phút`}
          </span>
        </div>
      </div>
    </div>
  );
}

/**
 * Cảnh báo không được tải lại trang.
 * Đây là hiểu lầm phổ biến nhất khi mua vé Tết: người dùng thấy màn hình đứng
 * yên nên bấm F5, và mất lượt. Cảnh báo phải thật to, đặt ngay dưới số thứ tự,
 * và nói rõ hậu quả chứ không chỉ "vui lòng không tải lại".
 */
export function NoRefreshWarning({ className }: { className?: string }) {
  return (
    <div
      className={cn("flex items-start gap-3 rounded-2xl border-2 border-mai-300 bg-mai-50 p-4", className)}
      role="note"
    >
      <span aria-hidden className="text-2xl">⚠️</span>
      <div>
        <p className="font-bold text-ink-900">Đừng tải lại trang và đừng tắt tab này</p>
        <p className="mt-1 text-sm text-ink-700">
          Nếu bạn nhấn F5, đóng tab hoặc bấm nút quay lại, bạn sẽ <strong>mất lượt</strong> và phải xếp hàng lại
          từ đầu. Trang này tự cập nhật, bạn chỉ cần chờ.
        </p>
      </div>
    </div>
  );
}
