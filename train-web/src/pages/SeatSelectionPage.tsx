import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "@/api/client";
import { ApiError, friendlyMessage } from "@/api/errors";
import { clearQueuePass } from "@/lib/queuePass";
import { SEAT_CLASS_LABEL, type Carriage, type Seat } from "@/api/types";
import { SeatMap, SeatMapSkeleton } from "@/components/SeatMap";
import { StickyActionBar } from "@/components/TrainCard";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Stepper } from "@/components/ui/Stepper";
import { EmptyState, ErrorState, LoadingRegion, RetryBanner } from "@/components/ui/StateView";
import { useAsync } from "@/hooks/useAsync";
import { useSubmitLock } from "@/hooks/useSubmitLock";
import { useBooking } from "@/store/BookingContext";
import { useToast } from "@/store/ToastContext";
import { formatTime, formatVnd } from "@/lib/format";
import { cn } from "@/lib/cn";

/**
 * Screen 4 — Pick a carriage and seats.
 *
 * The hardest situation in the whole flow: while the user is studying the
 * map, someone else takes the seat. How it is handled here:
 *
 *  - The map refreshes silently every 6 seconds, KEEPING the user's selection.
 *  - When "Hold" gets a 409 SEAT_TAKEN from the server: no glaring red error
 *    dialog. Instead, deselect exactly the lost seats, make them pulse,
 *    highlight the seats the server suggests, and say so in a gentle toast.
 *    Seats that could be held stay selected.
 *  - The "Hold" button is locked against double-clicks and carries an Idempotency-Key.
 */
