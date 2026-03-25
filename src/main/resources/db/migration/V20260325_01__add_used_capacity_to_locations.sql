-- Add used_capacity column to track current number of items in each location
ALTER TABLE locations
    ADD COLUMN used_capacity INT NOT NULL DEFAULT 0
    COMMENT 'Current number of items stored in this location'
AFTER capacity;

-- Initialize used_capacity based on existing ACTIVE inventory
-- This ensures data consistency at the time of migration
UPDATE locations l
    LEFT JOIN (
    SELECT
    location_id,
    SUM(quantity) AS total_quantity
    FROM inventory
    WHERE status = 'ACTIVE' -- only count inventory that occupies space
    GROUP BY location_id
    ) inv ON l.id = inv.location_id
    SET l.used_capacity = COALESCE(inv.total_quantity, 0);