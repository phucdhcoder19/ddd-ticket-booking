-- ─────────────────────────────────────────────────────────────────────────
-- SEED — dữ liệu mẫu cho train_ticket
--
-- Chạy được nhiều lần, lần sau ghi đè lần trước (idempotent).
-- LƯU Ý: seed đặt lại stock_available = stock_initial, nên chạy lại là
-- kho đầy trở lại — tiện khi test flash sale, nhưng đừng chạy trên thật.
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/seed.sql
--
-- Bảng do Hibernate tạo (ddl-auto: update), nên phải CHẠY APP MỘT LẦN
-- trước khi seed thì bảng station mới tồn tại.
-- ─────────────────────────────────────────────────────────────────────────

-- 1. GA TÀU — 15 ga trên trục Bắc – Nam, display_order theo đúng thứ tự tuyến
DELETE FROM station;
INSERT INTO station (code, name, region, km_from_hanoi, display_order) VALUES
  ('HNO', N'Hà Nội',                 N'Bắc',      0,  1),
  ('NDI', N'Nam Định',               N'Bắc',     87,  2),
  ('THA', N'Thanh Hoá',              N'Bắc',    175,  3),
  ('VIN', N'Vinh',                   N'Trung',  319,  4),
  ('DHO', N'Đồng Hới',               N'Trung',  522,  5),
  ('HUE', N'Huế',                    N'Trung',  688,  6),
  ('DNA', N'Đà Nẵng',                N'Trung',  791,  7),
  ('QNG', N'Quảng Ngãi',             N'Trung',  928,  8),
  ('DTH', N'Diêu Trì (Quy Nhơn)',    N'Trung', 1096,  9),
  ('TUY', N'Tuy Hoà',                N'Trung', 1198, 10),
  ('NTR', N'Nha Trang',              N'Trung', 1315, 11),
  ('THC', N'Tháp Chàm (Phan Rang)',  N'Nam',   1408, 12),
  ('BTH', N'Bình Thuận (Phan Thiết)',N'Nam',   1551, 13),
  ('BHO', N'Biên Hoà',               N'Nam',   1675, 14),
  ('SGO', N'Sài Gòn',                N'Nam',   1726, 15);

-- 2. VÉ — bốn trạng thái để thử cổng ⓪ của placeOrder()
--
--   id=1  ĐANG MỞ BÁN      -> mua được
--   id=2  CHƯA TỚI GIỜ MỞ  -> NOT_ON_SALE, và là mốc cho đồng hồ đếm ngược
--   id=3  ĐÃ HẾT GIỜ BÁN   -> SALE_ENDED
--   id=4  status = 0       -> NOT_ON_SALE (vé chưa kích hoạt)
INSERT INTO ticket_item
  (id, name, description, stock_initial, stock_available,
   price_original, price_flash, sale_start_time, sale_end_time,
   status, activity_id, created_at, updated_at)
VALUES
  (1, N'Tàu SE1 · Hà Nội → Sài Gòn', N'Ngồi mềm điều hoà, toa 3',
   1000, 1000, 1150000.00, 899000.00, '2026-09-01 08:00:00', '2027-02-10 23:59:59',
   1, 1, NOW(), NOW()),

  (2, N'Tàu SE7 · Hà Nội → Đà Nẵng', N'Nằm khoang 6, mở bán đợt Tết',
   800, 800, 920000.00, 736000.00, '2026-11-01 08:00:00', '2027-02-10 23:59:59',
   1, 1, NOW(), NOW()),

  (3, N'Tàu SE21 · Sài Gòn → Huế', N'Nằm khoang 4 — đợt hè, đã đóng',
   500, 500, 960000.00, 768000.00, '2026-06-01 08:00:00', '2026-09-10 23:59:59',
   1, 2, NOW(), NOW()),

  (4, N'Tàu TN3 · Sài Gòn → Nha Trang', N'Chưa kích hoạt, đang soạn giá',
   600, 600, 560000.00, 437000.00, '2026-09-01 08:00:00', '2027-02-10 23:59:59',
   0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name             = VALUES(name),
  description      = VALUES(description),
  stock_initial    = VALUES(stock_initial),
  stock_available  = VALUES(stock_initial),   -- nạp lại kho đầy
  price_original   = VALUES(price_original),
  price_flash      = VALUES(price_flash),
  sale_start_time  = VALUES(sale_start_time),
  sale_end_time    = VALUES(sale_end_time),
  status           = VALUES(status),
  activity_id      = VALUES(activity_id),
  updated_at       = NOW();

SELECT CONCAT('station: ', COUNT(*)) AS seeded FROM station
UNION ALL
SELECT CONCAT('ticket_item: ', COUNT(*)) FROM ticket_item;
