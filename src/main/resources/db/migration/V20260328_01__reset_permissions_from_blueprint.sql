-- ============================================================================
-- Flyway Migration: Seed RBAC permission catalog from endpoint blueprint
-- Version: V20260328_01
-- Description:
--   - upsert the canonical permission catalog used by endpoint RBAC
--   - ensure ADMIN / USER / MANAGER system roles exist
--   - reset system-role grants deterministically without deleting custom data
--   - preserve existing custom roles and custom permissions
-- ============================================================================

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

UPDATE permissions p
JOIN tmp_rbac_permission_blueprint bp ON bp.code = p.code
SET
    p.name = bp.name,
    p.resource = bp.resource,
    p.action = bp.action,
    p.description = bp.description;

INSERT INTO permissions (id, code, name, resource, action, description)
SELECT UUID(), bp.code, bp.name, bp.resource, bp.action, bp.description
FROM tmp_rbac_permission_blueprint bp
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions p
    WHERE p.code = bp.code
);

INSERT INTO roles (id, code, name, description, is_default)
SELECT UUID(), 'ROLE_ADMIN', 'ADMIN', 'System administrator with full RBAC access', FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'ROLE_ADMIN' OR name = 'ADMIN'
);

INSERT INTO roles (id, code, name, description, is_default)
SELECT UUID(), 'ROLE_USER', 'USER', 'Standard warehouse user for authenticated business flows', FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'ROLE_USER' OR name = 'USER'
);

INSERT INTO roles (id, code, name, description, is_default)
SELECT UUID(), 'ROLE_MANAGER', 'MANAGER', 'Warehouse manager with operational oversight', FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'ROLE_MANAGER' OR name = 'MANAGER'
);

SET @admin_role_id := COALESCE(
    (SELECT id FROM roles WHERE code = 'ROLE_ADMIN' LIMIT 1),
    (SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1)
);
SET @user_role_id := COALESCE(
    (SELECT id FROM roles WHERE code = 'ROLE_USER' LIMIT 1),
    (SELECT id FROM roles WHERE name = 'USER' LIMIT 1)
);
SET @manager_role_id := COALESCE(
    (SELECT id FROM roles WHERE code = 'ROLE_MANAGER' LIMIT 1),
    (SELECT id FROM roles WHERE name = 'MANAGER' LIMIT 1)
);

SET @has_other_default_role := EXISTS(
    SELECT 1
    FROM roles
    WHERE is_default = TRUE
      AND id <> @user_role_id
);

UPDATE roles
SET
    code = 'ROLE_ADMIN',
    name = 'ADMIN',
    description = 'System administrator with full RBAC access',
    is_default = FALSE
WHERE id = @admin_role_id;

UPDATE roles
SET
    code = 'ROLE_USER',
    name = 'USER',
    description = 'Standard warehouse user for authenticated business flows',
    is_default = IF(@has_other_default_role = 1, is_default, TRUE)
WHERE id = @user_role_id;

UPDATE roles
SET
    code = 'ROLE_MANAGER',
    name = 'MANAGER',
    description = 'Warehouse manager with operational oversight',
    is_default = FALSE
WHERE id = @manager_role_id;

DELETE rp
FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE rp.role_id IN (@admin_role_id, @user_role_id, @manager_role_id)
  AND p.code LIKE 'PERM_%';

INSERT INTO role_permissions (role_id, permission_id)
SELECT @admin_role_id, p.id
FROM permissions p
WHERE @admin_role_id IS NOT NULL;

