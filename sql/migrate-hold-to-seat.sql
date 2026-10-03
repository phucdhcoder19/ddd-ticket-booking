-- ─────────────────────────────────────────────────────────────────────────
-- MOVE HOLDS FROM "COUNT BY QUANTITY" TO "PER SEAT"
--
--   docker exec -i pre-event-mysql mysql -uroot -proot1234 train_ticket < sql/migrate-hold-to-seat.sql
--
-- Only needed on a database that ALREADY EXISTED. Skip this file on a fresh
-- machine: ddl-auto: update creates the new structure correctly by itself.
--
-- Why it must be run by hand: ddl-auto: update only knows how to ADD columns;
-- it never drops old columns or relaxes constraints. The old ticket_hold table
-- has ticket_id NOT NULL, which the new version no longer writes -> every hold
-- would fail on INSERT with "Field 'ticket_id' doesn't have a default value".
-- ─────────────────────────────────────────────────────────────────────────

-- Drop the whole table instead of ALTERing column by column: holds are data
-- that LIVES 10 MINUTES. Nothing in there is worth keeping, and old holds point
-- at the old stock model, so they are meaningless anyway.
DROP TABLE IF EXISTS ticket_hold;

-- Orders must NOT be deleted — they are payment records.
-- Only relax ticket_id to NULL so per-seat orders can be written.
-- (Ignore the error if the table does not exist: a fresh machine that never ran the app.)
ALTER TABLE ticket_order MODIFY ticket_id BIGINT NULL;
