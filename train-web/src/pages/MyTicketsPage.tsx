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
 * Screen 7 — My tickets.
 *
 * "Upcoming" is separate from "Travelled / refunded": right after buying,
 * users need the ticket they are about to use, not their history. Refunds
 * always ask for confirmation with the EXACT REFUND AMOUNT — holiday train
 * refunds carry a fee, and vague wording is the biggest source of complaints.
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
        title: "Refund request received",
        detail: `The money will be back in your account within 3–5 business days.`,
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
        <h1 className="text-xl font-bold">My tickets</h1>
        <p className="mt-1 text-sm text-ink-600">
          Show the QR code to the conductor when boarding. Take a screenshot in case there is no signal at the station.
        </p>
      </div>

      <div className="flex gap-2" role="tablist" aria-label="Filter tickets">
        {([
          { key: "upcoming" as const, label: `Upcoming (${upcoming.length})` },
          { key: "past" as const, label: `Travelled & refunded (${past.length})` },
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
                ? "border-brand-600 bg-brand-600 text-white"
                : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tickets.loading && tickets.isInitialLoad ? (
        <LoadingRegion label="Loading your tickets…">
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
          title={tab === "upcoming" ? "You have no upcoming tickets" : "No past tickets yet"}
          detail={
            tab === "upcoming"
              ? "Tickets appear here with their QR code once payment succeeds."
              : "Used or refunded tickets are kept in this section."
          }
          action={tab === "upcoming" ? <Button onClick={() => navigate("/")}>Book a Lunar New Year ticket</Button> : undefined}
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

      {/* Refund confirmation, spelling out the fee and the amount actually received */}
      <Modal
        open={confirming !== null}
        onClose={() => setConfirming(null)}
        title="Are you sure you want to refund this ticket?"
        description="Once refunded, the seat is sold to another passenger right away and you cannot get it back."
        footer={
          <>
            <Button variant="secondary" onClick={() => setConfirming(null)}>No, keep the ticket</Button>
            <Button
              variant="primary"
              loading={refunding}
              loadingText="Processing…"
              onClick={() => void doRefund().catch(() => {})}
            >
              Confirm refund
            </Button>
          </>
        }
      >
        {confirming && (
          <dl className="rounded-xl border border-ink-200 bg-ink-50 p-3 text-sm">
            <div className="flex justify-between py-1">
              <dt className="text-ink-600">Ticket price paid</dt>
              <dd className="tnum font-semibold">{formatVnd(confirming.price)}</dd>
            </div>
            <div className="flex justify-between py-1">
              <dt className="text-ink-600">Refund fee (20% during the holiday)</dt>
              <dd className="tnum font-semibold text-brand-700">−{formatVnd(Math.round(confirming.price * 0.2))}</dd>
            </div>
            <div className="mt-1 flex justify-between border-t border-ink-200 pt-2">
              <dt className="font-bold text-ink-900">You get back</dt>
              <dd className="tnum font-bold text-ink-900">{formatVnd(Math.round(confirming.price * 0.8))}</dd>
            </div>
          </dl>
        )}
      </Modal>
    </div>
  );
}
