-- ============================================================================
-- Flyway Migration: Seed Inventory Operations Dev Data
-- Version: V20260404_02
-- Description: Seed 5 records per inventory operations table for local dev test
-- ============================================================================

-- Ensure there is at least one account to reference in audit fields.
INSERT INTO accounts (id, username, password, status)
SELECT
    '99000000-0000-0000-0000-000000000900',
    'dev_seed_admin',
    '$2a$10$2FrGg2/7Rtr7lQWRZ9UNV..WQblwoUUgJgOxWYnhP.okeEd3Jo5si',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM accounts);

SET @seed_admin_id := COALESCE(
    (SELECT id FROM accounts WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM accounts WHERE username = 'dev_seed_admin' LIMIT 1),
    (SELECT id FROM accounts ORDER BY created_at LIMIT 1)
);

-- ================================
-- Master data for FK dependencies
-- ================================
INSERT INTO units_of_measure (id, code, name, description, type, created_by, updated_by)
VALUES
    ('91000000-0000-0000-0000-000000000001', 'UOMD001', 'Piece',  'Count by piece',   'COUNT',  @seed_admin_id, @seed_admin_id),
    ('91000000-0000-0000-0000-000000000002', 'UOMD002', 'Box',    'Count by box',     'COUNT',  @seed_admin_id, @seed_admin_id),
    ('91000000-0000-0000-0000-000000000003', 'UOMD003', 'Kg',     'Weight in kg',     'WEIGHT', @seed_admin_id, @seed_admin_id),
    ('91000000-0000-0000-0000-000000000004', 'UOMD004', 'Meter',  'Length in meter',  'LENGTH', @seed_admin_id, @seed_admin_id),
    ('91000000-0000-0000-0000-000000000005', 'UOMD005', 'Liter',  'Volume in liter',  'VOLUME', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO categories (id, code, name, description, status, created_by, updated_by)
VALUES
    ('92000000-0000-0000-0000-000000000001', 'CAT-DEV-001', 'Dev Category 1', 'Dev category seed 1', 'ACTIVE', @seed_admin_id, @seed_admin_id),
    ('92000000-0000-0000-0000-000000000002', 'CAT-DEV-002', 'Dev Category 2', 'Dev category seed 2', 'ACTIVE', @seed_admin_id, @seed_admin_id),
    ('92000000-0000-0000-0000-000000000003', 'CAT-DEV-003', 'Dev Category 3', 'Dev category seed 3', 'ACTIVE', @seed_admin_id, @seed_admin_id),
    ('92000000-0000-0000-0000-000000000004', 'CAT-DEV-004', 'Dev Category 4', 'Dev category seed 4', 'ACTIVE', @seed_admin_id, @seed_admin_id),
    ('92000000-0000-0000-0000-000000000005', 'CAT-DEV-005', 'Dev Category 5', 'Dev category seed 5', 'ACTIVE', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO warehouses (id, code, name, city, country, type, status, capacity, created_by, updated_by)
VALUES
    ('93000000-0000-0000-0000-000000000001', 'WH-DEV-001', 'Dev Warehouse 1', 'Ho Chi Minh', 'VN', 'MAIN',      'ACTIVE', 10000.00, @seed_admin_id, @seed_admin_id),
    ('93000000-0000-0000-0000-000000000002', 'WH-DEV-002', 'Dev Warehouse 2', 'Ha Noi',      'VN', 'SATELLITE', 'ACTIVE',  8000.00, @seed_admin_id, @seed_admin_id),
    ('93000000-0000-0000-0000-000000000003', 'WH-DEV-003', 'Dev Warehouse 3', 'Da Nang',     'VN', 'TRANSIT',   'ACTIVE',  6000.00, @seed_admin_id, @seed_admin_id),
    ('93000000-0000-0000-0000-000000000004', 'WH-DEV-004', 'Dev Warehouse 4', 'Can Tho',     'VN', 'RETURN',    'ACTIVE',  4000.00, @seed_admin_id, @seed_admin_id),
    ('93000000-0000-0000-0000-000000000005', 'WH-DEV-005', 'Dev Warehouse 5', 'Hai Phong',   'VN', 'SATELLITE', 'ACTIVE',  5000.00, @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO locations (id, warehouse_id, code, name, zone, type, capacity, status, notes, created_by, updated_by)
VALUES
    ('94000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', 'LOC-DEV-001', 'Dev Location 1', 'A', 'STORAGE', 1000.00, 'ACTIVE', 'Seed location 1', @seed_admin_id, @seed_admin_id),
    ('94000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', 'LOC-DEV-002', 'Dev Location 2', 'A', 'PICKING',  900.00, 'ACTIVE', 'Seed location 2', @seed_admin_id, @seed_admin_id),
    ('94000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', 'LOC-DEV-003', 'Dev Location 3', 'B', 'PACKING',  800.00, 'ACTIVE', 'Seed location 3', @seed_admin_id, @seed_admin_id),
    ('94000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', 'LOC-DEV-004', 'Dev Location 4', 'B', 'STAGING',  700.00, 'ACTIVE', 'Seed location 4', @seed_admin_id, @seed_admin_id),
    ('94000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', 'LOC-DEV-005', 'Dev Location 5', 'C', 'RETURN',   600.00, 'ACTIVE', 'Seed location 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO products
(
    id, sku, name, description, category_id, uom_id, status,
    min_stock_level, max_stock_level, reorder_point, cost_price, selling_price,
    requires_batch_tracking, created_by, updated_by
)
VALUES
    ('95000000-0000-0000-0000-000000000001', 'SKU-DEV-001', 'Dev Product 1', 'Seed product 1', '92000000-0000-0000-0000-000000000001', '91000000-0000-0000-0000-000000000001', 'ACTIVE',  20.00, 300.00,  50.00, 10.00, 14.00, TRUE,  @seed_admin_id, @seed_admin_id),
    ('95000000-0000-0000-0000-000000000002', 'SKU-DEV-002', 'Dev Product 2', 'Seed product 2', '92000000-0000-0000-0000-000000000002', '91000000-0000-0000-0000-000000000002', 'ACTIVE',  10.00, 250.00,  40.00, 12.00, 18.00, TRUE,  @seed_admin_id, @seed_admin_id),
    ('95000000-0000-0000-0000-000000000003', 'SKU-DEV-003', 'Dev Product 3', 'Seed product 3', '92000000-0000-0000-0000-000000000003', '91000000-0000-0000-0000-000000000003', 'ACTIVE',  15.00, 200.00,  35.00,  8.00, 12.00, TRUE,  @seed_admin_id, @seed_admin_id),
    ('95000000-0000-0000-0000-000000000004', 'SKU-DEV-004', 'Dev Product 4', 'Seed product 4', '92000000-0000-0000-0000-000000000004', '91000000-0000-0000-0000-000000000004', 'ACTIVE',  25.00, 400.00,  60.00, 20.00, 28.00, TRUE,  @seed_admin_id, @seed_admin_id),
    ('95000000-0000-0000-0000-000000000005', 'SKU-DEV-005', 'Dev Product 5', 'Seed product 5', '92000000-0000-0000-0000-000000000005', '91000000-0000-0000-0000-000000000005', 'ACTIVE',  30.00, 500.00,  80.00, 30.00, 42.00, TRUE,  @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO batches
(
    id, batch_number, product_id, manufacturing_date, expiry_date, status,
    supplier_batch_number, notes, created_by, updated_by
)
VALUES
    ('96000000-0000-0000-0000-000000000001', 'BATCH-DEV-001', '95000000-0000-0000-0000-000000000001', '2026-01-01', '2027-01-01', 'AVAILABLE', 'SUP-BATCH-001', 'Seed batch 1', @seed_admin_id, @seed_admin_id),
    ('96000000-0000-0000-0000-000000000002', 'BATCH-DEV-002', '95000000-0000-0000-0000-000000000002', '2026-01-02', '2027-01-02', 'AVAILABLE', 'SUP-BATCH-002', 'Seed batch 2', @seed_admin_id, @seed_admin_id),
    ('96000000-0000-0000-0000-000000000003', 'BATCH-DEV-003', '95000000-0000-0000-0000-000000000003', '2026-01-03', '2027-01-03', 'AVAILABLE', 'SUP-BATCH-003', 'Seed batch 3', @seed_admin_id, @seed_admin_id),
    ('96000000-0000-0000-0000-000000000004', 'BATCH-DEV-004', '95000000-0000-0000-0000-000000000004', '2026-01-04', '2027-01-04', 'AVAILABLE', 'SUP-BATCH-004', 'Seed batch 4', @seed_admin_id, @seed_admin_id),
    ('96000000-0000-0000-0000-000000000005', 'BATCH-DEV-005', '95000000-0000-0000-0000-000000000005', '2026-01-05', '2027-01-05', 'AVAILABLE', 'SUP-BATCH-005', 'Seed batch 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Inventory operations module data
-- 5 records per table
-- ================================
INSERT INTO inventory
(
    id, product_id, warehouse_id, location_id, batch_id,
    on_hand_quantity, reserved_quantity, version, last_movement_at,
    created_by, updated_by
)
VALUES
    ('97000000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001', 110.00, 10.00, 0, NOW(), @seed_admin_id, @seed_admin_id),
    ('97000000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002',  95.00,  5.00, 0, NOW(), @seed_admin_id, @seed_admin_id),
    ('97000000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000003',  60.00, 10.00, 0, NOW(), @seed_admin_id, @seed_admin_id),
    ('97000000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', 170.00, 20.00, 0, NOW(), @seed_admin_id, @seed_admin_id),
    ('97000000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000005', '96000000-0000-0000-0000-000000000005', 180.00, 15.00, 0, NOW(), @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO stock_adjustments
(
    id, adjustment_number, inventory_id, product_id, warehouse_id, location_id, batch_id,
    quantity_before, quantity_after, adjustment_quantity, reason, status,
    notes, requires_approval, approved_by, approved_at, rejection_reason,
    created_by, updated_by
)
VALUES
    ('98000000-0000-0000-0000-000000000001', 'ADJ-DEV-0001', '97000000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001', 110.00, 100.00, -10.00, 'DAMAGE',        'PENDING_APPROVAL', 'Dev pending adjustment 1', TRUE,  NULL,          NULL, NULL,                         @seed_admin_id, @seed_admin_id),
    ('98000000-0000-0000-0000-000000000002', 'ADJ-DEV-0002', '97000000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002',  80.00,  95.00,  15.00, 'COUNT_ERROR',   'APPROVED',         'Dev approved adjustment 2', TRUE,  @seed_admin_id, NOW(), NULL,                         @seed_admin_id, @seed_admin_id),
    ('98000000-0000-0000-0000-000000000003', 'ADJ-DEV-0003', '97000000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000003',  70.00,  60.00, -10.00, 'QUALITY_ISSUE', 'REJECTED',         'Dev rejected adjustment 3', TRUE,  @seed_admin_id, NOW(), 'Rejected for missing proof', @seed_admin_id, @seed_admin_id),
    ('98000000-0000-0000-0000-000000000004', 'ADJ-DEV-0004', '97000000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', 150.00, 170.00,  20.00, 'SYSTEM_ERROR',  'APPROVED',         'Dev auto-approved adj 4',   FALSE, @seed_admin_id, NOW(), NULL,                         @seed_admin_id, @seed_admin_id),
    ('98000000-0000-0000-0000-000000000005', 'ADJ-DEV-0005', '97000000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000005', '96000000-0000-0000-0000-000000000005', 200.00, 180.00, -20.00, 'THEFT',         'PENDING_APPROVAL', 'Dev pending adjustment 5', TRUE,  NULL,          NULL, NULL,                         @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO stock_transfers
(
    id, transfer_number, product_id, warehouse_id, batch_id,
    from_location_id, to_location_id, quantity, reason, notes,
    status, completed_at, created_by, updated_by
)
VALUES
    ('98100000-0000-0000-0000-000000000001', 'TRF-DEV-0001', '95000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002',  5.00, 'REORG',         'Dev draft transfer 1',     'DRAFT',     NULL,  @seed_admin_id, @seed_admin_id),
    ('98100000-0000-0000-0000-000000000002', 'TRF-DEV-0002', '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000002', '94000000-0000-0000-0000-000000000002', '94000000-0000-0000-0000-000000000003',  8.00, 'PICKING_PREP',  'Dev completed transfer 2', 'COMPLETED', NOW(), @seed_admin_id, @seed_admin_id),
    ('98100000-0000-0000-0000-000000000003', 'TRF-DEV-0003', '95000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000003', '94000000-0000-0000-0000-000000000003', '94000000-0000-0000-0000-000000000004',  6.00, 'OVERFLOW',      'Dev cancelled transfer 3', 'CANCELLED', NULL,  @seed_admin_id, @seed_admin_id),
    ('98100000-0000-0000-0000-000000000004', 'TRF-DEV-0004', '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000004', '94000000-0000-0000-0000-000000000004', '94000000-0000-0000-0000-000000000005', 10.00, 'CONSOLIDATION', 'Dev completed transfer 4', 'COMPLETED', NOW(), @seed_admin_id, @seed_admin_id),
    ('98100000-0000-0000-0000-000000000005', 'TRF-DEV-0005', '95000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000005', '94000000-0000-0000-0000-000000000005', '94000000-0000-0000-0000-000000000001',  4.00, 'OTHER',         'Dev draft transfer 5',     'DRAFT',     NULL,  @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO stock_movements
(
    id, movement_type, product_id, warehouse_id, location_id, batch_id,
    quantity_change, quantity_before, quantity_after, movement_date,
    reference_type, reference_id, reference_number, notes, created_by, updated_by
)
VALUES
    ('98200000-0000-0000-0000-000000000001', 'ADJUSTMENT_INCREASE', '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002',  15.00,  80.00,  95.00, NOW(), 'STOCK_ADJUSTMENT', '98000000-0000-0000-0000-000000000002', 'ADJ-DEV-0002', 'Seed movement for approved adjustment', @seed_admin_id, @seed_admin_id),
    ('98200000-0000-0000-0000-000000000002', 'TRANSFER_OUT',         '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002',  -8.00,  95.00,  87.00, NOW(), 'STOCK_TRANSFER',   '98100000-0000-0000-0000-000000000002', 'TRF-DEV-0002', 'Seed movement transfer out',            @seed_admin_id, @seed_admin_id),
    ('98200000-0000-0000-0000-000000000003', 'TRANSFER_IN',          '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000002',   8.00,  30.00,  38.00, NOW(), 'STOCK_TRANSFER',   '98100000-0000-0000-0000-000000000002', 'TRF-DEV-0002', 'Seed movement transfer in',             @seed_admin_id, @seed_admin_id),
    ('98200000-0000-0000-0000-000000000004', 'ADJUSTMENT_INCREASE', '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004',  20.00, 150.00, 170.00, NOW(), 'STOCK_ADJUSTMENT', '98000000-0000-0000-0000-000000000004', 'ADJ-DEV-0004', 'Seed movement for auto-approved adj',   @seed_admin_id, @seed_admin_id),
    ('98200000-0000-0000-0000-000000000005', 'TRANSFER_OUT',         '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', -10.00, 170.00, 160.00, NOW(), 'STOCK_TRANSFER',   '98100000-0000-0000-0000-000000000004', 'TRF-DEV-0004', 'Seed movement transfer out',            @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================================
-- End of Migration
-- ============================================================================
