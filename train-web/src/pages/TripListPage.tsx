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
 * Màn 3 — Danh sách chuyến.
 *
 * Số chỗ còn lại được làm mới NGẦM mỗi 8 giây: dữ liệu cũ vẫn nằm nguyên trên
 * màn hình, không có skeleton nhấp nháy, không có cuộn trang bị nhảy. Người
 * dùng chỉ thấy con số đổi. Đây là điểm khác nhau giữa "gần thời gian thực" và
 * "trang tự reload" — cái sau khiến người dùng mất chỗ đang đọc dở.
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

  // Làm mới ngầm số chỗ còn lại, và chỉ khi tab đang hiển thị
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
    navigate(`/chon-cho/${encodeURIComponent(trip.id)}`);
  };

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={1} />

      {/* Tóm tắt tìm kiếm — bấm vào là sửa lại, không cần bấm nút quay lại */}
      <button
        type="button"
        onClick={() => navigate("/")}
        className="flex w-full items-center gap-3 rounded-2xl border border-ink-200 bg-white px-4 py-3 text-left hover:border-son-300"
      >
        <div className="min-w-0 flex-1">
          <div className="truncate font-bold text-ink-900">
            {all[0]?.fromStation.name ?? query.from} → {all[0]?.toStation.name ?? query.to}
          </div>
          <div className="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-ink-600">
            <span>{weekdayLabel(fromDateKey(query.date))}, {formatDate(fromDateKey(query.date))}</span>
            <span aria-hidden>·</span>
            <span>{query.passengers} hành khách</span>
            {dayInfo.peak !== "none" && (
              <Badge tone={dayInfo.peak === "peak" ? "son" : "mai"}>{peakText[dayInfo.peak]}</Badge>
            )}
          </div>
        </div>
        <span className="shrink-0 text-sm font-semibold text-son-700">Sửa</span>
      </button>

      {trips.retrying && <RetryBanner attempt={trips.retrying.attempt} maxAttempts={trips.retrying.maxAttempts} />}

      {/* Sắp xếp + lọc */}
      {!trips.isInitialLoad && all.length > 0 && (
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {([
            { key: "depart", label: "Giờ đi sớm" },
            { key: "price", label: "Giá thấp" },
            { key: "duration", label: "Đi nhanh" },
          ] as const).map((opt) => (
            <button
              key={opt.key}
              type="button"
              aria-pressed={sort === opt.key}
              onClick={() => setSort(opt.key)}
              className={cn(
                "min-h-10 shrink-0 rounded-full border-2 px-4 text-sm font-semibold transition-colors",
                sort === opt.key
                  ? "border-son-600 bg-son-600 text-white"
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
                ? "border-son-600 bg-son-600 text-white"
                : "border-ink-200 bg-white text-ink-700 hover:border-ink-300",
            )}
          >
            Chỉ chuyến còn vé
          </button>
        </div>
      )}

      {/* Bốn trạng thái: loading → error → empty → success */}
      {trips.loading && trips.isInitialLoad ? (
        <LoadingRegion label="Đang tìm chuyến tàu phù hợp…">
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
          title={onlyAvailable ? "Không còn chuyến nào có vé" : "Không có chuyến tàu phù hợp"}
          detail={
            onlyAvailable
              ? "Tất cả chuyến trong ngày này đã hết vé. Bạn thử ngày liền kề, vé thường còn nhiều hơn."
              : "Tuyến này không có chuyến chạy vào ngày bạn chọn. Bạn thử đổi ngày hoặc đổi ga."
          }
          action={
            <div className="flex flex-col gap-2 sm:flex-row">
              {onlyAvailable && (
                <Button variant="secondary" onClick={() => setOnlyAvailable(false)}>
                  Xem cả chuyến hết vé
                </Button>
              )}
              <Button onClick={() => navigate("/")}>Đổi ngày hoặc ga</Button>
            </div>
          }
        />
      ) : (
        <>
          <div className="flex items-center justify-between text-sm">
            <p className="text-ink-600">
              <span className="font-semibold text-ink-900">{visible.length}</span> chuyến tàu
            </p>
            <p className="flex items-center gap-1.5 text-ink-500" aria-live="polite">
              <span aria-hidden className="size-2 animate-pulse rounded-full bg-ok-600" />
              {lastUpdated ? `Cập nhật lúc ${lastUpdated.toLocaleTimeString("vi-VN")}` : "Đang theo dõi số chỗ"}
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
            Số chỗ còn lại thay đổi liên tục trong giờ cao điểm. Chỗ chỉ thực sự là của bạn sau khi giữ chỗ thành công.
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
