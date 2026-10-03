import { useEffect, useRef, useState } from "react";

/**
 * Counts down to a moment in time.
 *
 * Two things that matter for the hold timer:
 *  1. The expiry comes from the server (expiresAt), the client only displays
 *     it — changing the device clock cannot extend a hold.
 *  2. Uses Date.now() instead of adding 1 second per tick, because mobile
 *     browsers throttle setInterval in background tabs; when the user comes
 *     back, the remaining seconds are still accurate.
 */
export function useCountdown(
  target: string | Date | null,
  opts: { onExpire?: () => void; intervalMs?: number } = {},
) {
  const { onExpire, intervalMs = 1000 } = opts;
  const targetMs = target ? new Date(target).getTime() : null;
  const [remaining, setRemaining] = useState(() =>
    targetMs == null ? 0 : Math.max(0, (targetMs - Date.now()) / 1000),
  );
  const firedRef = useRef(false);
  const onExpireRef = useRef(onExpire);
  useEffect(() => {
    onExpireRef.current = onExpire;
  });

  useEffect(() => {
    if (targetMs == null) return;
    firedRef.current = false;

    const tick = () => {
      const left = Math.max(0, (targetMs - Date.now()) / 1000);
      setRemaining(left);
      if (left <= 0 && !firedRef.current) {
        firedRef.current = true;
        onExpireRef.current?.();
      }
    };
    tick();
    const id = setInterval(tick, intervalMs);
    // Tab hidden then shown again: sync right away, do not wait for the next tick
    const onVisible = () => document.visibilityState === "visible" && tick();
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [targetMs, intervalMs]);

  return {
    remaining,
    seconds: Math.ceil(remaining),
    expired: targetMs != null && remaining <= 0,
  };
}
