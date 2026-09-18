import { useRef } from "react";
import type { Carriage, Seat } from "@/api/types";
import { formatVnd } from "@/lib/format";
import { cn } from "@/lib/cn";
import { Skeleton } from "./ui/StateView";

/**
 * Sơ đồ toa tàu.
 *
 * Bốn quyết định thiết kế chính:
 *
 * 1. KHÔNG CHỈ DÙNG MÀU để phân biệt trạng thái. Mỗi trạng thái có thêm ký hiệu
 *    riêng (✓ đang chọn, ✕ đã bán, ⏳ người khác giữ) và viền khác nhau, để
 *    người mù màu — khoảng 8% nam giới — vẫn dùng được.
 *
 * 2. Ghế là <button> thật, không phải <div onClick>. Nhờ đó Tab/Enter/Space
 *    hoạt động sẵn, và mỗi ghế có aria-label đầy đủ: "Chỗ 12, tầng 1, 1.250.000 ₫, còn trống".
 *
 * 3. Ô ghế tối thiểu 44×44px kể cả trên màn hình nhỏ; toa dài thì cuộn ngang
 *    chứ không thu nhỏ ghế — bấm nhầm ghế khi mạng đang đông là rất tốn thời gian.
 *
 * 4. Ghế vừa bị người khác giật mất được đánh dấu riêng (justTakenSeatIds) và
 *    ghế gợi ý thay thế có vòng nhấp nháy, để mắt tìm thấy ngay.
 */
export type SeatMapProps = {
  carriage: Carriage;
  selectedSeatIds: string[];
  onToggleSeat: (seat: Seat) => void;
  /** Số chỗ tối đa được chọn (= số hành khách) */
  maxSelectable: number;
  /** Ghế vừa bị mất trong lần giữ chỗ hỏng gần nhất */
  justTakenSeatIds?: string[];
  /** Ghế hệ thống gợi ý thay thế */
  suggestedSeatIds?: string[];
};

