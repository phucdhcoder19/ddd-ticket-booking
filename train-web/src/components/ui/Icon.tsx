/**
 * Bộ icon nét vẽ, thay cho emoji.
 *
 * Emoji trông không chuyên nghiệp và quan trọng hơn: mỗi hệ điều hành vẽ
 * một kiểu — 🚆 trên Windows khác hẳn trên macOS, còn Android thì đổi cả
 * màu. Không kiểm soát được giao diện.
 *
 * SVG nét vẽ thì: cùng một hình ở mọi máy, ăn theo màu chữ qua
 * currentColor, và nét mảnh 1.6 hợp với chữ hơn khối màu đặc của emoji.
 */

type IconProps = {
  /** Kích thước cạnh, px. Mặc định 22 — vừa với chữ 15-16px. */
  size?: number;
  className?: string;
};

const base = (size: number, className?: string) => ({
  width: size,
  height: size,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.6,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  "aria-hidden": true,
  className,
});

export function IconTrain({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="5" y="3" width="14" height="13" rx="3" />
      <path d="M5 10h14M9 20l-2 2M15 20l2 2M7 16h.01M17 16h.01" />
      <path d="M8 20h8" />
    </svg>
  );
}

export function IconBus({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="4" y="4" width="16" height="12" rx="2" />
      <path d="M4 10h16M7 20v-2M17 20v-2M7 13h.01M17 13h.01" />
    </svg>
  );
}

export function IconHotel({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M4 20V5a1 1 0 0 1 1-1h9a1 1 0 0 1 1 1v15" />
      <path d="M15 10h4a1 1 0 0 1 1 1v9M2 20h20" />
      <path d="M7 8h2M7 12h2M11 8h1M11 12h1" />
    </svg>
  );
}

export function IconTransfer({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M3 17h2l1-5h9l3 5h3" />
      <circle cx="7.5" cy="18.5" r="1.5" />
      <circle cx="17" cy="18.5" r="1.5" />
      <path d="M9 12V7a1 1 0 0 1 1-1h3" />
    </svg>
  );
}

export function IconCar({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M4 16v2M20 16v2" />
      <path d="M3 16v-3l2-5h14l2 5v3a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1Z" />
      <path d="M6 13h2M16 13h2" />
    </svg>
  );
}

export function IconTicket({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M4 8V6a1 1 0 0 1 1-1h14a1 1 0 0 1 1 1v2a2 2 0 0 0 0 4v2a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-2a2 2 0 0 0 0-4Z" />
      <path d="M13 5v2M13 11v2M13 17v2" strokeDasharray="1 2" />
    </svg>
  );
}

/** Lưới chấm — nút "Khác" */
export function IconGrid({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)} strokeWidth={2}>
      <path d="M5 6h.01M12 6h.01M19 6h.01M5 12h.01M12 12h.01M19 12h.01M5 18h.01M12 18h.01M19 18h.01" />
    </svg>
  );
}

export function IconPin({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M12 21s7-5.5 7-11a7 7 0 1 0-14 0c0 5.5 7 11 7 11Z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

export function IconFlag({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M5 21V4M5 4h11l-2 3.5L16 11H5" />
    </svg>
  );
}

export function IconCalendar({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <rect x="3.5" y="5" width="17" height="15" rx="2" />
      <path d="M3.5 10h17M8 3v4M16 3v4" />
    </svg>
  );
}

export function IconUser({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M5 20c0-3.5 3.1-5.5 7-5.5s7 2 7 5.5" />
    </svg>
  );
}

export function IconSearch({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)} strokeWidth={2.2}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="M15.5 15.5 20 20" />
    </svg>
  );
}

export function IconSwap({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <path d="M7 8h11l-3-3M17 16H6l3 3" />
    </svg>
  );
}

export function IconPercent({ size = 22, className }: IconProps) {
  return (
    <svg {...base(size, className)}>
      <circle cx="7.5" cy="7.5" r="2.5" />
      <circle cx="16.5" cy="16.5" r="2.5" />
      <path d="M18 6 6 18" />
    </svg>
  );
}

export function IconChevronDown({ size = 16, className }: IconProps) {
  return (
    <svg {...base(size, className)} strokeWidth={2}>
      <path d="m6 9 6 6 6-6" />
    </svg>
  );
}

export function IconLogin({ size = 18, className }: IconProps) {
  return (
    <svg {...base(size, className)} strokeWidth={2}>
      <path d="M10 4H6a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h4M15 12H9M15 12l-3-3M15 12l-3 3" />
    </svg>
  );
}