export function SeatSelectionPage() {
  const navigate = useNavigate();
  const { tripId = "" } = useParams();
  const { trip, seatClass, query, setHold } = useBooking();
  const toast = useToast();

  const [carriageIndex, setCarriageIndex] = useState(0);
  const [selected, setSelected] = useState<Seat[]>([]);
  const [justTaken, setJustTaken] = useState<string[]>([]);
  const [suggested, setSuggested] = useState<string[]>([]);

  const maxSeats = query?.passengers ?? 1;

  useEffect(() => {
    if (!trip || !seatClass || trip.id !== tripId) navigate("/trips", { replace: true });
  }, [trip, seatClass, tripId, navigate]);

  const carriages = useAsync(
    (o) =>
      seatClass && query
        ? api.getCarriages(tripId, seatClass, { from: query.from, to: query.to }, o)
        : Promise.resolve([] as Carriage[]),
    [tripId, seatClass, query],
  );

  // Silently refresh the seat map
  useEffect(() => {
    const id = setInterval(() => {
      if (document.visibilityState === "visible") void carriages.refreshSilently();
    }, 6000);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripId, seatClass]);

  const list = carriages.data ?? [];
  const carriage = list[carriageIndex];

  // A selected seat taken by someone else during a silent refresh → deselect it and tell the user
  useEffect(() => {
    if (!carriage || selected.length === 0) return;
    const byId = new Map(list.flatMap((c) => c.seats).map((s) => [s.id, s]));
    const lost = selected.filter((s) => byId.get(s.id)?.status !== "available");
    if (lost.length === 0) return;
    setSelected((prev) => prev.filter((s) => !lost.some((l) => l.id === s.id)));
    setJustTaken(lost.map((s) => s.id));
    toast.show({
      tone: "warning",
      title: lost.length === 1 ? `Seat ${lost[0].label} was just taken` : `${lost.length} of your seats were just taken`,
      detail: "Please choose other seats, free seats have a green border.",
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [list]);

  const toggleSeat = useCallback(
    (seat: Seat) => {
      setJustTaken([]);
      setSuggested([]);
      setSelected((prev) => {
        const exists = prev.some((s) => s.id === seat.id);
        if (exists) return prev.filter((s) => s.id !== seat.id);
        if (prev.length >= maxSeats) return prev;
        return [...prev, seat];
      });
    },
    [maxSeats],
  );

  const total = useMemo(() => selected.reduce((sum, s) => sum + s.price, 0), [selected]);

  const { submit: holdSeats, pending: holding, resetKey } = useSubmitLock(async ({ idempotencyKey }) => {
    if (!seatClass) return;
    try {
      if (!query) return;
      const hold = await api.holdSeats(
        {
          tripId,
          seatClass,
          seatIds: selected.map((s) => s.id),
          from: query.from,
          to: query.to,
        },
        { idempotencyKey },
      );
      setHold(hold);
      navigate("/passengers");
    } catch (e) {
      handleHoldError(e);
      throw e;
    }
  });

  /** Turn a hold error into a concrete next step instead of a curt message */
  function handleHoldError(e: unknown) {
    if (!(e instanceof ApiError)) {
      toast.show({ tone: "error", title: "Could not hold your seats", detail: "Please try again." });
      return;
    }
    if (e.kind === "SEAT_TAKEN") {
      const details = e.details as { takenSeatIds?: string[]; suggestedSeatIds?: string[] } | undefined;
      const taken = details?.takenSeatIds ?? [];
      const suggest = details?.suggestedSeatIds ?? [];
      setSelected((prev) => prev.filter((s) => !taken.includes(s.id)));
      setJustTaken(taken);
      setSuggested(suggest);
      resetKey(); // the next hold is a new action with a different seat list
      void carriages.refreshSilently();
      toast.show({
        tone: "warning",
        title: taken.length === 1 ? "Someone just took this seat" : `${taken.length} seats were just taken`,
        detail: suggest.length
          ? "We have marked a few free seats nearby, please pick again."
          : "Please pick other seats on the map.",
        durationMs: 7000,
      });
      return;
    }
    if (e.kind === "NOT_ADMITTED") {
      // The admission expired (over 15 minutes) or the user never queued: drop
      // the old pass and go back to the waiting room. The trip choice is still in
      // BookingContext, so once admitted again the user lands right back here.
      clearQueuePass();
      const msg = friendlyMessage(e);
      toast.show({ tone: "warning", title: msg.title, detail: msg.detail, durationMs: 7000 });
      navigate("/waiting-room", { replace: true });
      return;
    }
    if (e.kind === "SOLD_OUT") {
      toast.show({
        tone: "error",
        title: "This seat class just sold out",
        detail: "Please go back and choose another class or another trip.",
      });
      return;
    }
    if (e.kind === "OVERLOADED") {
      toast.show({
        tone: "warning",
        title: "The system is very busy",
        detail: "We retried a few times without success. Please wait a few seconds and try again.",
      });
      return;
    }
    toast.show({ tone: "error", title: "Could not hold your seats", detail: "Please try again." });
  }

  if (!trip || !seatClass) return null;

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={1} />

      {/* Summary of the chosen trip, always visible */}
      <div className="rounded-2xl border border-ink-200 bg-white px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-brand-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
          <span className="font-semibold text-ink-900">
            {trip.fromStation.name} → {trip.toStation.name}
          </span>
        </div>
        <p className="mt-1 text-sm text-ink-600">
          Departs {formatTime(trip.departAt)} · {SEAT_CLASS_LABEL[seatClass]} · Pick {maxSeats} {maxSeats === 1 ? "seat" : "seats"}
        </p>
      </div>

      {carriages.retrying && (
        <RetryBanner attempt={carriages.retrying.attempt} maxAttempts={carriages.retrying.maxAttempts} />
      )}

      {carriages.loading && carriages.isInitialLoad ? (
        <LoadingRegion label="Loading the carriage map…">
          <SeatMapSkeleton />
        </LoadingRegion>
      ) : carriages.error && list.length === 0 ? (
        <ErrorState error={carriages.error} onRetry={carriages.reload} />
      ) : list.length === 0 ? (
        <EmptyState
          icon="🚃"
          title="No seat map for this class yet"
          detail="Please go back and choose another seat class."
          action={<Button onClick={() => navigate("/trips")}>Choose another class</Button>}
        />
      ) : (
        <>
          {/* Carriage picker */}
          <div>
            <p className="mb-1.5 text-sm font-semibold text-ink-800">Choose a carriage</p>
            <div className="flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="Carriages">
              {list.map((c, i) => {
                const active = i === carriageIndex;
                const full = c.available === 0;
                return (
                  <button
                    key={c.id}
                    type="button"
                    role="tab"
                    aria-selected={active}
                    disabled={full}
                    onClick={() => setCarriageIndex(i)}
                    className={cn(
                      "min-h-16 shrink-0 rounded-2xl border-2 px-4 py-2 text-center transition-colors",
                      active ? "border-brand-600 bg-brand-50" : "border-ink-200 bg-white hover:border-ink-300",
                      full && "cursor-not-allowed opacity-55",
                    )}
                  >
                    <div className="font-bold text-ink-900">Carriage {c.number}</div>
                    <div className={cn("text-xs font-medium", c.available === 0 ? "text-ink-500" : c.available <= 8 ? "text-brand-700" : "text-ok-600")}>
                      {c.available === 0 ? "Full" : `${c.available} seats left`}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {carriage && (
            <SeatMap
              carriage={carriage}
              selectedSeatIds={selected.map((s) => s.id)}
              onToggleSeat={toggleSeat}
              maxSelectable={maxSeats}
              justTakenSeatIds={justTaken}
              suggestedSeatIds={suggested}
            />
          )}

          {suggested.length > 0 && (
            <div role="status" className="rounded-2xl border-2 border-accent-300 bg-accent-50 px-4 py-3">
              <p className="font-semibold text-ink-900">Free seats nearby</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {suggested.map((id) => {
                  const seat = list.flatMap((c) => c.seats).find((s) => s.id === id);
                  if (!seat || seat.status !== "available") return null;
                  return (
                    <button
                      key={id}
                      type="button"
                      onClick={() => {
                        const idx = list.findIndex((c) => c.seats.some((s) => s.id === id));
                        if (idx >= 0) setCarriageIndex(idx);
                        toggleSeat(seat);
                      }}
                      className="min-h-11 rounded-xl border-2 border-accent-400 bg-white px-3 font-semibold text-ink-900 hover:bg-accent-100"
                    >
                      Seat {seat.label}
                      {seat.berthLevel ? ` · level ${seat.berthLevel}` : ""}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <StickyActionBar
            note={
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <p className="text-sm text-ink-600">
                    Selected <span className="font-semibold text-ink-900">{selected.length}/{maxSeats}</span> seats
                  </p>
                  <div className="mt-0.5 flex flex-wrap gap-1">
                    {selected.map((s) => (
                      <Badge key={s.id} tone="brand">Carriage {carriage?.number} · seat {s.label}</Badge>
                    ))}
                  </div>
                </div>
                <p className="tnum shrink-0 text-lg font-bold text-brand-700">{formatVnd(total)}</p>
              </div>
            }
          >
            <Button
              size="lg"
              fullWidth
              loading={holding}
              loadingText="Holding your seats…"
              disabled={selected.length !== maxSeats}
              onClick={() => void holdSeats().catch(() => {})}
            >
              {selected.length === maxSeats
                ? `Hold ${maxSeats} ${maxSeats === 1 ? "seat" : "seats"} for 10 minutes`
                : `Pick ${maxSeats - selected.length} more ${maxSeats - selected.length === 1 ? "seat" : "seats"}`}
            </Button>
          </StickyActionBar>
        </>
      )}
    </div>
  );
}
