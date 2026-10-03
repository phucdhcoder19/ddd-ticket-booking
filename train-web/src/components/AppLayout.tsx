import { useEffect, useState } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { cn } from "@/lib/cn";
import { IconChevronDown, IconLogin, IconPercent } from "@/components/ui/Icon";

/**
 * App shell following Traveloka's layout:
 *   Row 1 — logo, currency/language, secondary links, Sign in/Register buttons
 *   Row 2 — product bar (Train tickets, Bus tickets, Hotels...)
 *
 * The home page runs edge to edge (full-bleed hero); every other page keeps
 * the narrow 640px reading column.
 */

const PRODUCTS = [
  { to: "/", label: "Train tickets", end: true },
  { to: "/trips", label: "Bus tickets", end: false },
  { to: "/trips", label: "Hotels", end: false },
  { to: "/trips", label: "Station transfers", end: false },
  { to: "/trips", label: "Car rental", end: false },
  { to: "/trips", label: "Things to do", end: false },
];

/** Exactly the height of the two header rows — used to pull the hero up underneath it. */
const HEADER_HEIGHT = 108;

export function AppLayout() {
  const { pathname } = useLocation();
  // The waiting room takes the whole screen with no navigation, so users do not lose their place
  const bare = pathname.startsWith("/waiting-room");
  const wide = pathname === "/";

  /**
   * Home page: the transparent header sits ON TOP of the hero image and only
   * turns white after scrolling. Other pages are always white, as there is no
   * image underneath.
   */
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    if (!wide) return;
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, [wide]);

  const onImage = wide && !scrolled;

  return (
    <div className="flex min-h-dvh flex-col bg-canvas">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:font-semibold focus:text-brand-700"
      >
        Skip to main content
      </a>

      <header
        className={cn(
          "sticky top-0 z-30 transition-colors duration-200",
          onImage ? "bg-transparent" : "bg-white shadow-[var(--shadow-soft)]",
        )}
      >
        {/* Row 1 */}
        <div className={onImage ? "bg-swoosh-dark" : "bg-brand-swoosh"}>
          <div className="mx-auto flex h-16 max-w-[1200px] items-center gap-6 px-4 sm:px-6">
            <NavLink to="/" className="shrink-0 rounded-lg" aria-label="traintix — back to the home page">
              <Wordmark light={onImage} />
            </NavLink>

            <nav
              aria-label="Secondary links"
              className={cn(
                "hidden flex-1 items-center gap-5 text-sm font-semibold lg:flex",
                onImage ? "text-white" : "text-ink-800",
              )}
            >
              <button type="button" className="flex items-center gap-1.5 rounded-lg hover:opacity-80">
                <span aria-hidden className="text-base leading-none">🌐</span>
                VND | EN
                <IconChevronDown size={14} />
              </button>
              <button type="button" className="flex items-center gap-1.5 rounded-lg hover:opacity-80">
                <IconPercent size={18} className={onImage ? "text-white" : "text-ok-600"} />
                Deals
              </button>
              <button type="button" className="rounded-lg hover:opacity-80">Partner with us</button>
              <button type="button" className="flex items-center gap-1 rounded-lg hover:opacity-80">
                Support <IconChevronDown size={14} />
              </button>
              <NavLink to="/my-tickets" className="rounded-lg hover:opacity-80">My bookings</NavLink>
            </nav>

            <div className="ml-auto flex items-center gap-2 lg:ml-0">
              <button
                type="button"
                className={cn(
                  "hidden rounded-full px-5 py-2 text-sm font-bold sm:block",
                  onImage
                    ? "bg-white/15 text-white ring-1 ring-white/50 hover:bg-white/25"
                    : "border border-brand-200 bg-brand-50 text-brand-700 hover:bg-brand-100",
                )}
              >
                <span className="flex items-center gap-1.5">
                  Sign in <IconLogin size={16} />
                </span>
              </button>
              <button
                type="button"
                className="rounded-full bg-brand-500 px-5 py-2 text-sm font-bold text-white hover:bg-brand-600"
              >
                Register
              </button>
            </div>
          </div>
        </div>

        {/* Row 2 — product bar */}
        <nav
          aria-label="Products"
          className={cn(onImage ? "" : "border-t border-ink-100")}
        >
          <ul className="no-scrollbar mx-auto flex max-w-[1200px] gap-1 overflow-x-auto px-4 sm:px-6">
            {PRODUCTS.map((p, i) => (
              <li key={`${p.label}-${i}`}>
                <NavLink
                  to={p.to}
                  end={p.end}
                  className={({ isActive }) =>
                    cn(
                      "block whitespace-nowrap rounded-lg px-3 py-3 text-sm font-semibold transition-colors",
                      onImage
                        ? "text-white hover:text-white/80"
                        : isActive && i === 0
                          ? "text-brand-600"
                          : "text-ink-700 hover:text-brand-600",
                    )
                  }
                >
                  {p.label}
                </NavLink>
              </li>
            ))}
            <li>
              <button
                type="button"
                className={cn(
                  "whitespace-nowrap rounded-lg px-3 py-3 text-sm font-semibold",
                  onImage ? "text-white hover:text-white/80" : "text-ink-700 hover:text-brand-600",
                )}
              >
                <span className="flex items-center gap-1">
                  More <IconChevronDown size={14} />
                </span>
              </button>
            </li>
          </ul>
        </nav>
      </header>

      <main
        id="main-content"
        className={cn("w-full flex-1", wide ? "" : "mx-auto max-w-2xl px-4 pb-6 pt-4 sm:px-6")}
        style={wide ? { marginTop: -HEADER_HEIGHT } : undefined}
      >
        <Outlet />
      </main>

      {!bare && <SiteFooter />}
    </div>
  );
}

