-- V20260405_01__Add_quarantine_quantity_to_inventory.sql
-- Add quarantine_quantity column to inventory table for QUARANTINE receipt handling

ALTER TABLE inventory
ADD COLUMN quarantine_quantity DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER on_hand_quantity;

