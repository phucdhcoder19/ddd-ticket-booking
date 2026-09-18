import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import type { QueueTicket } from "@/api/types";
import { NoRefreshWarning, QueueStatus } from "@/components/QueueStatus";
import { Button } from "@/components/ui/Button";
import { ErrorState, Skeleton } from "@/components/ui/StateView";
import { useBooking } from "@/store/BookingContext";
import { formatNumber } from "@/lib/format";

const QUEUE_KEY = "vetau.queue.token";

/**
 * Màn 2 — Phòng chờ.
 *
 * Bốn điều phải làm đúng, nếu không người dùng sẽ mất lượt:
 *
 * 1. Token lượt chờ lưu ở sessionStorage NGAY khi nhận được. Lỡ F5 thì vẫn
 *    quay lại đúng vị trí cũ chứ không xếp hàng lại.
 * 2. beforeunload cảnh báo trước khi đóng tab hoặc tải lại.
 * 3. Poll bằng setTimeout đệ quy, không phải setInterval: nếu một lần gọi
 *    chậm (mạng yếu) thì không bị dồn nhiều request chồng lên nhau.
 * 4. Màn hình luôn CÓ CHUYỂN ĐỘNG (số nhích, thanh chạy) để người dùng tin là
 *    hệ thống còn sống — đây chính là lý do chính khiến người ta bấm F5.
 */
export function WaitingRoomPage() {
  const navigate = useNavigate();
  const { query } = useBooking();
  const [queue, setQueue] = useState<QueueTicket | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [entryPosition, setEntryPosition] = useState<number | null>(null);
  const timerRef = useRef<number | null>(null);

  // Vào thẳng phòng chờ mà chưa tìm chuyến thì quay về trang chủ
  useEffect(() => {
    if (!query) navigate("/", { replace: true });
  }, [query, navigate]);

  // Cảnh báo trước khi rời trang
  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (queue?.status === "WAITING") e.preventDefault();
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [queue?.status]);

  useEffect(() => {
    let cancelled = false;

    const schedule = (token: string, delay: number) => {
      timerRef.current = window.setTimeout(() => void poll(token), delay);
    };

    const poll = async (token: string) => {
      try {
        const next = await api.getQueueStatus(token);
        if (cancelled) return;
        setEntryPosition((p) => p ?? next.position);
        setQueue(next);
        setError(null);
        if (next.status === "ADMITTED") {
          sessionStorage.removeItem(QUEUE_KEY);
          navigate("/chuyen-tau", { replace: true });
          return;
        }
        // Gần tới lượt thì hỏi dày hơn để không bỏ lỡ thời điểm được gọi
        schedule(token, next.position < 80 ? 1200 : 3000);
      } catch (e) {
        if (cancelled) return;
        // Lỗi tạm thời không đá người dùng ra khỏi hàng — cứ thử lại
        setError(e);
        schedule(token, 5000);
      }
    };

    const start = async () => {
      try {
        const saved = sessionStorage.getItem(QUEUE_KEY);
        const ticket = saved ? await api.getQueueStatus(saved).catch(() => api.joinQueue()) : await api.joinQueue();
        if (cancelled) return;
        sessionStorage.setItem(QUEUE_KEY, ticket.token);
        setEntryPosition((p) => p ?? ticket.position);
        setQueue(ticket);
        if (ticket.status === "ADMITTED") {
          sessionStorage.removeItem(QUEUE_KEY);
          navigate("/chuyen-tau", { replace: true });
          return;
        }
        schedule(ticket.token, 2000);
      } catch (e) {
        if (!cancelled) setError(e);
      }
    };

    void start();
    return () => {
      cancelled = true;
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [navigate]);

  const leaveQueue = () => {
    sessionStorage.removeItem(QUEUE_KEY);
    navigate("/", { replace: true });
  };

  if (!queue && error) {
    return (
      <div className="py-8">
        <ErrorState error={error} onRetry={() => window.location.reload()} retryLabel="Vào lại hàng chờ" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 py-2">
      <div className="text-center">
        <span aria-hidden className="text-4xl">🏮</span>
        <h1 className="mt-2 text-2xl font-bold">Bạn đang trong hàng chờ</h1>
        <p className="mt-1 text-ink-600">
          Rất nhiều người cùng đặt vé lúc này. Chúng tôi xếp lượt theo thứ tự để ai cũng có cơ hội công bằng.
        </p>
      </div>

      {queue ? (
        <QueueStatus queue={queue} positionAtEntry={entryPosition ?? queue.position} />
      ) : (
        <div className="rounded-3xl border border-ink-200 bg-white p-6">
          <Skeleton className="mx-auto h-4 w-40" />
          <Skeleton className="mx-auto mt-3 h-12 w-32" />
          <Skeleton className="mt-6 h-3 w-full" />
          <p className="sr-only" role="status">Đang lấy số thứ tự của bạn…</p>
        </div>
      )}

      <NoRefreshWarning />

      {error ? (
        <p role="status" className="rounded-xl border border-mai-200 bg-mai-50 px-4 py-3 text-sm text-ink-700">
          Kết nối đang chập chờn nên số thứ tự tạm thời chưa cập nhật. <strong>Lượt của bạn vẫn được giữ</strong>,
          chúng tôi đang tự động thử lại.
        </p>
      ) : null}

      <div className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 className="font-semibold text-ink-900">Trong lúc chờ, bạn có thể chuẩn bị</h2>
        <ul className="mt-2 flex flex-col gap-1.5 text-sm text-ink-700">
          <li>• Số CCCD của tất cả hành khách đi cùng</li>
          <li>• Thẻ ngân hàng hoặc ví điện tử đã sẵn sàng thanh toán</li>
          <li>• Chọn sẵn phương án dự phòng: chuyến khác hoặc ngày liền kề</li>
        </ul>
        {queue && queue.total > 0 && (
          <p className="mt-3 text-sm text-ink-500">
            Hiện có <span className="tnum font-semibold">{formatNumber(queue.total)}</span> người đang chờ cùng bạn.
          </p>
        )}
      </div>

      <Button variant="ghost" fullWidth onClick={leaveQueue}>
        Rời hàng chờ và quay lại trang chủ
      </Button>
    </div>
  );
}
