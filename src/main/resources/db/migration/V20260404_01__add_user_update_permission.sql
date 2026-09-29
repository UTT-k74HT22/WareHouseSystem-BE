-- ============================================================================
-- Flyway Migration: Add PERM_USER_UPDATE for user status/password management
-- Description:
--   - upsert PERM_USER_UPDATE permission used by user management endpoints
--   - grant it to the ADMIN system role
-- ============================================================================

INSERT INTO permissions (id, code, name, resource, action, description)
SELECT UUID(), 'PERM_USER_UPDATE', 'user.update', 'USER', 'UPDATE', 'Update user status and reset password'
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions p
    WHERE p.code = 'PERM_USER_UPDATE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'PERM_USER_UPDATE'
WHERE (r.code = 'ROLE_ADMIN' OR r.name = 'ADMIN')
AND NOT EXISTS (
    SELECT 1
    FROM role_permissions rp
    WHERE rp.role_id = r.id
      AND rp.permission_id = p.id
);
