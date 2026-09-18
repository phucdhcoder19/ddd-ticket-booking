import { useMemo, useState, type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "@/api/client";
import { useAsync } from "@/hooks/useAsync";
import { useBooking } from "@/store/BookingContext";
import { SaleCountdown } from "@/components/CountdownTimer";
import { LunarDatePicker } from "@/components/LunarDatePicker";
import { ErrorState, Skeleton } from "@/components/ui/StateView";
import { formatDate, formatVnd, fromDateKey, toDateKey, weekdayLabel } from "@/lib/format";
import { getDayInfo, peakText } from "@/lib/lunar";

/**
 * Trang chủ theo bố cục Traveloka:
 *   hero ảnh tối + tab sản phẩm + hộp tìm kiếm nổi đè lên mép ảnh
 *   → mã giảm giá → ưu đãi → vé tàu giá tốt → chuyến phổ biến
 *   → khám phá → đăng ký nhận tin
 *
 * Phần nghiệp vụ thật (ga đi, ga đến, ngày, số khách → phòng chờ) nằm nguyên
 * trong hộp tìm kiếm; các dải bên dưới là nội dung giới thiệu.
 */
export function HomePage() {
  return (
    <>
      <Hero />
      <PromoCodes />
      <DealBanners />
      <PriceDeals />
      <PopularTrains />
      <ExploreLinks />
      <Newsletter />
    </>
  );
}

/* ─────────────────────────── HERO + TÌM KIẾM ─────────────────────────── */

const PRODUCT_TABS = [
  { icon: "🚆", label: "Vé tàu" },
  { icon: "🚌", label: "Vé xe khách" },
  { icon: "🏨", label: "Khách sạn" },
  { icon: "🚐", label: "Đưa đón ga" },
  { icon: "🚗", label: "Cho thuê xe" },
  { icon: "🎡", label: "Hoạt động & Vui chơi" },
];

const SEAT_FILTERS = ["Tất cả", "Ngồi mềm", "Khoang 4", "Khoang 6"];

function Hero() {
  const navigate = useNavigate();
  const { query, setQuery } = useBooking();

  const stations = useAsync((o) => api.getStations(o), []);
  const sale = useAsync((o) => api.getSaleWindow(o), []);

  const [from, setFrom] = useState(query?.from ?? "HNO");
  const [to, setTo] = useState(query?.to ?? "SGO");
  const [date, setDate] = useState(query?.date ?? defaultTetDate());
  const [passengers, setPassengers] = useState(query?.passengers ?? 1);
  const [seatFilter, setSeatFilter] = useState(SEAT_FILTERS[0]);
  const [pickerOpen, setPickerOpen] = useState(false);

  const sameStation = from === to;
  const dayInfo = useMemo(() => getDayInfo(fromDateKey(date)), [date]);
  const stationList = stations.data ?? [];

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
    <section className="relative">
      <HeroBackdrop />

      <div className="relative mx-auto max-w-[1200px] px-4 pb-32 pt-12 sm:px-6 sm:pb-36 sm:pt-16">
        <h1 className="text-center text-[28px] font-bold leading-tight text-white sm:text-[38px]">
          Về nhà đón Tết, đặt vé trong một chạm
        </h1>

        {/* Tab sản phẩm */}
        <div className="no-scrollbar mt-8 flex justify-start gap-1 overflow-x-auto sm:justify-center">
          {PRODUCT_TABS.map((t, i) => (
            <button
              key={t.label}
              type="button"
              aria-current={i === 0 ? "true" : undefined}
              className={
                i === 0
                  ? "flex shrink-0 items-center gap-2 rounded-full bg-white px-4 py-2.5 text-sm font-bold text-ink-900 shadow-[var(--shadow-lift)]"
                  : "flex shrink-0 items-center gap-2 rounded-full px-4 py-2.5 text-sm font-semibold text-white/85 hover:bg-white/15"
              }
            >
              <span aria-hidden className="text-base">{t.icon}</span>
              {t.label}
            </button>
          ))}
        </div>
        <div className="mt-6 border-t border-white/25" />

        {/* Bộ lọc loại chỗ */}
        <div className="no-scrollbar mt-5 flex gap-2 overflow-x-auto">
          {SEAT_FILTERS.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setSeatFilter(f)}
              aria-pressed={seatFilter === f}
              className={
                seatFilter === f
                  ? "shrink-0 rounded-full bg-son-500 px-4 py-1.5 text-sm font-bold text-white"
                  : "shrink-0 rounded-full bg-white/15 px-4 py-1.5 text-sm font-semibold text-white hover:bg-white/25"
              }
            >
              {f}
            </button>
          ))}
        </div>

        {/* Hộp tìm kiếm */}
        <div className="mt-4 rounded-lg bg-white p-3 shadow-[var(--shadow-float)] sm:p-4">
          {stations.loading ? (
            <div className="flex flex-col gap-3 sm:flex-row">
              <Skeleton className="h-16 flex-1" />
              <Skeleton className="h-16 flex-1" />
              <Skeleton className="h-16 flex-1" />
            </div>
          ) : stations.error ? (
            <ErrorState error={stations.error} onRetry={stations.reload} compact />
          ) : (
            <>
              <div className="grid gap-2 sm:grid-cols-[1fr_1fr_1.1fr_0.9fr_auto]">
                <SearchField label="Ga đi" icon="📍">
                  <select
                    value={from}
                    onChange={(e) => setFrom(e.target.value)}
                    aria-label="Ga đi"
                    className="w-full cursor-pointer bg-transparent text-base font-bold text-ink-900 outline-none"
                  >
                    {stationList.map((s) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                </SearchField>

                <SearchField label="Ga đến" icon="🏁" error={sameStation ? "Ga đến phải khác ga đi" : null}>
                  <select
                    value={to}
                    onChange={(e) => setTo(e.target.value)}
                    aria-label="Ga đến"
                    className="w-full cursor-pointer bg-transparent text-base font-bold text-ink-900 outline-none"
                  >
                    {stationList.map((s) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={swap}
                    aria-label="Đổi chiều ga đi và ga đến"
                    className="absolute -top-4 left-[-22px] hidden size-9 place-items-center rounded-full border border-ink-200 bg-white text-son-600 shadow-[var(--shadow-soft)] hover:border-son-300 sm:grid"
                  >
                    <span aria-hidden>⇄</span>
                  </button>
                </SearchField>

                <SearchField label="Ngày đi" icon="📅">
                  <button
                    type="button"
                    onClick={() => setPickerOpen((v) => !v)}
                    aria-expanded={pickerOpen}
                    className="w-full truncate text-left text-base font-bold text-ink-900"
                  >
                    {weekdayLabel(fromDateKey(date))}, {formatDate(fromDateKey(date))}
                  </button>
                </SearchField>

                <SearchField label="Số hành khách" icon="👤">
                  <select
                    value={String(passengers)}
                    onChange={(e) => setPassengers(Number(e.target.value))}
                    aria-label="Số hành khách"
                    className="w-full cursor-pointer bg-transparent text-base font-bold text-ink-900 outline-none"
                  >
                    {[1, 2, 3, 4].map((n) => (
                      <option key={n} value={n}>{n} người</option>
                    ))}
                  </select>
                </SearchField>

                <button
                  type="button"
                  onClick={submit}
                  disabled={sameStation}
                  className="grid min-h-14 place-items-center rounded-md bg-mai-500 px-6 text-base font-bold text-white hover:bg-mai-600 disabled:cursor-not-allowed disabled:bg-ink-300 sm:px-5"
                >
                  <span className="sm:hidden">Tìm chuyến tàu</span>
                  <span aria-hidden className="hidden text-xl sm:block">🔍</span>
                  <span className="sr-only hidden sm:block">Tìm chuyến tàu</span>
                </button>
              </div>

              {pickerOpen && (
                <div className="mt-3 rounded-md border border-ink-200 p-3">
                  <div className="mb-2 flex flex-wrap items-center gap-2">
                    <span className="text-sm text-ink-600">Âm lịch {dayInfo.lunarText}</span>
                    {dayInfo.peak !== "none" && (
                      <span className="rounded-full bg-mai-50 px-2.5 py-0.5 text-xs font-bold text-mai-700">
                        {peakText[dayInfo.peak]}
                      </span>
                    )}
                  </div>
                  <LunarDatePicker value={date} onChange={(d) => { setDate(d); setPickerOpen(false); }} />
                </div>
              )}

              {dayInfo.peak === "peak" && !pickerOpen && (
                <p className="mt-3 rounded-md bg-mai-50 px-3 py-2 text-sm text-ink-700">
                  Ngày cao điểm Tết, vé hết rất nhanh. Chuẩn bị sẵn số CCCD của tất cả hành khách trước khi tìm chuyến.
                </p>
              )}
            </>
          )}
        </div>

        {/* Đếm ngược mở bán */}
        <div className="mt-4">
          {sale.loading ? (
            <Skeleton className="h-16 w-full bg-white/20" />
          ) : sale.data ? (
            <SaleCountdown opensAt={sale.data.opensAt} label={sale.data.label} />
          ) : null}
        </div>
      </div>

      {/* Dải đối tác đè lên mép dưới ảnh hero */}
      <div className="relative mx-auto -mb-10 max-w-[1000px] px-4 sm:px-6">
        <div className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3 rounded-lg bg-white px-6 py-5 shadow-[var(--shadow-lift)]">
          <span className="text-sm text-ink-500">Đối tác vận chuyển</span>
          {["Đường sắt Việt Nam", "Sài Gòn Railways", "Hà Nội Railways", "Ratraco"].map((p) => (
            <span key={p} className="text-sm font-bold text-ink-700">{p}</span>
          ))}
        </div>
      </div>
    </section>
  );
}

/**
 * Nen hero: troi dem chuyen sac + day nui nhieu lop, dung bang SVG.
 * Khong dung anh chup vi anh phai du lon moi khong vo o man rong,
 * ma tai ve nang; ve vector thi net o moi do phan giai va luon render duoc.
 */
function HeroBackdrop() {
  return (
    <div aria-hidden className="absolute inset-0 overflow-hidden bg-son-950">
      <div
        className="absolute inset-0"
        style={{
          backgroundImage:
            "linear-gradient(180deg,#071a2d 0%,#0d2c4a 38%,#1b4f78 68%,#2f7ba6 88%,#5aa8c9 100%)",
        }}
      />
      {/* Quang sang chan troi */}
      <div
        className="absolute inset-x-0 bottom-0 h-1/2"
        style={{
          backgroundImage:
            "radial-gradient(70% 100% at 50% 100%, rgb(255 196 128 / 0.35) 0%, transparent 70%)",
        }}
      />
      <svg viewBox="0 0 1440 520" preserveAspectRatio="none" className="absolute inset-0 size-full">
        {/* Sao */}
        <g fill="#ffffff">
          {STARS.map((st, i) => (
            <circle key={i} cx={st[0]} cy={st[1]} r={st[2]} opacity={st[3]} />
          ))}
        </g>
        {/* Day nui xa */}
        <path
          d="M0 520 L0 330 L150 250 L280 320 L420 214 L560 306 L700 236 L860 320 L1010 246 L1160 322 L1300 262 L1440 330 L1440 520Z"
          fill="#0d2c4a"
          opacity="0.85"
        />
        {/* Day nui gan */}
        <path
          d="M0 520 L0 404 L180 340 L330 412 L500 322 L660 420 L820 356 L980 430 L1140 368 L1300 436 L1440 390 L1440 520Z"
          fill="#071a2d"
          opacity="0.92"
        />
      </svg>
      {/* Lop phu de chu trang luon doc duoc */}
      <div className="hero-scrim absolute inset-0" />
    </div>
  );
}

/** [x, y, r, opacity] — rai tay cho tu nhien, khong dung random de khong nhay moi lan render */
const STARS: [number, number, number, number][] = [
  [90, 48, 1.4, 0.9], [210, 96, 1, 0.6], [318, 40, 1.6, 0.75], [402, 128, 1, 0.5],
  [520, 62, 1.2, 0.8], [640, 110, 1, 0.55], [712, 44, 1.5, 0.85], [820, 92, 1.1, 0.6],
  [930, 56, 1.3, 0.7], [1040, 120, 1, 0.5], [1128, 52, 1.6, 0.8], [1240, 100, 1.1, 0.6],
  [1352, 60, 1.3, 0.75], [160, 160, 1, 0.45], [470, 176, 1, 0.4], [880, 168, 1, 0.45],
  [1300, 172, 1, 0.4], [600, 24, 1, 0.5], [1000, 28, 1.2, 0.6],
];

function SearchField({
  label,
  icon,
  error,
  children,
}: {
  label: string;
  icon: string;
  error?: string | null;
  children: ReactNode;
}) {
  return (
    <div className="relative">
      <span className="mb-1 block text-xs font-semibold text-ink-500">{label}</span>
      <div
        className={
          "flex min-h-14 items-center gap-2 rounded-md border px-3 " +
          (error ? "border-mai-500 bg-mai-50" : "border-ink-200 bg-white")
        }
      >
        <span aria-hidden className="shrink-0 text-base">{icon}</span>
        <div className="min-w-0 flex-1">{children}</div>
      </div>
      {error && <p className="mt-1 text-xs font-semibold text-mai-700">{error}</p>}
    </div>
  );
}

/* ─────────────────────────── MÃ GIẢM GIÁ ─────────────────────────── */

const PROMOS = [
  { icon: "🚆", tint: "bg-son-50 text-son-600", title: "Giảm đến 75.000đ cho lần đặt vé tàu đầu tiên", code: "VETAUMOI" },
  { icon: "🏨", tint: "bg-ok-50 text-ok-600", title: "Giảm tới 250.000đ cho lần đặt phòng đầu tiên", code: "VETAUMOI" },
  { icon: "🎡", tint: "bg-mai-50 text-mai-600", title: "Giảm đến 10% cho lần đặt vé tham quan", code: "VETAUMOI" },
];

function PromoCodes() {
  const [copied, setCopied] = useState<number | null>(null);

  const copy = async (code: string, i: number) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(i);
      setTimeout(() => setCopied(null), 2000);
    } catch {
      setCopied(null);
    }
  };

  return (
    <Section title="Mã giảm giá cho người dùng mới" subtitle="Áp dụng cho lần đặt chỗ đầu tiên trên ứng dụng" pad="pt-20">
      <div className="grid gap-4 sm:grid-cols-3">
        {PROMOS.map((p, i) => (
          <div key={p.title} className="overflow-hidden rounded-lg border border-ink-200 bg-white">
            <div className="flex gap-3 p-4">
              <span aria-hidden className={`grid size-8 shrink-0 place-items-center rounded-full ${p.tint}`}>
                {p.icon}
              </span>
              <div className="min-w-0">
                <h3 className="text-[15px] font-bold leading-snug text-ink-900">{p.title}</h3>
                <p className="mt-1 truncate text-sm text-ink-500">Áp dụng cho lần đặt đầu tiên trên ứng dụng</p>
              </div>
            </div>
            <div className="flex items-center justify-between gap-3 border-t border-dashed border-ink-200 bg-ink-50 px-4 py-3">
              <span className="font-mono text-sm font-bold tracking-wide text-ink-800">{p.code}</span>
              <button
                type="button"
                onClick={() => copy(p.code, i)}
                className="rounded-md bg-son-50 px-3.5 py-1.5 text-sm font-bold text-son-600 hover:bg-son-100"
              >
                {copied === i ? "Đã chép" : "Chép mã"}
              </button>
            </div>
          </div>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── BANNER ƯU ĐÃI ─────────────────────────── */

const BANNERS = [
  {
    title: "Về quê ăn Tết",
    sub: "Săn vé sớm, giữ giá tốt",
    badge: "Giảm đến 30%",
    scene: "linear-gradient(135deg,#ffd76e 0%,#ffb020 55%,#f08a00 100%)",
  },
  {
    title: "Tổng hợp ưu đãi tàu",
    sub: "Ghé xem deal hời, chốt ngay giá tốt",
    badge: "Mỗi thứ Ba",
    scene: "linear-gradient(135deg,#38a9f5 0%,#0194f3 50%,#0f5ea3 100%)",
  },
  {
    title: "Du lịch miền Trung",
    sub: "Huế · Đà Nẵng · Nha Trang",
    badge: "Giảm đến 50%",
    scene: "linear-gradient(135deg,#7ddfb0 0%,#22b573 55%,#0f8a55 100%)",
  },
];

function DealBanners() {
  return (
    <Section title="Vô vàn ưu đãi">
      <div className="grid gap-4 sm:grid-cols-3">
        {BANNERS.map((b) => (
          <a
            key={b.title}
            href="#"
            className="card-scene relative block overflow-hidden rounded-lg p-5 text-white"
            style={{ ["--scene" as string]: b.scene }}
          >
            <Mountains />
            <span className="relative inline-block rounded bg-black/25 px-2 py-0.5 text-[11px] font-bold">
              {b.badge}
            </span>
            <h3 className="relative mt-16 text-xl font-bold drop-shadow">{b.title}</h3>
            <p className="relative text-sm text-white/90">{b.sub}</p>
          </a>
        ))}
      </div>
    </Section>
  );
}

/** Lớp trang trí núi/đường ray, thay cho ảnh chụp — luôn render được kể cả offline */
function Mountains() {
  return (
    <svg
      aria-hidden
      viewBox="0 0 400 120"
      preserveAspectRatio="none"
      className="pointer-events-none absolute inset-x-0 bottom-0 h-24 w-full"
    >
      <path d="M0 120 L90 46 L150 92 L232 28 L300 88 L400 40 L400 120Z" fill="rgb(255 255 255 / 0.18)" />
      <path d="M0 120 L70 78 L140 110 L226 62 L312 108 L400 74 L400 120Z" fill="rgb(255 255 255 / 0.22)" />
    </svg>
  );
}

/* ─────────────────────────── VÉ TÀU GIÁ TỐT ─────────────────────────── */

const ROUTE_CITIES = ["Hà Nội", "Sài Gòn", "Đà Nẵng", "Huế", "Nha Trang", "Khác"];

const ROUTES = [
  { from: "Hà Nội", to: "Huế", date: "27 thg 1 2027", price: 842_945, scene: "linear-gradient(140deg,#f6b26b,#c9611e)" },
  { from: "Hà Nội", to: "Sài Gòn", date: "24 thg 1 2027", price: 1_311_600, scene: "linear-gradient(140deg,#5ac8fa,#0f5ea3)" },
  { from: "Hà Nội", to: "Nha Trang", date: "27 thg 1 2027", price: 1_057_865, scene: "linear-gradient(140deg,#5be0c0,#0f8a55)" },
  { from: "Hà Nội", to: "Đà Nẵng", date: "3 thg 2 2027", price: 957_200, scene: "linear-gradient(140deg,#b39ddb,#5e35b1)" },
];

function PriceDeals() {
  const [city, setCity] = useState(ROUTE_CITIES[0]);
  return (
    <Section title="Vé tàu giá tốt" more>
      <Chips items={ROUTE_CITIES} value={city} onChange={setCity} />
      <div className="no-scrollbar mt-4 grid grid-flow-col auto-cols-[78%] gap-4 overflow-x-auto pb-1 sm:grid-flow-row sm:auto-cols-auto sm:grid-cols-4 sm:overflow-visible">
        {ROUTES.map((r) => (
          <a
            key={`${r.from}-${r.to}`}
            href="#"
            className="overflow-hidden rounded-lg border border-ink-200 bg-white transition-shadow hover:shadow-[var(--shadow-lift)]"
          >
            <div className="card-scene relative h-36" style={{ ["--scene" as string]: r.scene }}>
              <span className="absolute left-3 top-3 rounded bg-ink-900/80 px-2 py-1 text-[11px] font-bold text-white">
                MỘT CHIỀU
              </span>
              <Mountains />
              <span className="absolute bottom-0 right-0 bg-mai-500 px-2.5 py-1 text-[11px] font-bold text-white">
                Giá tốt chỉ từ
              </span>
            </div>
            <div className="p-3">
              <h3 className="font-bold text-ink-900">{r.from} — {r.to}</h3>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-500">
                <span aria-hidden>🌸</span> {r.date}
              </p>
              <p className="tnum mt-1 text-[17px] font-bold text-mai-600">{formatVnd(r.price)}</p>
            </div>
          </a>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── CHUYẾN PHỔ BIẾN ─────────────────────────── */

const TRAIN_REGIONS = ["Bắc — Nam", "Miền Bắc", "Miền Trung", "Miền Nam", "Tàu đêm", "Tàu nhanh"];

const TRAINS = [
  { code: "SE1", route: "Hà Nội → Sài Gòn", station: "Ga Hà Nội", rating: 8.5, reviews: "141", was: 1_787_879, now: 1_319_771, save: 26, scene: "linear-gradient(140deg,#ffd08a,#e07b1f)" },
  { code: "SE7", route: "Hà Nội → Đà Nẵng", station: "Ga Hà Nội", rating: 8.0, reviews: "117", was: 919_927, now: 736_698, save: 20, scene: "linear-gradient(140deg,#8fd3f4,#1976d2)" },
  { code: "SE21", route: "Sài Gòn → Huế", station: "Ga Sài Gòn", rating: 8.8, reviews: "1,4k", was: 958_961, now: 768_914, save: 19, scene: "linear-gradient(140deg,#a5e8c6,#14875b)" },
  { code: "TN3", route: "Sài Gòn → Nha Trang", station: "Ga Sài Gòn", rating: 8.4, reviews: "748", was: 561_472, now: 437_057, save: 22, scene: "linear-gradient(140deg,#f7a8c0,#c2185b)" },
];

function PopularTrains() {
  const [region, setRegion] = useState(TRAIN_REGIONS[0]);
  return (
    <Section title="Chuyến tàu được đặt nhiều" icon="🚆" more>
      <Chips items={TRAIN_REGIONS} value={region} onChange={setRegion} />
      <div className="no-scrollbar mt-4 grid grid-flow-col auto-cols-[78%] gap-4 overflow-x-auto pb-1 sm:grid-flow-row sm:auto-cols-auto sm:grid-cols-4 sm:overflow-visible">
        {TRAINS.map((t) => (
          <a
            key={t.code}
            href="#"
            className="overflow-hidden rounded-lg border border-ink-200 bg-white transition-shadow hover:shadow-[var(--shadow-lift)]"
          >
            <div className="card-scene relative h-36" style={{ ["--scene" as string]: t.scene }}>
              <span className="absolute left-3 top-3 flex items-center gap-1 rounded bg-son-500 px-2 py-1 text-[11px] font-bold text-white">
                <span aria-hidden>📍</span> {t.station}
              </span>
              <Mountains />
              <span className="absolute bottom-0 right-0 bg-mai-500 px-2.5 py-1 text-[11px] font-bold text-white">
                Tiết kiệm {t.save}%
              </span>
            </div>
            <div className="p-3">
              <h3 className="font-bold text-ink-900">Tàu {t.code}</h3>
              <p className="text-sm text-ink-600">{t.route}</p>
              <p className="mt-1.5 flex items-center gap-1.5 text-sm">
                <span className="font-bold text-son-600">{t.rating}/10</span>
                <span className="text-ink-400">·</span>
                <span className="text-ink-500">{t.reviews} đánh giá</span>
              </p>
              <p className="tnum mt-1.5 text-sm text-ink-400 line-through">{formatVnd(t.was)}</p>
              <p className="tnum text-[17px] font-bold text-mai-600">{formatVnd(t.now)}</p>
            </div>
          </a>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── KHÁM PHÁ ─────────────────────────── */

const EXPLORE: Record<string, string[]> = {
  "Tuyến phổ biến": [
    "Hà Nội — Sài Gòn", "Hà Nội — Huế", "Hà Nội — Đà Nẵng", "Hà Nội — Vinh",
    "Sài Gòn — Nha Trang", "Sài Gòn — Phan Thiết", "Sài Gòn — Quy Nhơn", "Sài Gòn — Đà Nẵng",
    "Huế — Đà Nẵng", "Vinh — Đồng Hới", "Nha Trang — Tuy Hòa", "Hải Phòng — Hà Nội",
  ],
  "Ga tàu lớn": [
    "Ga Hà Nội", "Ga Sài Gòn", "Ga Đà Nẵng", "Ga Huế", "Ga Vinh", "Ga Nha Trang",
    "Ga Hải Phòng", "Ga Đồng Hới", "Ga Quy Nhơn", "Ga Phan Thiết", "Ga Tuy Hòa", "Ga Thanh Hóa",
  ],
  "Loại chỗ": [
    "Ngồi mềm điều hoà", "Nằm khoang 4 tầng 1", "Nằm khoang 4 tầng 2", "Nằm khoang 6 tầng 1",
    "Nằm khoang 6 tầng 2", "Nằm khoang 6 tầng 3", "Ghế phụ", "Toa cộng đồng",
  ],
  "Mẹo đi tàu Tết": [
    "Nên đặt trước bao lâu", "Mang theo giấy tờ gì", "Đổi trả vé thế nào", "Hành lý được mang bao nhiêu",
    "Đi tàu với trẻ nhỏ", "Ăn uống trên tàu", "Giữ chỗ 10 phút là gì", "Thanh toán an toàn",
  ],
};

function ExploreLinks() {
  const tabs = Object.keys(EXPLORE);
  const [tab, setTab] = useState(tabs[0]);

  return (
    <Section title="Bạn muốn khám phá điều gì?">
      <div className="no-scrollbar -mt-2 flex gap-6 overflow-x-auto border-b border-ink-200">
        {tabs.map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            aria-current={tab === t ? "true" : undefined}
            className={
              "whitespace-nowrap border-b-2 px-1 pb-3 text-[15px] font-bold transition-colors " +
              (tab === t ? "border-son-500 text-son-600" : "border-transparent text-ink-500 hover:text-ink-800")
            }
          >
            {t}
          </button>
        ))}
      </div>
      <ul className="mt-6 grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
        {EXPLORE[tab].map((item) => (
          <li key={item}>
            <a href="#" className="rounded text-[15px] text-ink-700 hover:text-son-600 hover:underline">
              {item}
            </a>
          </li>
        ))}
      </ul>
    </Section>
  );
}

/* ─────────────────────────── ĐĂNG KÝ NHẬN TIN ─────────────────────────── */

function Newsletter() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  return (
    <section className="mt-14 bg-son-700">
      <div className="mx-auto grid max-w-[1200px] items-center gap-8 px-4 py-12 sm:px-6 lg:grid-cols-2">
        <div>
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Luôn được cập nhật lịch mở bán và khuyến mãi mới nhất
          </h2>
          <form
            className="mt-6 flex max-w-xl flex-col gap-2 sm:flex-row"
            onSubmit={(e) => {
              e.preventDefault();
              setSent(true);
            }}
          >
            <label htmlFor="email-tin" className="sr-only">Địa chỉ email của bạn</label>
            <input
              id="email-tin"
              type="email"
              required
              value={email}
              onChange={(e) => { setEmail(e.target.value); setSent(false); }}
              placeholder="địa chỉ email của bạn"
              className="min-h-12 flex-1 rounded-md border border-white/30 bg-white px-4 text-base text-ink-900 placeholder:text-ink-400"
            />
            <button
              type="submit"
              className="min-h-12 rounded-md bg-mai-500 px-6 font-bold text-white hover:bg-mai-600"
            >
              Đăng ký tin
            </button>
          </form>
          <p aria-live="polite" className="mt-2 min-h-5 text-sm text-son-100">
            {sent ? `Đã đăng ký ${email}. Lịch mở bán sẽ được gửi tới hộp thư này.` : ""}
          </p>
        </div>

        <div className="lg:justify-self-end">
          <h3 className="text-lg font-bold text-white">Đặt vé nhanh hơn trên ứng dụng</h3>
          <div className="mt-4 flex gap-3">
            {[
              { top: "GET IT ON", bottom: "Google Play" },
              { top: "Download on the", bottom: "App Store" },
            ].map((s) => (
              <a
                key={s.bottom}
                href="#"
                className="flex items-center gap-2 rounded-md border border-white/30 bg-black px-4 py-2"
              >
                <span aria-hidden className="text-xl">▷</span>
                <span className="leading-tight">
                  <span className="block text-[10px] text-white/70">{s.top}</span>
                  <span className="block font-semibold text-white">{s.bottom}</span>
                </span>
              </a>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}

/* ─────────────────────────── KHỐI DÙNG CHUNG ─────────────────────────── */

function Section({
  title,
  subtitle,
  icon,
  more,
  pad = "pt-14",
  children,
}: {
  title: string;
  subtitle?: string;
  icon?: string;
  more?: boolean;
  pad?: string;
  children: ReactNode;
}) {
  return (
    <section className={`mx-auto max-w-[1200px] px-4 sm:px-6 ${pad}`}>
      <div className="mb-4 flex items-center gap-3">
        <h2 className="flex items-center gap-2 text-xl font-bold text-ink-900 sm:text-2xl">
          {icon && <span aria-hidden>{icon}</span>}
          {title}
        </h2>
        {more && (
          <a href="#" className="ml-auto shrink-0 rounded text-sm font-bold text-son-600 hover:underline">
            Xem tất cả
          </a>
        )}
      </div>
      {subtitle && <p className="-mt-3 mb-5 text-ink-500">{subtitle}</p>}
      {children}
    </section>
  );
}

function Chips({
  items,
  value,
  onChange,
}: {
  items: string[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="no-scrollbar flex gap-2 overflow-x-auto pb-1">
      {items.map((it) => (
        <button
          key={it}
          type="button"
          onClick={() => onChange(it)}
          aria-pressed={value === it}
          className={
            "shrink-0 rounded-full px-4 py-1.5 text-sm font-semibold transition-colors " +
            (value === it
              ? "bg-son-500 text-white"
              : "bg-white text-son-600 ring-1 ring-ink-200 hover:bg-son-50")
          }
        >
          {it}
        </button>
      ))}
    </div>
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
