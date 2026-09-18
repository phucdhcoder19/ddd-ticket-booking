import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import type { Ticket } from "@/api/types";
import { TicketCard, TicketCardSkeleton } from "@/components/TicketCard";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { EmptyState, ErrorState, LoadingRegion } from "@/components/ui/StateView";
import { useAsync } from "@/hooks/useAsync";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useToast } from "@/store/ToastContext";
import { friendlyMessage } from "@/api/errors";
import { formatVnd, parseIso } from "@/lib/format";
import { cn } from "@/lib/cn";

type Tab = "upcoming" | "past";

/**
 * Màn 7 — Vé của tôi.
 *
 * Tách "Sắp đi" và "Đã đi / đã trả": ngay sau khi mua, thứ người dùng cần là
 * tấm vé sắp dùng, không phải lịch sử. Trả vé luôn hỏi xác nhận kèm SỐ TIỀN
 * HOÀN CỤ THỂ — trả vé tàu Tết bị trừ phí, nói mập mờ là nguồn khiếu nại lớn nhất.
 */
export function MyTicketsPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const [tab, setTab] = useState<Tab>("upcoming");
  const [confirming, setConfirming] = useState<Ticket | null>(null);

  const tickets = useAsync((o) => api.getMyTickets(o), []);

  const { submit: doRefund, pending: refunding } = useSubmitLock(async () => {
    if (!confirming) return;
    try {
      await api.refundTicket(confirming.id);
      toast.show({
        tone: "success",
        title: "Đã tiếp nhận yêu cầu trả vé",
        detail: `Tiền hoàn sẽ về tài khoản trong 3–5 ngày làm việc.`,
      });
      setConfirming(null);
      void tickets.refreshSilently();
    } catch (e) {
      const m = friendlyMessage(e);
      toast.show({ tone: "error", title: m.title, detail: m.detail });
      throw e;
    }
  });

  const all = tickets.data ?? [];
  const now = Date.now();
  const upcoming = all.filter((t) => t.status === "VALID" && parseIso(t.departAt).getTime() > now);
  const past = all.filter((t) => !upcoming.includes(t));
  const visible = tab === "upcoming" ? upcoming : past;

  return (
    <div className="flex flex-col gap-4">
      <div>
        <h1 className="text-xl font-bold">Vé của tôi</h1>
        <p className="mt-1 text-sm text-ink-600">
          Chìa mã QR cho nhân viên soát vé khi lên tàu. Nên chụp màn hình phòng khi ga không có sóng.
        </p>
      </div>

      <div className="flex gap-2" role="tablist" aria-label="Lọc vé">
        {([
          { key: "upcoming" as const, label: `Sắp đi (${upcoming.length})` },
          { key: "past" as const, label: `Đã đi & đã trả (${past.length})` },
        ]).map((t) => (
          <button
            key={t.key}
            type="button"
            role="tab"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={cn(
              "min-h-11 flex-1 rounded-xl border-2 px-3 text-sm font-semibold transition-colors",
              tab === t.key
                ? "border-son-600 bg-son-600 text-white"
                : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tickets.loading && tickets.isInitialLoad ? (
        <LoadingRegion label="Đang tải danh sách vé của bạn…">
          <div className="flex flex-col gap-3">
            <TicketCardSkeleton />
            <TicketCardSkeleton />
          </div>
        </LoadingRegion>
      ) : tickets.error && all.length === 0 ? (
        <ErrorState error={tickets.error} onRetry={tickets.reload} />
      ) : visible.length === 0 ? (
        <EmptyState
          icon={tab === "upcoming" ? "🎫" : "🗂"}
          title={tab === "upcoming" ? "Bạn chưa có vé nào sắp đi" : "Chưa có vé cũ"}
          detail={
            tab === "upcoming"
              ? "Vé sau khi thanh toán thành công sẽ xuất hiện ở đây kèm mã QR."
              : "Những vé đã sử dụng hoặc đã trả sẽ được lưu lại ở mục này."
          }
          action={tab === "upcoming" ? <Button onClick={() => navigate("/")}>Đặt vé tàu Tết</Button> : undefined}
        />
      ) : (
        <ul className="flex flex-col gap-3">
          {visible.map((t) => (
            <li key={t.id}>
              <TicketCard
                ticket={t}
                onRefund={setConfirming}
                refunding={refunding && confirming?.id === t.id}
              />
            </li>
          ))}
        </ul>
      )}

      {/* Xác nhận trả vé, nói rõ phí và số tiền thực nhận */}
      <Modal
        open={confirming !== null}
        onClose={() => setConfirming(null)}
        title="Bạn chắc chắn muốn trả vé?"
        description="Sau khi trả, chỗ này được bán lại ngay cho hành khách khác và bạn không thể lấy lại."
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(null)}>Không, giữ vé</Button>
            <Button
              variant="primary"
              loading={refunding}
              loadingText="Đang xử lý…"
              onClick={() => void doRefund().catch(() => {})}
            >
              Xác nhận trả vé
            </Button>
          </>
        }
      >
        {confirming && (
          <dl className="rounded-xl border border-ink-200 bg-ink-50 p-3 text-sm">
            <div className="flex justify-between py-1">
              <dt className="text-ink-600">Giá vé đã mua</dt>
              <dd className="tnum font-semibold">{formatVnd(confirming.price)}</dd>
            </div>
            <div className="flex justify-between py-1">
              <dt className="text-ink-600">Phí trả vé (20% trong dịp Tết)</dt>
              <dd className="tnum font-semibold text-son-700">−{formatVnd(Math.round(confirming.price * 0.2))}</dd>
            </div>
            <div className="mt-1 flex justify-between border-t border-ink-200 pt-2">
              <dt className="font-bold text-ink-900">Bạn nhận lại</dt>
              <dd className="tnum font-bold text-ink-900">{formatVnd(Math.round(confirming.price * 0.8))}</dd>
            </div>
          </dl>
        )}
      </Modal>
    </div>
  );
}
