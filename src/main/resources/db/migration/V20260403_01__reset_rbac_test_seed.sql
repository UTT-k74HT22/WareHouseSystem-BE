-- ============================================================================
-- Flyway Migration: Reset RBAC data for realistic UI/UX permission testing
-- Version: V20260403_01
-- Description:
--   - remove legacy RBAC permissions/roles/mappings
--   - reseed the canonical permission catalog only
--   - create deterministic role personas for FE RBAC and business UI testing
--   - standardize managed account seeds and password hash for 123456789
-- Notes:
--   - This migration intentionally resets RBAC core data.
--   - It does not delete domain accounts such as emp.dev*/cust.dev* because
--     other module seed data references them.
-- ============================================================================

SET @rbac_seed_actor := 'RBAC_TEST_SEED_20260403';
SET @seed_password := '$2a$10$2FrGg2/7Rtr7lQWRZ9UNV..WQblwoUUgJgOxWYnhP.okeEd3Jo5si';

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_permission_blueprint;
CREATE TEMPORARY TABLE tmp_rbac_permission_blueprint (
    code VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(20) NOT NULL,
    description VARCHAR(255) NOT NULL
);

INSERT INTO tmp_rbac_permission_blueprint (code, name, resource, action, description)
VALUES
    ('PERM_SYSTEM_DIAGNOSTIC_READ', 'system_diagnostic.read', 'SYSTEM_DIAGNOSTIC', 'READ', 'Read diagnostic endpoints'),
    ('PERM_PERMISSION_CREATE', 'permission.create', 'PERMISSION', 'CREATE', 'Create permission'),
    ('PERM_PERMISSION_READ', 'permission.read', 'PERMISSION', 'READ', 'Read permission'),
    ('PERM_PERMISSION_UPDATE', 'permission.update', 'PERMISSION', 'UPDATE', 'Update permission'),
    ('PERM_PERMISSION_DELETE', 'permission.delete', 'PERMISSION', 'DELETE', 'Delete permission'),
    ('PERM_ROLE_CREATE', 'role.create', 'ROLE', 'CREATE', 'Create role'),
    ('PERM_ROLE_READ', 'role.read', 'ROLE', 'READ', 'Read role'),
    ('PERM_ROLE_UPDATE', 'role.update', 'ROLE', 'UPDATE', 'Update role'),
    ('PERM_ROLE_DELETE', 'role.delete', 'ROLE', 'DELETE', 'Delete role'),
    ('PERM_ROLE_PERMISSION_CREATE', 'role_permission.create', 'ROLE_PERMISSION', 'CREATE', 'Assign permissions to role'),
    ('PERM_ROLE_PERMISSION_READ', 'role_permission.read', 'ROLE_PERMISSION', 'READ', 'Read role permissions'),
    ('PERM_ROLE_PERMISSION_DELETE', 'role_permission.delete', 'ROLE_PERMISSION', 'DELETE', 'Remove permission from role'),
    ('PERM_USER_ROLE_CREATE', 'user_role.create', 'USER_ROLE', 'CREATE', 'Assign role to user'),
    ('PERM_USER_ROLE_READ', 'user_role.read', 'USER_ROLE', 'READ', 'Read user roles'),
    ('PERM_USER_ROLE_DELETE', 'user_role.delete', 'USER_ROLE', 'DELETE', 'Remove role from user'),
    ('PERM_USER_READ', 'user.read', 'USER', 'READ', 'Read user data'),
    ('PERM_EMPLOYEE_CREATE', 'employee.create', 'EMPLOYEE', 'CREATE', 'Create employee'),
    ('PERM_EMPLOYEE_READ', 'employee.read', 'EMPLOYEE', 'READ', 'Read employee'),
    ('PERM_EMPLOYEE_UPDATE', 'employee.update', 'EMPLOYEE', 'UPDATE', 'Update employee'),
    ('PERM_EMPLOYEE_DELETE', 'employee.delete', 'EMPLOYEE', 'DELETE', 'Delete employee'),
    ('PERM_EMAIL_CREATE', 'email.create', 'EMAIL', 'CREATE', 'Send email'),
    ('PERM_EMAIL_READ', 'email.read', 'EMAIL', 'READ', 'Read email logs and statistics'),
    ('PERM_EMAIL_UPDATE', 'email.update', 'EMAIL', 'UPDATE', 'Retry or process email jobs'),
    ('PERM_CATEGORY_CREATE', 'category.create', 'CATEGORY', 'CREATE', 'Create category'),
    ('PERM_CATEGORY_READ', 'category.read', 'CATEGORY', 'READ', 'Read category'),
    ('PERM_CATEGORY_UPDATE', 'category.update', 'CATEGORY', 'UPDATE', 'Update category'),
    ('PERM_BUSINESS_PARTNER_CREATE', 'business_partner.create', 'BUSINESS_PARTNER', 'CREATE', 'Create business partner'),
    ('PERM_BUSINESS_PARTNER_READ', 'business_partner.read', 'BUSINESS_PARTNER', 'READ', 'Read business partner'),
    ('PERM_BUSINESS_PARTNER_UPDATE', 'business_partner.update', 'BUSINESS_PARTNER', 'UPDATE', 'Update business partner'),
    ('PERM_BUSINESS_PARTNER_DELETE', 'business_partner.delete', 'BUSINESS_PARTNER', 'DELETE', 'Delete business partner'),
    ('PERM_WAREHOUSE_CREATE', 'warehouse.create', 'WAREHOUSE', 'CREATE', 'Create warehouse'),
    ('PERM_WAREHOUSE_READ', 'warehouse.read', 'WAREHOUSE', 'READ', 'Read warehouse'),
    ('PERM_WAREHOUSE_UPDATE', 'warehouse.update', 'WAREHOUSE', 'UPDATE', 'Update warehouse'),
    ('PERM_WAREHOUSE_DELETE', 'warehouse.delete', 'WAREHOUSE', 'DELETE', 'Delete warehouse'),
    ('PERM_LOCATION_CREATE', 'location.create', 'LOCATION', 'CREATE', 'Create location'),
    ('PERM_LOCATION_READ', 'location.read', 'LOCATION', 'READ', 'Read location'),
    ('PERM_LOCATION_UPDATE', 'location.update', 'LOCATION', 'UPDATE', 'Update location'),
    ('PERM_LOCATION_DELETE', 'location.delete', 'LOCATION', 'DELETE', 'Delete location'),
    ('PERM_UNIT_OF_MEASURE_CREATE', 'unit_of_measure.create', 'UNIT_OF_MEASURE', 'CREATE', 'Create unit of measure'),
    ('PERM_UNIT_OF_MEASURE_READ', 'unit_of_measure.read', 'UNIT_OF_MEASURE', 'READ', 'Read unit of measure'),
    ('PERM_UNIT_OF_MEASURE_UPDATE', 'unit_of_measure.update', 'UNIT_OF_MEASURE', 'UPDATE', 'Update unit of measure'),
    ('PERM_UNIT_OF_MEASURE_DELETE', 'unit_of_measure.delete', 'UNIT_OF_MEASURE', 'DELETE', 'Delete unit of measure'),
    ('PERM_PRODUCT_CREATE', 'product.create', 'PRODUCT', 'CREATE', 'Create product'),
    ('PERM_PRODUCT_READ', 'product.read', 'PRODUCT', 'READ', 'Read product'),
    ('PERM_PRODUCT_UPDATE', 'product.update', 'PRODUCT', 'UPDATE', 'Update product'),
    ('PERM_PRODUCT_DELETE', 'product.delete', 'PRODUCT', 'DELETE', 'Delete product'),
    ('PERM_INVENTORY_READ', 'inventory.read', 'INVENTORY', 'READ', 'Read inventory'),
    ('PERM_INVENTORY_RESERVATION_UPDATE', 'inventory_reservation.update', 'INVENTORY_RESERVATION', 'UPDATE', 'Reserve and unreserve inventory'),
    ('PERM_INVENTORY_MUTATION_UPDATE', 'inventory_mutation.update', 'INVENTORY_MUTATION', 'UPDATE', 'Increase or decrease inventory'),
    ('PERM_BATCH_CREATE', 'batch.create', 'BATCH', 'CREATE', 'Create batch'),
    ('PERM_BATCH_READ', 'batch.read', 'BATCH', 'READ', 'Read batch'),
    ('PERM_BATCH_UPDATE', 'batch.update', 'BATCH', 'UPDATE', 'Update batch'),
    ('PERM_PURCHASE_ORDER_CREATE', 'purchase_order.create', 'PURCHASE_ORDER', 'CREATE', 'Create purchase order'),
    ('PERM_PURCHASE_ORDER_READ', 'purchase_order.read', 'PURCHASE_ORDER', 'READ', 'Read purchase order'),
    ('PERM_PURCHASE_ORDER_UPDATE', 'purchase_order.update', 'PURCHASE_ORDER', 'UPDATE', 'Update purchase order'),
    ('PERM_PURCHASE_ORDER_DELETE', 'purchase_order.delete', 'PURCHASE_ORDER', 'DELETE', 'Delete purchase order'),
    ('PERM_PURCHASE_ORDER_LINE_CREATE', 'purchase_order_line.create', 'PURCHASE_ORDER_LINE', 'CREATE', 'Create purchase order line'),
    ('PERM_PURCHASE_ORDER_LINE_READ', 'purchase_order_line.read', 'PURCHASE_ORDER_LINE', 'READ', 'Read purchase order line'),
    ('PERM_PURCHASE_ORDER_LINE_UPDATE', 'purchase_order_line.update', 'PURCHASE_ORDER_LINE', 'UPDATE', 'Update purchase order line'),
    ('PERM_PURCHASE_ORDER_LINE_DELETE', 'purchase_order_line.delete', 'PURCHASE_ORDER_LINE', 'DELETE', 'Delete purchase order line'),
    ('PERM_INBOUND_RECEIPT_CREATE', 'inbound_receipt.create', 'INBOUND_RECEIPT', 'CREATE', 'Create inbound receipt'),
    ('PERM_INBOUND_RECEIPT_READ', 'inbound_receipt.read', 'INBOUND_RECEIPT', 'READ', 'Read inbound receipt'),
    ('PERM_INBOUND_RECEIPT_UPDATE', 'inbound_receipt.update', 'INBOUND_RECEIPT', 'UPDATE', 'Update inbound receipt'),
    ('PERM_INBOUND_RECEIPT_DELETE', 'inbound_receipt.delete', 'INBOUND_RECEIPT', 'DELETE', 'Delete inbound receipt'),
    ('PERM_INBOUND_RECEIPT_LINE_CREATE', 'inbound_receipt_line.create', 'INBOUND_RECEIPT_LINE', 'CREATE', 'Create inbound receipt line'),
    ('PERM_INBOUND_RECEIPT_LINE_READ', 'inbound_receipt_line.read', 'INBOUND_RECEIPT_LINE', 'READ', 'Read inbound receipt line'),
    ('PERM_INBOUND_RECEIPT_LINE_UPDATE', 'inbound_receipt_line.update', 'INBOUND_RECEIPT_LINE', 'UPDATE', 'Update inbound receipt line'),
    ('PERM_INBOUND_RECEIPT_LINE_DELETE', 'inbound_receipt_line.delete', 'INBOUND_RECEIPT_LINE', 'DELETE', 'Delete inbound receipt line'),
    ('PERM_SALES_ORDER_CREATE', 'sales_order.create', 'SALES_ORDER', 'CREATE', 'Create sales order'),
    ('PERM_SALES_ORDER_READ', 'sales_order.read', 'SALES_ORDER', 'READ', 'Read sales order'),
    ('PERM_SALES_ORDER_UPDATE', 'sales_order.update', 'SALES_ORDER', 'UPDATE', 'Update sales order'),
    ('PERM_SALES_ORDER_LINE_CREATE', 'sales_order_line.create', 'SALES_ORDER_LINE', 'CREATE', 'Create sales order line'),
    ('PERM_SALES_ORDER_LINE_READ', 'sales_order_line.read', 'SALES_ORDER_LINE', 'READ', 'Read sales order line'),
    ('PERM_SALES_ORDER_LINE_UPDATE', 'sales_order_line.update', 'SALES_ORDER_LINE', 'UPDATE', 'Update sales order line'),
    ('PERM_OUTBOUND_SHIPMENT_CREATE', 'outbound_shipment.create', 'OUTBOUND_SHIPMENT', 'CREATE', 'Create outbound shipment'),
    ('PERM_OUTBOUND_SHIPMENT_READ', 'outbound_shipment.read', 'OUTBOUND_SHIPMENT', 'READ', 'Read outbound shipment'),
    ('PERM_OUTBOUND_SHIPMENT_UPDATE', 'outbound_shipment.update', 'OUTBOUND_SHIPMENT', 'UPDATE', 'Update outbound shipment'),
    ('PERM_OUTBOUND_SHIPMENT_LINE_CREATE', 'outbound_shipment_line.create', 'OUTBOUND_SHIPMENT_LINE', 'CREATE', 'Create outbound shipment line'),
    ('PERM_OUTBOUND_SHIPMENT_LINE_READ', 'outbound_shipment_line.read', 'OUTBOUND_SHIPMENT_LINE', 'READ', 'Read outbound shipment line'),
    ('PERM_OUTBOUND_SHIPMENT_LINE_UPDATE', 'outbound_shipment_line.update', 'OUTBOUND_SHIPMENT_LINE', 'UPDATE', 'Update outbound shipment line'),
    ('PERM_OUTBOUND_SHIPMENT_LINE_DELETE', 'outbound_shipment_line.delete', 'OUTBOUND_SHIPMENT_LINE', 'DELETE', 'Delete outbound shipment line'),
    ('PERM_STOCK_TRANSFER_CREATE', 'stock_transfer.create', 'STOCK_TRANSFER', 'CREATE', 'Create stock transfer'),
    ('PERM_STOCK_TRANSFER_READ', 'stock_transfer.read', 'STOCK_TRANSFER', 'READ', 'Read stock transfer'),
    ('PERM_STOCK_TRANSFER_UPDATE', 'stock_transfer.update', 'STOCK_TRANSFER', 'UPDATE', 'Update stock transfer'),
    ('PERM_STOCK_MOVEMENT_READ', 'stock_movement.read', 'STOCK_MOVEMENT', 'READ', 'Read stock movement'),
    ('PERM_STOCK_ADJUSTMENT_CREATE', 'stock_adjustment.create', 'STOCK_ADJUSTMENT', 'CREATE', 'Create stock adjustment'),
    ('PERM_STOCK_ADJUSTMENT_READ', 'stock_adjustment.read', 'STOCK_ADJUSTMENT', 'READ', 'Read stock adjustment'),
    ('PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE', 'stock_adjustment_approval.update', 'STOCK_ADJUSTMENT_APPROVAL', 'UPDATE', 'Approve or reject stock adjustment'),
    ('PERM_STORAGE_CREATE', 'storage.create', 'STORAGE', 'CREATE', 'Upload file'),
    ('PERM_STORAGE_READ', 'storage.read', 'STORAGE', 'READ', 'Read storage metadata'),
    ('PERM_STORAGE_DELETE', 'storage.delete', 'STORAGE', 'DELETE', 'Delete file'),
    ('PERM_REPORT_CURRENT_STOCK_READ', 'report_current_stock.read', 'REPORT_CURRENT_STOCK', 'READ', 'Export current stock report');

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_seed_profiles;
CREATE TEMPORARY TABLE tmp_rbac_seed_profiles (
    username VARCHAR(50) PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NULL,
    address VARCHAR(255) NULL
);

