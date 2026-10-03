# traintix — Lunar New Year train booking UI

React 19 + TypeScript + Tailwind CSS 4 + Vite. Mobile-first. Runs on **mock data** or on the real backend.

```bash
npm install
npm run dev      # http://localhost:5180
npm run build
```

## Connecting to the real backend

Every network call sits behind a single file: `src/api/client.ts`. Each function has two
branches — mock and the real `request()` — with the same signature. Change `.env`:

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=http://localhost:9999/api
```

No file in `src/pages` or `src/components` knows about `fetch`.

### API contract the UI expects

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/stations` | |
| GET | `/sale-window` | Returns `opensAt`, `serverNow` |
| GET | `/trips?from&to&date&passengers` | |
| GET | `/trips/{id}/carriages?seatClass=` | |
| POST | `/holds` | `{tripId, seatClass, seatIds}` → `Hold`. **Requires `Idempotency-Key`** and `X-Queue-Token` |
| GET / DELETE | `/holds/{id}` | |
| PUT | `/holds/{id}/passengers` | |
| POST | `/orders` | `{holdId, paymentMethod}`. **Requires `Idempotency-Key`** |
| GET | `/orders/{id}` | The UI polls every 2 seconds |
| GET | `/me/tickets` · POST `/tickets/{id}/refund` | |
| POST | `/queue` · GET `/queue/{token}` | Waiting room |

**Error codes the UI relies on** (see `src/lib/http.ts`):

- `409` + body `{"code":"SEAT_TAKEN", "details":{"takenSeatIds":[], "suggestedSeatIds":[]}}`
  → deselect exactly the lost seats and highlight the suggested ones. A `409` without `code` means sold out.
- `410` → the hold expired, show the dialog that forces picking seats again.
- `403` → not admitted by the waiting room (or the admission expired), go back to the waiting room.
- `429` (+ `Retry-After` if present) → automatic exponential backoff with jitter, with a
  "retrying" banner for the user. `5xx` and timeouts are retried too; other `4xx` are not.

The server **must** honour the `Idempotency-Key` header: the UI sends the same key on every
retry of the same action, and only changes the key when the user starts a new action.

## Structure

```
src/
├─ lib/         format (VND, dd/MM/yyyy) · lunar (lunar calendar, New Year peak) · http (retry/backoff) · validate · queuePass
├─ api/         types · errors (ApiError + user-facing messages) · client · mock/
├─ hooks/       useAsync (loading/empty/error/success) · useCountdown · useSubmitLock
├─ store/       BookingContext (sessionStorage, survives F5) · ToastContext
├─ components/  SeatMap · TrainCard · TicketCard · QueueStatus · CountdownTimer · HoldBar
│               LunarDatePicker · PassengerForm · OfflineBanner · ui/
└─ pages/       7 screens
```

## Trying failure scenarios

The mock server takes commands from the browser console:

```js
__mock.overloadRate = 0.9    // force 429s to see the auto-retry banner
__mock.seatStealRate = 1     // seats are always snatched when holding
__mock.paymentFailRate = 1   // payment always fails
__mock.timeoutRate = 0.5     // hanging network
__mock.holdSeconds = 20      // shrink the hold timer to 20 seconds
__mock.queueEnabled = false  // skip the waiting room
```

## Design tokens

Defined in `src/index.css` under `@theme`, used through Tailwind classes:

| Token | Value | Used for |
| --- | --- | --- |
| `brand-500` | `#0194f3` | Brand blue, background of large buttons |
| `brand-600` | `#0d73c8` | Primary buttons, links, blue text on white (4.6:1) |
| `accent-500` | `#ff5e1f` | Search button, price badges |
| `accent-600` | `#e8490b` | Price text on white (4.5:1) |
| `ink-900` | `#1a1a1a` | Main text |
| `canvas` | `#f5f7fa` | Page background |

Touch targets: `md` buttons 48px, `lg` 56px, seat cells 44px. Font Plus Jakarta Sans, base size 16px.
