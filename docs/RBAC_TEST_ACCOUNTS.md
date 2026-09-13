# RBAC Test Accounts

Password for every seeded account below: `123456789`

Source of truth:
- Migration: [V20260403_01__reset_rbac_test_seed.sql](/d:/BTL/WareHouseSystem-BE/src/main/resources/db/migration/V20260403_01__reset_rbac_test_seed.sql)
- Canonical permission catalog: [V20260403_01__reset_rbac_test_seed.sql](/d:/BTL/WareHouseSystem-BE/src/main/resources/db/migration/V20260403_01__reset_rbac_test_seed.sql#L27)

## Managed Test Accounts

These accounts are managed directly by the RBAC test-seed migration.

| Username | Role | Purpose |
|---|---|---|
| `admin` | `ROLE_ADMIN` | Full access for end-to-end verification across business modules, RBAC, and UI gating. |
| `dev_seed_admin` | `ROLE_ADMIN` | Legacy-compatible admin account kept for older dev data references. |
| `manager` | `ROLE_MANAGER` | Broad operational access with RBAC read-only visibility. |
| `user` | `ROLE_USER` | Minimal baseline account. Use to verify hidden menus and denied actions. |
| `read.only` | `ROLE_READ_ONLY` | Cross-module read-only account for UI visibility testing without write access. |
| `rbac.admin` | `ROLE_RBAC_ADMIN` | Dedicated RBAC management account without broad business permissions. |
| `rbac.permission.viewer` | `ROLE_RBAC_PERMISSION_VIEWER` | Permission-catalog-only account. |
| `rbac.role.viewer` | `ROLE_RBAC_ROLE_VIEWER` | Role-catalog-only account. |
| `rbac.user-role.manager` | `ROLE_RBAC_USER_ROLE_MANAGER` | User-role assignment account. |
| `inventory.staff` | `ROLE_INVENTORY_STAFF` | Inventory, inbound, stock adjustment, stock transfer, and batch testing. |
| `procurement.staff` | `ROLE_PROCUREMENT` | Purchase order and inbound-oriented testing. |
| `sales.staff` | `ROLE_SALES` | Sales order and outbound-oriented testing. |

## Legacy Accounts Kept For Domain Seed Data

These accounts are not hard-deleted because older `employees` and `customers` seed data still reference them.

| Username | Role after RBAC reset |
|---|---|
| `emp.dev01` | `ROLE_INVENTORY_STAFF` |
| `emp.dev02` | `ROLE_PROCUREMENT` |
| `emp.dev03` | `ROLE_SALES` |
| `emp.dev04` | `ROLE_MANAGER` |
| `emp.dev05` | `ROLE_USER` |
| `cust.dev01` to `cust.dev05` | `ROLE_USER` |

## Recommended FE Test Matrix

| Scenario | Recommended account |
|---|---|
| Verify everything is visible and editable | `admin` |
| Verify RBAC management only | `rbac.admin` |
| Verify only permission catalog tab is visible | `rbac.permission.viewer` |
| Verify only role catalog tab is visible | `rbac.role.viewer` |
| Verify user-role assignment UI only | `rbac.user-role.manager` |
| Verify broad operations with RBAC read-only behavior | `manager` |
| Verify strict hidden-menu baseline | `user` |
| Verify read-only business visibility | `read.only` |
| Verify inventory-focused UI | `inventory.staff` |
| Verify procurement-focused UI | `procurement.staff` |
| Verify sales-focused UI | `sales.staff` |

## Important Notes

- `ROLE_USER` is intentionally minimal. It is not the read-only business persona.
- `ROLE_READ_ONLY` is the correct account to test broad read-only UI visibility.
- RBAC core data is reset by the migration: `roles`, `permissions`, `role_permissions`, `account_roles`.
- Managed test accounts are recreated for deterministic testing, except `admin` and `dev_seed_admin`, which are updated in place when they already exist to avoid breaking legacy domain references.
- Canonical RBAC resource format is `UPPER_SNAKE_CASE`, for example `PURCHASE_ORDER`, `UNIT_OF_MEASURE`, `STOCK_ADJUSTMENT`.
