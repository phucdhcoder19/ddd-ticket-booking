import { useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { SEAT_CLASS_SHORT, type Ticket, type TicketStatus } from "@/api/types";
import { Badge } from "./ui/Badge";
import { Button } from "./ui/Button";
import { Skeleton } from "./ui/StateView";
import { formatDate, formatTime, formatVnd, maskIdNumber, weekdayLabel, parseIso } from "@/lib/format";
import { cn } from "@/lib/cn";

const STATUS: Record<TicketStatus, { label: string; tone: "ok" | "muted" | "warn" | "info" }> = {
  VALID: { label: "Valid", tone: "ok" },
  USED: { label: "Used", tone: "muted" },
  REFUNDED: { label: "Refunded", tone: "muted" },
  EXCHANGING: { label: "Being exchanged", tone: "warn" },
};

/**
 * Ticket card.
 *
 * The QR code is always shown large right on the card instead of behind a
 * tap: at the ticket gate people need to hold out their screen within a
 * second, and the signal at a station during the holidays is very weak.
 * Refunded or used tickets get a faded QR with a label across it, so nobody
 * shows an old ticket by mistake.
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
        inactive ? "border-ink-200 opacity-80" : "border-brand-200",
      )}
      aria-label={`Ticket ${ticket.code}, train ${ticket.trainCode}`}
    >
      <div className="flex items-center justify-between gap-2 bg-brand-700 px-4 py-2.5 text-white">
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
            <Item label="Carriage" value={String(ticket.carriageNumber)} />
            <Item label="Seat" value={ticket.seatLabel} />
            <Item label="Class" value={SEAT_CLASS_SHORT[ticket.seatClass]} />
            <Item label="Fare" value={formatVnd(ticket.price)} />
          </dl>
          <p className="mt-2 truncate text-sm font-semibold text-ink-900">{ticket.passengerName}</p>
          {ticket.passengerId && (
            <p className="tnum text-xs text-ink-500">ID {maskIdNumber(ticket.passengerId)}</p>
          )}
        </div>

        <button
          type="button"
          onClick={() => setZoomed((v) => !v)}
          aria-label={zoomed ? "Shrink the QR code" : "Enlarge the QR code for the ticket check"}
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
                {ticket.status === "REFUNDED" ? "REFUNDED" : "USED"}
              </span>
            </span>
          )}
        </button>
      </div>

      <div className="flex items-center justify-between gap-2 border-t border-dashed border-ink-200 bg-ink-50 px-4 py-2.5">
        <span className="tnum text-xs text-ink-500">Ticket code {ticket.code}</span>
        {ticket.status === "VALID" && onRefund && (
          <div className="flex gap-2">
            <Button variant="ghost" className="min-h-10 px-3 text-sm" onClick={() => alert("Ticket exchange is still being built.")}>
              Exchange
            </Button>
            <Button
              variant="danger"
              className="min-h-10 px-3 text-sm"
              loading={refunding}
              loadingText="Processing…"
              onClick={() => onRefund(ticket)}
            >
              Refund
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