INSERT INTO tmp_rbac_seed_profiles (username, first_name, last_name, email, phone_number, address)
VALUES
    ('admin', 'System', 'Administrator', 'admin@whs.local', '0900000001', 'Head Office'),
    ('dev_seed_admin', 'Seed', 'Administrator', 'dev_seed_admin@whs.local', '0900000002', 'Head Office'),
    ('manager', 'Warehouse', 'Manager', 'manager@whs.local', '0900000003', 'Warehouse Office'),
    ('user', 'Default', 'User', 'user@whs.local', '0900000004', 'User Desk'),
    ('rbac.admin', 'RBAC', 'Admin', 'rbac.admin@whs.local', '0900000005', 'Security Office'),
    ('rbac.permission.viewer', 'Permission', 'Viewer', 'rbac.permission.viewer@whs.local', '0900000006', 'Security Office'),
    ('rbac.role.viewer', 'Role', 'Viewer', 'rbac.role.viewer@whs.local', '0900000007', 'Security Office'),
    ('rbac.user-role.manager', 'User Role', 'Manager', 'rbac.user-role.manager@whs.local', '0900000008', 'Security Office'),
    ('inventory.staff', 'Inventory', 'Staff', 'inventory.staff@whs.local', '0900000009', 'Inbound Zone'),
    ('procurement.staff', 'Procurement', 'Staff', 'procurement.staff@whs.local', '0900000010', 'Procurement Office'),
    ('sales.staff', 'Sales', 'Staff', 'sales.staff@whs.local', '0900000011', 'Sales Office'),
    ('read.only', 'Read', 'Only', 'read.only@whs.local', '0900000012', 'Back Office');

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_seed_role_assignments;
CREATE TEMPORARY TABLE tmp_rbac_seed_role_assignments (
    username VARCHAR(50) NOT NULL,
    role_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (username, role_code)
);

