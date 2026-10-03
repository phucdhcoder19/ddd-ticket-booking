export type ApiErrorKind =
  | "SOLD_OUT"        // 409 — sold out
  | "SEAT_TAKEN"      // 409 — someone else just took the seat
  | "HOLD_EXPIRED"    // 410 — the hold has expired
  | "OVERLOADED"      // 429 — the system is overloaded
  | "TIMEOUT"         // slow network / no response
  | "OFFLINE"
  | "VALIDATION"      // 400
  | "UNAUTHORIZED"    // 401
  | "NOT_ADMITTED"    // 403 — not admitted by the waiting room, or the admission has expired
  | "NOT_FOUND"       // 404
  | "SERVER"          // 5xx
  | "UNKNOWN";

export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status?: number;
  /** How long the server suggests waiting before retrying (seconds) — from the Retry-After header */
  readonly retryAfter?: number;
  /** Extra data, e.g. the list of seats that were just taken */
  readonly details?: unknown;

  constructor(kind: ApiErrorKind, message: string, opts: { status?: number; retryAfter?: number; details?: unknown } = {}) {
    super(message);
    this.name = "ApiError";
    this.kind = kind;
    this.status = opts.status;
    this.retryAfter = opts.retryAfter;
    this.details = opts.details;
  }
}

/**
 * Messages shown to the user — always say CLEARLY what happened and WHAT TO
 * DO NEXT, never dump a technical error code on the screen.
 */
export function friendlyMessage(err: unknown): { title: string; detail: string; action?: string } {
  if (!(err instanceof ApiError)) {
    return {
      title: "Something went wrong",
      detail: "Sorry, the system ran into an unexpected problem. Please try again.",
      action: "Try again",
    };
  }
  switch (err.kind) {
    case "SOLD_OUT":
      return {
        title: "This trip is sold out",
        detail: "The tickets sold out while you were choosing. Try another trip or a nearby date.",
        action: "See other trips",
      };
    case "SEAT_TAKEN":
      return {
        title: "Someone just took that seat",
        detail: "Another passenger was a little faster. We have highlighted free seats nearby for you.",
        action: "Choose another seat",
      };
    case "HOLD_EXPIRED":
      return {
        title: "Your hold has expired",
        detail: "Your seats were released to other passengers. Pick your seats again to continue.",
        action: "Pick seats again",
      };
    case "OVERLOADED":
      return {
        title: "The system is very busy",
        detail: "Too many people are booking right now. We are retrying for you automatically, please keep this page open.",
      };
    case "TIMEOUT":
      return {
        title: "The network is slow",
        detail: "Your request waited too long without a response. Check your connection and try again.",
        action: "Try again",
      };
    case "OFFLINE":
      return {
        title: "No network connection",
        detail: "Your device is offline. Your tickets and unfinished steps are kept as they are.",
        action: "Try again",
      };
    case "VALIDATION":
      return { title: "Some details are not valid", detail: err.message, action: "Check again" };
    case "NOT_FOUND":
      return { title: "Not found", detail: "What you are looking for no longer exists or has been removed." };
    case "NOT_ADMITTED":
      return {
        title: "Your turn to buy has ended",
        detail: "To be fair to everyone, each turn lasts 15 minutes. Please join the queue again.",
        action: "Join the queue again",
      };
    case "UNAUTHORIZED":
      return { title: "Your session has expired", detail: "Please sign in again to continue.", action: "Sign in" };
    default:
      return {
        title: "The server is busy",
        detail: "The system cannot handle your request right now. Please try again in a few minutes.",
        action: "Try again",
      };
  }
}
