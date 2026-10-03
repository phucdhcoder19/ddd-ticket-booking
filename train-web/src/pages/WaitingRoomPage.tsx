import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import type { QueueTicket } from "@/api/types";
import { NoRefreshWarning, QueueStatus } from "@/components/QueueStatus";
import { Button } from "@/components/ui/Button";
import { ErrorState, Skeleton } from "@/components/ui/StateView";
import { useBooking } from "@/store/BookingContext";
import { formatNumber } from "@/lib/format";
import { clearQueuePass, getQueuePass, setQueuePass } from "@/lib/queuePass";

const QUEUE_KEY = "trainbooking.queue.token";

/**
 * Screen 2 — Waiting room.
 *
 * Four things must be done right, or users lose their place:
 *
 * 1. The queue token is saved to sessionStorage AS SOON AS it arrives. If the
 *    user presses F5 they return to the same position instead of queueing again.
 * 2. beforeunload warns before closing the tab or reloading.
 * 3. Polling uses a recursive setTimeout, not setInterval: if one call is slow
 *    (weak network), requests do not pile up on top of each other.
 * 4. The screen ALWAYS MOVES (numbers ticking, bar growing) so users trust the
 *    system is alive — that is the main reason people press F5.
 */
export function WaitingRoomPage() {
  const navigate = useNavigate();
  const { query } = useBooking();
  const [queue, setQueue] = useState<QueueTicket | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [entryPosition, setEntryPosition] = useState<number | null>(null);
  const timerRef = useRef<number | null>(null);

  // Landing on the waiting room without a search goes back to the home page
  useEffect(() => {
    if (!query) navigate("/", { replace: true });
  }, [query, navigate]);

  // Warn before leaving the page
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
          enter(next.token);
          return;
        }
        // Poll more often when close to the front, so the call is not missed
        schedule(token, next.position < 80 ? 1200 : 3000);
      } catch (e) {
        if (cancelled) return;
        // A temporary error does not kick the user out of the queue — keep retrying
        setError(e);
        schedule(token, 5000);
      }
    };

    /**
     * Your turn: the queue token becomes an admission pass. Move it to its own
     * place (queuePass) so lib/http.ts attaches it to every request — before,
     * the token was simply deleted at this step, so the server had no way to
     * tell whether a POST /holds caller had queued or not.
     */
    const enter = (token: string) => {
      sessionStorage.removeItem(QUEUE_KEY);
      setQueuePass(token);
      navigate("/trips", { replace: true });
    };

    const start = async () => {
      // Already holding a valid pass (e.g. went back to the home page to change
      // the date and searched again): go straight in, no queueing twice.
      const pass = getQueuePass();
      if (pass) {
        try {
          const current = await api.getQueueStatus(pass);
          if (cancelled) return;
          if (current.status === "ADMITTED") {
            enter(pass);
            return;
          }
        } catch {
          // 404: the pass has expired -> drop it and queue again like a newcomer
        }
        clearQueuePass();
      }

      try {
        const saved = sessionStorage.getItem(QUEUE_KEY);
        const ticket = saved ? await api.getQueueStatus(saved).catch(() => api.joinQueue()) : await api.joinQueue();
        if (cancelled) return;
        sessionStorage.setItem(QUEUE_KEY, ticket.token);
        setEntryPosition((p) => p ?? ticket.position);
        setQueue(ticket);
        if (ticket.status === "ADMITTED") {
          enter(ticket.token);
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
        <ErrorState error={error} onRetry={() => window.location.reload()} retryLabel="Rejoin the queue" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4 py-2">
      <div className="text-center">
        <span aria-hidden className="text-4xl">🏮</span>
        <h1 className="mt-2 text-2xl font-bold">You are in the queue</h1>
        <p className="mt-1 text-ink-600">
          Lots of people are booking right now. We let everyone in by order of arrival so everyone gets a fair chance.
        </p>
      </div>

      {queue ? (
        <QueueStatus queue={queue} positionAtEntry={entryPosition ?? queue.position} />
      ) : (
        <div className="rounded-3xl border border-ink-200 bg-white p-6">
          <Skeleton className="mx-auto h-4 w-40" />
          <Skeleton className="mx-auto mt-3 h-12 w-32" />
          <Skeleton className="mt-6 h-3 w-full" />
          <p className="sr-only" role="status">Getting your queue number…</p>
        </div>
      )}

      <NoRefreshWarning />

      {error ? (
        <p role="status" className="rounded-xl border border-accent-200 bg-accent-50 px-4 py-3 text-sm text-ink-700">
          The connection is unstable, so your queue number has not updated for a moment. <strong>Your place is still kept</strong>,
          and we are retrying automatically.
        </p>
      ) : null}

      <div className="rounded-2xl border border-ink-200 bg-white p-4">
        <h2 className="font-semibold text-ink-900">While you wait, you can prepare</h2>
        <ul className="mt-2 flex flex-col gap-1.5 text-sm text-ink-700">
          <li>• The ID numbers of everyone travelling with you</li>
          <li>• A bank card or e-wallet ready for payment</li>
          <li>• A backup plan: another trip or a nearby date</li>
        </ul>
        {queue && queue.total > 0 && (
          <p className="mt-3 text-sm text-ink-500">
            <span className="tnum font-semibold">{formatNumber(queue.total)}</span> people are waiting with you.
          </p>
        )}
      </div>

      <Button variant="ghost" fullWidth onClick={leaveQueue}>
        Leave the queue and go back to the home page
      </Button>
    </div>
  );
}
