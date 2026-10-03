import { useCallback, useEffect, useRef, useState } from "react";
import { newIdempotencyKey } from "@/lib/http";

/**
 * Prevents duplicate submissions on the "Book" / "Pay" buttons.
 *
 * Three layers of protection, because only disabling the button is not enough:
 *  1. A synchronous lock (ref) — blocks the second click in the same tick,
 *     before React has even re-rendered the button as disabled.
 *  2. A `pending` flag so the UI shows a spinner and disables the button.
 *  3. A fixed Idempotency-Key for the whole action (kept across retries), so
 *     when the server receives a repeated request it returns the previous
 *     result instead of deducting stock again. A new key is only generated
 *     after the previous action succeeded.
 */
export function useSubmitLock<Args extends unknown[], R>(
  action: (args: { idempotencyKey: string }, ...rest: Args) => Promise<R>,
) {
  const [pending, setPending] = useState(false);
  const lockRef = useRef(false);
  const keyRef = useRef<string | null>(null);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const submit = useCallback(
    async (...rest: Args): Promise<R | undefined> => {
      if (lockRef.current) return undefined; // duplicate click — ignore silently
      lockRef.current = true;
      setPending(true);
      keyRef.current ??= newIdempotencyKey();
      try {
        const result = await action({ idempotencyKey: keyRef.current }, ...rest);
        keyRef.current = null; // success: the next action is a new one
        return result;
        // On error the key is NOT cleared: pressing "Try again" is still the same
        // action, so the server recognises the repeat and does not deduct stock again.
      } finally {
        lockRef.current = false;
        if (mountedRef.current) setPending(false);
      }
    },
    [action],
  );

  /** Call when the user changes their choice entirely (other seats, other payment method) */
  const resetKey = useCallback(() => {
    keyRef.current = null;
  }, []);

  return { submit, pending, resetKey };
}
