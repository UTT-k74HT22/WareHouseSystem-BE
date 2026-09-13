# API Inventory Comprehensive

Last updated: `2026-04-03`

This file is the current high-level API inventory for the backend.

Source of truth:
- Controllers under [src/main/java/org/demo/whs/controller](/d:/BTL/WareHouseSystem-BE/src/main/java/org/demo/whs/controller)
- RBAC seed and permission catalog in [V20260403_01__reset_rbac_test_seed.sql](/d:/BTL/WareHouseSystem-BE/src/main/resources/db/migration/V20260403_01__reset_rbac_test_seed.sql)

## Current Module Status

| Module | Base path | Current status | Notes |
|---|---|---|---|
| Auth and session | `/api/v1/auth` | Implemented | Login, refresh-token, register, forgot-password, verify-forgot-password-otp, reset-password, change-password, check-permission, my-permissions |
| OTP | `/api/v1/otp` | Implemented | Send and verify OTP |
| Users | `/api/v1/users` | Implemented | Read-side user APIs and manager lookup |
| RBAC permissions | `/api/v1/permissions` | Implemented | CRUD, paged list, detail, and `GET /resources` |
| RBAC roles | `/api/v1/roles` | Implemented | CRUD, role users, role-permission assignment/read/remove |
| User-role assignment | `/api/v1/users/{userId}/roles` | Implemented | Assign, remove, and list user roles |
| Dashboard | `/api/v1/dashboard` | Implemented | Dashboard snapshot endpoint exists, auth model is authenticated-only |
| Background jobs | `/api/v1/jobs` | Implemented | My jobs, detail, status, retry, cancel, download |
| Reporting | `/api/v1/reports` | Partially implemented | Current stock PDF export exists |
| Warehouse | `/api/v1/warehouse` | Implemented | CRUD, list-all, status change |
| Location | `/api/v1/locations` | Implemented | CRUD, search, by-warehouse, status change |
| Product | `/api/v1/products` | Implemented | CRUD, search, sku lookup, category lookup, batch-tracking |
| Unit of measure | `/api/v1/units-of-measure` | Implemented | CRUD |
| Business partner | `/api/v1/business-partners` | Implemented | CRUD, search, status change |
| Category | `/api/v1/categories` | Implemented | CRUD and status change |
| Batch | `/api/v1/batches` | Implemented | CRUD, traceability, expiring, fifo recommendations, status workflows |
| Inventory | `/api/v1/inventories` | Implemented | List, summary, by-location, availability, reserve, unreserve, increase, decrease |
| Stock adjustments | `/api/v1/stock-adjustments` | Implemented | Create, read, approve, reject |
| Stock transfers | `/api/v1/stock-transfers` | Implemented | Create, read, update, complete, cancel |
| Purchase orders | `/api/v1/purchase-orders` | Implemented | CRUD and confirm |
| Purchase order lines | `/api/v1/purchase-order-lines` | Implemented | CRUD by purchase order |
| Inbound receipts | `/api/v1/inbound-receipts` | Implemented | CRUD, confirm, list by purchase order |
| Inbound receipt lines | `/api/v1/inbound-receipt-lines` | Implemented | CRUD |
| Sales orders | `/api/v1/sales-orders` | Implemented | Create, read, update, confirm, cancel |
| Sales order lines | `/api/v1/sales-order-lines` | Implemented | Create, read by sales order, update |
| Outbound shipments | `/api/v1/outbound-shipments` | Implemented | Create, read, update, start-picking, mark-as-packed, ship, confirm-dispatch, cancel |
| Outbound shipment lines | `/api/v1/outbound-shipment-lines` | Implemented | CRUD |
| Stock movements | `/api/v1/stock-movements` | Partially implemented | List, detail, reference lookup |
| Employee | `/api/v1/employees` | Implemented | CRUD |
| Email | `/api/v1/emails` | Implemented | Send, logs, filters, retry, statistics |
| Storage | `/api/v1/storage` | Implemented | Upload, batch upload, exists, presigned-url, delete |

## Auth and RBAC Reference

### Auth endpoints

| Method | Endpoint | Notes |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Rate-limited login |
| `POST` | `/api/v1/auth/refresh-token` | Refresh access token |
| `POST` | `/api/v1/auth/register` | Register account |
| `POST` | `/api/v1/auth/forgot-password` | Start forgot-password flow |
| `POST` | `/api/v1/auth/verify-forgot-password-otp` | Verify OTP |
| `POST` | `/api/v1/auth/reset-password` | Reset password with session token |
| `POST` | `/api/v1/auth/change-password` | Change password when authenticated |
| `POST` | `/api/v1/auth/check-permission` | Returns `CheckPermissionResponse` |
| `GET` | `/api/v1/auth/my-permissions` | Returns effective permission set for current user |

