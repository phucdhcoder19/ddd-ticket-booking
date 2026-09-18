import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { Hold, Passenger, SearchQuery, SeatClassCode, Trip } from "@/api/types";

/**
 * Trạng thái của một lượt đặt vé, đi xuyên các màn 3 → 6.
 *
 * Lưu vào sessionStorage: người dùng lỡ F5 giữa chừng (rất hay xảy ra khi
 * mạng chậm) thì vẫn còn phiên giữ chỗ và đồng hồ đếm ngược — mốc hết hạn là
 * thời điểm tuyệt đối do server cấp nên không bị "làm mới" khi tải lại trang.
 */
export type BookingState = {
  query: SearchQuery | null;
  trip: Trip | null;
  seatClass: SeatClassCode | null;
  hold: Hold | null;
  passengers: Passenger[];
  orderId: string | null;
};

const EMPTY: BookingState = {
  query: null, trip: null, seatClass: null, hold: null, passengers: [], orderId: null,
};

const STORAGE_KEY = "vetau.booking.v1";

type BookingApi = BookingState & {
  setQuery: (q: SearchQuery) => void;
  startSelection: (trip: Trip, seatClass: SeatClassCode) => void;
  setHold: (hold: Hold) => void;
  setPassengers: (p: Passenger[]) => void;
  setOrderId: (id: string | null) => void;
  clearHold: () => void;
  reset: () => void;
  totalAmount: number;
};

const Ctx = createContext<BookingApi | null>(null);

function load(): BookingState {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return EMPTY;
    const parsed = JSON.parse(raw) as BookingState;
    // Phiên giữ chỗ đã hết hạn từ trước khi tải lại trang thì bỏ luôn
    if (parsed.hold && new Date(parsed.hold.expiresAt).getTime() < Date.now()) {
      return { ...parsed, hold: null, passengers: [], orderId: null };
    }
    return { ...EMPTY, ...parsed };
  } catch {
    return EMPTY;
  }
}

export function BookingProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<BookingState>(load);

  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      /* chế độ riêng tư chặn storage — bỏ qua, chỉ mất khả năng khôi phục sau F5 */
    }
  }, [state]);

  const setQuery = useCallback((query: SearchQuery) => setState((s) => ({ ...s, query })), []);

  const startSelection = useCallback(
    (trip: Trip, seatClass: SeatClassCode) =>
      setState((s) => ({ ...s, trip, seatClass, hold: null, passengers: [], orderId: null })),
    [],
  );

  const setHold = useCallback((hold: Hold) => {
    setState((s) => ({
      ...s,
      hold,
      // Dựng sẵn khung thông tin hành khách theo đúng số chỗ vừa giữ
      passengers: hold.items.map(
        (item, i) =>
          s.passengers[i] ?? {
            seatId: item.seatId,
            fullName: "",
            idNumber: "",
            phone: "",
            discount: "NONE" as const,
          },
      ),
    }));
  }, []);

  const setPassengers = useCallback((passengers: Passenger[]) => setState((s) => ({ ...s, passengers })), []);
  const setOrderId = useCallback((orderId: string | null) => setState((s) => ({ ...s, orderId })), []);
  const clearHold = useCallback(
    () => setState((s) => ({ ...s, hold: null, passengers: [], orderId: null })),
    [],
  );
  const reset = useCallback(() => setState(EMPTY), []);

  const totalAmount = useMemo(
    () => state.hold?.items.reduce((sum, i) => sum + i.price, 0) ?? 0,
    [state.hold],
  );

  const value = useMemo<BookingApi>(
    () => ({ ...state, setQuery, startSelection, setHold, setPassengers, setOrderId, clearHold, reset, totalAmount }),
    [state, setQuery, startSelection, setHold, setPassengers, setOrderId, clearHold, reset, totalAmount],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useBooking() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error("useBooking phải nằm trong BookingProvider");
  return ctx;
}
