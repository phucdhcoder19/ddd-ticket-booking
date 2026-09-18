import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import { useAsync } from "@/hooks/useAsync";
import { useBooking } from "@/store/BookingContext";
import { SaleCountdown } from "@/components/CountdownTimer";
import { LunarDatePicker } from "@/components/LunarDatePicker";
import { Button } from "@/components/ui/Button";
import { SelectInput } from "@/components/ui/Field";
import { Badge } from "@/components/ui/Badge";
import { ErrorState, Skeleton } from "@/components/ui/StateView";
import { formatDate, fromDateKey, toDateKey, weekdayLabel } from "@/lib/format";
import { getDayInfo, peakText } from "@/lib/lunar";

/**
 * Màn 1 — Tìm chuyến.
 *
 * Thứ tự trên màn hình bám theo thứ tự người dùng nghĩ: "tôi đi từ đâu, về
 * đâu, ngày nào, mấy người". Lịch mở sẵn ngay trong trang (không giấu sau
 * modal) vì chọn ngày là bước tốn thời gian nhất của vé Tết.
 */
export function HomePage() {
  const navigate = useNavigate();
  const { query, setQuery } = useBooking();

  const stations = useAsync((o) => api.getStations(o), []);
  const sale = useAsync((o) => api.getSaleWindow(o), []);

  const [from, setFrom] = useState(query?.from ?? "HNO");
  const [to, setTo] = useState(query?.to ?? "SGO");
  const [date, setDate] = useState(query?.date ?? defaultTetDate());
  const [passengers, setPassengers] = useState(query?.passengers ?? 1);

  const sameStation = from === to;
  const dayInfo = useMemo(() => getDayInfo(fromDateKey(date)), [date]);

  const stationOptions = (stations.data ?? []).map((s) => ({
    value: s.code,
    label: `${s.name} (${s.region})`,
  }));

  const swap = () => {
    setFrom(to);
    setTo(from);
  };

  const submit = () => {
    if (sameStation) return;
    setQuery({ from, to, date, passengers });
    // Mọi lượt mua đều đi qua phòng chờ; phòng chờ tự cho qua khi hệ thống rảnh
    navigate("/phong-cho");
  };

  return (
    <div className="flex flex-col gap-5">
      {/* Banner đếm ngược mở bán */}
      <section
        aria-label="Thông tin đợt mở bán"
        className="bg-mai-pattern overflow-hidden rounded-3xl bg-gradient-to-br from-son-700 via-son-600 to-son-700 px-5 py-5 text-white shadow-[var(--shadow-lift)]"
      >
        <div className="flex items-center gap-2">
          <Badge tone="mai" className="border-mai-200/40 bg-white/15 text-mai-50">🌸 Tết Đinh Mùi 2027</Badge>
        </div>
        <h1 className="mt-2 text-2xl font-bold sm:text-3xl">Về nhà đón Tết</h1>
        <p className="mt-1 text-mai-100">Đặt vé tàu Tết trực tuyến, giữ chỗ 10 phút để nhập thông tin.</p>
        <div className="mt-4">
          {sale.loading ? (
            <Skeleton className="h-20 w-full bg-white/20" />
          ) : sale.error ? (
            <p className="rounded-xl bg-white/15 px-4 py-3 text-sm">
              Chưa lấy được lịch mở bán. Bạn vẫn có thể tìm chuyến bên dưới.
            </p>
          ) : sale.data ? (
            <SaleCountdown opensAt={sale.data.opensAt} label={sale.data.label} />
          ) : null}
        </div>
      </section>

      {/* Form tìm chuyến */}
      <section aria-labelledby="tim-chuyen" className="rounded-3xl border border-ink-200 bg-white p-4 shadow-[var(--shadow-soft)] sm:p-5">
        <h2 id="tim-chuyen" className="text-lg font-bold">Tìm chuyến tàu</h2>

        {stations.loading ? (
          <div className="mt-3 flex flex-col gap-3">
            <Skeleton className="h-16 w-full" />
            <Skeleton className="h-16 w-full" />
          </div>
        ) : stations.error ? (
          <ErrorState error={stations.error} onRetry={stations.reload} compact />
        ) : (
          <>
            <div className="mt-3 flex flex-col gap-1">
              <SelectInput
                label="Ga đi"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                options={stationOptions}
                required
              />
              <div className="-my-1 flex justify-center">
                <button
                  type="button"
                  onClick={swap}
                  aria-label="Đổi chiều ga đi và ga đến"
                  className="grid size-11 place-items-center rounded-full border-2 border-ink-200 bg-white text-son-600 transition-transform hover:border-son-300 active:rotate-180"
                >
                  <span aria-hidden className="text-lg">⇅</span>
                </button>
              </div>
              <SelectInput
                label="Ga đến"
                value={to}
                onChange={(e) => setTo(e.target.value)}
                options={stationOptions}
                error={sameStation ? "Ga đến phải khác ga đi." : null}
                required
              />
            </div>

            <div className="mt-2">
              <p className="mb-1.5 text-sm font-semibold text-ink-800">
                Ngày đi <span className="text-son-600" aria-hidden>*</span>
              </p>
              <div className="mb-2 flex flex-wrap items-center gap-2 rounded-xl bg-ink-50 px-3 py-2">
                <span className="font-semibold text-ink-900">
                  {weekdayLabel(fromDateKey(date))}, {formatDate(fromDateKey(date))}
                </span>
                <span className="text-sm text-ink-600">(âm lịch {dayInfo.lunarText})</span>
                {dayInfo.peak !== "none" && (
                  <Badge tone={dayInfo.peak === "peak" ? "son" : "mai"}>{peakText[dayInfo.peak]}</Badge>
                )}
              </div>
              <LunarDatePicker value={date} onChange={setDate} />
            </div>

            <div className="mt-3">
              <SelectInput
                label="Số hành khách"
                hint="Tối đa 4 chỗ cho mỗi lần đặt trong đợt cao điểm."
                value={String(passengers)}
                onChange={(e) => setPassengers(Number(e.target.value))}
                options={[1, 2, 3, 4].map((n) => ({ value: String(n), label: `${n} người` }))}
              />
            </div>

            <Button size="lg" fullWidth className="mt-2" onClick={submit} disabled={sameStation}>
              Tìm chuyến tàu
            </Button>

            {dayInfo.peak === "peak" && (
              <p className="mt-3 rounded-xl border border-mai-200 bg-mai-50 px-3 py-2 text-sm text-ink-700">
                <strong>Lưu ý:</strong> đây là ngày cao điểm Tết, vé thường hết rất nhanh. Bạn nên chuẩn bị sẵn
                số CCCD của tất cả hành khách trước khi bấm tìm chuyến.
              </p>
            )}
          </>
        )}
      </section>

      <QuickTips />
    </div>
  );
}

