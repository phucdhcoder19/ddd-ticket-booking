import { useCallback, useEffect, useRef, useState } from "react";
import type { CallOptions } from "@/api/client";
import type { RetryNotice } from "@/lib/http";

export type AsyncState<T> = {
  data: T | undefined;
  loading: boolean;
  error: unknown;
  /** Đang tự động thử lại sau 429/timeout — dùng để hiện dải băng "đang thử lại" */
  retrying: RetryNotice | null;
  /** Lần tải đầu tiên (hiện skeleton) khác với lần làm mới nền (giữ nguyên dữ liệu cũ) */
  isInitialLoad: boolean;
};

/**
 * Bọc một lời gọi API thành 4 trạng thái mà mọi màn hình đều cần:
 * loading → (empty | error | success). Tự huỷ request khi rời màn hình.
 */
export function useAsync<T>(
  fn: (opts: CallOptions) => Promise<T>,
  deps: unknown[],
  options: { immediate?: boolean } = {},
) {
  const { immediate = true } = options;
  const [state, setState] = useState<AsyncState<T>>({
    data: undefined,
    loading: immediate,
    error: null,
    retrying: null,
    isInitialLoad: true,
  });
  const fnRef = useRef(fn);
  // Luôn dùng phiên bản mới nhất của hàm gọi API mà không phải chạy lại effect
  useEffect(() => {
    fnRef.current = fn;
  });
  const abortRef = useRef<AbortController | null>(null);

  const run = useCallback(
    async (opts: { silent?: boolean } = {}) => {
      abortRef.current?.abort();
      const ctl = new AbortController();
      abortRef.current = ctl;
      setState((s) => ({ ...s, loading: !opts.silent, error: opts.silent ? s.error : null, retrying: null }));
      try {
        const data = await fnRef.current({
          signal: ctl.signal,
          onRetry: (notice) => setState((s) => ({ ...s, retrying: notice })),
        });
        if (ctl.signal.aborted) return;
        setState({ data, loading: false, error: null, retrying: null, isInitialLoad: false });
        return data;
      } catch (e) {
        if (ctl.signal.aborted) return;
        setState((s) => ({ ...s, loading: false, error: e, retrying: null, isInitialLoad: false }));
      }
    },
    [],
  );

  useEffect(() => {
    if (immediate) void run();
    return () => abortRef.current?.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  return { ...state, run, reload: () => run(), refreshSilently: () => run({ silent: true }) };
}
