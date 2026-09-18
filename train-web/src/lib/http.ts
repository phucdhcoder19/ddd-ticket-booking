import { ApiError, type ApiErrorKind } from "@/api/errors";

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
  /** Huỷ yêu cầu nếu quá thời gian này (ms). Mặc định 12s cho mạng chậm. */
  timeoutMs?: number;
  signal?: AbortSignal;
  /** Số lần thử lại tối đa với lỗi 429/5xx/timeout. Mặc định 3. */
  maxRetries?: number;
  /** Được gọi trước mỗi lần chờ để thử lại — dùng để báo cho người dùng biết. */
  onRetry?: (notice: RetryNotice) => void;
  /**
   * Khoá chống gửi trùng. Server dùng key này để bỏ qua request lặp
   * (double-click, retry sau timeout) — bắt buộc với mọi thao tác trừ tồn kho.
   */
  idempotencyKey?: string;
};

const RETRYABLE: ApiErrorKind[] = ["OVERLOADED", "SERVER", "TIMEOUT"];

/**
 * Backend bọc MỌI phản hồi trong ResultMessage:
 *   { success, code, message, timestamp, result }
 *
 * và luôn trả HTTP 200 — mã lỗi thật nằm ở trường "code" trong thân, không
 * nằm ở status dòng đầu. Nên không thể tin res.ok, phải mở phong bì ra xem.
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

/** Backoff luỹ thừa + jitter: 0.6s, 1.2s, 2.4s (±25%), tôn trọng Retry-After của server. */
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
  if (status === 401 || status === 403) return "UNAUTHORIZED";
  if (status === 404) return "NOT_FOUND";
  if (status >= 500) return "SERVER";
  return "UNKNOWN";
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, timeoutMs = 12_000, signal, maxRetries = 3, onRetry, idempotencyKey } = options;
  const maxAttempts = maxRetries + 1;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    if (typeof navigator !== "undefined" && navigator.onLine === false) {
      throw new ApiError("OFFLINE", "Thiết bị đang ngoại tuyến");
    }
    const timeoutCtl = new AbortController();
    const timer = setTimeout(() => timeoutCtl.abort(new DOMException("Timeout", "TimeoutError")), timeoutMs);
    // Người dùng huỷ (rời màn hình) và hết giờ chờ là hai nguồn huỷ khác nhau
    const composed = signal ? AbortSignal.any([signal, timeoutCtl.signal]) : timeoutCtl.signal;

    try {
      const res = await fetch(`${BASE_URL}${path}`, {
        method,
        headers: {
          "Content-Type": "application/json",
          ...(idempotencyKey ? { "Idempotency-Key": idempotencyKey } : {}),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: composed,
      });
      clearTimeout(timer);

      if (res.ok) {
        if (res.status === 204) return undefined as T;
        const payload: unknown = await res.json();

        // Không phải phong bì (ví dụ server khác) -> trả thẳng
        if (!isEnvelope(payload)) return payload as T;

        if (payload.success) return payload.result as T;

        // success = false -> lỗi nghiệp vụ, đọc mã trong thân
        const kind = kindFromStatus(payload.code);
        const err = new ApiError(kind, payload.message ?? "Yêu cầu không thành công", {
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
      // Người dùng chủ động huỷ — không phải lỗi, không retry
      if (signal?.aborted) throw e;
      const isTimeout = e instanceof DOMException && (e.name === "TimeoutError" || e.name === "AbortError");
      const kind: ApiErrorKind = isTimeout ? "TIMEOUT" : "OFFLINE";
      if (attempt === maxAttempts) {
        throw new ApiError(kind, isTimeout ? "Yêu cầu quá thời gian chờ" : "Không kết nối được máy chủ");
      }
      const delay = backoffDelay(attempt);
      onRetry?.({ attempt, maxAttempts, delayMs: delay, kind });
      await sleep(delay, signal);
    }
  }
  throw new ApiError("UNKNOWN", "Không thể hoàn tất yêu cầu");
}

/** Khoá idempotency cho một thao tác — giữ nguyên qua các lần retry của cùng một hành động. */
export const newIdempotencyKey = () =>
  globalThis.crypto?.randomUUID?.() ?? `k_${Date.now()}_${Math.random().toString(36).slice(2)}`;
