-- ============================================================================
-- Flyway Migration: Remove Request Key from Inventory Reservation
-- Version: V20260319_02
-- Description: Remove unused request_key column to simplify reservation logic
-- Author: Your Name
-- Date: 2026-03-19
--
-- Rationale:
-- - request_key is no longer used for idempotency
-- - system now relies on order_line_id as unique business key
-- - removing this column reduces complexity and avoids confusion
--
-- Rollback note:
-- - Re-add column if needed:
--     ALTER TABLE inventory_reservations ADD request_key VARCHAR(100);
-- - Recreate index/constraint if it previously existed
-- ============================================================================

ALTER TABLE inventory_reservations
DROP COLUMN request_key;