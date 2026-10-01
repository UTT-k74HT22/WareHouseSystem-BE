-- Business permissions replacing runtime role checks and split technical permissions.
INSERT INTO permissions (id, code, name, resource, action, description)
SELECT UUID(), definitions.code, definitions.name, definitions.resource,
       definitions.action, definitions.description
FROM (
    SELECT 'PERM_WAREHOUSE_GLOBAL_ACCESS' AS code, 'warehouse.global.access' AS name,
           'WAREHOUSE_GLOBAL' AS resource, 'READ' AS action,
           'Access warehouses outside the assigned warehouse; module permissions still required' AS description
    UNION ALL
    SELECT 'PERM_STOCK_ADJUSTMENT_AUTO_APPROVAL_UPDATE', 'stock_adjustment.auto_approval.update',
           'STOCK_ADJUSTMENT_AUTO_APPROVAL', 'UPDATE', 'Apply adjustments immediately regardless of reason or quantity'
    UNION ALL
    SELECT 'PERM_STOCK_ADJUSTMENT_SMALL_AUTO_APPROVAL_UPDATE', 'stock_adjustment.small_auto_approval.update',
           'STOCK_ADJUSTMENT_SMALL_AUTO_APPROVAL', 'UPDATE',
           'Apply non-sensitive adjustments immediately when absolute quantity change is below 5'
    UNION ALL
    SELECT 'PERM_SYSTEM_MONITOR_READ', 'system.monitor.read',
           'SYSTEM_MONITOR', 'READ', 'Read protected system monitoring endpoints'
    UNION ALL
    SELECT 'PERM_USER_ROLE_UPDATE', 'user_role.update',
           'USER_ROLE', 'UPDATE', 'Replace the complete role assignment of a user'
    UNION ALL
    SELECT 'PERM_ROLE_PERMISSION_UPDATE', 'role_permission.update',
           'ROLE_PERMISSION', 'UPDATE', 'Replace the complete permission assignment of a role'
    UNION ALL
    SELECT 'PERM_USER_PASSWORD_RESET_UPDATE', 'user_password_reset.update',
           'USER_PASSWORD_RESET', 'UPDATE', 'Reset another user password'
) definitions
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = definitions.code);

-- Preserve access from old split permissions before removing them.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, replacement.id
FROM role_permissions rp
JOIN permissions old_permission ON old_permission.id = rp.permission_id
JOIN permissions replacement ON replacement.code = CASE
    WHEN old_permission.code IN ('PERM_USER_ROLE_CREATE', 'PERM_USER_ROLE_DELETE') THEN 'PERM_USER_ROLE_UPDATE'
    WHEN old_permission.code IN ('PERM_ROLE_PERMISSION_CREATE', 'PERM_ROLE_PERMISSION_DELETE') THEN 'PERM_ROLE_PERMISSION_UPDATE'
    WHEN old_permission.code = 'PERM_USER_UPDATE' THEN 'PERM_USER_PASSWORD_RESET_UPDATE'
END
WHERE old_permission.code IN (
    'PERM_USER_ROLE_CREATE', 'PERM_USER_ROLE_DELETE',
    'PERM_ROLE_PERMISSION_CREATE', 'PERM_ROLE_PERMISSION_DELETE', 'PERM_USER_UPDATE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions current_mapping
    WHERE current_mapping.role_id = rp.role_id AND current_mapping.permission_id = replacement.id
);

-- A line is part of its parent business document, not a separately assigned capability.
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, replacement.id
FROM role_permissions rp
JOIN permissions old_permission ON old_permission.id = rp.permission_id
JOIN permissions replacement ON replacement.code = CASE old_permission.code
    WHEN 'PERM_PURCHASE_ORDER_LINE_CREATE' THEN 'PERM_PURCHASE_ORDER_UPDATE'
    WHEN 'PERM_PURCHASE_ORDER_LINE_READ' THEN 'PERM_PURCHASE_ORDER_READ'
    WHEN 'PERM_PURCHASE_ORDER_LINE_UPDATE' THEN 'PERM_PURCHASE_ORDER_UPDATE'
    WHEN 'PERM_PURCHASE_ORDER_LINE_DELETE' THEN 'PERM_PURCHASE_ORDER_UPDATE'
    WHEN 'PERM_SALES_ORDER_LINE_CREATE' THEN 'PERM_SALES_ORDER_UPDATE'
    WHEN 'PERM_SALES_ORDER_LINE_READ' THEN 'PERM_SALES_ORDER_READ'
    WHEN 'PERM_SALES_ORDER_LINE_UPDATE' THEN 'PERM_SALES_ORDER_UPDATE'
    WHEN 'PERM_SALES_ORDER_LINE_DELETE' THEN 'PERM_SALES_ORDER_UPDATE'
    WHEN 'PERM_INBOUND_RECEIPT_LINE_CREATE' THEN 'PERM_INBOUND_RECEIPT_UPDATE'
    WHEN 'PERM_INBOUND_RECEIPT_LINE_READ' THEN 'PERM_INBOUND_RECEIPT_READ'
    WHEN 'PERM_INBOUND_RECEIPT_LINE_UPDATE' THEN 'PERM_INBOUND_RECEIPT_UPDATE'
    WHEN 'PERM_INBOUND_RECEIPT_LINE_DELETE' THEN 'PERM_INBOUND_RECEIPT_UPDATE'
    WHEN 'PERM_OUTBOUND_SHIPMENT_LINE_CREATE' THEN 'PERM_OUTBOUND_SHIPMENT_UPDATE'
    WHEN 'PERM_OUTBOUND_SHIPMENT_LINE_READ' THEN 'PERM_OUTBOUND_SHIPMENT_READ'
    WHEN 'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE' THEN 'PERM_OUTBOUND_SHIPMENT_UPDATE'
    WHEN 'PERM_OUTBOUND_SHIPMENT_LINE_DELETE' THEN 'PERM_OUTBOUND_SHIPMENT_UPDATE'
