-- ============================================================================
-- Flyway Migration: Reset RBAC permissions from endpoint permission blueprint
-- Version: V20260328_01
-- Description:
--   - remove legacy permission grants and permission rows
--   - seed the new permission catalog
--   - grant all permissions to ROLE_ADMIN / ADMIN
-- Notes:
--   - account_roles and roles are preserved
--   - non-admin roles will have no permissions after this migration
-- ============================================================================

-- Ensure admin role exists before re-granting permissions.
INSERT INTO roles (id, code, name, description, is_default)
SELECT UUID(), 'ROLE_ADMIN', 'ADMIN', 'System administrator with full RBAC access', FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE code = 'ROLE_ADMIN' OR name = 'ADMIN'
);

SET @admin_role_id := COALESCE(
    (SELECT id FROM roles WHERE code = 'ROLE_ADMIN' LIMIT 1),
    (SELECT id FROM roles WHERE name = 'ADMIN' LIMIT 1)
);

-- Remove all existing grants and permission rows so the new catalog is the only source of truth.
DELETE FROM role_permissions;
DELETE FROM permissions;

INSERT INTO permissions (id, code, name, resource, action, description)
VALUES
    (UUID(), 'PERM_SYSTEM_DIAGNOSTIC_READ', 'system_diagnostic.read', 'SYSTEM_DIAGNOSTIC', 'READ', 'Read diagnostic endpoints'),
    (UUID(), 'PERM_PERMISSION_CREATE', 'permission.create', 'PERMISSION', 'CREATE', 'Create permission'),
    (UUID(), 'PERM_PERMISSION_READ', 'permission.read', 'PERMISSION', 'READ', 'Read permission'),
    (UUID(), 'PERM_PERMISSION_UPDATE', 'permission.update', 'PERMISSION', 'UPDATE', 'Update permission'),
    (UUID(), 'PERM_PERMISSION_DELETE', 'permission.delete', 'PERMISSION', 'DELETE', 'Delete permission'),
    (UUID(), 'PERM_ROLE_CREATE', 'role.create', 'ROLE', 'CREATE', 'Create role'),
    (UUID(), 'PERM_ROLE_READ', 'role.read', 'ROLE', 'READ', 'Read role'),
    (UUID(), 'PERM_ROLE_UPDATE', 'role.update', 'ROLE', 'UPDATE', 'Update role'),
    (UUID(), 'PERM_ROLE_DELETE', 'role.delete', 'ROLE', 'DELETE', 'Delete role'),
    (UUID(), 'PERM_ROLE_PERMISSION_CREATE', 'role_permission.create', 'ROLE_PERMISSION', 'CREATE', 'Assign permissions to role'),
    (UUID(), 'PERM_ROLE_PERMISSION_READ', 'role_permission.read', 'ROLE_PERMISSION', 'READ', 'Read role permissions'),
    (UUID(), 'PERM_ROLE_PERMISSION_DELETE', 'role_permission.delete', 'ROLE_PERMISSION', 'DELETE', 'Remove permission from role'),
    (UUID(), 'PERM_USER_ROLE_CREATE', 'user_role.create', 'USER_ROLE', 'CREATE', 'Assign role to user'),
    (UUID(), 'PERM_USER_ROLE_READ', 'user_role.read', 'USER_ROLE', 'READ', 'Read user roles'),
    (UUID(), 'PERM_USER_ROLE_DELETE', 'user_role.delete', 'USER_ROLE', 'DELETE', 'Remove role from user'),
    (UUID(), 'PERM_USER_READ', 'user.read', 'USER', 'READ', 'Read user data'),
    (UUID(), 'PERM_EMPLOYEE_CREATE', 'employee.create', 'EMPLOYEE', 'CREATE', 'Create employee'),
    (UUID(), 'PERM_EMPLOYEE_READ', 'employee.read', 'EMPLOYEE', 'READ', 'Read employee'),
    (UUID(), 'PERM_EMPLOYEE_UPDATE', 'employee.update', 'EMPLOYEE', 'UPDATE', 'Update employee'),
    (UUID(), 'PERM_EMPLOYEE_DELETE', 'employee.delete', 'EMPLOYEE', 'DELETE', 'Delete employee'),
    (UUID(), 'PERM_EMAIL_CREATE', 'email.create', 'EMAIL', 'CREATE', 'Send email'),
    (UUID(), 'PERM_EMAIL_READ', 'email.read', 'EMAIL', 'READ', 'Read email logs and statistics'),
    (UUID(), 'PERM_EMAIL_UPDATE', 'email.update', 'EMAIL', 'UPDATE', 'Retry or process email jobs'),
    (UUID(), 'PERM_CATEGORY_CREATE', 'category.create', 'CATEGORY', 'CREATE', 'Create category'),
    (UUID(), 'PERM_CATEGORY_READ', 'category.read', 'CATEGORY', 'READ', 'Read category'),
    (UUID(), 'PERM_CATEGORY_UPDATE', 'category.update', 'CATEGORY', 'UPDATE', 'Update category'),
    (UUID(), 'PERM_BUSINESS_PARTNER_CREATE', 'business_partner.create', 'BUSINESS_PARTNER', 'CREATE', 'Create business partner'),
    (UUID(), 'PERM_BUSINESS_PARTNER_READ', 'business_partner.read', 'BUSINESS_PARTNER', 'READ', 'Read business partner'),
    (UUID(), 'PERM_BUSINESS_PARTNER_UPDATE', 'business_partner.update', 'BUSINESS_PARTNER', 'UPDATE', 'Update business partner'),
    (UUID(), 'PERM_BUSINESS_PARTNER_DELETE', 'business_partner.delete', 'BUSINESS_PARTNER', 'DELETE', 'Delete business partner'),
    (UUID(), 'PERM_WAREHOUSE_CREATE', 'warehouse.create', 'WAREHOUSE', 'CREATE', 'Create warehouse'),
    (UUID(), 'PERM_WAREHOUSE_READ', 'warehouse.read', 'WAREHOUSE', 'READ', 'Read warehouse'),
    (UUID(), 'PERM_WAREHOUSE_UPDATE', 'warehouse.update', 'WAREHOUSE', 'UPDATE', 'Update warehouse'),
    (UUID(), 'PERM_WAREHOUSE_DELETE', 'warehouse.delete', 'WAREHOUSE', 'DELETE', 'Delete warehouse'),
    (UUID(), 'PERM_LOCATION_CREATE', 'location.create', 'LOCATION', 'CREATE', 'Create location'),
    (UUID(), 'PERM_LOCATION_READ', 'location.read', 'LOCATION', 'READ', 'Read location'),
    (UUID(), 'PERM_LOCATION_UPDATE', 'location.update', 'LOCATION', 'UPDATE', 'Update location'),
    (UUID(), 'PERM_LOCATION_DELETE', 'location.delete', 'LOCATION', 'DELETE', 'Delete location'),
    (UUID(), 'PERM_UNIT_OF_MEASURE_CREATE', 'unit_of_measure.create', 'UNIT_OF_MEASURE', 'CREATE', 'Create unit of measure'),
    (UUID(), 'PERM_UNIT_OF_MEASURE_READ', 'unit_of_measure.read', 'UNIT_OF_MEASURE', 'READ', 'Read unit of measure'),
    (UUID(), 'PERM_UNIT_OF_MEASURE_UPDATE', 'unit_of_measure.update', 'UNIT_OF_MEASURE', 'UPDATE', 'Update unit of measure'),
    (UUID(), 'PERM_UNIT_OF_MEASURE_DELETE', 'unit_of_measure.delete', 'UNIT_OF_MEASURE', 'DELETE', 'Delete unit of measure'),
    (UUID(), 'PERM_PRODUCT_CREATE', 'product.create', 'PRODUCT', 'CREATE', 'Create product'),
    (UUID(), 'PERM_PRODUCT_READ', 'product.read', 'PRODUCT', 'READ', 'Read product'),
    (UUID(), 'PERM_PRODUCT_UPDATE', 'product.update', 'PRODUCT', 'UPDATE', 'Update product'),
    (UUID(), 'PERM_PRODUCT_DELETE', 'product.delete', 'PRODUCT', 'DELETE', 'Delete product'),
    (UUID(), 'PERM_INVENTORY_READ', 'inventory.read', 'INVENTORY', 'READ', 'Read inventory'),
    (UUID(), 'PERM_INVENTORY_RESERVATION_UPDATE', 'inventory_reservation.update', 'INVENTORY_RESERVATION', 'UPDATE', 'Reserve and unreserve inventory'),
    (UUID(), 'PERM_INVENTORY_MUTATION_UPDATE', 'inventory_mutation.update', 'INVENTORY_MUTATION', 'UPDATE', 'Increase or decrease inventory'),
    (UUID(), 'PERM_BATCH_CREATE', 'batch.create', 'BATCH', 'CREATE', 'Create batch'),
    (UUID(), 'PERM_BATCH_READ', 'batch.read', 'BATCH', 'READ', 'Read batch'),
    (UUID(), 'PERM_BATCH_UPDATE', 'batch.update', 'BATCH', 'UPDATE', 'Update batch'),
    (UUID(), 'PERM_PURCHASE_ORDER_CREATE', 'purchase_order.create', 'PURCHASE_ORDER', 'CREATE', 'Create purchase order'),
    (UUID(), 'PERM_PURCHASE_ORDER_READ', 'purchase_order.read', 'PURCHASE_ORDER', 'READ', 'Read purchase order'),
    (UUID(), 'PERM_PURCHASE_ORDER_UPDATE', 'purchase_order.update', 'PURCHASE_ORDER', 'UPDATE', 'Update purchase order'),
    (UUID(), 'PERM_PURCHASE_ORDER_DELETE', 'purchase_order.delete', 'PURCHASE_ORDER', 'DELETE', 'Delete purchase order'),
    (UUID(), 'PERM_PURCHASE_ORDER_LINE_CREATE', 'purchase_order_line.create', 'PURCHASE_ORDER_LINE', 'CREATE', 'Create purchase order line'),
    (UUID(), 'PERM_PURCHASE_ORDER_LINE_READ', 'purchase_order_line.read', 'PURCHASE_ORDER_LINE', 'READ', 'Read purchase order line'),
    (UUID(), 'PERM_PURCHASE_ORDER_LINE_UPDATE', 'purchase_order_line.update', 'PURCHASE_ORDER_LINE', 'UPDATE', 'Update purchase order line'),
    (UUID(), 'PERM_PURCHASE_ORDER_LINE_DELETE', 'purchase_order_line.delete', 'PURCHASE_ORDER_LINE', 'DELETE', 'Delete purchase order line'),
    (UUID(), 'PERM_INBOUND_RECEIPT_CREATE', 'inbound_receipt.create', 'INBOUND_RECEIPT', 'CREATE', 'Create inbound receipt'),
    (UUID(), 'PERM_INBOUND_RECEIPT_READ', 'inbound_receipt.read', 'INBOUND_RECEIPT', 'READ', 'Read inbound receipt'),
    (UUID(), 'PERM_INBOUND_RECEIPT_UPDATE', 'inbound_receipt.update', 'INBOUND_RECEIPT', 'UPDATE', 'Update inbound receipt'),
    (UUID(), 'PERM_INBOUND_RECEIPT_DELETE', 'inbound_receipt.delete', 'INBOUND_RECEIPT', 'DELETE', 'Delete inbound receipt'),
    (UUID(), 'PERM_INBOUND_RECEIPT_LINE_CREATE', 'inbound_receipt_line.create', 'INBOUND_RECEIPT_LINE', 'CREATE', 'Create inbound receipt line'),
    (UUID(), 'PERM_INBOUND_RECEIPT_LINE_READ', 'inbound_receipt_line.read', 'INBOUND_RECEIPT_LINE', 'READ', 'Read inbound receipt line'),
    (UUID(), 'PERM_INBOUND_RECEIPT_LINE_UPDATE', 'inbound_receipt_line.update', 'INBOUND_RECEIPT_LINE', 'UPDATE', 'Update inbound receipt line'),
    (UUID(), 'PERM_INBOUND_RECEIPT_LINE_DELETE', 'inbound_receipt_line.delete', 'INBOUND_RECEIPT_LINE', 'DELETE', 'Delete inbound receipt line'),
    (UUID(), 'PERM_SALES_ORDER_CREATE', 'sales_order.create', 'SALES_ORDER', 'CREATE', 'Create sales order'),
    (UUID(), 'PERM_SALES_ORDER_READ', 'sales_order.read', 'SALES_ORDER', 'READ', 'Read sales order'),
    (UUID(), 'PERM_SALES_ORDER_UPDATE', 'sales_order.update', 'SALES_ORDER', 'UPDATE', 'Update sales order'),
    (UUID(), 'PERM_SALES_ORDER_LINE_CREATE', 'sales_order_line.create', 'SALES_ORDER_LINE', 'CREATE', 'Create sales order line'),
    (UUID(), 'PERM_SALES_ORDER_LINE_READ', 'sales_order_line.read', 'SALES_ORDER_LINE', 'READ', 'Read sales order line'),
    (UUID(), 'PERM_SALES_ORDER_LINE_UPDATE', 'sales_order_line.update', 'SALES_ORDER_LINE', 'UPDATE', 'Update sales order line'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_CREATE', 'outbound_shipment.create', 'OUTBOUND_SHIPMENT', 'CREATE', 'Create outbound shipment'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_READ', 'outbound_shipment.read', 'OUTBOUND_SHIPMENT', 'READ', 'Read outbound shipment'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_UPDATE', 'outbound_shipment.update', 'OUTBOUND_SHIPMENT', 'UPDATE', 'Update outbound shipment'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_LINE_CREATE', 'outbound_shipment_line.create', 'OUTBOUND_SHIPMENT_LINE', 'CREATE', 'Create outbound shipment line'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_LINE_READ', 'outbound_shipment_line.read', 'OUTBOUND_SHIPMENT_LINE', 'READ', 'Read outbound shipment line'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE', 'outbound_shipment_line.update', 'OUTBOUND_SHIPMENT_LINE', 'UPDATE', 'Update outbound shipment line'),
    (UUID(), 'PERM_OUTBOUND_SHIPMENT_LINE_DELETE', 'outbound_shipment_line.delete', 'OUTBOUND_SHIPMENT_LINE', 'DELETE', 'Delete outbound shipment line'),
    (UUID(), 'PERM_STOCK_TRANSFER_CREATE', 'stock_transfer.create', 'STOCK_TRANSFER', 'CREATE', 'Create stock transfer'),
    (UUID(), 'PERM_STOCK_TRANSFER_READ', 'stock_transfer.read', 'STOCK_TRANSFER', 'READ', 'Read stock transfer'),
    (UUID(), 'PERM_STOCK_TRANSFER_UPDATE', 'stock_transfer.update', 'STOCK_TRANSFER', 'UPDATE', 'Update stock transfer'),
    (UUID(), 'PERM_STOCK_MOVEMENT_READ', 'stock_movement.read', 'STOCK_MOVEMENT', 'READ', 'Read stock movement'),
    (UUID(), 'PERM_STOCK_ADJUSTMENT_CREATE', 'stock_adjustment.create', 'STOCK_ADJUSTMENT', 'CREATE', 'Create stock adjustment'),
    (UUID(), 'PERM_STOCK_ADJUSTMENT_READ', 'stock_adjustment.read', 'STOCK_ADJUSTMENT', 'READ', 'Read stock adjustment'),
    (UUID(), 'PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE', 'stock_adjustment_approval.update', 'STOCK_ADJUSTMENT_APPROVAL', 'UPDATE', 'Approve or reject stock adjustment'),
    (UUID(), 'PERM_STORAGE_CREATE', 'storage.create', 'STORAGE', 'CREATE', 'Upload file'),
    (UUID(), 'PERM_STORAGE_READ', 'storage.read', 'STORAGE', 'READ', 'Read storage metadata'),
    (UUID(), 'PERM_STORAGE_DELETE', 'storage.delete', 'STORAGE', 'DELETE', 'Delete file'),
    (UUID(), 'PERM_REPORT_CURRENT_STOCK_READ', 'report_current_stock.read', 'REPORT_CURRENT_STOCK', 'READ', 'Export current stock report');

-- Grant full permission catalog to admin.
INSERT INTO role_permissions (role_id, permission_id)
SELECT @admin_role_id, p.id
FROM permissions p
WHERE @admin_role_id IS NOT NULL;
