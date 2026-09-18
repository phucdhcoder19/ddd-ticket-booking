import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "md" | "lg";

/**
 * Nút bấm chuẩn của hệ thống.
 * - Chiều cao tối thiểu 48px (lg: 56px) — đủ lớn để bấm bằng ngón cái trên điện thoại.
 * - Khi `loading`, nút bị vô hiệu hoá nhưng vẫn giữ nguyên chiều rộng để layout không giật.
 * - `aria-busy` cho trình đọc màn hình biết đang xử lý.
 */
export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  loadingText?: string;
  fullWidth?: boolean;
  leading?: ReactNode;
};

const VARIANTS: Record<Variant, string> = {
  primary: "bg-son-600 text-white hover:bg-son-700 active:bg-son-800 shadow-[var(--shadow-soft)]",
  secondary: "bg-white text-ink-900 border-2 border-ink-200 hover:border-ink-300 hover:bg-ink-50",
  ghost: "bg-transparent text-son-700 hover:bg-son-50",
  danger: "bg-white text-son-700 border-2 border-son-200 hover:bg-son-50",
};

const SIZES: Record<Size, string> = {
  md: "min-h-12 px-5 text-base",
  lg: "min-h-14 px-6 text-lg",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  loadingText,
  fullWidth = false,
  leading,
  className,
  disabled,
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      {...props}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition-colors",
        "disabled:cursor-not-allowed disabled:opacity-55",
        VARIANTS[variant],
        SIZES[size],
        fullWidth && "w-full",
        className,
      )}
    >
      {loading ? <Spinner /> : leading}
      <span>{loading && loadingText ? loadingText : children}</span>
    </button>
  );
}

export function Spinner({ className }: { className?: string }) {
  return (
    <svg
      className={cn("size-5 animate-spin", className)}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="3" opacity="0.25" />
      <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}