function QuickTips() {
  const tips = [
    { icon: "⏱", title: "Giữ chỗ 10 phút", detail: "Sau khi chọn chỗ, bạn có 10 phút để nhập thông tin và thanh toán." },
    { icon: "🪪", title: "Chuẩn bị CCCD", detail: "Mỗi vé cần đúng họ tên và số CCCD của người đi tàu." },
    { icon: "📶", title: "Mạng chậm vẫn được", detail: "Nếu hệ thống quá tải, trang sẽ tự thử lại giúp bạn." },
  ];
  return (
    <section aria-label="Lưu ý khi đặt vé" className="grid gap-2 sm:grid-cols-3">
      {tips.map((t) => (
        <div key={t.title} className="rounded-2xl border border-ink-200 bg-white p-3">
          <span aria-hidden className="text-xl">{t.icon}</span>
          <h3 className="mt-1 font-semibold text-ink-900">{t.title}</h3>
          <p className="text-sm text-ink-600">{t.detail}</p>
        </div>
      ))}
    </section>
  );
}

/** Mặc định nhắm tới đợt cao điểm gần nhất: 27 tháng Chạp của cái Tết sắp tới */
function defaultTetDate(): string {
  const today = new Date();
  for (let i = 1; i <= 400; i++) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
    const info = getDayInfo(d);
    if (info.lunar.month === 12 && info.lunar.day === 27) return toDateKey(d);
  }
  return toDateKey(new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7));
}
