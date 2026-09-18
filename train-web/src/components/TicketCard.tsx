import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { SEAT_CLASS_SHORT, type Ticket, type TicketStatus } from "@/api/types";
import { Badge } from "./ui/Badge";
import { Button } from "./ui/Button";
import { Skeleton } from "./ui/StateView";
import { formatDate, formatTime, formatVnd, maskIdNumber, weekdayLabel, parseIso } from "@/lib/format";
import { cn } from "@/lib/cn";

const STATUS: Record<TicketStatus, { label: string; tone: "ok" | "muted" | "warn" | "info" }> = {
  VALID: { label: "Còn hiệu lực", tone: "ok" },
  USED: { label: "Đã sử dụng", tone: "muted" },
  REFUNDED: { label: "Đã trả vé", tone: "muted" },
  EXCHANGING: { label: "Đang đổi vé", tone: "warn" },
};

/**
 * Thẻ vé.
 *
 * Mã QR hiện luôn ở kích thước lớn ngay trên thẻ chứ không giấu sau một lần
 * bấm: ở cửa soát vé, người dùng cần chìa màn hình ra trong một giây, và sóng
 * ở ga tàu ngày Tết thì rất yếu. Vé đã trả hoặc đã dùng thì QR mờ đi và có
 * dải chữ đè lên, để không ai chìa nhầm vé cũ.
 */
export function TicketCard({
  ticket, onRefund, refunding,
}: { ticket: Ticket; onRefund?: (t: Ticket) => void; refunding?: boolean }) {
  const [zoomed, setZoomed] = useState(false);
  const status = STATUS[ticket.status];
  const inactive = ticket.status === "REFUNDED" || ticket.status === "USED";
  const departDate = parseIso(ticket.departAt);

  return (
    <article
      className={cn(
        "overflow-hidden rounded-2xl border bg-white shadow-[var(--shadow-soft)]",
        inactive ? "border-ink-200 opacity-80" : "border-son-200",
      )}
      aria-label={`Vé ${ticket.code}, tàu ${ticket.trainCode}`}
    >
      <div className="bg-mai-pattern flex items-center justify-between gap-2 bg-son-700 px-4 py-2.5 text-white">
        <span className="font-bold">{ticket.trainCode}</span>
        <Badge tone={status.tone} className="border-white/30 bg-white/15 text-white">{status.label}</Badge>
      </div>

      <div className="flex items-start gap-4 p-4">
        <div className="min-w-0 flex-1">
          <div className="flex items-baseline gap-2">
            <span className="tnum text-xl font-bold text-ink-900">{formatTime(ticket.departAt)}</span>
            <span className="text-sm text-ink-600">{weekdayLabel(departDate)}, {formatDate(departDate)}</span>
          </div>
          <p className="mt-0.5 font-semibold text-ink-900">
            {ticket.fromStation} → {ticket.toStation}
          </p>
          <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-sm">
            <Item label="Toa" value={String(ticket.carriageNumber)} />
            <Item label="Chỗ" value={ticket.seatLabel} />
            <Item label="Loại chỗ" value={SEAT_CLASS_SHORT[ticket.seatClass]} />
            <Item label="Giá vé" value={formatVnd(ticket.price)} />
          </dl>
          <p className="mt-2 truncate text-sm font-semibold text-ink-900">{ticket.passengerName}</p>
          {ticket.passengerId && (
            <p className="tnum text-xs text-ink-500">CCCD {maskIdNumber(ticket.passengerId)}</p>
          )}
        </div>

        <button
          type="button"
          onClick={() => setZoomed((v) => !v)}
          aria-label={zoomed ? "Thu nhỏ mã QR" : "Phóng to mã QR để soát vé"}
          className="relative shrink-0 rounded-xl border-2 border-ink-200 bg-white p-1.5"
        >
          <QRCodeSVG
            value={ticket.qrPayload}
            size={zoomed ? 160 : 88}
            level="M"
            className={cn("transition-all", inactive && "opacity-25")}
          />
          {inactive && (
            <span className="absolute inset-0 grid place-items-center">
              <span className="rotate-[-12deg] rounded-md bg-ink-800 px-2 py-0.5 text-[10px] font-bold text-white">
                {ticket.status === "REFUNDED" ? "ĐÃ TRẢ" : "ĐÃ DÙNG"}
              </span>
            </span>
          )}
        </button>
      </div>

      <div className="flex items-center justify-between gap-2 border-t border-dashed border-ink-200 bg-ink-50 px-4 py-2.5">
        <span className="tnum text-xs text-ink-500">Mã vé {ticket.code}</span>
        {ticket.status === "VALID" && onRefund && (
          <div className="flex gap-2">
            <Button variant="ghost" className="min-h-10 px-3 text-sm" onClick={() => alert("Chức năng đổi vé đang được hoàn thiện.")}>
              Đổi vé
            </Button>
            <Button
              variant="danger"
              className="min-h-10 px-3 text-sm"
              loading={refunding}
              loadingText="Đang xử lý…"
              onClick={() => onRefund(ticket)}
            >
              Trả vé
            </Button>
          </div>
        )}
      </div>
    </article>
  );
}

function Item({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-ink-500">{label}</dt>
      <dd className="font-semibold text-ink-900">{value}</dd>
    </div>
  );
}

export function TicketCardSkeleton() {
  return (
    <div className="overflow-hidden rounded-2xl border border-ink-200 bg-white">
      <Skeleton className="h-10 w-full rounded-none" />
      <div className="flex gap-4 p-4">
        <div className="flex-1 space-y-2">
          <Skeleton className="h-6 w-40" />
          <Skeleton className="h-5 w-48" />
          <Skeleton className="h-10 w-full" />
        </div>
        <Skeleton className="size-24 rounded-xl" />
      </div>
    </div>
  );
}
