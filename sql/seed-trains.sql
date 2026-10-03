-- ─────────────────────────────────────────────────────────────────────────
-- SEED TRAINS + CARRIAGE LAYOUTS
--
-- These are TEMPLATES, not tied to any date. Real runs (the trip table) and
-- seats (the seat table) are provisioned LAZILY when the first person searches
-- for that date.
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/seed-trains.sql
-- ─────────────────────────────────────────────────────────────────────────

DELETE FROM train_carriage;
DELETE FROM train;

-- Seven trains, differing in departure time and speed
INSERT INTO train (id, code, depart_hour, depart_minute, speed_kmh, status) VALUES
  (1, 'SE1',  20, 25, 60, 1),
  (2, 'SE3',  19, 20, 62, 1),
  (3, 'SE5',   9,  0, 54, 1),
  (4, 'SE7',   6,  0, 52, 1),
  (5, 'SE9',  14, 30, 50, 1),
  (6, 'TN3',  22, 15, 45, 1),
  (7, 'SE22', 11, 45, 58, 1);

-- Carriage layouts.
--   row_count for a seating carriage = number of seat ROWS (4 seats per row)
--   row_count for a sleeper carriage = number of COMPARTMENTS (4-berth -> 4 berths, 6-berth -> 6 berths)
--
-- SE1/SE3/SE5/SE9: 3 seating + 3 six-berth + 2 four-berth carriages = 240 places
-- SE7/TN3        : no four-berth carriages
-- SE22           : no six-berth carriages
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
  -- SE7 — no four-berth carriages
  (4, 1, 'SOFT_SEAT', 'seat-2-2', 16), (4, 2, 'SOFT_SEAT', 'seat-2-2', 16), (4, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (4, 4, 'SOFT_SEAT', 'seat-2-2', 16),
  (4, 5, 'BERTH_6',   'berth-6',   7), (4, 6, 'BERTH_6',   'berth-6',   7), (4, 7, 'BERTH_6',   'berth-6',   7),
  -- SE9
  (5, 1, 'SOFT_SEAT', 'seat-2-2', 16), (5, 2, 'SOFT_SEAT', 'seat-2-2', 16), (5, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (5, 4, 'BERTH_6',   'berth-6',   7), (5, 5, 'BERTH_6',   'berth-6',   7), (5, 6, 'BERTH_6',   'berth-6',   7),
  (5, 7, 'BERTH_4',   'berth-4',   7), (5, 8, 'BERTH_4',   'berth-4',   7),
  -- TN3 — local train, mostly seating
  (6, 1, 'SOFT_SEAT', 'seat-2-2', 16), (6, 2, 'SOFT_SEAT', 'seat-2-2', 16), (6, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (6, 4, 'SOFT_SEAT', 'seat-2-2', 16), (6, 5, 'SOFT_SEAT', 'seat-2-2', 16),
  (6, 6, 'BERTH_6',   'berth-6',   7), (6, 7, 'BERTH_6',   'berth-6',   7),
  -- SE22 — no six-berth carriages
  (7, 1, 'SOFT_SEAT', 'seat-2-2', 16), (7, 2, 'SOFT_SEAT', 'seat-2-2', 16), (7, 3, 'SOFT_SEAT', 'seat-2-2', 16),
  (7, 4, 'BERTH_4',   'berth-4',   7), (7, 5, 'BERTH_4',   'berth-4',   7), (7, 6, 'BERTH_4',   'berth-4',   7);

SELECT CONCAT('train: ', COUNT(*)) AS seeded FROM train
UNION ALL
SELECT CONCAT('train_carriage: ', COUNT(*)) FROM train_carriage;
