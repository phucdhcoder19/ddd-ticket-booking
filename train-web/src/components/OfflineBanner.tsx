import { useEffect, useState } from "react";

/**
 * Dải băng báo mất mạng.
 *
 * Với người dùng mạng chậm, phân biệt được "mất mạng" và "hệ thống quá tải" là
 * rất quan trọng: một bên cần bật lại 4G, một bên chỉ cần chờ. Dải này chỉ nói
 * về tình trạng máy của họ, và trấn an rằng chỗ đang giữ không bị mất.
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
      Mất kết nối mạng. Chỗ bạn đang giữ vẫn được giữ nguyên trên hệ thống — bật lại mạng rồi thao tác tiếp nhé.
    </div>
  );
}
