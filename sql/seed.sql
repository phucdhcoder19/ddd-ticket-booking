-- ─────────────────────────────────────────────────────────────────────────
-- SEED — sample data for train_ticket
--
-- Safe to run repeatedly; each run overwrites the previous one (idempotent).
-- NOTE: the seed resets stock_available = stock_initial, so running it again
-- refills the stock — handy for flash sale tests, but never run it on real data.
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/seed.sql
--
-- Tables are created by Hibernate (ddl-auto: update), so RUN THE APP ONCE
-- before seeding, otherwise the station table does not exist yet.
-- ─────────────────────────────────────────────────────────────────────────

-- 1. STATIONS — 15 stations on the North–South line, display_order follows the route
DELETE FROM station;
INSERT INTO station (code, name, region, km_from_hanoi, display_order) VALUES
  ('HNO', 'Hanoi',                    'North',      0,  1),
  ('NDI', 'Nam Dinh',                 'North',     87,  2),
  ('THA', 'Thanh Hoa',                'North',    175,  3),
  ('VIN', 'Vinh',                     'Central',  319,  4),
  ('DHO', 'Dong Hoi',                 'Central',  522,  5),
  ('HUE', 'Hue',                      'Central',  688,  6),
  ('DNA', 'Da Nang',                  'Central',  791,  7),
  ('QNG', 'Quang Ngai',               'Central',  928,  8),
  ('DTH', 'Dieu Tri (Quy Nhon)',      'Central', 1096,  9),
  ('TUY', 'Tuy Hoa',                  'Central', 1198, 10),
  ('NTR', 'Nha Trang',                'Central', 1315, 11),
  ('THC', 'Thap Cham (Phan Rang)',    'South',   1408, 12),
  ('BTH', 'Binh Thuan (Phan Thiet)',  'South',   1551, 13),
  ('BHO', 'Bien Hoa',                 'South',   1675, 14),
  ('SGO', 'Saigon',                   'South',   1726, 15);

-- 2. TICKETS — four states to exercise gate ⓪ of placeOrder()
--
--   id=1  ON SALE          -> can be bought
--   id=2  NOT OPEN YET     -> NOT_ON_SALE, and the target of the countdown
--   id=3  SALE ENDED       -> SALE_ENDED
--   id=4  status = 0       -> NOT_ON_SALE (ticket not activated)
INSERT INTO ticket_item
  (id, name, description, stock_initial, stock_available,
   price_original, price_flash, sale_start_time, sale_end_time,
   status, activity_id, created_at, updated_at)
VALUES
  (1, 'Train SE1 · Hanoi → Saigon', 'Air-conditioned soft seat, carriage 3',
   1000, 1000, 1150000.00, 899000.00, '2026-09-01 08:00:00', '2027-02-10 23:59:59',
   1, 1, NOW(), NOW()),

  (2, 'Train SE7 · Hanoi → Da Nang', '6-berth sleeper, Lunar New Year sale',
   800, 800, 920000.00, 736000.00, '2026-11-01 08:00:00', '2027-02-10 23:59:59',
   1, 1, NOW(), NOW()),

  (3, 'Train SE21 · Saigon → Hue', '4-berth sleeper — summer sale, closed',
   500, 500, 960000.00, 768000.00, '2026-06-01 08:00:00', '2026-09-10 23:59:59',
   1, 2, NOW(), NOW()),

  (4, 'Train TN3 · Saigon → Nha Trang', 'Not activated, prices being prepared',
   600, 600, 560000.00, 437000.00, '2026-09-01 08:00:00', '2027-02-10 23:59:59',
   0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name             = VALUES(name),
  description      = VALUES(description),
  stock_initial    = VALUES(stock_initial),
  stock_available  = VALUES(stock_initial),   -- refill the stock
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
