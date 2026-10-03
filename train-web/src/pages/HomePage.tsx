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
import heroImage from "@/assets/hero-bromo.jpg";
import {
  IconBus, IconCalendar, IconCar, IconGrid, IconHotel, IconPin, IconSearch,
  IconSwap, IconTicket, IconTrain, IconTransfer, IconUser, IconFlag,
} from "@/components/ui/Icon";

/**
 * Home page following Traveloka's layout:
 *   dark hero image + product tabs + search box floating over the image edge
 *   → promo codes → deals → great-value train tickets → popular trains
 *   → explore → newsletter sign-up
 *
 * The real business part (departure, arrival, date, passengers → waiting room)
 * lives entirely in the search box; the strips below are introductory content.
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

/* ─────────────────────────── HERO + SEARCH ─────────────────────────── */

const PRODUCT_TABS = [
  { Icon: IconTrain,    label: "Train tickets" },
  { Icon: IconBus,      label: "Bus tickets" },
  { Icon: IconHotel,    label: "Hotels" },
  { Icon: IconTransfer, label: "Station transfers" },
  { Icon: IconCar,      label: "Car rental" },
  { Icon: IconTicket,   label: "Things to do" },
];

const SEAT_FILTERS = ["All", "Soft seat", "4-berth", "6-berth"];