export function SeatMap({
  carriage, selectedSeatIds, onToggleSeat, maxSelectable, justTakenSeatIds = [], suggestedSeatIds = [],
}: SeatMapProps) {
  const isBerth = carriage.layout !== "seat-2-2";
  const gridRef = useRef<HTMLDivElement>(null);

  // Nhóm ghế theo hàng (toa ngồi) hoặc theo khoang (toa nằm)
  const groups = new Map<number, Seat[]>();
  for (const s of carriage.seats) {
    const key = s.row;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)!.push(s);
  }

  return (
    <div>
      <SeatLegend />
      <div className="mt-3 overflow-x-auto pb-2">
        <div
          ref={gridRef}
          role="group"
          aria-label={`Sơ đồ toa ${carriage.number}, còn ${carriage.available} chỗ trống`}
          className="mx-auto w-fit rounded-3xl border-2 border-ink-300 bg-ink-50 p-3"
        >
          {/* Đầu toa — giúp người dùng định hướng trước/sau toa */}
          <div className="mb-2 flex items-center justify-between px-2 text-xs font-semibold text-ink-500">
            <span aria-hidden>◄ Đầu tàu</span>
            <span>Toa {carriage.number}</span>
            <span aria-hidden>Cuối tàu ►</span>
          </div>

          <div className="flex flex-col gap-1.5">
            {[...groups.entries()].map(([rowKey, seats]) => (
              <div key={rowKey} className="flex items-center gap-2">
                <span className="w-10 shrink-0 text-right text-xs font-medium text-ink-500" aria-hidden>
                  {isBerth ? `Kh.${rowKey}` : `H.${rowKey}`}
                </span>
                <div className={cn("flex gap-1.5", isBerth && "rounded-xl bg-white/70 p-1.5 ring-1 ring-ink-200")}>
                  {seats.map((seat, i) => (
                    <span key={seat.id} className="flex items-center gap-1.5">
                      <SeatButton
                        seat={seat}
                        selected={selectedSeatIds.includes(seat.id)}
                        justTaken={justTakenSeatIds.includes(seat.id)}
                        suggested={suggestedSeatIds.includes(seat.id)}
                        disabledByLimit={
                          selectedSeatIds.length >= maxSelectable && !selectedSeatIds.includes(seat.id)
                        }
                        onToggle={onToggleSeat}
                      />
                      {/* Lối đi: giữa hai cặp ghế (toa ngồi) hoặc giữa hai dãy giường */}
                      {i === seats.length / 2 - 1 && (
                        <span aria-hidden className="mx-0.5 h-10 w-px bg-ink-300" />
                      )}
                    </span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
      {isBerth && (
        <p className="mt-2 text-center text-xs text-ink-500">
          Mỗi khung là một khoang. Giường tầng 1 thuận tiện hơn nên giá cao hơn tầng 2 và 3.
        </p>
      )}
    </div>
  );
}

function SeatButton({
  seat, selected, justTaken, suggested, disabledByLimit, onToggle,
}: {
  seat: Seat;
  selected: boolean;
  justTaken: boolean;
  suggested: boolean;
  disabledByLimit: boolean;
  onToggle: (seat: Seat) => void;
}) {
  const unavailable = seat.status !== "available";
  const disabled = unavailable || disabledByLimit;

  const statusText = selected
    ? "bạn đang chọn"
    : seat.status === "sold"
      ? "đã bán"
      : seat.status === "held"
        ? justTaken
          ? "vừa bị hành khách khác giữ mất"
          : "hành khách khác đang giữ"
        : disabledByLimit
          ? "còn trống, nhưng bạn đã chọn đủ số chỗ"
          : "còn trống";

  const mark = selected ? "✓" : seat.status === "sold" ? "✕" : seat.status === "held" ? "⏳" : null;

  return (
    <button
      type="button"
      disabled={disabled}
      aria-pressed={selected}
      onClick={() => onToggle(seat)}
      title={`Chỗ ${seat.label} — ${formatVnd(seat.price)}`}
      aria-label={[
        `Chỗ ${seat.label}`,
        seat.berthLevel ? `tầng ${seat.berthLevel}` : null,
        formatVnd(seat.price),
        statusText,
      ].filter(Boolean).join(", ")}
      className={cn(
        "relative grid size-11 shrink-0 place-items-center rounded-lg border-2 text-sm font-bold transition-all",
        // còn trống
        !unavailable && !selected && !disabledByLimit &&
          "border-ok-600/40 bg-white text-ink-900 hover:border-son-500 hover:bg-son-50 active:scale-95",
        // đang chọn — đảo màu để nổi bật nhất trên sơ đồ
        selected && "border-son-700 bg-son-600 text-white shadow-[var(--shadow-soft)]",
        // người khác giữ
        seat.status === "held" && !selected && "cursor-not-allowed border-mai-300 bg-mai-50 text-mai-700",
        // đã bán
        seat.status === "sold" && "cursor-not-allowed border-ink-200 bg-ink-200 text-ink-500",
        // trống nhưng đã chọn đủ số chỗ
        !unavailable && disabledByLimit && !selected && "cursor-not-allowed border-ink-200 bg-white text-ink-400",
        justTaken && "animate-pulse ring-3 ring-son-400",
        suggested && "ring-3 ring-mai-400 ring-offset-1",
      )}
    >
      <span className="leading-none">{seat.label}</span>
      {mark && (
        <span aria-hidden className="absolute -right-0.5 -top-0.5 text-[10px] leading-none">
          {mark}
        </span>
      )}
      {seat.berthLevel && (
        <span aria-hidden className="absolute bottom-0.5 text-[9px] font-medium opacity-70">
          T{seat.berthLevel}
        </span>
      )}
    </button>
  );
}

export function SeatLegend() {
  const items = [
    { cls: "border-ok-600/40 bg-white", mark: "", label: "Còn trống" },
    { cls: "border-son-700 bg-son-600 text-white", mark: "✓", label: "Bạn đang chọn" },
    { cls: "border-mai-300 bg-mai-50 text-mai-700", mark: "⏳", label: "Người khác đang giữ" },
    { cls: "border-ink-200 bg-ink-200 text-ink-500", mark: "✕", label: "Đã bán" },
  ];
  return (
    <ul className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2">
      {items.map((it) => (
        <li key={it.label} className="flex items-center gap-1.5 text-sm text-ink-700">
          <span aria-hidden className={cn("grid size-6 place-items-center rounded-md border-2 text-[10px] font-bold", it.cls)}>
            {it.mark}
          </span>
          {it.label}
        </li>
      ))}
    </ul>
  );
}

export function SeatMapSkeleton() {
  return (
    <div className="mx-auto w-fit rounded-3xl border-2 border-ink-200 bg-ink-50 p-3">
      <div className="flex flex-col gap-1.5">
        {Array.from({ length: 8 }).map((_, r) => (
          <div key={r} className="flex gap-1.5">
            {Array.from({ length: 5 }).map((_, c) => (
              <Skeleton key={c} className="size-11 rounded-lg" />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
