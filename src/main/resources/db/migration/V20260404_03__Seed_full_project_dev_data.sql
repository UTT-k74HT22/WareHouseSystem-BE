-- ============================================================================
-- Flyway Migration: Seed Full Project Dev Data
-- Version: V20260404_03
-- Description: Seed remaining project tables for local development/testing
-- ============================================================================

SET @seed_password := '$2a$10$2FrGg2/7Rtr7lQWRZ9UNV..WQblwoUUgJgOxWYnhP.okeEd3Jo5si';

-- Ensure USER role exists (ADMIN is created in earlier migrations).
INSERT INTO roles (id, code, name, description)
SELECT
    'A1000000-0000-0000-0000-000000000001',
    'ROLE_USER',
    'USER',
    'Standard user role'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'USER'
);

SET @role_admin_id := (SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1);
SET @role_user_id  := (SELECT id FROM roles WHERE name = 'USER' LIMIT 1);

SET @seed_admin_id := COALESCE(
    (SELECT id FROM accounts WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM accounts WHERE username = 'dev_seed_admin' LIMIT 1),
    (SELECT id FROM accounts ORDER BY created_at LIMIT 1)
);

-- ================================
-- Accounts + RBAC
-- ================================
INSERT INTO accounts (id, username, password, status)
VALUES
    ('A2000000-0000-0000-0000-000000000001', 'emp.dev01', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000002', 'emp.dev02', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000003', 'emp.dev03', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000004', 'emp.dev04', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000005', 'emp.dev05', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000006', 'cust.dev01', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000007', 'cust.dev02', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000008', 'cust.dev03', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000009', 'cust.dev04', @seed_password, 'ACTIVE'),
    ('A2000000-0000-0000-0000-000000000010', 'cust.dev05', @seed_password, 'ACTIVE')
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO permissions (id, code, name, description)
VALUES
    ('A3000000-0000-0000-0000-000000000001', 'PERM_INV_READ',       'Inventory Read',      'Can read inventory data'),
    ('A3000000-0000-0000-0000-000000000002', 'PERM_INV_WRITE',      'Inventory Write',     'Can update inventory data'),
    ('A3000000-0000-0000-0000-000000000003', 'PERM_ORDER_READ',     'Order Read',          'Can read order data'),
    ('A3000000-0000-0000-0000-000000000004', 'PERM_ORDER_WRITE',    'Order Write',         'Can update order data'),
    ('A3000000-0000-0000-0000-000000000005', 'PERM_REPORT_EXPORT',  'Report Export',       'Can export reports')
ON DUPLICATE KEY UPDATE id = id;

INSERT IGNORE INTO account_roles (account_id, role_id)
SELECT id, @role_user_id
FROM accounts
WHERE username IN (
    'emp.dev01', 'emp.dev02', 'emp.dev03', 'emp.dev04', 'emp.dev05',
    'cust.dev01', 'cust.dev02', 'cust.dev03', 'cust.dev04', 'cust.dev05'
);

INSERT IGNORE INTO account_roles (account_id, role_id)
SELECT @seed_admin_id, @role_admin_id
WHERE @seed_admin_id IS NOT NULL AND @role_admin_id IS NOT NULL;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT @role_admin_id, p.id
FROM permissions p
WHERE @role_admin_id IS NOT NULL;

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT @role_user_id, p.id
FROM permissions p
WHERE @role_user_id IS NOT NULL
  AND p.code IN ('PERM_INV_READ', 'PERM_ORDER_READ', 'PERM_REPORT_EXPORT');

