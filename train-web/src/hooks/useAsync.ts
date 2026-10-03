import { useCallback, useEffect, useRef, useState } from "react";
import type { CallOptions } from "@/api/client";
import type { RetryNotice } from "@/lib/http";

export type AsyncState<T> = {
  data: T | undefined;
  loading: boolean;
  error: unknown;
  /** Retrying automatically after a 429/timeout — used to show the "retrying" banner */
  retrying: RetryNotice | null;
  /** The first load (shows a skeleton) differs from a background refresh (keeps old data) */
  isInitialLoad: boolean;
};

/**
 * Wraps an API call into the 4 states every screen needs:
 * loading → (empty | error | success). Cancels the request when leaving the screen.
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
  // Always use the latest version of the API function without re-running the effect
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