/** Text logo + wing, drawn in SVG so it stays crisp at any resolution */
function Wordmark({ light = false }: { light?: boolean }) {
  return (
    <span className="flex items-baseline gap-1">
      <span className={cn("text-[26px] font-bold leading-none tracking-tight", light ? "text-white" : "text-brand-900")}>
        traintix
      </span>
      <svg width="22" height="16" viewBox="0 0 22 16" aria-hidden className="translate-y-[-3px]">
        <path d="M1 11c6-1 9-4 12-8 1 3 0 5-2 7 3-1 6-3 9-6-1 6-6 11-12 11-3 0-6-2-7-4z" fill="#0194f3" />
      </svg>
    </span>
  );
}

const FOOTER_COLS = [
  {
    title: "About us",
    links: ["How to book", "Contact us", "Help centre", "Careers", "About traintix"],
  },
  {
    title: "Products",
    links: ["Train tickets", "Bus tickets", "Hotels", "Station transfers", "Car rental", "Things to do"],
  },
  {
    title: "Other",
    links: [
      "Privacy policy",
      "Terms & Conditions",
      "List your property",
      "Press room",
      "Operating regulations",
    ],
  },
];

const PAYMENT_PARTNERS = [
  "Mastercard", "VISA", "JCB", "AMEX",
  "VietQR", "MoMo", "Techcombank", "VPBank",
  "VIB", "Vietcombank", "OnePay", "MB",
  "HSBC", "Sacombank", "ACB", "TPBank",
];

const SOCIALS = [
  { label: "Facebook", icon: "f" },
  { label: "Instagram", icon: "◉" },
  { label: "TikTok", icon: "♪" },
  { label: "Youtube", icon: "▶" },
  { label: "Telegram", icon: "✈" },
];

function SiteFooter() {
  return (
    <footer className="mt-12 bg-brand-900 text-ink-100">
      <div className="mx-auto grid max-w-[1200px] gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)]">
        {/* Brand column */}
        <div>
          <Wordmark light />
          <div className="mt-5 flex flex-wrap gap-2">
            {["IATA", "ISO 27001", "Registered with the Ministry of Industry and Trade"].map((c) => (
              <span
                key={c}
                className="rounded border border-white/25 px-2.5 py-1 text-[11px] font-semibold text-white/80"
              >
                {c}
              </span>
            ))}
          </div>

          <h3 className="mt-8 text-sm font-bold text-white">Payment partners</h3>
          <ul className="mt-3 grid grid-cols-4 gap-1.5">
            {PAYMENT_PARTNERS.map((p) => (
              <li
                key={p}
                className="grid h-9 place-items-center rounded bg-white px-1 text-[9px] font-bold leading-tight text-ink-800"
              >
                <span className="truncate px-0.5">{p}</span>
              </li>
            ))}
          </ul>
        </div>

        {FOOTER_COLS.map((col) => (
          <div key={col.title}>
            <h3 className="text-sm font-bold text-white">{col.title}</h3>
            <ul className="mt-3 space-y-2.5 text-sm">
              {col.links.map((l) => (
                <li key={l}>
                  <a href="#" className="rounded text-ink-200 hover:text-white hover:underline">
                    {l}
                  </a>
                </li>
              ))}
            </ul>

            {col.title === "Products" && (
              <>
                <h3 className="mt-8 text-sm font-bold text-white">Follow us on</h3>
                <ul className="mt-3 space-y-2.5 text-sm">
                  {SOCIALS.map((s) => (
                    <li key={s.label}>
                      <a href="#" className="flex items-center gap-2.5 rounded text-ink-200 hover:text-white">
                        <span
                          aria-hidden
                          className="grid size-6 place-items-center rounded bg-white/15 text-[11px] font-bold"
                        >
                          {s.icon}
                        </span>
                        {s.label}
                      </a>
                    </li>
                  ))}
                </ul>
              </>
            )}

            {col.title === "Other" && (
              <>
                <h3 className="mt-8 text-sm font-bold text-white">Get the traintix app</h3>
                <div className="mt-3 flex flex-col gap-2">
                  {[
                    { top: "GET IT ON", bottom: "Google Play" },
                    { top: "Download on the", bottom: "App Store" },
                  ].map((s) => (
                    <a
                      key={s.bottom}
                      href="#"
                      className="flex w-40 items-center gap-2 rounded-md border border-white/30 bg-black px-3 py-1.5"
                    >
                      <span aria-hidden className="text-lg">▷</span>
                      <span className="leading-tight">
                        <span className="block text-[9px] text-white/70">{s.top}</span>
                        <span className="block text-sm font-semibold text-white">{s.bottom}</span>
                      </span>
                    </a>
                  ))}
                </div>
              </>
            )}
          </div>
        ))}
      </div>

      <div className="border-t border-white/15">
        <p className="mx-auto max-w-[1200px] px-4 py-5 text-xs leading-relaxed text-ink-300 sm:px-6">
          traintix — a demo interface for a lesson on ticketing system architecture. The data shown is
          simulated, not real fares. Support hotline 1900 0109.
        </p>
      </div>
    </footer>
  );
}