### RBAC endpoints

| Method | Endpoint | Required permission |
|---|---|---|
| `POST` | `/api/v1/permissions` | `PERM_PERMISSION_CREATE` |
| `GET` | `/api/v1/permissions` | `PERM_PERMISSION_READ` |
| `GET` | `/api/v1/permissions/resources` | `PERM_PERMISSION_READ` |
| `GET` | `/api/v1/permissions/{id}` | `PERM_PERMISSION_READ` |
| `PUT` | `/api/v1/permissions/{id}` | `PERM_PERMISSION_UPDATE` |
| `DELETE` | `/api/v1/permissions/{id}` | `PERM_PERMISSION_DELETE` |
| `POST` | `/api/v1/roles` | `PERM_ROLE_CREATE` |
| `GET` | `/api/v1/roles` | `PERM_ROLE_READ` |
| `GET` | `/api/v1/roles/{id}` | `PERM_ROLE_READ` |
| `PUT` | `/api/v1/roles/{id}` | `PERM_ROLE_UPDATE` |
| `DELETE` | `/api/v1/roles/{id}` | `PERM_ROLE_DELETE` |
| `GET` | `/api/v1/roles/{id}/users` | `PERM_ROLE_READ` |
| `POST` | `/api/v1/roles/{id}/permissions` | `PERM_ROLE_PERMISSION_CREATE` |
| `DELETE` | `/api/v1/roles/{id}/permissions/{permId}` | `PERM_ROLE_PERMISSION_DELETE` |
| `GET` | `/api/v1/roles/{id}/permissions` | `PERM_ROLE_PERMISSION_READ` |
| `POST` | `/api/v1/users/{userId}/roles` | `PERM_USER_ROLE_CREATE` |
| `DELETE` | `/api/v1/users/{userId}/roles/{roleId}` | `PERM_USER_ROLE_DELETE` |
| `GET` | `/api/v1/users/{userId}/roles` | `PERM_USER_ROLE_READ` |

### RBAC implementation notes

- Canonical resource format is `UPPER_SNAKE_CASE`.
- Permission code format is `PERM_<RESOURCE>_<ACTION>`.
- `check-permission` and permission creation reject non-canonical resource values.
- Permission-source failures return `503` with `PERM_013` instead of silently returning an empty permission set.
- The authoritative RBAC test seed is [V20260403_01__reset_rbac_test_seed.sql](/d:/BTL/WareHouseSystem-BE/src/main/resources/db/migration/V20260403_01__reset_rbac_test_seed.sql).
- Test accounts are documented in [RBAC_TEST_ACCOUNTS.md](/d:/BTL/WareHouseSystem-BE/docs/RBAC_TEST_ACCOUNTS.md).

## Dashboard, Jobs, and Reporting

### Dashboard

| Method | Endpoint | Auth model |
|---|---|---|
| `GET` | `/api/v1/dashboard` | Authenticated user |

Current note:
- Dashboard is implemented.
- It does not yet use a dedicated dashboard permission.

### Background jobs

| Method | Endpoint | Auth model |
|---|---|---|
| `GET` | `/api/v1/jobs/my` | Authenticated user, user-scoped data |
| `GET` | `/api/v1/jobs/{jobId}` | Authenticated user, user-scoped data |
| `GET` | `/api/v1/jobs/{jobId}/status` | Authenticated user, user-scoped data |
| `POST` | `/api/v1/jobs/{jobId}/retry` | Authenticated user, user-scoped data |
| `POST` | `/api/v1/jobs/{jobId}/cancel` | Authenticated user, user-scoped data |
| `GET` | `/api/v1/jobs/{jobId}/download` | Authenticated user, user-scoped data |

Current note:
- Job APIs are implemented.
- They are currently protected by authenticated ownership checks, not dedicated RBAC permissions.

### Reporting

| Method | Endpoint | Required permission |
|---|---|---|
| `GET` | `/api/v1/reports/current-stock/export/pdf` | `PERM_REPORT_CURRENT_STOCK_READ` |

Current note:
- Reporting is no longer `0%`.
- Only current-stock PDF export is implemented at this time.

## Known Gaps

- `verify-email` and `resend-verification` are still not implemented under `/api/v1/auth`.
- Dashboard and background-job APIs do not yet have dedicated permission codes.
- Stock movement read-side is only partially implemented compared with the broader roadmap.
- Some older audit/session documents under `docs/` are historical snapshots and should not be treated as the current source of truth.
