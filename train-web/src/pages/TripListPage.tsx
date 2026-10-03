import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import type { SeatClassCode, Trip } from "@/api/types";
import { TrainCard, TrainCardSkeleton } from "@/components/TrainCard";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Stepper } from "@/components/ui/Stepper";
import { EmptyState, ErrorState, LoadingRegion, RetryBanner } from "@/components/ui/StateView";
import { useAsync } from "@/hooks/useAsync";
import { useBooking } from "@/store/BookingContext";
import { formatDate, fromDateKey, weekdayLabel } from "@/lib/format";
import { getDayInfo, peakText } from "@/lib/lunar";
import { cn } from "@/lib/cn";

type SortKey = "depart" | "price" | "duration";

/**
 * Screen 3 — Trip list.
 *
 * Remaining seats refresh SILENTLY every 8 seconds: the old data stays on
 * screen, no flashing skeleton, no jumping scroll. Users only see the numbers
 * change. That is the difference between "near real time" and "the page
 * reloads itself" — the latter makes users lose their place while reading.
 */
export function TripListPage() {
  const navigate = useNavigate();
  const { query, startSelection } = useBooking();
  const [sort, setSort] = useState<SortKey>("depart");
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);

  useEffect(() => {
    if (!query) navigate("/", { replace: true });
  }, [query, navigate]);

  const trips = useAsync(
    (o) => (query ? api.searchTrips(query, o) : Promise.resolve([] as Trip[])),
    [query?.from, query?.to, query?.date],
  );

  // Silently refresh remaining seats, only while the tab is visible
  useEffect(() => {
    const id = setInterval(() => {
      if (document.visibilityState !== "visible") return;
      void trips.refreshSilently().then(() => setLastUpdated(new Date()));
    }, 8000);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query?.from, query?.to, query?.date]);

  if (!query) return null;

  const dayInfo = getDayInfo(fromDateKey(query.date));
  const all = trips.data ?? [];
  const visible = [...all]
    .filter((t) => !onlyAvailable || t.availableTotal > 0)
    .sort((a, b) => {
      if (sort === "price") return minPrice(a) - minPrice(b);
      if (sort === "duration") return a.durationMinutes - b.durationMinutes;
      return a.departAt.localeCompare(b.departAt);
    });

  const onSelectClass = (trip: Trip, seatClass: SeatClassCode) => {
    startSelection(trip, seatClass);
    navigate(`/seats/${encodeURIComponent(trip.id)}`);
  };

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={1} />

      {/* Search summary — tap to edit, no need for a back button */}
      <button
        type="button"
        onClick={() => navigate("/")}
        className="flex w-full items-center gap-3 rounded-2xl border border-ink-200 bg-white px-4 py-3 text-left hover:border-brand-300"
      >
        <div className="min-w-0 flex-1">
          <div className="truncate font-bold text-ink-900">
            {all[0]?.fromStation.name ?? query.from} → {all[0]?.toStation.name ?? query.to}
          </div>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-ink-600">
            <span>{weekdayLabel(fromDateKey(query.date))}, {formatDate(fromDateKey(query.date))}</span>
            <span aria-hidden>·</span>
            <span>{query.passengers} {query.passengers === 1 ? "passenger" : "passengers"}</span>
            {dayInfo.peak !== "none" && (
              <Badge tone={dayInfo.peak === "peak" ? "brand" : "accent"}>{peakText[dayInfo.peak]}</Badge>
            )}
          </div>
        </div>
        <span className="shrink-0 text-sm font-semibold text-brand-700">Edit</span>
      </button>

      {trips.retrying && <RetryBanner attempt={trips.retrying.attempt} maxAttempts={trips.retrying.maxAttempts} />}

      {/* Sort + filter */}
      {!trips.isInitialLoad && all.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {([
            { key: "depart", label: "Earliest departure" },
            { key: "price", label: "Lowest price" },
            { key: "duration", label: "Fastest" },
          ] as const).map((opt) => (
            <button
              key={opt.key}
              type="button"
              aria-pressed={sort === opt.key}
              onClick={() => setSort(opt.key)}
              className={cn(
                "min-h-10 shrink-0 rounded-full border-2 px-4 text-sm font-semibold transition-colors",
                sort === opt.key
                  ? "border-brand-600 bg-brand-600 text-white"
                  : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
              )}
            >
              {opt.label}
            </button>
          ))}
          <button
            type="button"
            aria-pressed={onlyAvailable}
            onClick={() => setOnlyAvailable((v) => !v)}
            className={cn(
              "min-h-10 shrink-0 rounded-full border-2 px-4 text-sm font-semibold transition-colors",
              onlyAvailable
                ? "border-brand-600 bg-brand-600 text-white"
                : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
            )}
          >
            Only trips with seats
          </button>
        </div>
      )}

      {/* Four states: loading → error → empty → success */}
      {trips.loading && trips.isInitialLoad ? (
        <LoadingRegion label="Finding matching trips…">
          <div className="flex flex-col gap-3">
            <TrainCardSkeleton />
            <TrainCardSkeleton />
            <TrainCardSkeleton />
          </div>
        </LoadingRegion>
      ) : trips.error && all.length === 0 ? (
        <ErrorState error={trips.error} onRetry={trips.reload} />
      ) : visible.length === 0 ? (
        <EmptyState
          icon="🚉"
          title={onlyAvailable ? "No trips with seats left" : "No matching trips"}
          detail={
            onlyAvailable
              ? "Every trip on this day is sold out. Try a nearby date, there are usually more seats."
              : "No trains run on this route on the date you picked. Try another date or other stations."
          }
          action={
            <div className="flex flex-col gap-2 sm:flex-row">
              {onlyAvailable && (
                <Button variant="secondary" onClick={() => setOnlyAvailable(false)}>
                  Show sold-out trips too
                </Button>
              )}
              <Button onClick={() => navigate("/")}>Change date or stations</Button>
            </div>
          }
        />
      ) : (
        <>
          <div className="flex items-center justify-between text-sm">
            <p className="text-ink-600">
              <span className="font-semibold text-ink-900">{visible.length}</span> {visible.length === 1 ? "trip" : "trips"}
            </p>
            <p className="flex items-center gap-1.5 text-ink-500" aria-live="polite">
              <span aria-hidden className="size-2 animate-pulse rounded-full bg-ok-600" />
              {lastUpdated ? `Updated at ${lastUpdated.toLocaleTimeString("en-GB")}` : "Watching seat counts"}
            </p>
          </div>

          <ul className="flex flex-col gap-3">
            {visible.map((trip) => (
              <li key={trip.id}>
                <TrainCard trip={trip} onSelectClass={(c) => onSelectClass(trip, c)} />
              </li>
            ))}
          </ul>

          <p className="pb-2 text-center text-sm text-ink-500">
            Seat counts change constantly at peak time. A seat is only yours once it has been held successfully.
          </p>
        </>
      )}
    </div>
  );
}

const minPrice = (t: Trip) => {
  const open = t.classes.filter((c) => c.available > 0);
  return open.length ? Math.min(...open.map((c) => c.price)) : Number.MAX_SAFE_INTEGER;
};
