import { NavLink, Outlet, useLocation } from "react-router-dom";
import { cn } from "@/lib/cn";

/**
 * Khung ứng dụng theo bố cục Traveloka:
 *   Hàng 1 — logo, tiền tệ/ngôn ngữ, các mục phụ, nút Đăng nhập/Đăng ký
 *   Hàng 2 — thanh sản phẩm (Vé tàu, Vé xe khách, Khách sạn...)
 *
 * Trang chủ chạy tràn viền (hero full-bleed), các trang còn lại vẫn là một
 * cột đọc hẹp 640px như cũ.
 */

const PRODUCTS = [
  { to: "/", label: "Vé tàu", end: true },
  { to: "/chuyen-tau", label: "Vé xe khách", end: false },
  { to: "/chuyen-tau", label: "Khách sạn", end: false },
  { to: "/chuyen-tau", label: "Đưa đón ga", end: false },
  { to: "/chuyen-tau", label: "Cho thuê xe", end: false },
  { to: "/chuyen-tau", label: "Hoạt động & Vui chơi", end: false },
];

export function AppLayout() {
  const { pathname } = useLocation();
  // Phòng chờ chiếm trọn màn hình, không có điều hướng để người dùng không bỏ lượt
  const bare = pathname.startsWith("/phong-cho");
  const wide = pathname === "/";

  return (
    <div className="flex min-h-dvh flex-col bg-canvas">
      <a
        href="#noi-dung"
        className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:font-semibold focus:text-son-700"
      >
        Bỏ qua, tới nội dung chính
      </a>

      <header className="sticky top-0 z-30 bg-white shadow-[var(--shadow-soft)]">
        {/* Hàng 1 */}
        <div className="bg-brand-swoosh">
          <div className="mx-auto flex h-16 max-w-[1200px] items-center gap-6 px-4 sm:px-6">
            <NavLink to="/" className="shrink-0 rounded-lg" aria-label="Vé Tàu Tết — về trang chủ">
              <Wordmark />
            </NavLink>

            <nav aria-label="Liên kết phụ" className="hidden flex-1 items-center gap-5 text-sm font-semibold text-ink-800 lg:flex">
              <button type="button" className="flex items-center gap-1.5 rounded-lg hover:text-son-600">
                <span aria-hidden>🇻🇳</span> VND | VI
              </button>
              <button type="button" className="flex items-center gap-1.5 rounded-lg hover:text-son-600">
                <span aria-hidden className="text-ok-600">%</span> Khuyến mãi
              </button>
              <button type="button" className="rounded-lg hover:text-son-600">Hợp tác với chúng tôi</button>
              <button type="button" className="rounded-lg hover:text-son-600">Hỗ trợ</button>
              <NavLink to="/ve-cua-toi" className="rounded-lg hover:text-son-600">Đặt chỗ của tôi</NavLink>
            </nav>

            <div className="ml-auto flex items-center gap-2 lg:ml-0">
              <button
                type="button"
                className="hidden rounded-full border border-son-200 bg-son-50 px-5 py-2 text-sm font-bold text-son-700 hover:bg-son-100 sm:block"
              >
                Đăng nhập
              </button>
              <button
                type="button"
                className="rounded-full bg-son-500 px-5 py-2 text-sm font-bold text-white hover:bg-son-600"
              >
                Đăng ký
              </button>
            </div>
          </div>
        </div>

        {/* Hàng 2 — thanh sản phẩm */}
        <nav aria-label="Sản phẩm" className="border-t border-ink-100">
          <ul className="no-scrollbar mx-auto flex max-w-[1200px] gap-1 overflow-x-auto px-4 sm:px-6">
            {PRODUCTS.map((p, i) => (
              <li key={`${p.label}-${i}`}>
                <NavLink
                  to={p.to}
                  end={p.end}
                  className={({ isActive }) =>
                    cn(
                      "block whitespace-nowrap rounded-lg px-3 py-3 text-sm font-semibold transition-colors",
                      isActive && i === 0 ? "text-son-600" : "text-ink-700 hover:text-son-600",
                    )
                  }
                >
                  {p.label}
                </NavLink>
              </li>
            ))}
            <li>
              <button type="button" className="whitespace-nowrap rounded-lg px-3 py-3 text-sm font-semibold text-ink-700 hover:text-son-600">
                Thêm ⌄
              </button>
            </li>
          </ul>
        </nav>
      </header>

      <main
        id="noi-dung"
        className={cn("w-full flex-1", wide ? "" : "mx-auto max-w-2xl px-4 pb-6 pt-4 sm:px-6")}
      >
        <Outlet />
      </main>

      {!bare && <SiteFooter />}
    </div>
  );
}

/** Logo chữ + cánh chim, dựng bằng SVG nên nét ở mọi độ phân giải */
function Wordmark({ light = false }: { light?: boolean }) {
  return (
    <span className="flex items-baseline gap-1">
      <span className={cn("text-[26px] font-bold leading-none tracking-tight", light ? "text-white" : "text-son-900")}>
        vétàu
      </span>
      <svg width="22" height="16" viewBox="0 0 22 16" aria-hidden className="translate-y-[-3px]">
        <path d="M1 11c6-1 9-4 12-8 1 3 0 5-2 7 3-1 6-3 9-6-1 6-6 11-12 11-3 0-6-2-7-4z" fill="#0194f3" />
      </svg>
    </span>
  );
}

const FOOTER_COLS = [
  {
    title: "Về chúng tôi",
    links: ["Cách đặt chỗ", "Liên hệ chúng tôi", "Trợ giúp", "Tuyển dụng", "Về Vé Tàu Tết"],
  },
  {
    title: "Sản phẩm",
    links: ["Vé tàu", "Vé xe khách", "Khách sạn", "Đưa đón ga", "Cho thuê xe", "Hoạt động & Vui chơi"],
  },
  {
    title: "Khác",
    links: [
      "Chính sách bảo mật",
      "Điều khoản & Điều kiện",
      "Đăng ký nơi nghỉ của bạn",
      "Khu vực báo chí",
      "Quy chế hoạt động",
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
    <footer className="mt-12 bg-son-900 text-ink-100">
      <div className="mx-auto grid max-w-[1200px] gap-10 px-4 py-12 sm:px-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)]">
        {/* Cột thương hiệu */}
        <div>
          <Wordmark light />
          <div className="mt-5 flex flex-wrap gap-2">
            {["IATA", "ISO 27001", "Đã đăng ký Bộ Công Thương"].map((c) => (
              <span
                key={c}
                className="rounded border border-white/25 px-2.5 py-1 text-[11px] font-semibold text-white/80"
              >
                {c}
              </span>
            ))}
          </div>

          <h3 className="mt-8 text-sm font-bold text-white">Đối tác thanh toán</h3>
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

            {col.title === "Sản phẩm" && (
              <>
                <h3 className="mt-8 text-sm font-bold text-white">Theo dõi chúng tôi trên</h3>
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

            {col.title === "Khác" && (
              <>
                <h3 className="mt-8 text-sm font-bold text-white">Tải ứng dụng Vé Tàu Tết</h3>
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
          Vé Tàu Tết — giao diện minh hoạ cho bài học kiến trúc hệ thống bán vé. Dữ liệu hiển thị là dữ liệu
          giả lập, không phải giá vé thật. Tổng đài hỗ trợ 1900 0109.
        </p>
      </div>
    </footer>
  );
}
