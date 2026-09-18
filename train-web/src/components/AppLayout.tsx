import { NavLink, Outlet, useLocation } from "react-router-dom";
import { cn } from "@/lib/cn";

/**
 * Khung ứng dụng: header mỏng có họa tiết hoa mai rất nhẹ, nội dung giới hạn
 * 640px (tối ưu cho điện thoại, trên máy tính thì căn giữa như một cột đọc),
 * và thanh điều hướng dưới đáy theo kiểu ứng dụng di động.
 */
export function AppLayout() {
  const { pathname } = useLocation();
  // Phòng chờ chiếm trọn màn hình, không có điều hướng để người dùng không bỏ lượt
  const bare = pathname.startsWith("/phong-cho");

  return (
    <div className="flex min-h-dvh flex-col bg-canvas">
      <a
        href="#noi-dung"
        className="sr-only focus:not-sr-only focus:absolute focus:left-3 focus:top-3 focus:z-50 focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:font-semibold focus:text-son-700"
      >
        Bỏ qua, tới nội dung chính
      </a>

      <header className="bg-mai-pattern sticky top-0 z-30 bg-son-700 text-white shadow-[var(--shadow-soft)]">
        <div className="mx-auto flex max-w-2xl items-center justify-between gap-3 px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2 rounded-lg" aria-label="Vé Tàu Tết — về trang chủ">
            <span aria-hidden className="grid size-9 place-items-center rounded-xl bg-white/15 text-lg">🚆</span>
            <span className="leading-tight">
              <span className="block font-bold">Vé Tàu Tết</span>
              <span className="block text-[11px] text-mai-100">Đường sắt Việt Nam</span>
            </span>
          </NavLink>
          <NavLink
            to="/ve-cua-toi"
            className="rounded-xl bg-white/15 px-3 py-2 text-sm font-semibold hover:bg-white/25"
          >
            Vé của tôi
          </NavLink>
        </div>
      </header>

      <main id="noi-dung" className="mx-auto w-full max-w-2xl flex-1 px-4 pb-6 pt-4 sm:px-6">
        <Outlet />
      </main>

      {!bare && (
        <nav
          aria-label="Điều hướng chính"
          className="sticky bottom-0 z-30 border-t border-ink-200 bg-white/95 backdrop-blur-sm sm:hidden"
        >
          <ul className="mx-auto flex max-w-2xl">
            {[
              { to: "/", label: "Tìm chuyến", icon: "🔍", end: true },
              { to: "/ve-cua-toi", label: "Vé của tôi", icon: "🎫", end: false },
            ].map((item) => (
              <li key={item.to} className="flex-1">
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn(
                      "flex min-h-14 flex-col items-center justify-center gap-0.5 py-2 text-xs font-semibold",
                      isActive ? "text-son-700" : "text-ink-500",
                    )
                  }
                >
                  <span aria-hidden className="text-lg">{item.icon}</span>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}

      <footer className="hidden border-t border-ink-200 bg-white py-6 text-center text-sm text-ink-500 sm:block">
        <p>Tổng đài hỗ trợ 1900 0109 · Giao diện minh hoạ, dữ liệu là dữ liệu giả lập.</p>
      </footer>
    </div>
  );
}
