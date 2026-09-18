import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { api } from "@/api/client";
import { ApiError } from "@/api/errors";
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
 * Màn 4 — Chọn toa và ghế.
 *
 * Tình huống khó nhất của cả luồng: giữa lúc người dùng đang ngắm sơ đồ thì
 * ghế bị người khác lấy mất. Cách xử lý ở đây:
 *
 *  - Sơ đồ tự làm mới ngầm mỗi 6 giây, GIỮ NGUYÊN các ghế người dùng đang chọn.
 *  - Khi bấm "Giữ chỗ" mà server trả 409 SEAT_TAKEN: không hiện hộp thoại lỗi
 *    đỏ chót. Thay vào đó bỏ chọn đúng những ghế đã mất, đánh dấu chúng nhấp
 *    nháy, làm nổi các ghế server gợi ý, và báo bằng toast giọng nhẹ nhàng.
 *    Ghế nào giữ được thì vẫn giữ nguyên lựa chọn.
 *  - Nút "Giữ chỗ" khoá chống double-click và gắn Idempotency-Key.
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
    if (!trip || !seatClass || trip.id !== tripId) navigate("/chuyen-tau", { replace: true });
  }, [trip, seatClass, tripId, navigate]);

  const carriages = useAsync(
    (o) => (seatClass ? api.getCarriages(tripId, seatClass, o) : Promise.resolve([] as Carriage[])),
    [tripId, seatClass],
  );

  // Làm mới ngầm sơ đồ ghế
  useEffect(() => {
    const id = setInterval(() => {
      if (document.visibilityState === "visible") void carriages.refreshSilently();
    }, 6000);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripId, seatClass]);

  const list = carriages.data ?? [];
  const carriage = list[carriageIndex];

  // Ghế đang chọn mà bị người khác giữ mất trong lúc làm mới ngầm → bỏ chọn và báo
  useEffect(() => {
    if (!carriage || selected.length === 0) return;
    const byId = new Map(list.flatMap((c) => c.seats).map((s) => [s.id, s]));
    const lost = selected.filter((s) => byId.get(s.id)?.status !== "available");
    if (lost.length === 0) return;
    setSelected((prev) => prev.filter((s) => !lost.some((l) => l.id === s.id)));
    setJustTaken(lost.map((s) => s.id));
    toast.show({
      tone: "warning",
      title: lost.length === 1 ? `Chỗ ${lost[0].label} vừa có người giữ` : `${lost.length} chỗ bạn chọn vừa có người giữ`,
      detail: "Bạn chọn giúp chỗ khác nhé, các chỗ trống được tô viền xanh.",
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
      const hold = await api.holdSeats(
        { tripId, seatClass, seatIds: selected.map((s) => s.id) },
        { idempotencyKey },
      );
      setHold(hold);
      navigate("/thong-tin-hanh-khach");
    } catch (e) {
      handleHoldError(e);
      throw e;
    }
  });

  /** Biến lỗi giữ chỗ thành hành động cụ thể thay vì một thông báo cụt lủn */
  function handleHoldError(e: unknown) {
    if (!(e instanceof ApiError)) {
      toast.show({ tone: "error", title: "Chưa giữ được chỗ", detail: "Bạn thử lại giúp chúng tôi nhé." });
      return;
    }
    if (e.kind === "SEAT_TAKEN") {
      const details = e.details as { takenSeatIds?: string[]; suggestedSeatIds?: string[] } | undefined;
      const taken = details?.takenSeatIds ?? [];
      const suggest = details?.suggestedSeatIds ?? [];
      setSelected((prev) => prev.filter((s) => !taken.includes(s.id)));
      setJustTaken(taken);
      setSuggested(suggest);
      resetKey(); // lần giữ tới là một thao tác mới với danh sách ghế khác
      void carriages.refreshSilently();
      toast.show({
        tone: "warning",
        title: taken.length === 1 ? "Chỗ này vừa có người giữ mất" : `${taken.length} chỗ vừa có người giữ mất`,
        detail: suggest.length
          ? "Chúng tôi đã đánh dấu vài chỗ trống gần đó, bạn chọn lại giúp nhé."
          : "Bạn chọn chỗ khác trên sơ đồ giúp nhé.",
        durationMs: 7000,
      });
      return;
    }
    if (e.kind === "SOLD_OUT") {
      toast.show({
        tone: "error",
        title: "Hạng chỗ này vừa hết vé",
        detail: "Bạn quay lại chọn hạng chỗ khác hoặc chuyến khác nhé.",
      });
      return;
    }
    if (e.kind === "OVERLOADED") {
      toast.show({
        tone: "warning",
        title: "Hệ thống đang rất đông",
        detail: "Chúng tôi đã thử lại vài lần nhưng chưa được. Bạn đợi vài giây rồi bấm lại nhé.",
      });
      return;
    }
    toast.show({ tone: "error", title: "Chưa giữ được chỗ", detail: "Bạn thử lại giúp chúng tôi nhé." });
  }

  if (!trip || !seatClass) return null;

  return (
    <div className="flex flex-col gap-4">
      <Stepper current={1} />

      {/* Tóm tắt chuyến đang chọn, luôn nhìn thấy */}
      <div className="rounded-2xl border border-ink-200 bg-white px-4 py-3">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-son-600 px-2 py-0.5 text-sm font-bold text-white">{trip.trainCode}</span>
          <span className="font-semibold text-ink-900">
            {trip.fromStation.name} → {trip.toStation.name}
          </span>
        </div>
        <p className="mt-1 text-sm text-ink-600">
          Khởi hành {formatTime(trip.departAt)} · {SEAT_CLASS_LABEL[seatClass]} · Chọn {maxSeats} chỗ
        </p>
      </div>

      {carriages.retrying && (
        <RetryBanner attempt={carriages.retrying.attempt} maxAttempts={carriages.retrying.maxAttempts} />
      )}

      {carriages.loading && carriages.isInitialLoad ? (
        <LoadingRegion label="Đang tải sơ đồ toa tàu…">
          <SeatMapSkeleton />
        </LoadingRegion>
      ) : carriages.error && list.length === 0 ? (
        <ErrorState error={carriages.error} onRetry={carriages.reload} />
      ) : list.length === 0 ? (
        <EmptyState
          icon="🚃"
          title="Chưa có sơ đồ chỗ cho hạng này"
          detail="Bạn quay lại chọn hạng chỗ khác giúp chúng tôi nhé."
          action={<Button onClick={() => navigate("/chuyen-tau")}>Chọn hạng khác</Button>}
        />
      ) : (
        <>
          {/* Chọn toa */}
          <div>
            <p className="mb-1.5 text-sm font-semibold text-ink-800">Chọn toa</p>
            <div className="flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="Danh sách toa">
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
                      active ? "border-son-600 bg-son-50" : "border-ink-200 bg-white hover:border-ink-300",
                      full && "cursor-not-allowed opacity-55",
                    )}
                  >
                    <div className="font-bold text-ink-900">Toa {c.number}</div>
                    <div className={cn("text-xs font-medium", c.available === 0 ? "text-ink-500" : c.available <= 8 ? "text-son-700" : "text-ok-600")}>
                      {c.available === 0 ? "Hết chỗ" : `Còn ${c.available} chỗ`}
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
            <div role="status" className="rounded-2xl border-2 border-mai-300 bg-mai-50 px-4 py-3">
              <p className="font-semibold text-ink-900">Gợi ý chỗ trống gần đó</p>
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
                      className="min-h-11 rounded-xl border-2 border-mai-400 bg-white px-3 font-semibold text-ink-900 hover:bg-mai-100"
                    >
                      Chỗ {seat.label}
                      {seat.berthLevel ? ` · tầng ${seat.berthLevel}` : ""}
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
                    Đã chọn <span className="font-semibold text-ink-900">{selected.length}/{maxSeats}</span> chỗ
                  </p>
                  <div className="mt-0.5 flex flex-wrap gap-1">
                    {selected.map((s) => (
                      <Badge key={s.id} tone="son">Toa {carriage?.number} · chỗ {s.label}</Badge>
                    ))}
                  </div>
                </div>
                <p className="tnum shrink-0 text-lg font-bold text-son-700">{formatVnd(total)}</p>
              </div>
            }
          >
            <Button
              size="lg"
              fullWidth
              loading={holding}
              loadingText="Đang giữ chỗ cho bạn…"
              disabled={selected.length !== maxSeats}
              onClick={() => void holdSeats().catch(() => {})}
            >
              {selected.length === maxSeats
                ? `Giữ ${maxSeats} chỗ trong 10 phút`
                : `Chọn thêm ${maxSeats - selected.length} chỗ`}
            </Button>
          </StickyActionBar>
        </>
      )}
    </div>
  );
}
