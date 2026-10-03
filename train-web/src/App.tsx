import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "@/components/AppLayout";
import { BookingProvider } from "@/store/BookingContext";
import { ToastProvider } from "@/store/ToastContext";
import { HomePage } from "@/pages/HomePage";
import { WaitingRoomPage } from "@/pages/WaitingRoomPage";
import { TripListPage } from "@/pages/TripListPage";
import { SeatSelectionPage } from "@/pages/SeatSelectionPage";
import { PassengerInfoPage } from "@/pages/PassengerInfoPage";
import { PaymentPage } from "@/pages/PaymentPage";
import { MyTicketsPage } from "@/pages/MyTicketsPage";
import { OfflineBanner } from "@/components/OfflineBanner";

/** Readable paths that are easy to share with each other in a message */
export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <BookingProvider>
          <OfflineBanner />
          <Routes>
            <Route element={<AppLayout />}>
              <Route index element={<HomePage />} />
              <Route path="waiting-room" element={<WaitingRoomPage />} />
              <Route path="trips" element={<TripListPage />} />
              <Route path="seats/:tripId" element={<SeatSelectionPage />} />
              <Route path="passengers" element={<PassengerInfoPage />} />
              <Route path="payment" element={<PaymentPage />} />
              <Route path="my-tickets" element={<MyTicketsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </BookingProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
