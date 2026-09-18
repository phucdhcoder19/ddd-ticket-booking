import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";

export type ToastTone = "info" | "success" | "warning" | "error";
export type Toast = { id: number; tone: ToastTone; title: string; detail?: string; durationMs: number };

type ToastApi = {
  show: (t: { tone?: ToastTone; title: string; detail?: string; durationMs?: number }) => number;
  dismiss: (id: number) => void;
};

const Ctx = createContext<ToastApi | null>(null);

const TONE_STYLE: Record<ToastTone, string> = {
  info: "border-info-600/25 bg-info-50 text-ink-900",
  success: "border-ok-600/25 bg-ok-50 text-ink-900",
  warning: "border-warn-600/30 bg-warn-50 text-ink-900",
  error: "border-son-600/25 bg-son-50 text-ink-900",
};

const TONE_ICON: Record<ToastTone, string> = {
  info: "ℹ️",
  success: "✓",
  warning: "⚠️",
  error: "!",
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const idRef = useRef(1);

  const dismiss = useCallback((id: number) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const show = useCallback<ToastApi["show"]>(
    ({ tone = "info", title, detail, durationMs = 5000 }) => {
      const id = idRef.current++;
      setToasts((list) => [...list.slice(-2), { id, tone, title, detail, durationMs }]);
      if (durationMs > 0) setTimeout(() => dismiss(id), durationMs);
      return id;
    },
    [dismiss],
  );

  const api = useMemo(() => ({ show, dismiss }), [show, dismiss]);

  return (
    <Ctx.Provider value={api}>
      {children}
      {/*
        aria-live="polite" để trình đọc màn hình đọc thông báo mà không cắt
        ngang thao tác đang làm. Đặt trên cùng để không che nút thao tác chính
        nằm dưới đáy màn hình điện thoại.
      */}
      <div
        className="pointer-events-none fixed inset-x-0 top-2 z-50 flex flex-col items-center gap-2 px-3"
        role="region"
        aria-live="polite"
        aria-label="Thông báo"
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className={`pointer-events-auto w-full max-w-md rounded-2xl border px-4 py-3 shadow-[var(--shadow-lift)] ${TONE_STYLE[t.tone]}`}
          >
            <div className="flex items-start gap-3">
              <span aria-hidden className="mt-0.5 grid size-6 shrink-0 place-items-center rounded-full bg-white/70 text-sm font-bold">
                {TONE_ICON[t.tone]}
              </span>
              <div className="min-w-0 flex-1">
                <p className="font-semibold">{t.title}</p>
                {t.detail && <p className="mt-0.5 text-sm text-ink-700">{t.detail}</p>}
              </div>
              <button
                type="button"
                onClick={() => dismiss(t.id)}
                aria-label="Đóng thông báo"
                className="-m-2 grid size-9 shrink-0 place-items-center rounded-full text-ink-500 hover:bg-white/60 hover:text-ink-900"
              >
                ✕
              </button>
            </div>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useToast phải nằm trong ToastProvider");
  return ctx;
}
