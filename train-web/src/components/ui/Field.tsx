import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

/**
 * Ô nhập liệu có nhãn, gợi ý và báo lỗi.
 *
 * Quy ước accessibility dùng chung cho cả form:
 *  - Nhãn luôn hiện (không dùng placeholder thay nhãn — người lớn tuổi hay
 *    quên trường đang nhập là gì khi placeholder biến mất).
 *  - Lỗi gắn với ô qua aria-describedby + aria-invalid, và có role="alert"
 *    để trình đọc màn hình đọc ngay khi xuất hiện.
 *  - Lỗi báo bằng cả màu ĐỎ lẫn biểu tượng + chữ, không chỉ dựa vào màu.
 */
type BaseProps = {
  label: string;
  hint?: string;
  error?: string | null;
  required?: boolean;
  /** Nhãn phụ bên phải, ví dụ "Không bắt buộc" */
  trailingLabel?: ReactNode;
};

export function Field({
  label, hint, error, required, trailingLabel, children, htmlFor, describedById,
}: BaseProps & { children: ReactNode; htmlFor: string; describedById: string }) {
  return (
    <div className="w-full">
      <div className="mb-1.5 flex items-baseline justify-between gap-2">
        <label htmlFor={htmlFor} className="text-sm font-semibold text-ink-800">
          {label}
          {required && <span className="ml-1 text-son-600" aria-hidden>*</span>}
          {required && <span className="sr-only"> (bắt buộc)</span>}
        </label>
        {trailingLabel && <span className="text-xs text-ink-500">{trailingLabel}</span>}
      </div>
      {children}
      <div id={describedById} className="min-h-5">
        {error ? (
          <p role="alert" className="mt-1 flex items-start gap-1 text-sm font-medium text-son-700">
            <span aria-hidden>⚠</span>
            <span>{error}</span>
          </p>
        ) : hint ? (
          <p className="mt-1 text-sm text-ink-500">{hint}</p>
        ) : null}
      </div>
    </div>
  );
}

const CONTROL_BASE =
  "w-full min-h-12 rounded-xl border-2 bg-white px-3.5 text-base text-ink-900 " +
  "placeholder:text-ink-400 transition-colors";

export type TextInputProps = BaseProps & InputHTMLAttributes<HTMLInputElement>;

export function TextInput({ label, hint, error, required, trailingLabel, className, id, ...props }: TextInputProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const describedById = `${inputId}-desc`;
  return (
    <Field label={label} hint={hint} error={error} required={required} trailingLabel={trailingLabel} htmlFor={inputId} describedById={describedById}>
      <input
        {...props}
        id={inputId}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedById}
        className={cn(
          CONTROL_BASE,
          error ? "border-son-500 bg-son-50/40" : "border-ink-200 hover:border-ink-300 focus:border-son-500",
          className,
        )}
      />
    </Field>
  );
}

export type SelectInputProps = BaseProps &
  SelectHTMLAttributes<HTMLSelectElement> & { options: Array<{ value: string; label: string; disabled?: boolean }> };

export function SelectInput({
  label, hint, error, required, trailingLabel, options, className, id, ...props
}: SelectInputProps) {
  const autoId = useId();
  const selectId = id ?? autoId;
  const describedById = `${selectId}-desc`;
  return (
    <Field label={label} hint={hint} error={error} required={required} trailingLabel={trailingLabel} htmlFor={selectId} describedById={describedById}>
      <div className="relative">
        <select
          {...props}
          id={selectId}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedById}
          className={cn(
            CONTROL_BASE,
            "appearance-none pr-10",
            error ? "border-son-500 bg-son-50/40" : "border-ink-200 hover:border-ink-300 focus:border-son-500",
            className,
          )}
        >
          {options.map((o) => (
            <option key={o.value} value={o.value} disabled={o.disabled}>
              {o.label}
            </option>
          ))}
        </select>
        <span aria-hidden className="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-ink-500">
          ▾
        </span>
      </div>
    </Field>
  );
}
