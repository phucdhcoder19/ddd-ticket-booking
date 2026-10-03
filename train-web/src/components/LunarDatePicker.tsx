import { useMemo, useState } from "react";
import { getDayInfo, peakText, type PeakLevel } from "@/lib/lunar";
import { formatDate, toDateKey, weekdayShort } from "@/lib/format";
import { cn } from "@/lib/cn";

const MONTHS = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December",
];

/**
 * Departure date picker showing the solar and lunar calendars side by side.
 *
 * For Lunar New Year tickets, the lunar date is how people remember their plans
 * ("going home on the 28th", "back on day 6"), so the lunar date is shown under
 * the solar date in every cell, not hidden behind a tap. Peak days get a light
 * red background plus a text label ("29/12 lunar") — not colour alone.
 */
export function LunarDatePicker({
  value, onChange, minDate, maxMonthsAhead = 6,
}: {
  value: string;                 // yyyy-MM-dd
  onChange: (dateKey: string) => void;
  minDate?: Date;
  maxMonthsAhead?: number;
}) {
  const selected = useMemo(() => {
    const [y, m, d] = value.split("-").map(Number);
    return new Date(y, m - 1, d);
  }, [value]);

  const [viewMonth, setViewMonth] = useState(() => new Date(selected.getFullYear(), selected.getMonth(), 1));

  const today = useMemo(() => {
    const t = new Date();
    t.setHours(0, 0, 0, 0);
    return t;
  }, []);
  const min = minDate ?? today;
  const maxMonth = useMemo(() => {
    const m = new Date(today.getFullYear(), today.getMonth() + maxMonthsAhead, 1);
    return m;
  }, [today, maxMonthsAhead]);

  const days = useMemo(() => {
    const first = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 1);
    const last = new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 0);
    // Weeks start on Monday
    const leading = (first.getDay() + 6) % 7;
    const cells: Array<Date | null> = Array.from({ length: leading }, () => null);
    for (let d = 1; d <= last.getDate(); d++) {
      cells.push(new Date(viewMonth.getFullYear(), viewMonth.getMonth(), d));
    }
    return cells;
  }, [viewMonth]);

  const canGoPrev = viewMonth > new Date(min.getFullYear(), min.getMonth(), 1);
  const canGoNext = viewMonth < maxMonth;

  const shift = (delta: number) =>
    setViewMonth((m) => new Date(m.getFullYear(), m.getMonth() + delta, 1));

  return (
    <div className="rounded-2xl border-2 border-ink-200 bg-white p-3">
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => shift(-1)}
          disabled={!canGoPrev}
          aria-label="Previous month"
          className="grid size-11 place-items-center rounded-xl text-xl text-ink-700 hover:bg-ink-100 disabled:opacity-35"
        >
          ‹
        </button>
        <div className="text-center">
          <div className="font-bold text-ink-900" aria-live="polite">
            {MONTHS[viewMonth.getMonth()]} {viewMonth.getFullYear()}
          </div>
          <div className="text-xs text-ink-500">Solar calendar · lunar date in small print below</div>
        </div>
        <button
          type="button"
          onClick={() => shift(1)}
          disabled={!canGoNext}
          aria-label="Next month"
          className="grid size-11 place-items-center rounded-xl text-xl text-ink-700 hover:bg-ink-100 disabled:opacity-35"
        >
          ›
        </button>
      </div>

      <div className="mt-2 grid grid-cols-7 gap-1 text-center text-xs font-semibold text-ink-500">
        {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map((d) => (
          <div key={d} className="py-1">{d}</div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1" role="grid" aria-label="Choose a departure date">
        {days.map((date, i) => {
          if (!date) return <div key={`e${i}`} />;
          const key = toDateKey(date);
          const info = getDayInfo(date);
          const isSelected = key === value;
          const isPast = date < min;
          const isToday = toDateKey(date) === toDateKey(today);

          return (
            <button
              key={key}
              type="button"
              role="gridcell"
              disabled={isPast}
              aria-selected={isSelected}
              aria-label={`${weekdayShort(date)} ${formatDate(date)}, lunar ${info.lunarText}${
                info.peak !== "none" ? `, ${peakText[info.peak]}` : ""
              }`}
              onClick={() => onChange(key)}
              className={cn(
                "flex min-h-14 flex-col items-center justify-center rounded-xl border-2 px-0.5 py-1 transition-colors",
                isPast && "cursor-not-allowed border-transparent text-ink-300",
                !isPast && !isSelected && info.peak === "none" && "border-transparent text-ink-900 hover:bg-ink-100",
                !isPast && !isSelected && info.peak === "high" && "border-accent-200 bg-accent-50 text-ink-900 hover:bg-accent-100",
                !isPast && !isSelected && info.peak === "peak" && "border-brand-200 bg-brand-50 text-brand-800 hover:bg-brand-100",
                isSelected && "border-brand-700 bg-brand-600 text-white",
                isToday && !isSelected && "ring-1 ring-ink-400",
              )}
            >
              <span className="tnum text-base leading-tight font-semibold">{date.getDate()}</span>
              <span className={cn("text-[10px] leading-tight", isSelected ? "text-brand-100" : "text-ink-500")}>
                {info.peakLabel ?? info.lunarText}
              </span>
            </button>
          );
        })}
      </div>

      <PeakLegend />
    </div>
  );
}

function PeakLegend() {
  const items: Array<{ peak: PeakLevel; cls: string }> = [
    { peak: "peak", cls: "border-brand-200 bg-brand-50" },
    { peak: "high", cls: "border-accent-200 bg-accent-50" },
  ];
  return (
    <ul className="mt-3 flex flex-wrap items-center justify-center gap-x-4 gap-y-1 border-t border-ink-100 pt-3 text-xs text-ink-600">
      {items.map((it) => (
        <li key={it.peak} className="flex items-center gap-1.5">
          <span aria-hidden className={cn("size-4 rounded border-2", it.cls)} />
          {peakText[it.peak]} — scarce tickets, higher fares
        </li>
      ))}
    </ul>
  );
}
