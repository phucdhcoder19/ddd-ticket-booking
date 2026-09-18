import { useNavigate } from "react-router-dom";
import { HoldCountdown } from "./CountdownTimer";
import { Button } from "./ui/Button";
import { Modal } from "./ui/Modal";
import type { Hold } from "@/api/types";
import { formatVnd } from "@/lib/format";

/**
 * Thanh giữ chỗ dính trên đầu màn 5 và 6.
 *
 * Đồng hồ phải LUÔN nhìn thấy trong suốt lúc nhập liệu, nên thanh này dính
 * (sticky) ngay dưới header. Nó cũng nhắc lại đang giữ chỗ nào — người dùng
 * nhập CCCD cho 3 người rất dễ quên mình đang ở toa nào, ghế nào.
 */
export function HoldBar({
  hold, total, onExpire,
}: { hold: Hold; total: number; onExpire: () => void }) {
  return (
    <div className="sticky top-16 z-20 -mx-4 border-b border-ink-200 bg-canvas/95 px-4 py-2.5 backdrop-blur-sm sm:-mx-6 sm:px-6">
      <div className="flex items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ink-900">
            Đang giữ {hold.items.length} chỗ
          </p>
          <p className="truncate text-xs text-ink-600">
            {hold.items.map((i) => `Toa ${i.carriageNumber} · chỗ ${i.seatLabel}`).join(" · ")}
          </p>
          <p className="tnum text-xs font-semibold text-son-700">{formatVnd(total)}</p>
        </div>
        <HoldCountdown expiresAt={hold.expiresAt} onExpire={onExpire} />
      </div>
    </div>
  );
}

/**
 * Hộp thoại khi hết giờ giữ chỗ.
 *
 * Không cho đóng bằng Esc hay bấm ra ngoài: người dùng bắt buộc phải chọn một
 * hướng đi tiếp, vì mọi dữ liệu đang nhập dở đã không còn giá trị. Giọng văn
 * nhận lỗi về phía hệ thống chứ không trách người dùng chậm.
 */
export function HoldExpiredDialog({ open, tripId }: { open: boolean; tripId?: string | null }) {
  const navigate = useNavigate();
  return (
    <Modal
      open={open}
      onClose={() => {}}
      dismissible={false}
      title="Đã hết thời gian giữ chỗ"
      description="Sau 10 phút, chỗ được trả lại cho hành khách khác để ai cũng có cơ hội mua vé. Thông tin bạn vừa nhập vẫn được giữ tạm, bạn chỉ cần chọn lại chỗ."
      footer={
        <>
          <Button variant="secondary" onClick={() => navigate("/chuyen-tau")}>
            Xem chuyến khác
          </Button>
          <Button
            onClick={() => navigate(tripId ? `/chon-cho/${encodeURIComponent(tripId)}` : "/chuyen-tau")}
          >
            Chọn lại chỗ trên chuyến này
          </Button>
        </>
      }
    />
  );
}
