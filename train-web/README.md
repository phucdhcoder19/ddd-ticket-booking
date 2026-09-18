# Vé Tàu Tết — Giao diện đặt vé tàu dịp Tết

React 19 + TypeScript + Tailwind CSS 4 + Vite. Mobile-first, tiếng Việt, đang chạy trên **mock data**.

```bash
npm install
npm run dev      # http://localhost:5180
npm run build
```

## Nối với backend thật

Toàn bộ lời gọi mạng nằm sau một file duy nhất: `src/api/client.ts`. Mỗi hàm có hai
nhánh — mock và `request()` thật — với cùng chữ ký. Đổi `.env`:

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

Không có file nào trong `src/pages` hay `src/components` biết đến `fetch`.

### Hợp đồng API mà giao diện đang trông đợi

| Method | Đường dẫn | Ghi chú |
| --- | --- | --- |
| GET | `/stations` | |
| GET | `/sale-window` | Trả `opensAt`, `serverNow` |
| GET | `/trips?from&to&date&passengers` | |
| GET | `/trips/{id}/carriages?seatClass=` | |
| POST | `/holds` | `{tripId, seatClass, seatIds}` → `Hold`. **Cần `Idempotency-Key`** |
| GET / DELETE | `/holds/{id}` | |
| PUT | `/holds/{id}/passengers` | |
| POST | `/orders` | `{holdId, paymentMethod}`. **Cần `Idempotency-Key`** |
| GET | `/orders/{id}` | Giao diện poll 2 giây/lần |
| GET | `/me/tickets` · POST `/tickets/{id}/refund` | |
| POST | `/queue` · GET `/queue/{token}` | Phòng chờ |

**Mã lỗi giao diện dựa vào** (xem `src/lib/http.ts`):

- `409` + body `{"code":"SEAT_TAKEN", "details":{"takenSeatIds":[], "suggestedSeatIds":[]}}`
  → bỏ chọn đúng ghế đã mất, làm nổi ghế gợi ý. `409` không có `code` được hiểu là hết vé.
- `410` → hết hạn giữ chỗ, hiện hộp thoại bắt buộc chọn lại chỗ.
- `429` (+ `Retry-After` nếu có) → tự retry backoff luỹ thừa có jitter, hiện dải băng
  "đang thử lại" cho người dùng. `5xx` và timeout cũng retry; `4xx` khác thì không.

Server **phải** tôn trọng header `Idempotency-Key`: giao diện gửi cùng một key qua mọi
lần retry của cùng một thao tác, và chỉ đổi key khi người dùng bắt đầu thao tác mới.

## Cấu trúc

```
src/
├─ lib/         format (VND, dd/MM/yyyy) · lunar (âm lịch, cao điểm Tết) · http (retry/backoff) · validate
├─ api/         types · errors (ApiError + câu chữ tiếng Việt) · client · mock/
├─ hooks/       useAsync (loading/empty/error/success) · useCountdown · useSubmitLock
├─ store/       BookingContext (sessionStorage, sống sót F5) · ToastContext
├─ components/  SeatMap · TrainCard · TicketCard · QueueStatus · CountdownTimer · HoldBar
│               LunarDatePicker · PassengerForm · OfflineBanner · ui/
└─ pages/       7 màn hình
```

## Thử các tình huống hỏng

Mock server nhận lệnh từ console trình duyệt:

```js
__mock.overloadRate = 0.9    // ép 429 để xem dải băng tự retry
__mock.seatStealRate = 1     // ghế luôn bị giật mất khi bấm giữ chỗ
__mock.paymentFailRate = 1   // thanh toán luôn thất bại
__mock.timeoutRate = 0.5     // mạng treo
__mock.holdSeconds = 20      // rút đồng hồ giữ chỗ xuống 20 giây
__mock.queueEnabled = false  // bỏ qua phòng chờ
```

## Design tokens

Định nghĩa trong `src/index.css` dưới `@theme`, dùng qua class Tailwind:

| Token | Giá trị | Dùng cho |
| --- | --- | --- |
| `son-600` | `#c41e28` | Nút chính, nhấn mạnh (6.4:1 với chữ trắng) |
| `son-700` | `#a41722` | Header, hover |
| `mai-400` | `#d99a2b` | Vàng ấm — viền, badge, họa tiết. Không làm nền chữ trắng |
| `ink-900` | `#1c1917` | Chữ chính |
| `canvas` | `#fdfbf7` | Nền trang |

Chiều cao chạm: nút `md` 48px, `lg` 56px, ô ghế 44px. Font Be Vietnam Pro, cỡ gốc 16px.
