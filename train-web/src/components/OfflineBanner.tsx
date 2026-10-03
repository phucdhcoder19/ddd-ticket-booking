import { useEffect, useState } from "react";

/**
 * Offline banner.
 *
 * For users on slow networks, telling "no connection" apart from "system
 * overloaded" matters a lot: one needs 4G turned back on, the other only needs
 * waiting. This banner only talks about their device, and reassures them that
 * the seats they hold are not lost.
 */
export function OfflineBanner() {
  const [offline, setOffline] = useState(() => typeof navigator !== "undefined" && !navigator.onLine);

  useEffect(() => {
    const on = () => setOffline(false);
    const off = () => setOffline(true);
    window.addEventListener("online", on);
    window.addEventListener("offline", off);
    return () => {
      window.removeEventListener("online", on);
      window.removeEventListener("offline", off);
    };
  }, []);

  if (!offline) return null;

  return (
    <div
      role="alert"
      className="sticky top-0 z-40 bg-ink-900 px-4 py-2.5 text-center text-sm font-semibold text-white"
    >
      <span aria-hidden>📵 </span>
      You are offline. The seats you hold are still kept on our system — reconnect and carry on.
    </div>
  );
}
