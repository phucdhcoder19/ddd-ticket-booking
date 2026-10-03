import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import type { Hold, Passenger, SearchQuery, SeatClassCode, Trip } from "@/api/types";

/**
 * State of one booking, carried across screens 3 → 6.
 *
 * Saved to sessionStorage: if the user presses F5 midway (very common on slow
 * networks), the hold and its countdown survive — the expiry is an absolute
 * moment issued by the server, so reloading the page does not "reset" it.
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

const STORAGE_KEY = "trainbooking.booking.v1";

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
    // A hold that expired before the page was reloaded is dropped
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
      /* private mode blocks storage — ignore, we only lose recovery after F5 */
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
      // Prepare one passenger form per seat just held
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
  if (!ctx) throw new Error("useBooking must be used inside BookingProvider");
  return ctx;
}