INSERT INTO tmp_rbac_seed_role_assignments (username, role_code)
VALUES
    ('admin', 'ROLE_ADMIN'),
    ('dev_seed_admin', 'ROLE_ADMIN'),
    ('manager', 'ROLE_MANAGER'),
    ('user', 'ROLE_USER'),
    ('rbac.admin', 'ROLE_RBAC_ADMIN'),
    ('rbac.permission.viewer', 'ROLE_RBAC_PERMISSION_VIEWER'),
    ('rbac.role.viewer', 'ROLE_RBAC_ROLE_VIEWER'),
    ('rbac.user-role.manager', 'ROLE_RBAC_USER_ROLE_MANAGER'),
    ('inventory.staff', 'ROLE_INVENTORY_STAFF'),
    ('procurement.staff', 'ROLE_PROCUREMENT'),
    ('sales.staff', 'ROLE_SALES'),
    ('read.only', 'ROLE_READ_ONLY'),
    ('emp.dev01', 'ROLE_INVENTORY_STAFF'),
    ('emp.dev02', 'ROLE_PROCUREMENT'),
    ('emp.dev03', 'ROLE_SALES'),
    ('emp.dev04', 'ROLE_MANAGER'),
    ('emp.dev05', 'ROLE_USER'),
    ('cust.dev01', 'ROLE_USER'),
    ('cust.dev02', 'ROLE_USER'),
    ('cust.dev03', 'ROLE_USER'),
    ('cust.dev04', 'ROLE_USER'),
    ('cust.dev05', 'ROLE_USER');

