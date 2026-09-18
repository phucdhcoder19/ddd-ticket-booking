import { useEffect, useRef, useState } from "react";

/**
 * Đếm ngược tới một mốc thời gian.
 *
 * Hai điểm quan trọng cho đồng hồ giữ chỗ:
 *  1. Mốc hết hạn lấy từ server (expiresAt), client chỉ hiển thị — đổi giờ máy
 *     không kéo dài được thời gian giữ chỗ.
 *  2. Dùng Date.now() thay vì cộng dồn 1 giây mỗi tick, vì trình duyệt trên
 *     điện thoại sẽ bóp nghẹt setInterval khi tab chạy nền; khi quay lại tab,
 *     số giây còn lại vẫn chính xác.
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
    // Tab bị ẩn rồi hiện lại: đồng bộ ngay, không đợi tick kế tiếp
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