END
WHERE old_permission.code IN (
    'PERM_PURCHASE_ORDER_LINE_CREATE', 'PERM_PURCHASE_ORDER_LINE_READ',
    'PERM_PURCHASE_ORDER_LINE_UPDATE', 'PERM_PURCHASE_ORDER_LINE_DELETE',
    'PERM_SALES_ORDER_LINE_CREATE', 'PERM_SALES_ORDER_LINE_READ',
    'PERM_SALES_ORDER_LINE_UPDATE', 'PERM_SALES_ORDER_LINE_DELETE',
    'PERM_INBOUND_RECEIPT_LINE_CREATE', 'PERM_INBOUND_RECEIPT_LINE_READ',
    'PERM_INBOUND_RECEIPT_LINE_UPDATE', 'PERM_INBOUND_RECEIPT_LINE_DELETE',
    'PERM_OUTBOUND_SHIPMENT_LINE_CREATE', 'PERM_OUTBOUND_SHIPMENT_LINE_READ',
    'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE', 'PERM_OUTBOUND_SHIPMENT_LINE_DELETE'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions current_mapping
    WHERE current_mapping.role_id = rp.role_id AND current_mapping.permission_id = replacement.id
);

-- Preserve privileges that were previously hard-coded by role name.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'PERM_WAREHOUSE_GLOBAL_ACCESS',
    'PERM_STOCK_ADJUSTMENT_AUTO_APPROVAL_UPDATE',
    'PERM_STOCK_ADJUSTMENT_SMALL_AUTO_APPROVAL_UPDATE',
    'PERM_SYSTEM_MONITOR_READ',
    'PERM_USER_ROLE_UPDATE',
    'PERM_ROLE_PERMISSION_UPDATE',
    'PERM_USER_PASSWORD_RESET_UPDATE'
)
WHERE (r.code = 'ROLE_ADMIN' OR r.name = 'ADMIN')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PERM_STOCK_ADJUSTMENT_SMALL_AUTO_APPROVAL_UPDATE'
WHERE (r.code = 'ROLE_MANAGER' OR r.name = 'MANAGER')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

DELETE FROM role_permissions
WHERE permission_id IN (
    SELECT id FROM permissions WHERE code IN (
        'PERM_USER_ROLE_CREATE', 'PERM_USER_ROLE_DELETE',
        'PERM_ROLE_PERMISSION_CREATE', 'PERM_ROLE_PERMISSION_DELETE',
        'PERM_PURCHASE_ORDER_LINE_CREATE', 'PERM_PURCHASE_ORDER_LINE_READ',
        'PERM_PURCHASE_ORDER_LINE_UPDATE', 'PERM_PURCHASE_ORDER_LINE_DELETE',
        'PERM_SALES_ORDER_LINE_CREATE', 'PERM_SALES_ORDER_LINE_READ',
        'PERM_SALES_ORDER_LINE_UPDATE', 'PERM_SALES_ORDER_LINE_DELETE',
        'PERM_INBOUND_RECEIPT_LINE_CREATE', 'PERM_INBOUND_RECEIPT_LINE_READ',
        'PERM_INBOUND_RECEIPT_LINE_UPDATE', 'PERM_INBOUND_RECEIPT_LINE_DELETE',
        'PERM_OUTBOUND_SHIPMENT_LINE_CREATE', 'PERM_OUTBOUND_SHIPMENT_LINE_READ',
        'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE', 'PERM_OUTBOUND_SHIPMENT_LINE_DELETE'
    )
);

DELETE FROM permissions
WHERE code IN (
    'PERM_USER_ROLE_CREATE', 'PERM_USER_ROLE_DELETE',
    'PERM_ROLE_PERMISSION_CREATE', 'PERM_ROLE_PERMISSION_DELETE',
    'PERM_PURCHASE_ORDER_LINE_CREATE', 'PERM_PURCHASE_ORDER_LINE_READ',
    'PERM_PURCHASE_ORDER_LINE_UPDATE', 'PERM_PURCHASE_ORDER_LINE_DELETE',
    'PERM_SALES_ORDER_LINE_CREATE', 'PERM_SALES_ORDER_LINE_READ',
    'PERM_SALES_ORDER_LINE_UPDATE', 'PERM_SALES_ORDER_LINE_DELETE',
    'PERM_INBOUND_RECEIPT_LINE_CREATE', 'PERM_INBOUND_RECEIPT_LINE_READ',
    'PERM_INBOUND_RECEIPT_LINE_UPDATE', 'PERM_INBOUND_RECEIPT_LINE_DELETE',
    'PERM_OUTBOUND_SHIPMENT_LINE_CREATE', 'PERM_OUTBOUND_SHIPMENT_LINE_READ',
    'PERM_OUTBOUND_SHIPMENT_LINE_UPDATE', 'PERM_OUTBOUND_SHIPMENT_LINE_DELETE'
);
