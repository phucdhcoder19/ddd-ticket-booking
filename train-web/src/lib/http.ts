import { ApiError, type ApiErrorKind } from "@/api/errors";
import { getQueuePass } from "@/lib/queuePass";

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";

export type RetryNotice = {
  attempt: number;
  maxAttempts: number;
  delayMs: number;
  kind: ApiErrorKind;
};

export type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  /** Cancel the request after this many ms. Default 12s for slow networks. */
  timeoutMs?: number;
  signal?: AbortSignal;
  /** Max retries for 429/5xx/timeout errors. Default 3. */
  maxRetries?: number;
  /** Called before each wait-and-retry — used to let the user know. */
  onRetry?: (notice: RetryNotice) => void;
  /**
   * Duplicate-submission key. The server uses it to ignore repeated requests
   * (double-click, retry after timeout) — required for every stock-deducting action.
   */
  idempotencyKey?: string;
};

const RETRYABLE: ApiErrorKind[] = ["OVERLOADED", "SERVER", "TIMEOUT"];

/**
 * The backend wraps EVERY response in a ResultMessage:
 *   { success, code, message, timestamp, result }
 *
 * and always returns HTTP 200 — the real error code is in the "code" field of
 * the body, not in the status line. So res.ok cannot be trusted; the envelope
 * has to be opened.
 */
type Envelope<T> = {
  success: boolean;
  code: number;
  message: string;
  timestamp?: number;
  result: T;
};

function isEnvelope(x: unknown): x is Envelope<unknown> {
  return typeof x === "object" && x !== null && "success" in x && "result" in x;
}

/** Exponential backoff + jitter: 0.6s, 1.2s, 2.4s (±25%), honouring the server's Retry-After. */
function backoffDelay(attempt: number, retryAfterSeconds?: number): number {
  if (retryAfterSeconds != null) return Math.min(retryAfterSeconds * 1000, 15_000);
  const base = 600 * 2 ** (attempt - 1);
  const jitter = base * 0.25 * (Math.random() * 2 - 1);
  return Math.min(base + jitter, 8_000);
}

const sleep = (ms: number, signal?: AbortSignal) =>
  new Promise<void>((resolve, reject) => {
    const t = setTimeout(resolve, ms);
    signal?.addEventListener("abort", () => {
      clearTimeout(t);
      reject(new DOMException("Aborted", "AbortError"));
    }, { once: true });
  });

function kindFromStatus(status: number, code?: string): ApiErrorKind {
  if (status === 409) {
    if (code === "SEAT_TAKEN" || code === "SEAT_ALREADY_HELD") return "SEAT_TAKEN";
    return "SOLD_OUT";
  }
  if (status === 410) return "HOLD_EXPIRED";
  if (status === 429) return "OVERLOADED";
  if (status === 400 || status === 422) return "VALIDATION";
  if (status === 401) return "UNAUTHORIZED";
  if (status === 403) return "NOT_ADMITTED";
  if (status === 404) return "NOT_FOUND";
  if (status >= 500) return "SERVER";
  return "UNKNOWN";
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, timeoutMs = 12_000, signal, maxRetries = 3, onRetry, idempotencyKey } = options;
  const maxAttempts = maxRetries + 1;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      throw new ApiError("OFFLINE", "The device is offline");
    }
    const timeoutCtl = new AbortController();
    const timer = setTimeout(() => timeoutCtl.abort(new DOMException("Timeout", "TimeoutError")), timeoutMs);
    // The user cancelling (leaving the screen) and the timeout are two different abort sources
    const composed = signal ? AbortSignal.any([signal, timeoutCtl.signal]) : timeoutCtl.signal;
    const queuePass = getQueuePass();

    try {
      const res = await fetch(`${BASE_URL}${path}`, {
        method,
        headers: {
          "Content-Type": "application/json",
          ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {}),
          // Attach the admission pass to EVERY request, not just /holds: the API
          // layer does not need to know which endpoints require it, the server decides.
          ...(queuePass ? { "X-Queue-Token": queuePass } : {}),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: composed,
      });
      clearTimeout(timer);

      if (res.ok) {
        if (res.status === 204) return undefined as T;
        const payload: unknown = await res.json();

        // Not an envelope (e.g. a different server) -> return as is
        if (!isEnvelope(payload)) return payload as T;

        if (payload.success) return payload.result as T;

        // success = false -> business error, read the code from the body
        const kind = kindFromStatus(payload.code);
        const err = new ApiError(kind, payload.message ?? "The request did not succeed", {
          status: payload.code,
        });
        if (!RETRYABLE.includes(kind) || attempt === maxAttempts) throw err;
        const retryDelay = backoffDelay(attempt);
        onRetry?.({ attempt, maxAttempts, delayMs: retryDelay, kind });
        await sleep(retryDelay, signal);
        continue;
      }

      const payload = await res.json().catch(() => ({}) as Record<string, unknown>);
      const kind = kindFromStatus(res.status, payload.code as string | undefined);
      const retryAfterHeader = res.headers.get("Retry-After");
      const err = new ApiError(kind, (payload.message as string) ?? res.statusText, {
        status: res.status,
        retryAfter: retryAfterHeader ? Number(retryAfterHeader) : undefined,
        details: payload.details,
      });

      if (!RETRYABLE.includes(kind) || attempt === maxAttempts) throw err;
      const delay = backoffDelay(attempt, err.retryAfter);
      onRetry?.({ attempt, maxAttempts, delayMs: delay, kind });
      await sleep(delay, signal);
      continue;
    } catch (e) {
      clearTimeout(timer);
      if (e instanceof ApiError) throw e;
      // The user cancelled on purpose — not an error, no retry
      if (signal?.aborted) throw e;
      const isTimeout = e instanceof DOMException && (e.name === "TimeoutError" || e.name === "AbortError");
      const kind: ApiErrorKind = isTimeout ? "TIMEOUT" : "OFFLINE";
      if (attempt === maxAttempts) {
        throw new ApiError(kind, isTimeout ? "The request timed out" : "Could not connect to the server");
      }
      const delay = backoffDelay(attempt);
      onRetry?.({ attempt, maxAttempts, delayMs: delay, kind });
      await sleep(delay, signal);
    }
  }
  throw new ApiError("UNKNOWN", "The request could not be completed");
}

/** Idempotency key for one action — stays the same across retries of that action. */
export const newIdempotencyKey = () =>
  globalThis.crypto?.randomUUID?.() ?? `k_${Date.now()}_${Math.random().toString(36).slice(2)}`;