-- ================================
-- User Profiles
-- ================================
INSERT INTO user_profiles
(
    id, account_id, first_name, last_name, email, phone_number, address, date_of_birth,
    created_by, updated_by
)
VALUES
    ('A4000000-0000-0000-0000-000000000001', 'A2000000-0000-0000-0000-000000000001', 'Emp',  'Dev01',  'emp.dev01@whs.local',  '0901000001', 'HCM City', '1994-01-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000002', 'A2000000-0000-0000-0000-000000000002', 'Emp',  'Dev02',  'emp.dev02@whs.local',  '0901000002', 'HCM City', '1994-02-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000003', 'A2000000-0000-0000-0000-000000000003', 'Emp',  'Dev03',  'emp.dev03@whs.local',  '0901000003', 'HCM City', '1994-03-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000004', 'A2000000-0000-0000-0000-000000000004', 'Emp',  'Dev04',  'emp.dev04@whs.local',  '0901000004', 'HCM City', '1994-04-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000005', 'A2000000-0000-0000-0000-000000000005', 'Emp',  'Dev05',  'emp.dev05@whs.local',  '0901000005', 'HCM City', '1994-05-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000006', 'A2000000-0000-0000-0000-000000000006', 'Cust', 'Dev01',  'cust.dev01@whs.local', '0902000001', 'Ha Noi',   '1996-01-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000007', 'A2000000-0000-0000-0000-000000000007', 'Cust', 'Dev02',  'cust.dev02@whs.local', '0902000002', 'Da Nang',  '1996-02-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000008', 'A2000000-0000-0000-0000-000000000008', 'Cust', 'Dev03',  'cust.dev03@whs.local', '0902000003', 'Can Tho',  '1996-03-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000009', 'A2000000-0000-0000-0000-000000000009', 'Cust', 'Dev04',  'cust.dev04@whs.local', '0902000004', 'Hue',      '1996-04-01', @seed_admin_id, @seed_admin_id),
    ('A4000000-0000-0000-0000-000000000010', 'A2000000-0000-0000-0000-000000000010', 'Cust', 'Dev05',  'cust.dev05@whs.local', '0902000005', 'Hai Phong','1996-05-01', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Business Partners
-- ================================
INSERT INTO business_partners
(
    id, code, name, type, contact_person, email, phone, address, city, country,
    tax_id, payment_terms, credit_limit, status, notes, created_by, updated_by
)
VALUES
    ('A5000000-0000-0000-0000-000000000001', 'BP-DEV-001', 'Dev Supplier 1', 'SUPPLIER', 'Nguyen A', 'supplier1@dev.local', '0911000001', 'Address 1', 'HCM City', 'VN', 'TAX-SUP-001', 'NET 30',  50000.00, 'ACTIVE', 'Seed supplier 1', @seed_admin_id, @seed_admin_id),
    ('A5000000-0000-0000-0000-000000000002', 'BP-DEV-002', 'Dev Supplier 2', 'SUPPLIER', 'Nguyen B', 'supplier2@dev.local', '0911000002', 'Address 2', 'Ha Noi',   'VN', 'TAX-SUP-002', 'NET 15',  30000.00, 'ACTIVE', 'Seed supplier 2', @seed_admin_id, @seed_admin_id),
    ('A5000000-0000-0000-0000-000000000003', 'BP-DEV-003', 'Dev Customer 1', 'CUSTOMER', 'Tran C',   'customer1@dev.local', '0912000001', 'Address 3', 'Da Nang',  'VN', 'TAX-CUS-001', 'NET 07',  20000.00, 'ACTIVE', 'Seed customer 1', @seed_admin_id, @seed_admin_id),
    ('A5000000-0000-0000-0000-000000000004', 'BP-DEV-004', 'Dev Customer 2', 'CUSTOMER', 'Tran D',   'customer2@dev.local', '0912000002', 'Address 4', 'Can Tho',  'VN', 'TAX-CUS-002', 'NET 07',  15000.00, 'ACTIVE', 'Seed customer 2', @seed_admin_id, @seed_admin_id),
    ('A5000000-0000-0000-0000-000000000005', 'BP-DEV-005', 'Dev Partner 3',  'BOTH',     'Le E',     'partner3@dev.local',  '0913000003', 'Address 5', 'Hai Phong','VN', 'TAX-BTH-003', 'NET 30',  40000.00, 'ACTIVE', 'Seed both type',  @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Employees
-- ================================
INSERT INTO employees
(
    id, account_id, employee_code, department, position, hire_date, termination_date,
    salary_grade, warehouse_id, status, created_by, updated_by
)
VALUES
    ('A6000000-0000-0000-0000-000000000001', 'A2000000-0000-0000-0000-000000000001', 'EMP-DEV-001', 'Inbound',  'Operator',   '2025-01-10', NULL, 'G5', '93000000-0000-0000-0000-000000000001', 'ACTIVE',   @seed_admin_id, @seed_admin_id),
    ('A6000000-0000-0000-0000-000000000002', 'A2000000-0000-0000-0000-000000000002', 'EMP-DEV-002', 'Outbound', 'Picker',     '2025-01-11', NULL, 'G4', '93000000-0000-0000-0000-000000000001', 'ACTIVE',   @seed_admin_id, @seed_admin_id),
    ('A6000000-0000-0000-0000-000000000003', 'A2000000-0000-0000-0000-000000000003', 'EMP-DEV-003', 'Inventory','Controller', '2025-01-12', NULL, 'G6', '93000000-0000-0000-0000-000000000001', 'ON_LEAVE', @seed_admin_id, @seed_admin_id),
    ('A6000000-0000-0000-0000-000000000004', 'A2000000-0000-0000-0000-000000000004', 'EMP-DEV-004', 'Inbound',  'Supervisor', '2025-01-13', NULL, 'G7', '93000000-0000-0000-0000-000000000001', 'ACTIVE',   @seed_admin_id, @seed_admin_id),
    ('A6000000-0000-0000-0000-000000000005', 'A2000000-0000-0000-0000-000000000005', 'EMP-DEV-005', 'QA',       'Inspector',  '2025-01-14', NULL, 'G5', '93000000-0000-0000-0000-000000000001', 'ACTIVE',   @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Customers
-- ================================
INSERT INTO customers
(
    id, account_id, customer_code, credit_limit, loyalty_points, customer_tier, tax_id,
    business_partner_id, status, email_verified, notes, created_by, updated_by
)
VALUES
    ('A7000000-0000-0000-0000-000000000001', 'A2000000-0000-0000-0000-000000000006', 'CUST-DEV-001',  5000.00,  100, 'STANDARD', 'CUS-TAX-001', 'A5000000-0000-0000-0000-000000000003', 'ACTIVE',    TRUE,  'Seed customer profile 1', @seed_admin_id, @seed_admin_id),
    ('A7000000-0000-0000-0000-000000000002', 'A2000000-0000-0000-0000-000000000007', 'CUST-DEV-002', 10000.00,  250, 'SILVER',   'CUS-TAX-002', 'A5000000-0000-0000-0000-000000000004', 'ACTIVE',    TRUE,  'Seed customer profile 2', @seed_admin_id, @seed_admin_id),
    ('A7000000-0000-0000-0000-000000000003', 'A2000000-0000-0000-0000-000000000008', 'CUST-DEV-003', 15000.00,  500, 'GOLD',     'CUS-TAX-003', 'A5000000-0000-0000-0000-000000000005', 'ACTIVE',    TRUE,  'Seed customer profile 3', @seed_admin_id, @seed_admin_id),
    ('A7000000-0000-0000-0000-000000000004', 'A2000000-0000-0000-0000-000000000009', 'CUST-DEV-004', 25000.00,  900, 'PLATINUM', 'CUS-TAX-004', NULL,                                    'SUSPENDED', FALSE, 'Seed customer profile 4', @seed_admin_id, @seed_admin_id),
    ('A7000000-0000-0000-0000-000000000005', 'A2000000-0000-0000-0000-000000000010', 'CUST-DEV-005',  8000.00,   50, 'STANDARD', 'CUS-TAX-005', NULL,                                    'ACTIVE',    FALSE, 'Seed customer profile 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Inbound Operations
-- ================================
INSERT INTO purchase_orders
(
    id, purchase_order_number, supplier_id, warehouse_id, order_date, expected_delivery_date, status,
    sub_total, tax_amount, total_amount, currency, payment_terms, notes, confirmed_at, confirmed_by,
    created_by, updated_by
)
VALUES
    ('A8000000-0000-0000-0000-000000000001', 'PO-DEV-0001', 'A5000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '2026-03-01', '2026-03-05', 'DRAFT',              500.00, 50.00, 550.00, 'USD', 'NET 30', 'Dev PO 1', NULL, NULL, @seed_admin_id, @seed_admin_id),
    ('A8000000-0000-0000-0000-000000000002', 'PO-DEV-0002', 'A5000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '2026-03-02', '2026-03-06', 'CONFIRMED',          960.00, 96.00,1056.00, 'USD', 'NET 15', 'Dev PO 2', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8000000-0000-0000-0000-000000000003', 'PO-DEV-0003', 'A5000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '2026-03-03', '2026-03-07', 'PARTIALLY_RECEIVED', 700.00, 70.00, 770.00, 'USD', 'NET 30', 'Dev PO 3', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8000000-0000-0000-0000-000000000004', 'PO-DEV-0004', 'A5000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '2026-03-04', '2026-03-08', 'COMPLETED',         1200.00,120.00,1320.00, 'USD', 'NET 30', 'Dev PO 4', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8000000-0000-0000-0000-000000000005', 'PO-DEV-0005', 'A5000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '2026-03-05', '2026-03-09', 'CANCELLED',          300.00, 30.00, 330.00, 'USD', 'NET 15', 'Dev PO 5', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO purchase_order_lines
(
    id, purchase_order_id, product_id, line_number, quantity_ordered, quantity_received,
    unit_price, line_total, notes, created_by, updated_by
)
VALUES
    ('A8100000-0000-0000-0000-000000000001', 'A8000000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', 1,  50.00,  0.00, 10.00,  500.00, 'PO line 1', @seed_admin_id, @seed_admin_id),
    ('A8100000-0000-0000-0000-000000000002', 'A8000000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', 1,  80.00, 80.00, 12.00,  960.00, 'PO line 2', @seed_admin_id, @seed_admin_id),
    ('A8100000-0000-0000-0000-000000000003', 'A8000000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', 1, 100.00, 40.00,  7.00,  700.00, 'PO line 3', @seed_admin_id, @seed_admin_id),
    ('A8100000-0000-0000-0000-000000000004', 'A8000000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', 1,  60.00, 60.00, 20.00, 1200.00, 'PO line 4', @seed_admin_id, @seed_admin_id),
    ('A8100000-0000-0000-0000-000000000005', 'A8000000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', 1,  10.00,  0.00, 30.00,  300.00, 'PO line 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO inbound_receipts
(
    id, receipt_number, purchase_order_id, warehouse_id, receipt_date, status,
    confirmed_at, confirmed_by, delivery_note_number, notes, created_by, updated_by
)
VALUES
    ('A8200000-0000-0000-0000-000000000001', 'IR-DEV-0001', 'A8000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '2026-03-05', 'DRAFT',     NULL, NULL,            'DN-0001', 'Inbound receipt 1', @seed_admin_id, @seed_admin_id),
    ('A8200000-0000-0000-0000-000000000002', 'IR-DEV-0002', 'A8000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '2026-03-06', 'CONFIRMED', NOW(), @seed_admin_id, 'DN-0002', 'Inbound receipt 2', @seed_admin_id, @seed_admin_id),
    ('A8200000-0000-0000-0000-000000000003', 'IR-DEV-0003', 'A8000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '2026-03-07', 'CONFIRMED', NOW(), @seed_admin_id, 'DN-0003', 'Inbound receipt 3', @seed_admin_id, @seed_admin_id),
    ('A8200000-0000-0000-0000-000000000004', 'IR-DEV-0004', 'A8000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '2026-03-08', 'CONFIRMED', NOW(), @seed_admin_id, 'DN-0004', 'Inbound receipt 4', @seed_admin_id, @seed_admin_id),
    ('A8200000-0000-0000-0000-000000000005', 'IR-DEV-0005', 'A8000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '2026-03-09', 'CANCELLED', NOW(), @seed_admin_id, 'DN-0005', 'Inbound receipt 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO inbound_receipt_lines
(
    id, inbound_receipt_id, purchase_order_line_id, product_id, batch_id, location_id,
    line_number, quantity_received, quality_status, notes, created_by, updated_by
)
VALUES
    ('A8300000-0000-0000-0000-000000000001', 'A8200000-0000-0000-0000-000000000001', 'A8100000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', 1, 10.00, 'PASS',       'Receipt line 1', @seed_admin_id, @seed_admin_id),
    ('A8300000-0000-0000-0000-000000000002', 'A8200000-0000-0000-0000-000000000002', 'A8100000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002', '94000000-0000-0000-0000-000000000002', 1, 80.00, 'PASS',       'Receipt line 2', @seed_admin_id, @seed_admin_id),
    ('A8300000-0000-0000-0000-000000000003', 'A8200000-0000-0000-0000-000000000003', 'A8100000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000003', '94000000-0000-0000-0000-000000000003', 1, 40.00, 'QUARANTINE', 'Receipt line 3', @seed_admin_id, @seed_admin_id),
    ('A8300000-0000-0000-0000-000000000004', 'A8200000-0000-0000-0000-000000000004', 'A8100000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', '94000000-0000-0000-0000-000000000004', 1, 60.00, 'PASS',       'Receipt line 4', @seed_admin_id, @seed_admin_id),
    ('A8300000-0000-0000-0000-000000000005', 'A8200000-0000-0000-0000-000000000005', 'A8100000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', '96000000-0000-0000-0000-000000000005', '94000000-0000-0000-0000-000000000005', 1,  5.00, 'PASS',       'Receipt line 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Outbound Operations
-- ================================
INSERT INTO sales_orders
(
    id, so_number, customer_id, warehouse_id, order_date, requested_delivery_date, status,
    sub_total, tax_amount, total_amount, currency, notes, confirmed_at, confirmed_by,
    created_by, updated_by
)
VALUES
    ('A8400000-0000-0000-0000-000000000001', 'SO-DEV-0001', 'A5000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '2026-03-10', '2026-03-12', 'DRAFT',             420.00, 42.00, 462.00, 'USD', 'Sales order 1', NULL, NULL, @seed_admin_id, @seed_admin_id),
    ('A8400000-0000-0000-0000-000000000002', 'SO-DEV-0002', 'A5000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '2026-03-10', '2026-03-12', 'CONFIRMED',         300.00, 30.00, 330.00, 'USD', 'Sales order 2', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8400000-0000-0000-0000-000000000003', 'SO-DEV-0003', 'A5000000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '2026-03-11', '2026-03-13', 'PARTIALLY_SHIPPED', 200.00, 20.00, 220.00, 'USD', 'Sales order 3', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8400000-0000-0000-0000-000000000004', 'SO-DEV-0004', 'A5000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '2026-03-11', '2026-03-14', 'COMPLETED',         560.00, 56.00, 616.00, 'USD', 'Sales order 4', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id),
    ('A8400000-0000-0000-0000-000000000005', 'SO-DEV-0005', 'A5000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '2026-03-12', '2026-03-15', 'CANCELLED',         150.00, 15.00, 165.00, 'USD', 'Sales order 5', NOW(), @seed_admin_id, @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO sales_order_lines
(
    id, sales_order_id, product_id, line_number, quantity_ordered, quantity_shipped,
    unit_price, line_total, notes, created_by, updated_by
)
VALUES
    ('A8500000-0000-0000-0000-000000000001', 'A8400000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', 1, 30.00,  0.00, 14.00, 420.00, 'SO line 1', @seed_admin_id, @seed_admin_id),
    ('A8500000-0000-0000-0000-000000000002', 'A8400000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', 1, 20.00, 20.00, 15.00, 300.00, 'SO line 2', @seed_admin_id, @seed_admin_id),
    ('A8500000-0000-0000-0000-000000000003', 'A8400000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', 1, 25.00, 10.00,  8.00, 200.00, 'SO line 3', @seed_admin_id, @seed_admin_id),
    ('A8500000-0000-0000-0000-000000000004', 'A8400000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', 1, 20.00, 20.00, 28.00, 560.00, 'SO line 4', @seed_admin_id, @seed_admin_id),
    ('A8500000-0000-0000-0000-000000000005', 'A8400000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', 1,  5.00,  0.00, 30.00, 150.00, 'SO line 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO outbound_shipments
(
    id, shipment_number, sales_order_id, warehouse_id, shipment_date, status,
    tracking_number, carrier, shipped_at, confirmed_by, notes, created_by, updated_by
)
VALUES
    ('A8600000-0000-0000-0000-000000000001', 'OS-DEV-0001', 'A8400000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '2026-03-12', 'DRAFT',     NULL,       NULL,        NULL, NULL,          'Shipment 1', @seed_admin_id, @seed_admin_id),
    ('A8600000-0000-0000-0000-000000000002', 'OS-DEV-0002', 'A8400000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '2026-03-12', 'SHIPPED',   'TRK-0002', 'DHL',       NOW(), @seed_admin_id, 'Shipment 2', @seed_admin_id, @seed_admin_id),
    ('A8600000-0000-0000-0000-000000000003', 'OS-DEV-0003', 'A8400000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '2026-03-13', 'PACKED',    'TRK-0003', 'FedEx',     NULL,  @seed_admin_id, 'Shipment 3', @seed_admin_id, @seed_admin_id),
    ('A8600000-0000-0000-0000-000000000004', 'OS-DEV-0004', 'A8400000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '2026-03-13', 'SHIPPED',   'TRK-0004', 'VNPost',    NOW(), @seed_admin_id, 'Shipment 4', @seed_admin_id, @seed_admin_id),
    ('A8600000-0000-0000-0000-000000000005', 'OS-DEV-0005', 'A8400000-0000-0000-0000-000000000005', '93000000-0000-0000-0000-000000000001', '2026-03-14', 'CANCELLED', 'TRK-0005', 'JNT',       NULL,  @seed_admin_id, 'Shipment 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

INSERT INTO outbound_shipment_lines
(
    id, outbound_shipment_id, sales_order_line_id, product_id, batch_id, location_id,
    line_number, quantity_shipped, picked_at, picked_by, notes, created_by, updated_by
)
VALUES
    ('A8700000-0000-0000-0000-000000000001', 'A8600000-0000-0000-0000-000000000001', 'A8500000-0000-0000-0000-000000000001', '95000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', 1,  0.00, NULL, NULL,          'Shipment line 1', @seed_admin_id, @seed_admin_id),
    ('A8700000-0000-0000-0000-000000000002', 'A8600000-0000-0000-0000-000000000002', 'A8500000-0000-0000-0000-000000000002', '95000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002', '94000000-0000-0000-0000-000000000002', 1, 20.00, NOW(), @seed_admin_id, 'Shipment line 2', @seed_admin_id, @seed_admin_id),
    ('A8700000-0000-0000-0000-000000000003', 'A8600000-0000-0000-0000-000000000003', 'A8500000-0000-0000-0000-000000000003', '95000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000003', '94000000-0000-0000-0000-000000000003', 1, 10.00, NOW(), @seed_admin_id, 'Shipment line 3', @seed_admin_id, @seed_admin_id),
    ('A8700000-0000-0000-0000-000000000004', 'A8600000-0000-0000-0000-000000000004', 'A8500000-0000-0000-0000-000000000004', '95000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', '94000000-0000-0000-0000-000000000004', 1, 20.00, NOW(), @seed_admin_id, 'Shipment line 4', @seed_admin_id, @seed_admin_id),
    ('A8700000-0000-0000-0000-000000000005', 'A8600000-0000-0000-0000-000000000005', 'A8500000-0000-0000-0000-000000000005', '95000000-0000-0000-0000-000000000005', '96000000-0000-0000-0000-000000000005', '94000000-0000-0000-0000-000000000005', 1,  0.00, NULL, NULL,          'Shipment line 5', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Additional stock movements for inbound/outbound flows
-- ================================
INSERT INTO stock_movements
(
    id, movement_type, product_id, warehouse_id, location_id, batch_id,
    quantity_change, quantity_before, quantity_after, movement_date,
    reference_type, reference_id, reference_number, notes, created_by, updated_by
)
VALUES
    ('A8800000-0000-0000-0000-000000000001', 'INBOUND',             '95000000-0000-0000-0000-000000000001', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000001', '96000000-0000-0000-0000-000000000001',  10.00, 100.00, 110.00, NOW(), 'INBOUND_RECEIPT',  'A8200000-0000-0000-0000-000000000001', 'IR-DEV-0001', 'Inbound from receipt 1',   @seed_admin_id, @seed_admin_id),
    ('A8800000-0000-0000-0000-000000000002', 'INBOUND',             '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002',  80.00,  15.00,  95.00, NOW(), 'INBOUND_RECEIPT',  'A8200000-0000-0000-0000-000000000002', 'IR-DEV-0002', 'Inbound from receipt 2',   @seed_admin_id, @seed_admin_id),
    ('A8800000-0000-0000-0000-000000000003', 'OUTBOUND',            '95000000-0000-0000-0000-000000000002', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000002', '96000000-0000-0000-0000-000000000002', -20.00,  95.00,  75.00, NOW(), 'OUTBOUND_SHIPMENT','A8600000-0000-0000-0000-000000000002', 'OS-DEV-0002', 'Outbound for shipment 2', @seed_admin_id, @seed_admin_id),
    ('A8800000-0000-0000-0000-000000000004', 'OUTBOUND',            '95000000-0000-0000-0000-000000000003', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000003', '96000000-0000-0000-0000-000000000003', -10.00,  60.00,  50.00, NOW(), 'OUTBOUND_SHIPMENT','A8600000-0000-0000-0000-000000000003', 'OS-DEV-0003', 'Outbound for shipment 3', @seed_admin_id, @seed_admin_id),
    ('A8800000-0000-0000-0000-000000000005', 'OUTBOUND',            '95000000-0000-0000-0000-000000000004', '93000000-0000-0000-0000-000000000001', '94000000-0000-0000-0000-000000000004', '96000000-0000-0000-0000-000000000004', -20.00, 170.00, 150.00, NOW(), 'OUTBOUND_SHIPMENT','A8600000-0000-0000-0000-000000000004', 'OS-DEV-0004', 'Outbound for shipment 4', @seed_admin_id, @seed_admin_id)
ON DUPLICATE KEY UPDATE id = id;

-- ================================
-- Email logs
-- ================================
INSERT INTO email_logs
(
    id, recipient, cc, bcc, subject, content, email_type, status,
    retry_count, max_retry, error_message, sent_at, has_attachment, attachment_path,
    priority, scheduled_at, triggered_by, created_by, updated_by
)
VALUES
    ('A8900000-0000-0000-0000-000000000001', 'cust.dev01@whs.local', NULL, NULL, 'Welcome to WMS',      'Welcome email content',         'WELCOME',         'SENT',    0, 3, NULL, NOW(), TRUE,  '/tmp/welcome-1.pdf', 5, NOW(), @seed_admin_id, 'system', 'system'),
    ('A8900000-0000-0000-0000-000000000002', 'cust.dev02@whs.local', NULL, NULL, 'Password Reset',      'Password reset email content',  'PASSWORD_RESET',  'FAILED',  2, 3, 'SMTP timeout', NULL, FALSE, NULL,               4, NOW(), @seed_admin_id, 'system', 'system'),
    ('A8900000-0000-0000-0000-000000000003', 'emp.dev01@whs.local',  NULL, NULL, 'Inventory Alert',     'Inventory threshold alert',     'ALERT',           'PENDING', 0, 3, NULL, NULL, FALSE, NULL,               3, NOW(), @seed_admin_id, 'system', 'system'),
    ('A8900000-0000-0000-0000-000000000004', 'emp.dev02@whs.local',  NULL, NULL, 'Shipment Notification','Shipment processed notification','NOTIFICATION',    'SENT',    0, 3, NULL, NOW(), FALSE, NULL,               6, NOW(), @seed_admin_id, 'system', 'system'),
    ('A8900000-0000-0000-0000-000000000005', 'cust.dev03@whs.local', NULL, NULL, 'Order Confirmation',  'Order confirmation content',    'ORDER_CONFIRM',   'RETRY',   1, 3, 'Temporary SMTP error', NULL, FALSE, NULL,      5, NOW(), @seed_admin_id, 'system', 'system')
ON DUPLICATE KEY UPDATE id = id;

-- ============================================================================
-- End of Migration
-- ============================================================================