function Hero() {
  const navigate = useNavigate();
  const { query, setQuery } = useBooking();

  const stations = useAsync((o) => api.getStations(o), []);
  const sale = useAsync((o) => api.getSaleWindow(o), []);

  const [from, setFrom] = useState(query?.from ?? "HNO");
  const [to, setTo] = useState(query?.to ?? "SGO");
  const [date, setDate] = useState(query?.date ?? defaultNewYearDate());
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
    // Every purchase goes through the waiting room; it lets people straight in when the system is quiet
    navigate("/waiting-room");
  };

  return (
    <section className="relative">
      <HeroBackdrop />

      {/* pt-[124px]: leaves room for the transparent header sitting on top */}
      <div className="relative mx-auto max-w-[1200px] px-4 pb-[72px] pt-[124px] sm:px-6 sm:pb-20 sm:pt-[140px]">
        <h1 className="text-center text-[30px] font-extrabold leading-tight text-white sm:text-[44px]">
          Home for the New Year, booked in one tap
        </h1>

        {/* Product tabs */}
        <div className="no-scrollbar mt-7 flex justify-start gap-1 overflow-x-auto sm:justify-center">
          {PRODUCT_TABS.map((t, i) => (
            <button
              key={t.label}
              type="button"
              aria-current={i === 0 ? "true" : undefined}
              className={
                i === 0
                  ? "flex shrink-0 items-center gap-2 rounded-full bg-white px-5 py-2.5 text-[15px] font-bold text-ink-900"
                  : "flex shrink-0 items-center gap-2 rounded-full px-4 py-2.5 text-[15px] font-semibold text-white hover:bg-white/10"
              }
            >
              <t.Icon size={20} className={i === 0 ? "text-brand-500" : undefined} />
              {t.label}
            </button>
          ))}
          <button
            type="button"
            className="flex shrink-0 items-center gap-2 rounded-full px-4 py-2.5 text-[15px] font-semibold text-white hover:bg-white/10"
          >
            <IconGrid size={20} />
            More
          </button>
        </div>
        <div className="mx-auto mt-5 max-w-[1140px] border-t border-white/35" />

        {/* Seat class filter */}
        <div className="no-scrollbar mt-4 flex gap-2 overflow-x-auto">
          {SEAT_FILTERS.map((f) => (
            <button
              key={f}
              type="button"
              onClick={() => setSeatFilter(f)}
              aria-pressed={seatFilter === f}
              className={
                seatFilter === f
                  ? "shrink-0 rounded-full bg-brand-500 px-4 py-1.5 text-sm font-bold text-white"
                  : "shrink-0 rounded-full bg-white px-4 py-1.5 text-sm font-semibold text-ink-800 hover:bg-ink-100"
              }
            >
              {f}
            </button>
          ))}
        </div>

        {/* Labels sit OUTSIDE the white box, white text over the image — exactly
            how Traveloka does it. The white box only holds values, so it stays
            slim, and the fields line up instead of each having its own frame. */}
        {stations.loading ? (
          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <Skeleton className="h-16 flex-1" />
            <Skeleton className="h-16 flex-1" />
            <Skeleton className="h-16 flex-1" />
          </div>
        ) : stations.error ? (
          <div className="mt-5 rounded-lg bg-white p-4">
            <ErrorState error={stations.error} onRetry={stations.reload} compact />
          </div>
        ) : (
          <>
            <div className="mt-4 hidden gap-2 text-sm font-semibold text-white sm:grid sm:grid-cols-[1fr_1fr_1.1fr_0.9fr_56px]">
              <span>From</span>
              <span>To</span>
              <span>Departure date</span>
              <span>Passengers</span>
              <span />
            </div>

            {/* Fields are SEPARATED by a 2px gap — Traveloka uses no solid dividers,
                the box background shows through as a gap. The search button
                hugs the right edge and only rounds its right corners. */}
            <div className="mt-1.5 overflow-hidden rounded-lg bg-white p-0 shadow-[var(--shadow-float)]">
              <div className="grid gap-px bg-ink-200 sm:grid-cols-[1fr_1fr_1.1fr_0.9fr_60px]">
                <SearchCell label="From" Icon={IconPin}>
                  <select
                    value={from}
                    onChange={(e) => setFrom(e.target.value)}
                    aria-label="Departure station"
                    className="w-full cursor-pointer truncate bg-transparent text-[15px] font-bold text-ink-900 outline-none"
                  >
                    {stationList.map((s) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                </SearchCell>

                <SearchCell label="To" Icon={IconFlag}>
                  <select
                    value={to}
                    onChange={(e) => setTo(e.target.value)}
                    aria-label="Arrival station"
                    className="w-full cursor-pointer truncate bg-transparent text-[15px] font-bold text-ink-900 outline-none"
                  >
                    {stationList.map((s) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    onClick={swap}
                    aria-label="Swap departure and arrival stations"
                    className="absolute -left-4 top-1/2 z-10 hidden size-8 -translate-y-1/2 place-items-center rounded-full border border-ink-200 bg-white text-brand-600 shadow-[var(--shadow-soft)] hover:border-brand-400 sm:grid"
                  >
                    <IconSwap size={15} />
                  </button>
                </SearchCell>

                <SearchCell label="Departure date" Icon={IconCalendar}>
                  <button
                    type="button"
                    onClick={() => setPickerOpen((v) => !v)}
                    aria-expanded={pickerOpen}
                    className="w-full truncate text-left text-[15px] font-bold text-ink-900"
                  >
                    {weekdayLabel(fromDateKey(date))}, {formatDate(fromDateKey(date))}
                  </button>
                </SearchCell>

                <SearchCell label="Passengers" Icon={IconUser}>
                  <select
                    value={String(passengers)}
                    onChange={(e) => setPassengers(Number(e.target.value))}
                    aria-label="Number of passengers"
                    className="w-full cursor-pointer bg-transparent text-[15px] font-bold text-ink-900 outline-none"
                  >
                    {[1, 2, 3, 4].map((n) => (
                      <option key={n} value={n}>{n} {n === 1 ? "person" : "people"}</option>
                    ))}
                  </select>
                </SearchCell>

                <button
                  type="button"
                  onClick={submit}
                  disabled={sameStation}
                  aria-label="Search trains"
                  className="flex min-h-[62px] items-center justify-center gap-2 bg-accent-500 text-base font-bold text-white hover:bg-accent-600 disabled:cursor-not-allowed disabled:bg-ink-300"
                >
                  <IconSearch size={22} />
                  <span className="sm:hidden">Search trains</span>
                </button>
              </div>
            </div>

            {sameStation && (
              <p className="mt-2 inline-block rounded bg-accent-600 px-2.5 py-1 text-sm font-semibold text-white">
                The arrival station must differ from the departure station
              </p>
            )}

            {pickerOpen && (
              <div className="mt-2 rounded-lg bg-white p-4 shadow-[var(--shadow-float)]">
                <div className="mb-2 flex flex-wrap items-center gap-2">
                  <span className="text-sm text-ink-600">Lunar date {dayInfo.lunarText}</span>
                  {dayInfo.peak !== "none" && (
                    <span className="rounded-full bg-accent-50 px-2.5 py-0.5 text-xs font-bold text-accent-700">
                      {peakText[dayInfo.peak]}
                    </span>
                  )}
                </div>
                <LunarDatePicker value={date} onChange={(d) => { setDate(d); setPickerOpen(false); }} />
              </div>
            )}

            {dayInfo.peak === "peak" && !pickerOpen && (
              <p className="mt-2.5 inline-flex items-center gap-1.5 rounded-full bg-accent-500/90 px-3 py-1 text-[13px] font-semibold text-white">
                New Year peak · tickets sell fast, have ID numbers ready
              </p>
            )}
          </>
        )}

        {/* Sale opening countdown */}
        <div className="mt-3.5">
          {sale.loading ? (
            <Skeleton className="h-16 w-full bg-white/20" />
          ) : sale.data ? (
            <SaleCountdown opensAt={sale.data.opensAt} label={sale.data.label} />
          ) : null}
        </div>
      </div>

      {/* Partner strip overlapping the bottom edge of the hero image */}
      <div className="relative mx-auto -mb-8 max-w-[1000px] px-4 sm:px-6">
        <div className="flex flex-wrap items-center justify-center gap-x-8 gap-y-3 rounded-lg bg-white px-6 py-4 shadow-[var(--shadow-lift)]">
          <span className="text-sm text-ink-500">Rail partners</span>
          {["Vietnam Railways", "Saigon Railways", "Hanoi Railways", "Ratraco"].map((p) => (
            <span key={p} className="text-sm font-bold text-ink-700">{p}</span>
          ))}
        </div>
      </div>
    </section>
  );
}

/**
 * Hero background: Mount Bromo at sunset.
 *
 * The original was a 1.77 MB PNG — 13 times heavier than a background needs.
 * Compressed to JPEG quality 82 it is 139 KB; the eye cannot tell the
 * difference because a dark overlay always sits on top and the image is never
 * shown as is.
 *
 * object-cover keeps the image covering the frame on wide and narrow screens
 * without distortion. object-center keeps the mountain centred when the sides are cropped.
 */
function HeroBackdrop() {
  return (
    <div aria-hidden className="absolute inset-0 overflow-hidden bg-brand-950">
      <img
        src={heroImage}
        alt=""
        className="size-full object-cover object-center"
        // This image is at the very top of the page, load it as early as possible — no lazy loading
        loading="eager"
        fetchPriority="high"
      />
      {/* Dark overlay so white text and the search box are always readable */}
      <div className="hero-scrim absolute inset-0" />
    </div>
  );
}

/**
 * One cell of the search box.
 *
 * NO border of its own — the dividers between cells come from the parent box.
 * The label here only shows on narrow screens; on wide screens the label sits
 * outside the box, white text over the hero image.
 */
function SearchCell({
  label,
  Icon,
  children,
}: {
  label: string;
  Icon: (p: { size?: number; className?: string }) => ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="relative flex min-h-[62px] items-center gap-2.5 bg-white px-4">
      <Icon size={20} className="shrink-0 text-brand-500" />
      <div className="min-w-0 flex-1">
        <span className="block text-[11px] font-semibold text-ink-500 sm:hidden">{label}</span>
        {children}
      </div>
    </div>
  );
}

/* ─────────────────────────── PROMO CODES ─────────────────────────── */

const PROMOS = [
  { Icon: IconTrain,  tint: "bg-brand-50 text-brand-600", title: "Up to ₫75,000 off your first train booking", code: "TRAINNEW" },
  { Icon: IconHotel,  tint: "bg-ok-50 text-ok-600",   title: "Up to ₫250,000 off your first hotel booking", code: "TRAINNEW" },
  { Icon: IconTicket, tint: "bg-accent-50 text-accent-600", title: "Up to 10% off your first attraction booking", code: "TRAINNEW" },
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
    <Section title="Promo codes for new users" subtitle="Valid on your first booking in the app" pad="pt-16">
      <div className="grid gap-4 sm:grid-cols-3">
        {PROMOS.map((p, i) => (
          <div key={p.title} className="overflow-hidden rounded-lg border border-ink-200 bg-white">
            <div className="flex gap-3 p-4">
              <span className={`grid size-9 shrink-0 place-items-center rounded-full ${p.tint}`}>
                <p.Icon size={18} />
              </span>
              <div className="min-w-0">
                <h3 className="text-[15px] font-bold leading-snug text-ink-900">{p.title}</h3>
                <p className="mt-1 truncate text-sm text-ink-500">Valid on your first booking in the app</p>
              </div>
            </div>
            <div className="flex items-center justify-between gap-3 border-t border-dashed border-ink-200 bg-ink-50 px-4 py-3">
              <span className="font-mono text-sm font-bold tracking-wide text-ink-800">{p.code}</span>
              <button
                type="button"
                onClick={() => copy(p.code, i)}
                className="rounded-md bg-brand-50 px-3.5 py-1.5 text-sm font-bold text-brand-600 hover:bg-brand-100"
              >
                {copied === i ? "Copied" : "Copy code"}
              </button>
            </div>
          </div>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── DEAL BANNERS ─────────────────────────── */

const BANNERS = [
  {
    title: "Home for the New Year",
    sub: "Book early, lock in a good price",
    badge: "Up to 30% off",
    scene: "linear-gradient(135deg,#ffd76e 0%,#ffb020 55%,#f08a00 100%)",
  },
  {
    title: "All train deals",
    sub: "Browse the bargains, grab the best price",
    badge: "Every Tuesday",
    scene: "linear-gradient(135deg,#38a9f5 0%,#0194f3 50%,#0f5ea3 100%)",
  },
  {
    title: "Explore Central Vietnam",
    sub: "Hue · Da Nang · Nha Trang",
    badge: "Up to 50% off",
    scene: "linear-gradient(135deg,#7ddfb0 0%,#22b573 55%,#0f8a55 100%)",
  },
];

function DealBanners() {
  return (
    <Section title="Endless deals">
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

/** Decorative mountains/rails layer instead of a photo — always renders, even offline */
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

/* ─────────────────────────── GREAT-VALUE TRAIN TICKETS ─────────────────────────── */

const ROUTE_CITIES = ["Hanoi", "Saigon", "Da Nang", "Hue", "Nha Trang", "Other"];

const ROUTES = [
  { from: "Hanoi", to: "Hue", date: "27 Jan 2027", price: 842_945, scene: "linear-gradient(140deg,#f6b26b,#c9611e)" },
  { from: "Hanoi", to: "Saigon", date: "24 Jan 2027", price: 1_311_600, scene: "linear-gradient(140deg,#5ac8fa,#0f5ea3)" },
  { from: "Hanoi", to: "Nha Trang", date: "27 Jan 2027", price: 1_057_865, scene: "linear-gradient(140deg,#5be0c0,#0f8a55)" },
  { from: "Hanoi", to: "Da Nang", date: "3 Feb 2027", price: 957_200, scene: "linear-gradient(140deg,#b39ddb,#5e35b1)" },
];

function PriceDeals() {
  const [city, setCity] = useState(ROUTE_CITIES[0]);
  return (
    <Section title="Great-value train tickets" more>
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
                ONE WAY
              </span>
              <Mountains />
              <span className="absolute bottom-0 right-0 bg-accent-500 px-2.5 py-1 text-[11px] font-bold text-white">
                Great price from
              </span>
            </div>
            <div className="p-3">
              <h3 className="font-bold text-ink-900">{r.from} — {r.to}</h3>
              <p className="mt-1 flex items-center gap-1.5 text-sm text-ink-500">
                <IconCalendar size={15} /> {r.date}
              </p>
              <p className="tnum mt-1 text-[17px] font-bold text-accent-600">{formatVnd(r.price)}</p>
            </div>
          </a>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── POPULAR TRAINS ─────────────────────────── */

const TRAIN_REGIONS = ["North — South", "North", "Central", "South", "Night trains", "Express trains"];

const TRAINS = [
  { code: "SE1", route: "Hanoi → Saigon", station: "Hanoi Station", rating: 8.5, reviews: "141", was: 1_787_879, now: 1_319_771, save: 26, scene: "linear-gradient(140deg,#ffd08a,#e07b1f)" },
  { code: "SE7", route: "Hanoi → Da Nang", station: "Hanoi Station", rating: 8.0, reviews: "117", was: 919_927, now: 736_698, save: 20, scene: "linear-gradient(140deg,#8fd3f4,#1976d2)" },
  { code: "SE21", route: "Saigon → Hue", station: "Saigon Station", rating: 8.8, reviews: "1.4k", was: 958_961, now: 768_914, save: 19, scene: "linear-gradient(140deg,#a5e8c6,#14875b)" },
  { code: "TN3", route: "Saigon → Nha Trang", station: "Saigon Station", rating: 8.4, reviews: "748", was: 561_472, now: 437_057, save: 22, scene: "linear-gradient(140deg,#f7a8c0,#c2185b)" },
];

function PopularTrains() {
  const [region, setRegion] = useState(TRAIN_REGIONS[0]);
  return (
    <Section title="Most booked trains" Icon={IconTrain} more>
      <Chips items={TRAIN_REGIONS} value={region} onChange={setRegion} />
      <div className="no-scrollbar mt-4 grid grid-flow-col auto-cols-[78%] gap-4 overflow-x-auto pb-1 sm:grid-flow-row sm:auto-cols-auto sm:grid-cols-4 sm:overflow-visible">
        {TRAINS.map((t) => (
          <a
            key={t.code}
            href="#"
            className="overflow-hidden rounded-lg border border-ink-200 bg-white transition-shadow hover:shadow-[var(--shadow-lift)]"
          >
            <div className="card-scene relative h-36" style={{ ["--scene" as string]: t.scene }}>
              <span className="absolute left-3 top-3 flex items-center gap-1 rounded bg-brand-500 px-2 py-1 text-[11px] font-bold text-white">
                <IconPin size={13} /> {t.station}
              </span>
              <Mountains />
              <span className="absolute bottom-0 right-0 bg-accent-500 px-2.5 py-1 text-[11px] font-bold text-white">
                Save {t.save}%
              </span>
            </div>
            <div className="p-3">
              <h3 className="font-bold text-ink-900">Train {t.code}</h3>
              <p className="text-sm text-ink-600">{t.route}</p>
              <p className="mt-1.5 flex items-center gap-1.5 text-sm">
                <span className="font-bold text-brand-600">{t.rating}/10</span>
                <span className="text-ink-400">·</span>
                <span className="text-ink-500">{t.reviews} reviews</span>
              </p>
              <p className="tnum mt-1.5 text-sm text-ink-400 line-through">{formatVnd(t.was)}</p>
              <p className="tnum text-[17px] font-bold text-accent-600">{formatVnd(t.now)}</p>
            </div>
          </a>
        ))}
      </div>
    </Section>
  );
}

/* ─────────────────────────── EXPLORE ─────────────────────────── */

const EXPLORE: Record<string, string[]> = {
  "Popular routes": [
    "Hanoi — Saigon", "Hanoi — Hue", "Hanoi — Da Nang", "Hanoi — Vinh",
    "Saigon — Nha Trang", "Saigon — Phan Thiet", "Saigon — Quy Nhon", "Saigon — Da Nang",
    "Hue — Da Nang", "Vinh — Dong Hoi", "Nha Trang — Tuy Hoa", "Hai Phong — Hanoi",
  ],
  "Major stations": [
    "Hanoi Station", "Saigon Station", "Da Nang Station", "Hue Station", "Vinh Station", "Nha Trang Station",
    "Hai Phong Station", "Dong Hoi Station", "Quy Nhon Station", "Phan Thiet Station", "Tuy Hoa Station", "Thanh Hoa Station",
  ],
  "Seat classes": [
    "Air-conditioned soft seat", "4-berth lower", "4-berth upper", "6-berth lower",
    "6-berth middle", "6-berth upper", "Extra seat", "Shared carriage",
  ],
  "New Year travel tips": [
    "How early to book", "Which documents to bring", "How to exchange or refund", "Luggage allowance",
    "Travelling with young children", "Food on board", "What a 10-minute hold means", "Paying safely",
  ],
};

function ExploreLinks() {
  const tabs = Object.keys(EXPLORE);
  const [tab, setTab] = useState(tabs[0]);

  return (
    <Section title="What would you like to explore?">
      <div className="no-scrollbar -mt-2 flex gap-6 overflow-x-auto border-b border-ink-200">
        {tabs.map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            aria-current={tab === t ? "true" : undefined}
            className={
              "whitespace-nowrap border-b-2 px-1 pb-3 text-[15px] font-bold transition-colors " +
              (tab === t ? "border-brand-500 text-brand-600" : "border-transparent text-ink-500 hover:text-ink-800")
            }
          >
            {t}
          </button>
        ))}
      </div>
      <ul className="mt-6 grid gap-x-8 gap-y-4 sm:grid-cols-2 lg:grid-cols-4">
        {EXPLORE[tab].map((item) => (
          <li key={item}>
            <a href="#" className="rounded text-[15px] text-ink-700 hover:text-brand-600 hover:underline">
              {item}
            </a>
          </li>
        ))}
      </ul>
    </Section>
  );
}

/* ─────────────────────────── NEWSLETTER ─────────────────────────── */

function Newsletter() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  return (
    <section className="mt-14 bg-brand-700">
      <div className="mx-auto grid max-w-[1200px] items-center gap-8 px-4 py-12 sm:px-6 lg:grid-cols-2">
        <div>
          <h2 className="text-2xl font-bold text-white sm:text-3xl">
            Get the latest sale dates and deals first
          </h2>
          <form
            className="mt-6 flex max-w-xl flex-col gap-2 sm:flex-row"
            onSubmit={(e) => {
              e.preventDefault();
              setSent(true);
            }}
          >
            <label htmlFor="newsletter-email" className="sr-only">Your email address</label>
            <input
              id="newsletter-email"
              type="email"
              required
              value={email}
              onChange={(e) => { setEmail(e.target.value); setSent(false); }}
              placeholder="your email address"
              className="min-h-12 flex-1 rounded-md border border-white/30 bg-white px-4 text-base text-ink-900 placeholder:text-ink-400"
            />
            <button
              type="submit"
              className="min-h-12 rounded-md bg-accent-500 px-6 font-bold text-white hover:bg-accent-600"
            >
              Subscribe
            </button>
          </form>
          <p aria-live="polite" className="mt-2 min-h-5 text-sm text-brand-100">
            {sent ? `${email} is subscribed. Sale dates will be sent to this inbox.` : ""}
          </p>
        </div>

        <div className="lg:justify-self-end">
          <h3 className="text-lg font-bold text-white">Book faster in the app</h3>
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

/* ─────────────────────────── SHARED BLOCKS ─────────────────────────── */

function Section({
  title,
  subtitle,
  Icon,
  more,
  pad = "pt-14",
  children,
}: {
  title: string;
  subtitle?: string;
  Icon?: (p: { size?: number; className?: string }) => ReactNode;
  more?: boolean;
  pad?: string;
  children: ReactNode;
}) {
  return (
    <section className={`mx-auto max-w-[1200px] px-4 sm:px-6 ${pad}`}>
      <div className="mb-4 flex items-center gap-3">
        <h2 className="flex items-center gap-2 text-xl font-bold text-ink-900 sm:text-2xl">
          {Icon && <Icon size={26} className="text-brand-500" />}
          {title}
        </h2>
        {more && (
          <a href="#" className="ml-auto shrink-0 rounded text-sm font-bold text-brand-600 hover:underline">
            See all
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
              ? "bg-brand-500 text-white"
              : "bg-white text-brand-600 ring-1 ring-ink-200 hover:bg-brand-50")
          }
        >
          {it}
        </button>
      ))}
    </div>
  );
}

/** Defaults to the nearest peak: the 27th of the 12th lunar month before the coming New Year */
function defaultNewYearDate(): string {
  const today = new Date();
  for (let i = 1; i <= 400; i++) {
    const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
    const info = getDayInfo(d);
    if (info.lunar.month === 12 && info.lunar.day === 27) return toDateKey(d);
  }
  return toDateKey(new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7));
}
