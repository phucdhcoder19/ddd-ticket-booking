-- ─────────────────────────────────────────────────────────────────────────
-- SEED ĐOÀN TÀU + SƠ ĐỒ TOA
--
-- Đây là KHUÔN MẪU, không gắn với ngày nào. Chuyến chạy thật (bảng trip)
-- và ghế (bảng seat) được sinh LƯỜI khi có người đầu tiên tìm tới ngày đó.
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/seed-trains.sql
-- ─────────────────────────────────────────────────────────────────────────

DELETE FROM train_carriage;
DELETE FROM train;

-- Bảy đoàn tàu, khác nhau ở giờ chạy và tốc độ
INSERT INTO train (id, code, depart_hour, depart_minute, speed_kmh, status) VALUES
  (1, 'SE1',  20, 25, 60, 1),
  (2, 'SE3',  19, 20, 62, 1),
  (3, 'SE5',   9,  0, 54, 1),
  (4, 'SE7',   6,  0, 52, 1),
  (5, 'SE9',  14, 30, 50, 1),
  (6, 'TN3',  22, 15, 45, 1),
  (7, 'SE22', 11, 45, 58, 1);

-- Sơ đồ toa.
--   row_count với toa ngồi  = số HÀNG ghế  (mỗi hàng 4 chỗ)
--   row_count với toa nằm   = số KHOANG    (khoang 4 -> 4 giường, khoang 6 -> 6 giường)
--
-- SE1/SE3/SE5/SE9: 3 toa ngồi + 3 toa khoang 6 + 2 toa khoang 4 = 240 chỗ
-- SE7/TN3        : không có khoang 4
-- SE22           : không có khoang 6
INSERT INTO train_carriage (train_id, number, seat_class, layout, row_count) VALUES
  -- SE1
  (1, 1, 'SOFT_SEAT', 'seat-2-2', 16), (1, 2, 'SOFT_SEAT', 'seat-2-2', 16), (1, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (1, 4, 'BERTH_6',   'berth-6',   7), (1, 5, 'BERTH_6',   'berth-6',   7), (1, 6, 'BERTH_6',   'berth-6',   7),
  (1, 7, 'BERTH_4',   'berth-4',   7), (1, 8, 'BERTH_4',   'berth-4',   7),
  -- SE3
  (2, 1, 'SOFT_SEAT', 'seat-2-2', 16), (2, 2, 'SOFT_SEAT', 'seat-2-2', 16), (2, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (2, 4, 'BERTH_6',   'berth-6',   7), (2, 5, 'BERTH_6',   'berth-6',   7), (2, 6, 'BERTH_6',   'berth-6',   7),
  (2, 7, 'BERTH_4',   'berth-4',   7), (2, 8, 'BERTH_4',   'berth-4',   7),
  -- SE5
  (3, 1, 'SOFT_SEAT', 'seat-2-2', 16), (3, 2, 'SOFT_SEAT', 'seat-2-2', 16), (3, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (3, 4, 'BERTH_6',   'berth-6',   7), (3, 5, 'BERTH_6',   'berth-6',   7), (3, 6, 'BERTH_6',   'berth-6',   7),
  (3, 7, 'BERTH_4',   'berth-4',   7), (3, 8, 'BERTH_4',   'berth-4',   7),
  -- SE7 — không có khoang 4
  (4, 1, 'SOFT_SEAT', 'seat-2-2', 16), (4, 2, 'SOFT_SEAT', 'seat-2-2', 16), (4, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (4, 4, 'SOFT_SEAT', 'seat-2-2', 16),
  (4, 5, 'BERTH_6',   'berth-6',   7), (4, 6, 'BERTH_6',   'berth-6',   7), (4, 7, 'BERTH_6',   'berth-6',   7),
  -- SE9
  (5, 1, 'SOFT_SEAT', 'seat-2-2', 16), (5, 2, 'SOFT_SEAT', 'seat-2-2', 16), (5, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (5, 4, 'BERTH_6',   'berth-6',   7), (5, 5, 'BERTH_6',   'berth-6',   7), (5, 6, 'BERTH_6',   'berth-6',   7),
  (5, 7, 'BERTH_4',   'berth-4',   7), (5, 8, 'BERTH_4',   'berth-4',   7),
  -- TN3 — tàu chợ, nhiều ghế ngồi
  (6, 1, 'SOFT_SEAT', 'seat-2-2', 16), (6, 2, 'SOFT_SEAT', 'seat-2-2', 16), (6, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (6, 4, 'SOFT_SEAT', 'seat-2-2', 16), (6, 5, 'SOFT_SEAT', 'seat-2-2', 16),
  (6, 6, 'BERTH_6',   'berth-6',   7), (6, 7, 'BERTH_6',   'berth-6',   7),
  -- SE22 — không có khoang 6
  (7, 1, 'SOFT_SEAT', 'seat-2-2', 16), (7, 2, 'SOFT_SEAT', 'seat-2-2', 16), (7, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (7, 4, 'BERTH_4',   'berth-4',   7), (7, 5, 'BERTH_4',   'berth-4',   7), (7, 6, 'BERTH_4',   'berth-4',   7);

SELECT CONCAT('train: ', COUNT(*)) AS seeded FROM train
UNION ALL
SELECT CONCAT('train_carriage: ', COUNT(*)) FROM train_carriage;
