-- Migration to make location_id nullable in outbound_shipment_lines
-- and add to_location_id to stock_movements for better internal move tracking

ALTER TABLE outbound_shipment_lines MODIFY location_id CHAR(36) NULL;
ALTER TABLE outbound_shipment_lines MODIFY batch_id CHAR(36) NULL;

-- Add to_location_id to stock_movements if not exists
-- Using a procedure to safely add column if it doesn't exist (standard for MySQL migrations)
DELIMITER //
CREATE PROCEDURE AddToLocationIdColumn()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM information_schema.columns 
        WHERE table_name = 'stock_movements' AND column_name = 'to_location_id'
    ) THEN
        ALTER TABLE stock_movements ADD COLUMN to_location_id CHAR(36) AFTER location_id;
    END IF;
END //
DELIMITER ;
CALL AddToLocationIdColumn();
DROP PROCEDURE AddToLocationIdColumn;
