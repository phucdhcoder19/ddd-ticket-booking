import { useCallback, useEffect, useRef, useState } from "react";
import { newIdempotencyKey } from "@/lib/http";

/**
 * Chặn gửi trùng cho các nút "Đặt vé" / "Thanh toán".
 *
 * Ba lớp bảo vệ, vì chỉ disable nút là chưa đủ:
 *  1. Khoá đồng bộ (ref) — chặn ngay cú click thứ hai trong cùng một tick,
 *     trước cả khi React kịp render lại nút ở trạng thái disabled.
 *  2. Cờ `pending` để UI hiện spinner và disable nút.
 *  3. Idempotency-Key cố định cho suốt một lần thao tác (giữ nguyên qua các
 *     lần retry), để server nhận request lặp thì trả lại kết quả cũ chứ không
 *     trừ tồn kho lần nữa. Chỉ sinh key mới khi lần trước đã thất bại.
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
      if (lockRef.current) return undefined; // bấm trùng — bỏ qua im lặng
      lockRef.current = true;
      setPending(true);
      keyRef.current ??= newIdempotencyKey();
      try {
        const result = await action({ idempotencyKey: keyRef.current }, ...rest);
        keyRef.current = null; // thành công: thao tác sau là thao tác mới
        return result;
        // Lỗi thì KHÔNG xoá key: người dùng bấm "Thử lại" vẫn là cùng một thao tác,
        // nên server nhận ra request lặp và không trừ tồn kho lần nữa.
      } finally {
        lockRef.current = false;
        if (mountedRef.current) setPending(false);
      }
    },
    [action],
  );

  /** Gọi khi người dùng đổi hẳn lựa chọn (đổi ghế, đổi phương thức) */
  const resetKey = useCallback(() => {
    keyRef.current = null;
  }, []);

  return { submit, pending, resetKey };
}