INSERT INTO role_permissions (role_id, permission_id)
SELECT @user_role_id, p.id
FROM permissions p
WHERE p.code IN (
    'PERM_PERMISSION_READ',
    'PERM_ROLE_READ',
    'PERM_USER_READ',
    'PERM_CATEGORY_READ',
    'PERM_BUSINESS_PARTNER_READ',
    'PERM_WAREHOUSE_CREATE',
    'PERM_WAREHOUSE_READ',
    'PERM_WAREHOUSE_UPDATE',
    'PERM_WAREHOUSE_DELETE',
    'PERM_LOCATION_CREATE',
    'PERM_LOCATION_READ',
    'PERM_LOCATION_UPDATE',
    'PERM_LOCATION_DELETE',
    'PERM_UNIT_OF_MEASURE_CREATE',
    'PERM_UNIT_OF_MEASURE_READ',
    'PERM_UNIT_OF_MEASURE_UPDATE',
    'PERM_UNIT_OF_MEASURE_DELETE',
    'PERM_PRODUCT_CREATE',
    'PERM_PRODUCT_READ',
    'PERM_PRODUCT_UPDATE',
    'PERM_PRODUCT_DELETE',
    'PERM_INVENTORY_READ',
    'PERM_INVENTORY_RESERVATION_UPDATE',
    'PERM_INVENTORY_MUTATION_UPDATE',
    'PERM_BATCH_CREATE',
    'PERM_BATCH_READ',
    'PERM_BATCH_UPDATE',
    'PERM_PURCHASE_ORDER_CREATE',
    'PERM_PURCHASE_ORDER_READ',
    'PERM_PURCHASE_ORDER_UPDATE',
    'PERM_PURCHASE_ORDER_DELETE',
    'PERM_PURCHASE_ORDER_LINE_CREATE',
    'PERM_PURCHASE_ORDER_LINE_READ',
    'PERM_PURCHASE_ORDER_LINE_UPDATE',
    'PERM_PURCHASE_ORDER_LINE_DELETE',
    'PERM_INBOUND_RECEIPT_CREATE',
    'PERM_INBOUND_RECEIPT_READ',
    'PERM_INBOUND_RECEIPT_UPDATE',
    'PERM_INBOUND_RECEIPT_DELETE',
    'PERM_INBOUND_RECEIPT_LINE_CREATE',
    'PERM_INBOUND_RECEIPT_LINE_READ',
    'PERM_INBOUND_RECEIPT_LINE_UPDATE',
    'PERM_INBOUND_RECEIPT_LINE_DELETE',
    'PERM_SALES_ORDER_CREATE',
    'PERM_SALES_ORDER_READ',
    'PERM_SALES_ORDER_UPDATE',
    'PERM_SALES_ORDER_LINE_CREATE',
    'PERM_SALES_ORDER_LINE_READ',
    'PERM_SALES_ORDER_LINE_UPDATE',
    'PERM_STOCK_TRANSFER_CREATE',
    'PERM_STOCK_TRANSFER_READ',
    'PERM_STOCK_TRANSFER_UPDATE',
    'PERM_STOCK_MOVEMENT_READ',
    'PERM_STOCK_ADJUSTMENT_CREATE',
    'PERM_STOCK_ADJUSTMENT_READ',
    'PERM_STORAGE_CREATE',
    'PERM_STORAGE_READ',
    'PERM_REPORT_CURRENT_STOCK_READ'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT @manager_role_id, p.id
FROM permissions p
WHERE p.code IN (
    'PERM_PERMISSION_READ',
    'PERM_ROLE_READ',
    'PERM_USER_READ',
    'PERM_CATEGORY_READ',
    'PERM_BUSINESS_PARTNER_READ',
    'PERM_WAREHOUSE_CREATE',
    'PERM_WAREHOUSE_READ',
    'PERM_WAREHOUSE_UPDATE',
    'PERM_WAREHOUSE_DELETE',
    'PERM_LOCATION_CREATE',
    'PERM_LOCATION_READ',
    'PERM_LOCATION_UPDATE',
    'PERM_LOCATION_DELETE',
    'PERM_UNIT_OF_MEASURE_CREATE',
    'PERM_UNIT_OF_MEASURE_READ',
    'PERM_UNIT_OF_MEASURE_UPDATE',
    'PERM_UNIT_OF_MEASURE_DELETE',
    'PERM_PRODUCT_CREATE',
    'PERM_PRODUCT_READ',
    'PERM_PRODUCT_UPDATE',
    'PERM_PRODUCT_DELETE',
    'PERM_INVENTORY_READ',
    'PERM_INVENTORY_RESERVATION_UPDATE',
    'PERM_INVENTORY_MUTATION_UPDATE',
    'PERM_BATCH_CREATE',
    'PERM_BATCH_READ',
    'PERM_BATCH_UPDATE',
    'PERM_PURCHASE_ORDER_CREATE',
    'PERM_PURCHASE_ORDER_READ',
    'PERM_PURCHASE_ORDER_UPDATE',
    'PERM_PURCHASE_ORDER_DELETE',
    'PERM_PURCHASE_ORDER_LINE_CREATE',
    'PERM_PURCHASE_ORDER_LINE_READ',
    'PERM_PURCHASE_ORDER_LINE_UPDATE',
    'PERM_PURCHASE_ORDER_LINE_DELETE',
    'PERM_INBOUND_RECEIPT_CREATE',
    'PERM_INBOUND_RECEIPT_READ',
    'PERM_INBOUND_RECEIPT_UPDATE',
    'PERM_INBOUND_RECEIPT_DELETE',
    'PERM_INBOUND_RECEIPT_LINE_CREATE',
    'PERM_INBOUND_RECEIPT_LINE_READ',
    'PERM_INBOUND_RECEIPT_LINE_UPDATE',
    'PERM_INBOUND_RECEIPT_LINE_DELETE',
    'PERM_SALES_ORDER_CREATE',
    'PERM_SALES_ORDER_READ',
    'PERM_SALES_ORDER_UPDATE',
    'PERM_SALES_ORDER_LINE_CREATE',
    'PERM_SALES_ORDER_LINE_READ',
    'PERM_SALES_ORDER_LINE_UPDATE',
    'PERM_STOCK_TRANSFER_CREATE',
    'PERM_STOCK_TRANSFER_READ',
    'PERM_STOCK_TRANSFER_UPDATE',
    'PERM_STOCK_MOVEMENT_READ',
    'PERM_STOCK_ADJUSTMENT_CREATE',
    'PERM_STOCK_ADJUSTMENT_READ',
    'PERM_STORAGE_CREATE',
    'PERM_STORAGE_READ',
    'PERM_REPORT_CURRENT_STOCK_READ',
    'PERM_EMPLOYEE_READ',
    'PERM_OUTBOUND_SHIPMENT_CREATE',
    'PERM_OUTBOUND_SHIPMENT_READ',
    'PERM_OUTBOUND_SHIPMENT_UPDATE',
    'PERM_OUTBOUND_SHIPMENT_LINE_CREATE',
    'PERM_OUTBOUND_SHIPMENT_LINE_READ',
    'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE',
    'PERM_OUTBOUND_SHIPMENT_LINE_DELETE',
    'PERM_STORAGE_DELETE'
);

DROP TEMPORARY TABLE IF EXISTS tmp_rbac_permission_blueprint;