DELETE FROM role_permissions;
DELETE FROM account_roles;
DELETE FROM permissions;
DELETE FROM roles;

INSERT INTO permissions (id, code, name, resource, action, description, created_by, updated_by)
SELECT
    UUID(),
    bp.code,
    bp.name,
    bp.resource,
    bp.action,
    bp.description,
    @rbac_seed_actor,
    @rbac_seed_actor
FROM tmp_rbac_permission_blueprint bp;

INSERT INTO roles (id, code, name, description, is_default, created_by, updated_by)
VALUES
    (UUID(), 'ROLE_ADMIN', 'ADMIN', 'System administrator with full access', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_MANAGER', 'MANAGER', 'Broad operational role with read-only RBAC visibility', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_USER', 'USER', 'Default baseline user with no business permissions', TRUE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_READ_ONLY', 'READ_ONLY', 'Read-only business role for UI visibility testing', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_RBAC_ADMIN', 'RBAC_ADMIN', 'Full RBAC management role', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_RBAC_PERMISSION_VIEWER', 'RBAC_PERMISSION_VIEWER', 'Can only read permission catalog', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_RBAC_ROLE_VIEWER', 'RBAC_ROLE_VIEWER', 'Can only read role catalog', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_RBAC_USER_ROLE_MANAGER', 'RBAC_USER_ROLE_MANAGER', 'Can manage user-role assignments', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_INVENTORY_STAFF', 'INVENTORY_STAFF', 'Inventory and inbound operations role', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_PROCUREMENT', 'PROCUREMENT', 'Procurement and purchase order role', FALSE, @rbac_seed_actor, @rbac_seed_actor),
    (UUID(), 'ROLE_SALES', 'SALES', 'Sales and outbound operations role', FALSE, @rbac_seed_actor, @rbac_seed_actor);

SET @role_admin_id := (SELECT id FROM roles WHERE code = 'ROLE_ADMIN' LIMIT 1);
SET @role_manager_id := (SELECT id FROM roles WHERE code = 'ROLE_MANAGER' LIMIT 1);
SET @role_user_id := (SELECT id FROM roles WHERE code = 'ROLE_USER' LIMIT 1);
SET @role_read_only_id := (SELECT id FROM roles WHERE code = 'ROLE_READ_ONLY' LIMIT 1);
SET @role_rbac_admin_id := (SELECT id FROM roles WHERE code = 'ROLE_RBAC_ADMIN' LIMIT 1);
SET @role_rbac_permission_viewer_id := (SELECT id FROM roles WHERE code = 'ROLE_RBAC_PERMISSION_VIEWER' LIMIT 1);
SET @role_rbac_role_viewer_id := (SELECT id FROM roles WHERE code = 'ROLE_RBAC_ROLE_VIEWER' LIMIT 1);
SET @role_rbac_user_role_manager_id := (SELECT id FROM roles WHERE code = 'ROLE_RBAC_USER_ROLE_MANAGER' LIMIT 1);
SET @role_inventory_staff_id := (SELECT id FROM roles WHERE code = 'ROLE_INVENTORY_STAFF' LIMIT 1);
SET @role_procurement_id := (SELECT id FROM roles WHERE code = 'ROLE_PROCUREMENT' LIMIT 1);
SET @role_sales_id := (SELECT id FROM roles WHERE code = 'ROLE_SALES' LIMIT 1);

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_admin_id, p.id
FROM permissions p
WHERE @role_admin_id IS NOT NULL;

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_manager_id, p.id
FROM permissions p
WHERE @role_manager_id IS NOT NULL
  AND p.code NOT IN (
      'PERM_PERMISSION_CREATE',
      'PERM_PERMISSION_UPDATE',
      'PERM_PERMISSION_DELETE',
      'PERM_ROLE_CREATE',
      'PERM_ROLE_UPDATE',
      'PERM_ROLE_DELETE',
      'PERM_ROLE_PERMISSION_CREATE',
      'PERM_ROLE_PERMISSION_DELETE',
      'PERM_USER_ROLE_CREATE',
      'PERM_USER_ROLE_DELETE',
      'PERM_SYSTEM_DIAGNOSTIC_READ'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_read_only_id, p.id
FROM permissions p
WHERE @role_read_only_id IS NOT NULL
  AND p.action = 'READ'
  AND p.resource NOT IN ('PERMISSION', 'ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER', 'SYSTEM_DIAGNOSTIC');

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_rbac_admin_id, p.id
FROM permissions p
WHERE @role_rbac_admin_id IS NOT NULL
  AND p.resource IN ('PERMISSION', 'ROLE', 'ROLE_PERMISSION', 'USER_ROLE', 'USER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_rbac_permission_viewer_id, p.id
FROM permissions p
WHERE @role_rbac_permission_viewer_id IS NOT NULL
  AND p.code = 'PERM_PERMISSION_READ';

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_rbac_role_viewer_id, p.id
FROM permissions p
WHERE @role_rbac_role_viewer_id IS NOT NULL
  AND p.code = 'PERM_ROLE_READ';

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_rbac_user_role_manager_id, p.id
FROM permissions p
WHERE @role_rbac_user_role_manager_id IS NOT NULL
  AND p.code IN (
      'PERM_USER_READ',
      'PERM_ROLE_READ',
      'PERM_USER_ROLE_CREATE',
      'PERM_USER_ROLE_READ',
      'PERM_USER_ROLE_DELETE'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_inventory_staff_id, p.id
FROM permissions p
WHERE @role_inventory_staff_id IS NOT NULL
  AND (
      (p.action = 'READ' AND p.resource IN (
          'CATEGORY',
          'BUSINESS_PARTNER',
          'WAREHOUSE',
          'LOCATION',
          'UNIT_OF_MEASURE',
          'PRODUCT',
          'BATCH',
          'INVENTORY',
          'PURCHASE_ORDER',
          'PURCHASE_ORDER_LINE',
          'INBOUND_RECEIPT',
          'INBOUND_RECEIPT_LINE',
          'STOCK_TRANSFER',
          'STOCK_MOVEMENT',
          'STOCK_ADJUSTMENT',
          'STORAGE',
          'REPORT_CURRENT_STOCK'
      ))
      OR p.code IN (
          'PERM_INVENTORY_RESERVATION_UPDATE',
          'PERM_INVENTORY_MUTATION_UPDATE',
          'PERM_BATCH_CREATE',
          'PERM_BATCH_UPDATE',
          'PERM_INBOUND_RECEIPT_CREATE',
          'PERM_INBOUND_RECEIPT_UPDATE',
          'PERM_INBOUND_RECEIPT_DELETE',
          'PERM_INBOUND_RECEIPT_LINE_CREATE',
          'PERM_INBOUND_RECEIPT_LINE_UPDATE',
          'PERM_INBOUND_RECEIPT_LINE_DELETE',
          'PERM_STOCK_TRANSFER_CREATE',
          'PERM_STOCK_TRANSFER_UPDATE',
          'PERM_STOCK_ADJUSTMENT_CREATE',
          'PERM_STORAGE_CREATE'
      )
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_procurement_id, p.id
FROM permissions p
WHERE @role_procurement_id IS NOT NULL
  AND (
      (p.action = 'READ' AND p.resource IN (
          'CATEGORY',
          'BUSINESS_PARTNER',
          'WAREHOUSE',
          'LOCATION',
          'UNIT_OF_MEASURE',
          'PRODUCT',
          'BATCH',
          'INVENTORY',
          'PURCHASE_ORDER',
          'PURCHASE_ORDER_LINE',
          'INBOUND_RECEIPT',
          'INBOUND_RECEIPT_LINE',
          'STORAGE',
          'REPORT_CURRENT_STOCK'
      ))
      OR p.code IN (
          'PERM_PURCHASE_ORDER_CREATE',
          'PERM_PURCHASE_ORDER_UPDATE',
          'PERM_PURCHASE_ORDER_DELETE',
          'PERM_PURCHASE_ORDER_LINE_CREATE',
          'PERM_PURCHASE_ORDER_LINE_UPDATE',
          'PERM_PURCHASE_ORDER_LINE_DELETE',
          'PERM_STORAGE_CREATE'
      )
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT @role_sales_id, p.id
FROM permissions p
WHERE @role_sales_id IS NOT NULL
  AND (
      (p.action = 'READ' AND p.resource IN (
          'CATEGORY',
          'BUSINESS_PARTNER',
          'WAREHOUSE',
          'LOCATION',
          'UNIT_OF_MEASURE',
          'PRODUCT',
          'BATCH',
          'INVENTORY',
          'SALES_ORDER',
          'SALES_ORDER_LINE',
          'OUTBOUND_SHIPMENT',
          'OUTBOUND_SHIPMENT_LINE',
          'STOCK_MOVEMENT',
          'STORAGE',
          'REPORT_CURRENT_STOCK'
      ))
      OR p.code IN (
          'PERM_SALES_ORDER_CREATE',
          'PERM_SALES_ORDER_UPDATE',
          'PERM_SALES_ORDER_LINE_CREATE',
          'PERM_SALES_ORDER_LINE_UPDATE',
          'PERM_OUTBOUND_SHIPMENT_CREATE',
          'PERM_OUTBOUND_SHIPMENT_UPDATE',
          'PERM_OUTBOUND_SHIPMENT_LINE_CREATE',
          'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE',
          'PERM_OUTBOUND_SHIPMENT_LINE_DELETE',
          'PERM_STORAGE_CREATE'
      )
  );

DELETE FROM accounts
WHERE username IN (
    'manager',
    'user',
    'rbac.admin',
    'rbac.permission.viewer',
    'rbac.role.viewer',
    'rbac.user-role.manager',
    'inventory.staff',
    'procurement.staff',
    'sales.staff',
    'read.only'
);

UPDATE accounts
SET
    password = @seed_password,
    status = 'ACTIVE',
    updated_by = @rbac_seed_actor
WHERE username IN (
    'admin',
    'dev_seed_admin',
    'emp.dev01',
    'emp.dev02',
    'emp.dev03',
    'emp.dev04',
    'emp.dev05',
    'cust.dev01',
    'cust.dev02',
    'cust.dev03',
    'cust.dev04',
    'cust.dev05'
);

INSERT INTO accounts (id, username, password, status, created_by, updated_by)
SELECT 'B1000000-0000-0000-0000-000000000001', 'admin', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'admin');

INSERT INTO accounts (id, username, password, status, created_by, updated_by)
SELECT 'B1000000-0000-0000-0000-000000000002', 'dev_seed_admin', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE username = 'dev_seed_admin');

INSERT INTO accounts (id, username, password, status, created_by, updated_by)
VALUES
    ('B1000000-0000-0000-0000-000000000003', 'manager', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000004', 'user', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000005', 'rbac.admin', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000006', 'rbac.permission.viewer', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000007', 'rbac.role.viewer', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000008', 'rbac.user-role.manager', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000009', 'inventory.staff', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000010', 'procurement.staff', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000011', 'sales.staff', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor),
    ('B1000000-0000-0000-0000-000000000012', 'read.only', @seed_password, 'ACTIVE', @rbac_seed_actor, @rbac_seed_actor);

UPDATE user_profiles up
JOIN accounts a ON a.id = up.account_id
JOIN tmp_rbac_seed_profiles sp ON sp.username = a.username
SET
    up.first_name = sp.first_name,
    up.last_name = sp.last_name,
    up.email = sp.email,
    up.phone_number = sp.phone_number,
    up.address = sp.address,
    up.updated_by = @rbac_seed_actor;

INSERT INTO user_profiles (
    id,
    account_id,
    first_name,
    last_name,
    email,
    phone_number,
    address,
    created_by,
    updated_by
)
SELECT
    UUID(),
    a.id,
    sp.first_name,
    sp.last_name,
    sp.email,
    sp.phone_number,
    sp.address,
    @rbac_seed_actor,
    @rbac_seed_actor
FROM tmp_rbac_seed_profiles sp
JOIN accounts a ON a.username = sp.username
LEFT JOIN user_profiles up ON up.account_id = a.id
WHERE up.account_id IS NULL;

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, r.id
FROM tmp_rbac_seed_role_assignments s
JOIN accounts a ON a.username = s.username
JOIN roles r ON r.code = s.role_code;

INSERT INTO account_roles (account_id, role_id)
SELECT a.id, @role_user_id
FROM accounts a
LEFT JOIN account_roles ar ON ar.account_id = a.id
WHERE @role_user_id IS NOT NULL
  AND a.status <> 'DELETED'
  AND ar.account_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_seed_role_assignments;
DROP TEMPORARY TABLE IF EXISTS tmp_rbac_seed_profiles;
DROP TEMPORARY TABLE IF EXISTS tmp_rbac_permission_blueprint;
