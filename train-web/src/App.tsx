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

/** Đường dẫn tiếng Việt, dễ đọc và dễ chia sẻ cho nhau qua tin nhắn */
export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <BookingProvider>
          <OfflineBanner />
          <Routes>
            <Route element={<AppLayout />}>
              <Route index element={<HomePage />} />
              <Route path="phong-cho" element={<WaitingRoomPage />} />
              <Route path="chuyen-tau" element={<TripListPage />} />
              <Route path="chon-cho/:tripId" element={<SeatSelectionPage />} />
              <Route path="thong-tin-hanh-khach" element={<PassengerInfoPage />} />
              <Route path="thanh-toan" element={<PaymentPage />} />
              <Route path="ve-cua-toi" element={<MyTicketsPage />} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </BookingProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}
