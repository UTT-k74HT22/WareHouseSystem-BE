# RBAC Endpoint Permission Blueprint

## 1. Purpose

This document defines the recommended target-state RBAC model for the current WMS backend.

Goals:
- keep only true public endpoints as `permitAll`
- keep a small set of self-service endpoints as `authenticated`
- move business and administration APIs to permission-based checks using `PERM_<RESOURCE>_<ACTION>`
- allow seeding one `ADMIN` role with all permissions first, then expand other roles later

This blueprint is based on:
- current controllers under `src/main/java/org/demo/whs/controller`
- current enum `ActionType { CREATE, READ, UPDATE, DELETE }`
- current permission code convention in `PermissionServiceImpl` and `AuthServiceImpl`

## 2. Classification Rules

### 2.1 Keep Public

These endpoints should stay `permitAll`:

| Method | Endpoint | Reason |
|---|---|---|
| POST | `/api/v1/auth/login` | Entry point for authentication |
| POST | `/api/v1/auth/register` | Self-registration |
| POST | `/api/v1/auth/refresh-token` | Token renewal |
| POST | `/api/v1/auth/forgot-password` | Start password recovery |
| POST | `/api/v1/auth/verify-forgot-password-otp` | Verify forgot-password OTP |
| POST | `/api/v1/auth/reset-password` | Should be public, but must validate dedicated reset token |
| POST | `/api/v1/otp/send` | Public OTP send flow |
| POST | `/api/v1/otp/verify` | Public OTP verify flow |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`, `/api-docs/**`, `/favicon.ico` | Documentation and tooling |
| GET | `/actuator/health`, `/actuator/info` | Health endpoints |

### 2.2 Keep Authenticated Only

These endpoints should require login, but do not need business permission because they are self-service or already scoped to current user:

| Method | Endpoint | Reason |
|---|---|---|
| POST | `/api/v1/auth/change-password` | Self-service account action |
| POST | `/api/v1/auth/check-permission` | Utility endpoint to check current user's effective permission |
| GET | `/api/v1/home/me` | Current user info |
| GET | `/api/v1/jobs/my` | Current user's own jobs |
| GET | `/api/v1/jobs/{jobId}` | Service already enforces ownership |
| GET | `/api/v1/jobs/{jobId}/status` | Service already enforces ownership |
| POST | `/api/v1/jobs/{jobId}/retry` | Service already enforces ownership |
| POST | `/api/v1/jobs/{jobId}/cancel` | Service already enforces ownership |
| GET | `/api/v1/jobs/{jobId}/download` | Service already enforces ownership |

### 2.3 Do Not Keep as Generic `authenticated`

These endpoints should be moved to permission-based authorization:
- all master-data CRUD
- all inventory and warehouse operations
- all order, receipt, shipment, transfer, adjustment flows
- all RBAC administration APIs
- all email administration APIs
- file storage APIs
- report export APIs

### 2.4 Diagnostic Endpoints

These endpoints should not stay open to all authenticated users:

| Method | Endpoint | Recommendation |
|---|---|---|
| GET | `/api/v1/home/test` | `PERM_SYSTEM_DIAGNOSTIC_READ` or remove from production |
| GET | `/api/v1/warehouse/test` | `PERM_SYSTEM_DIAGNOSTIC_READ` or remove from production |
| GET/POST | `/actuator/**` except health/info | keep `ROLE_ADMIN` or map to ops-only access outside application RBAC |

## 3. Recommended Permission Catalog

Use current code convention:
- `code`: `PERM_<RESOURCE>_<ACTION>`
- `name`: lowercase dot style, for example `role.create`
- `resource`: uppercase snake case
- `action`: one of `CREATE`, `READ`, `UPDATE`, `DELETE`

Seed only permissions that map to current endpoints.

| Code | Name | Resource | Action | Purpose |
|---|---|---|---|---|
| `PERM_SYSTEM_DIAGNOSTIC_READ` | `system_diagnostic.read` | `SYSTEM_DIAGNOSTIC` | `READ` | test/debug endpoints |
| `PERM_PERMISSION_CREATE` | `permission.create` | `PERMISSION` | `CREATE` | create permission |
| `PERM_PERMISSION_READ` | `permission.read` | `PERMISSION` | `READ` | list/detail permission |
| `PERM_PERMISSION_UPDATE` | `permission.update` | `PERMISSION` | `UPDATE` | update permission |
| `PERM_PERMISSION_DELETE` | `permission.delete` | `PERMISSION` | `DELETE` | delete permission |
| `PERM_ROLE_CREATE` | `role.create` | `ROLE` | `CREATE` | create role |
| `PERM_ROLE_READ` | `role.read` | `ROLE` | `READ` | list/detail role, role users |
| `PERM_ROLE_UPDATE` | `role.update` | `ROLE` | `UPDATE` | update role |
| `PERM_ROLE_DELETE` | `role.delete` | `ROLE` | `DELETE` | delete role |
| `PERM_ROLE_PERMISSION_CREATE` | `role_permission.create` | `ROLE_PERMISSION` | `CREATE` | assign permissions to role |
| `PERM_ROLE_PERMISSION_READ` | `role_permission.read` | `ROLE_PERMISSION` | `READ` | view role permissions |
| `PERM_ROLE_PERMISSION_DELETE` | `role_permission.delete` | `ROLE_PERMISSION` | `DELETE` | remove permission from role |
| `PERM_USER_ROLE_CREATE` | `user_role.create` | `USER_ROLE` | `CREATE` | assign role to user |
| `PERM_USER_ROLE_READ` | `user_role.read` | `USER_ROLE` | `READ` | view roles of user |
| `PERM_USER_ROLE_DELETE` | `user_role.delete` | `USER_ROLE` | `DELETE` | remove role from user |
| `PERM_USER_READ` | `user.read` | `USER` | `READ` | read user data |
| `PERM_EMPLOYEE_CREATE` | `employee.create` | `EMPLOYEE` | `CREATE` | create employee |
| `PERM_EMPLOYEE_READ` | `employee.read` | `EMPLOYEE` | `READ` | list/detail employee |
| `PERM_EMPLOYEE_UPDATE` | `employee.update` | `EMPLOYEE` | `UPDATE` | update employee |
| `PERM_EMPLOYEE_DELETE` | `employee.delete` | `EMPLOYEE` | `DELETE` | delete employee |
| `PERM_EMAIL_CREATE` | `email.create` | `EMAIL` | `CREATE` | send email |
| `PERM_EMAIL_READ` | `email.read` | `EMAIL` | `READ` | read logs/statistics |
| `PERM_EMAIL_UPDATE` | `email.update` | `EMAIL` | `UPDATE` | retry/process email jobs |
| `PERM_CATEGORY_CREATE` | `category.create` | `CATEGORY` | `CREATE` | create category |
| `PERM_CATEGORY_READ` | `category.read` | `CATEGORY` | `READ` | list/detail category |
| `PERM_CATEGORY_UPDATE` | `category.update` | `CATEGORY` | `UPDATE` | update/status category |
| `PERM_BUSINESS_PARTNER_CREATE` | `business_partner.create` | `BUSINESS_PARTNER` | `CREATE` | create business partner |
| `PERM_BUSINESS_PARTNER_READ` | `business_partner.read` | `BUSINESS_PARTNER` | `READ` | list/detail/search business partner |
| `PERM_BUSINESS_PARTNER_UPDATE` | `business_partner.update` | `BUSINESS_PARTNER` | `UPDATE` | update/status business partner |
| `PERM_BUSINESS_PARTNER_DELETE` | `business_partner.delete` | `BUSINESS_PARTNER` | `DELETE` | delete business partner |
| `PERM_WAREHOUSE_CREATE` | `warehouse.create` | `WAREHOUSE` | `CREATE` | create warehouse |
| `PERM_WAREHOUSE_READ` | `warehouse.read` | `WAREHOUSE` | `READ` | list/detail warehouse |
| `PERM_WAREHOUSE_UPDATE` | `warehouse.update` | `WAREHOUSE` | `UPDATE` | update/status warehouse |
| `PERM_WAREHOUSE_DELETE` | `warehouse.delete` | `WAREHOUSE` | `DELETE` | delete warehouse |
| `PERM_LOCATION_CREATE` | `location.create` | `LOCATION` | `CREATE` | create location |
| `PERM_LOCATION_READ` | `location.read` | `LOCATION` | `READ` | list/detail/search location |
| `PERM_LOCATION_UPDATE` | `location.update` | `LOCATION` | `UPDATE` | update/status location |
| `PERM_LOCATION_DELETE` | `location.delete` | `LOCATION` | `DELETE` | delete location |
| `PERM_UNIT_OF_MEASURE_CREATE` | `unit_of_measure.create` | `UNIT_OF_MEASURE` | `CREATE` | create unit |
| `PERM_UNIT_OF_MEASURE_READ` | `unit_of_measure.read` | `UNIT_OF_MEASURE` | `READ` | list/detail unit |
| `PERM_UNIT_OF_MEASURE_UPDATE` | `unit_of_measure.update` | `UNIT_OF_MEASURE` | `UPDATE` | update unit |
| `PERM_UNIT_OF_MEASURE_DELETE` | `unit_of_measure.delete` | `UNIT_OF_MEASURE` | `DELETE` | delete unit |
| `PERM_PRODUCT_CREATE` | `product.create` | `PRODUCT` | `CREATE` | create product |
| `PERM_PRODUCT_READ` | `product.read` | `PRODUCT` | `READ` | list/detail/search product |
| `PERM_PRODUCT_UPDATE` | `product.update` | `PRODUCT` | `UPDATE` | update product |
| `PERM_PRODUCT_DELETE` | `product.delete` | `PRODUCT` | `DELETE` | delete product |
| `PERM_INVENTORY_READ` | `inventory.read` | `INVENTORY` | `READ` | inventory queries and availability |
| `PERM_INVENTORY_RESERVATION_UPDATE` | `inventory_reservation.update` | `INVENTORY_RESERVATION` | `UPDATE` | reserve/unreserve inventory |
| `PERM_INVENTORY_MUTATION_UPDATE` | `inventory_mutation.update` | `INVENTORY_MUTATION` | `UPDATE` | direct increase/decrease inventory |
| `PERM_BATCH_CREATE` | `batch.create` | `BATCH` | `CREATE` | create batch |
| `PERM_BATCH_READ` | `batch.read` | `BATCH` | `READ` | batch list/detail/traceability/fifo/expiring |
| `PERM_BATCH_UPDATE` | `batch.update` | `BATCH` | `UPDATE` | update/status/quarantine/release batch |
| `PERM_PURCHASE_ORDER_CREATE` | `purchase_order.create` | `PURCHASE_ORDER` | `CREATE` | create purchase order |
| `PERM_PURCHASE_ORDER_READ` | `purchase_order.read` | `PURCHASE_ORDER` | `READ` | list/detail purchase order |
| `PERM_PURCHASE_ORDER_UPDATE` | `purchase_order.update` | `PURCHASE_ORDER` | `UPDATE` | update/confirm purchase order |
| `PERM_PURCHASE_ORDER_DELETE` | `purchase_order.delete` | `PURCHASE_ORDER` | `DELETE` | delete purchase order |
| `PERM_PURCHASE_ORDER_LINE_CREATE` | `purchase_order_line.create` | `PURCHASE_ORDER_LINE` | `CREATE` | create purchase order line |
| `PERM_PURCHASE_ORDER_LINE_READ` | `purchase_order_line.read` | `PURCHASE_ORDER_LINE` | `READ` | read purchase order lines |
| `PERM_PURCHASE_ORDER_LINE_UPDATE` | `purchase_order_line.update` | `PURCHASE_ORDER_LINE` | `UPDATE` | update purchase order line |
| `PERM_PURCHASE_ORDER_LINE_DELETE` | `purchase_order_line.delete` | `PURCHASE_ORDER_LINE` | `DELETE` | delete purchase order line |
| `PERM_INBOUND_RECEIPT_CREATE` | `inbound_receipt.create` | `INBOUND_RECEIPT` | `CREATE` | create inbound receipt |
| `PERM_INBOUND_RECEIPT_READ` | `inbound_receipt.read` | `INBOUND_RECEIPT` | `READ` | list/detail inbound receipt |
| `PERM_INBOUND_RECEIPT_UPDATE` | `inbound_receipt.update` | `INBOUND_RECEIPT` | `UPDATE` | update/confirm inbound receipt |
| `PERM_INBOUND_RECEIPT_DELETE` | `inbound_receipt.delete` | `INBOUND_RECEIPT` | `DELETE` | delete inbound receipt |
| `PERM_INBOUND_RECEIPT_LINE_CREATE` | `inbound_receipt_line.create` | `INBOUND_RECEIPT_LINE` | `CREATE` | create inbound receipt line |
| `PERM_INBOUND_RECEIPT_LINE_READ` | `inbound_receipt_line.read` | `INBOUND_RECEIPT_LINE` | `READ` | read inbound receipt lines |
| `PERM_INBOUND_RECEIPT_LINE_UPDATE` | `inbound_receipt_line.update` | `INBOUND_RECEIPT_LINE` | `UPDATE` | update inbound receipt line |
| `PERM_INBOUND_RECEIPT_LINE_DELETE` | `inbound_receipt_line.delete` | `INBOUND_RECEIPT_LINE` | `DELETE` | delete inbound receipt line |
| `PERM_SALES_ORDER_CREATE` | `sales_order.create` | `SALES_ORDER` | `CREATE` | create sales order |
| `PERM_SALES_ORDER_READ` | `sales_order.read` | `SALES_ORDER` | `READ` | list/detail sales order |
| `PERM_SALES_ORDER_UPDATE` | `sales_order.update` | `SALES_ORDER` | `UPDATE` | update/confirm/cancel sales order |
| `PERM_SALES_ORDER_LINE_CREATE` | `sales_order_line.create` | `SALES_ORDER_LINE` | `CREATE` | create sales order line |
| `PERM_SALES_ORDER_LINE_READ` | `sales_order_line.read` | `SALES_ORDER_LINE` | `READ` | read sales order lines |
| `PERM_SALES_ORDER_LINE_UPDATE` | `sales_order_line.update` | `SALES_ORDER_LINE` | `UPDATE` | update sales order line |
| `PERM_OUTBOUND_SHIPMENT_CREATE` | `outbound_shipment.create` | `OUTBOUND_SHIPMENT` | `CREATE` | create outbound shipment |
| `PERM_OUTBOUND_SHIPMENT_READ` | `outbound_shipment.read` | `OUTBOUND_SHIPMENT` | `READ` | list/detail outbound shipment |
| `PERM_OUTBOUND_SHIPMENT_UPDATE` | `outbound_shipment.update` | `OUTBOUND_SHIPMENT` | `UPDATE` | update and state transitions on shipment |
| `PERM_OUTBOUND_SHIPMENT_LINE_CREATE` | `outbound_shipment_line.create` | `OUTBOUND_SHIPMENT_LINE` | `CREATE` | create outbound shipment line |
| `PERM_OUTBOUND_SHIPMENT_LINE_READ` | `outbound_shipment_line.read` | `OUTBOUND_SHIPMENT_LINE` | `READ` | read outbound shipment lines |
| `PERM_OUTBOUND_SHIPMENT_LINE_UPDATE` | `outbound_shipment_line.update` | `OUTBOUND_SHIPMENT_LINE` | `UPDATE` | update outbound shipment line |
| `PERM_OUTBOUND_SHIPMENT_LINE_DELETE` | `outbound_shipment_line.delete` | `OUTBOUND_SHIPMENT_LINE` | `DELETE` | delete outbound shipment line |
| `PERM_STOCK_TRANSFER_CREATE` | `stock_transfer.create` | `STOCK_TRANSFER` | `CREATE` | create stock transfer |
| `PERM_STOCK_TRANSFER_READ` | `stock_transfer.read` | `STOCK_TRANSFER` | `READ` | list/detail stock transfer |
| `PERM_STOCK_TRANSFER_UPDATE` | `stock_transfer.update` | `STOCK_TRANSFER` | `UPDATE` | submit/complete/cancel stock transfer |
| `PERM_STOCK_MOVEMENT_READ` | `stock_movement.read` | `STOCK_MOVEMENT` | `READ` | query stock movements |
| `PERM_STOCK_ADJUSTMENT_CREATE` | `stock_adjustment.create` | `STOCK_ADJUSTMENT` | `CREATE` | create stock adjustment |
| `PERM_STOCK_ADJUSTMENT_READ` | `stock_adjustment.read` | `STOCK_ADJUSTMENT` | `READ` | list/detail stock adjustment |
| `PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE` | `stock_adjustment_approval.update` | `STOCK_ADJUSTMENT_APPROVAL` | `UPDATE` | approve/reject stock adjustment |
| `PERM_STORAGE_CREATE` | `storage.create` | `STORAGE` | `CREATE` | upload file |
| `PERM_STORAGE_READ` | `storage.read` | `STORAGE` | `READ` | presigned-url and exists check |
| `PERM_STORAGE_DELETE` | `storage.delete` | `STORAGE` | `DELETE` | delete file |
| `PERM_REPORT_CURRENT_STOCK_READ` | `report_current_stock.read` | `REPORT_CURRENT_STOCK` | `READ` | export current stock report |

## 4. Endpoint to Permission Mapping

### 4.1 RBAC and Administration

| Endpoint Pattern | Required Permission |
|---|---|
| `POST /api/v1/permissions` | `PERM_PERMISSION_CREATE` |
| `GET /api/v1/permissions`, `GET /api/v1/permissions/{id}` | `PERM_PERMISSION_READ` |
| `PUT /api/v1/permissions/{id}` | `PERM_PERMISSION_UPDATE` |
| `DELETE /api/v1/permissions/{id}` | `PERM_PERMISSION_DELETE` |
| `POST /api/v1/roles` | `PERM_ROLE_CREATE` |
| `GET /api/v1/roles`, `GET /api/v1/roles/{id}`, `GET /api/v1/roles/{id}/users` | `PERM_ROLE_READ` |
| `PUT /api/v1/roles/{id}` | `PERM_ROLE_UPDATE` |
| `DELETE /api/v1/roles/{id}` | `PERM_ROLE_DELETE` |
| `POST /api/v1/roles/{id}/permissions` | `PERM_ROLE_PERMISSION_CREATE` |
| `GET /api/v1/roles/{id}/permissions` | `PERM_ROLE_PERMISSION_READ` |
| `DELETE /api/v1/roles/{id}/permissions/{permId}` | `PERM_ROLE_PERMISSION_DELETE` |
| `POST /api/v1/users/{userId}/roles` | `PERM_USER_ROLE_CREATE` |
| `GET /api/v1/users/{userId}/roles` | `PERM_USER_ROLE_READ` |
| `DELETE /api/v1/users/{userId}/roles/{roleId}` | `PERM_USER_ROLE_DELETE` |
| `GET /api/v1/users/managers`, `GET /api/v1/users/{accountId}` | `PERM_USER_READ` |
| `GET /api/v1/employees` and `GET /api/v1/employees/{id}` | `PERM_EMPLOYEE_READ` |
| `POST /api/v1/employees` | `PERM_EMPLOYEE_CREATE` |
| `PUT /api/v1/employees/{id}` | `PERM_EMPLOYEE_UPDATE` |
| `DELETE /api/v1/employees/{id}` | `PERM_EMPLOYEE_DELETE` |
| `GET /api/v1/home/test`, `GET /api/v1/warehouse/test` | `PERM_SYSTEM_DIAGNOSTIC_READ` |

### 4.2 Master Data

| Endpoint Pattern | Required Permission |
|---|---|
| `POST /api/v1/categories` | `PERM_CATEGORY_CREATE` |
| `GET /api/v1/categories`, `GET /api/v1/categories/{id}` | `PERM_CATEGORY_READ` |
| `PUT /api/v1/categories/{id}`, `PATCH /api/v1/categories/{id}/status` | `PERM_CATEGORY_UPDATE` |
| `POST /api/v1/business-partners` | `PERM_BUSINESS_PARTNER_CREATE` |
| `GET /api/v1/business-partners`, `GET /api/v1/business-partners/{id}`, `GET /api/v1/business-partners/search` | `PERM_BUSINESS_PARTNER_READ` |
| `PUT /api/v1/business-partners/{id}`, `PATCH /api/v1/business-partners/{id}/status` | `PERM_BUSINESS_PARTNER_UPDATE` |
| `DELETE /api/v1/business-partners/{id}` | `PERM_BUSINESS_PARTNER_DELETE` |
| `POST /api/v1/warehouse` | `PERM_WAREHOUSE_CREATE` |
| `GET /api/v1/warehouse`, `GET /api/v1/warehouse/all`, `GET /api/v1/warehouse/{id}` | `PERM_WAREHOUSE_READ` |
| `PUT /api/v1/warehouse/{id}`, `PATCH /api/v1/warehouse/{id}/status` | `PERM_WAREHOUSE_UPDATE` |
| `DELETE /api/v1/warehouse/{id}` | `PERM_WAREHOUSE_DELETE` |
| `POST /api/v1/locations` | `PERM_LOCATION_CREATE` |
| `GET /api/v1/locations`, `GET /api/v1/locations/{id}`, `GET /api/v1/locations/warehouse/{warehouseId}`, `GET /api/v1/locations/search` | `PERM_LOCATION_READ` |
| `PUT /api/v1/locations/{id}`, `PATCH /api/v1/locations/{id}/status` | `PERM_LOCATION_UPDATE` |
| `DELETE /api/v1/locations/{id}` | `PERM_LOCATION_DELETE` |
| `POST /api/v1/units-of-measure` | `PERM_UNIT_OF_MEASURE_CREATE` |
| `GET /api/v1/units-of-measure`, `GET /api/v1/units-of-measure/{id}` | `PERM_UNIT_OF_MEASURE_READ` |
| `PUT /api/v1/units-of-measure/{id}` | `PERM_UNIT_OF_MEASURE_UPDATE` |
| `DELETE /api/v1/units-of-measure/{id}` | `PERM_UNIT_OF_MEASURE_DELETE` |
| `POST /api/v1/products` | `PERM_PRODUCT_CREATE` |
| `GET /api/v1/products`, `GET /api/v1/products/{id}`, `GET /api/v1/products/sku/{sku}`, `POST /api/v1/products/search`, `GET /api/v1/products/category/{categoryId}`, `GET /api/v1/products/batch-tracking` | `PERM_PRODUCT_READ` |
| `PUT /api/v1/products/{id}` | `PERM_PRODUCT_UPDATE` |
| `DELETE /api/v1/products/{id}` | `PERM_PRODUCT_DELETE` |

### 4.3 Inventory and Warehouse Operations

| Endpoint Pattern | Required Permission |
|---|---|
| `GET /api/v1/inventories`, `GET /api/v1/inventories/summary/{productId}`, `GET /api/v1/inventories/by-location`, `POST /api/v1/inventories/check-availability` | `PERM_INVENTORY_READ` |
| `POST /api/v1/inventories/reserve`, `POST /api/v1/inventories/unreserve` | `PERM_INVENTORY_RESERVATION_UPDATE` |
| `POST /api/v1/inventories/increase`, `POST /api/v1/inventories/decrease` | `PERM_INVENTORY_MUTATION_UPDATE` |
| `POST /api/v1/batches` | `PERM_BATCH_CREATE` |
| `GET /api/v1/batches`, `GET /api/v1/batches/{id}`, `GET /api/v1/batches/{id}/traceability`, `GET /api/v1/batches/expiring`, `GET /api/v1/batches/fifo-recommendations`, `GET /api/v1/batches/by-product/{productId}` | `PERM_BATCH_READ` |
| `PUT /api/v1/batches/{id}`, `PATCH /api/v1/batches/{id}/status`, `PUT /api/v1/batches/{id}/quarantine`, `PUT /api/v1/batches/{id}/release` | `PERM_BATCH_UPDATE` |
| `POST /api/v1/stock-transfers` | `PERM_STOCK_TRANSFER_CREATE` |
| `GET /api/v1/stock-transfers`, `GET /api/v1/stock-transfers/{id}` | `PERM_STOCK_TRANSFER_READ` |
| `PUT /api/v1/stock-transfers/{id}/submit`, `PUT /api/v1/stock-transfers/{id}/complete`, `PUT /api/v1/stock-transfers/{id}/cancel` | `PERM_STOCK_TRANSFER_UPDATE` |
| `GET /api/v1/stock-movements`, `GET /api/v1/stock-movements/{id}`, `GET /api/v1/stock-movements/reference/{referenceType}/{referenceId}` | `PERM_STOCK_MOVEMENT_READ` |
| `POST /api/v1/stock-adjustments` | `PERM_STOCK_ADJUSTMENT_CREATE` |
| `GET /api/v1/stock-adjustments`, `GET /api/v1/stock-adjustments/{id}` | `PERM_STOCK_ADJUSTMENT_READ` |
| `PUT /api/v1/stock-adjustments/{id}/approve`, `PUT /api/v1/stock-adjustments/{id}/reject` | `PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE` |

### 4.4 Inbound and Outbound Flows

| Endpoint Pattern | Required Permission |
|---|---|
| `POST /api/v1/purchase-orders` | `PERM_PURCHASE_ORDER_CREATE` |
| `GET /api/v1/purchase-orders`, `GET /api/v1/purchase-orders/{id}` | `PERM_PURCHASE_ORDER_READ` |
| `PUT /api/v1/purchase-orders/{id}`, `PUT /api/v1/purchase-orders/{id}/confirm` | `PERM_PURCHASE_ORDER_UPDATE` |
| `DELETE /api/v1/purchase-orders/{id}` | `PERM_PURCHASE_ORDER_DELETE` |
| `POST /api/v1/purchase-order-lines` | `PERM_PURCHASE_ORDER_LINE_CREATE` |
| `GET /api/v1/purchase-order-lines/purchase-order/{purchaseOrderId}` | `PERM_PURCHASE_ORDER_LINE_READ` |
| `PUT /api/v1/purchase-order-lines/{id}` | `PERM_PURCHASE_ORDER_LINE_UPDATE` |
| `DELETE /api/v1/purchase-order-lines/{id}` | `PERM_PURCHASE_ORDER_LINE_DELETE` |
| `POST /api/v1/inbound-receipts` | `PERM_INBOUND_RECEIPT_CREATE` |
| `GET /api/v1/inbound-receipts`, `GET /api/v1/inbound-receipts/{id}`, `GET /api/v1/inbound-receipts/by-po/{purchaseOrderId}` | `PERM_INBOUND_RECEIPT_READ` |
| `PUT /api/v1/inbound-receipts/{id}`, `PUT /api/v1/inbound-receipts/{id}/confirm` | `PERM_INBOUND_RECEIPT_UPDATE` |
| `DELETE /api/v1/inbound-receipts/{id}` | `PERM_INBOUND_RECEIPT_DELETE` |
| `POST /api/v1/inbound-receipt-lines` | `PERM_INBOUND_RECEIPT_LINE_CREATE` |
| `GET /api/v1/inbound-receipt-lines` | `PERM_INBOUND_RECEIPT_LINE_READ` |
| `PUT /api/v1/inbound-receipt-lines/{id}` | `PERM_INBOUND_RECEIPT_LINE_UPDATE` |
| `DELETE /api/v1/inbound-receipt-lines/{id}` | `PERM_INBOUND_RECEIPT_LINE_DELETE` |
| `POST /api/v1/sales-orders` | `PERM_SALES_ORDER_CREATE` |
| `GET /api/v1/sales-orders`, `GET /api/v1/sales-orders/{id}` | `PERM_SALES_ORDER_READ` |
| `PUT /api/v1/sales-orders/{id}`, `PUT /api/v1/sales-orders/{id}/confirm`, `PUT /api/v1/sales-orders/{id}/cancel` | `PERM_SALES_ORDER_UPDATE` |
| `POST /api/v1/sales-order-lines` | `PERM_SALES_ORDER_LINE_CREATE` |
| `GET /api/v1/sales-order-lines/by-so/{soId}` | `PERM_SALES_ORDER_LINE_READ` |
| `PUT /api/v1/sales-order-lines/{id}` | `PERM_SALES_ORDER_LINE_UPDATE` |
| `POST /api/v1/outbound-shipments` | `PERM_OUTBOUND_SHIPMENT_CREATE` |
| `GET /api/v1/outbound-shipments`, `GET /api/v1/outbound-shipments/{id}` | `PERM_OUTBOUND_SHIPMENT_READ` |
| `PUT /api/v1/outbound-shipments/{id}`, `PUT /api/v1/outbound-shipments/{id}/start-picking`, `PUT /api/v1/outbound-shipments/{id}/mark-as-packed`, `PUT /api/v1/outbound-shipments/{id}/ship`, `PUT /api/v1/outbound-shipments/{id}/confirm-dispatch`, `PUT /api/v1/outbound-shipments/{id}/cancel` | `PERM_OUTBOUND_SHIPMENT_UPDATE` |
| `POST /api/v1/outbound-shipment-lines` | `PERM_OUTBOUND_SHIPMENT_LINE_CREATE` |
| `GET /api/v1/outbound-shipment-lines/shipment/{shipmentId}`, `GET /api/v1/outbound-shipment-lines/{id}` | `PERM_OUTBOUND_SHIPMENT_LINE_READ` |
| `PUT /api/v1/outbound-shipment-lines/{id}` | `PERM_OUTBOUND_SHIPMENT_LINE_UPDATE` |
| `DELETE /api/v1/outbound-shipment-lines/{id}` | `PERM_OUTBOUND_SHIPMENT_LINE_DELETE` |

### 4.5 Storage, Email, Reports

| Endpoint Pattern | Required Permission |
|---|---|
| `POST /api/v1/storage/upload`, `POST /api/v1/storage/upload/batch` | `PERM_STORAGE_CREATE` |
| `GET /api/v1/storage/presigned-url`, `GET /api/v1/storage/exists` | `PERM_STORAGE_READ` |
| `DELETE /api/v1/storage?objectName=...`, `DELETE /api/v1/storage/{*objectName}` | `PERM_STORAGE_DELETE` |
| `POST /api/v1/emails/send` | `PERM_EMAIL_CREATE` |
| `GET /api/v1/emails`, `GET /api/v1/emails/{id}`, `GET /api/v1/emails/status/{status}`, `GET /api/v1/emails/type/{type}`, `GET /api/v1/emails/recipient/{email}`, `GET /api/v1/emails/statistics` | `PERM_EMAIL_READ` |
| `POST /api/v1/emails/{id}/retry`, `POST /api/v1/emails/process-pending`, `POST /api/v1/emails/retry-failed` | `PERM_EMAIL_UPDATE` |
| `GET /api/v1/reports/current-stock/export/pdf` | `PERM_REPORT_CURRENT_STOCK_READ` |

## 5. Recommended Initial Role Strategy

### 5.1 ADMIN

Seed one role:
- `code = ROLE_ADMIN`
- `name = ADMIN`
- assign all permissions in section 3

This gives you a clean bootstrap role while keeping permission checks future-proof.

### 5.2 Suggested Next Roles

Do not seed these yet unless you need them immediately. First stabilize `ADMIN`.

Suggested future roles:
- `warehouse_manager`
  - warehouse/location/product/category/business-partner read
  - inventory read
  - inventory reservation update
  - batch create/read/update
  - purchase order read/update
  - inbound receipt create/read/update
  - sales order read
  - outbound shipment create/read/update
  - stock transfer create/read/update
  - stock movement read
  - storage create/read/delete
  - report current stock read
- `inventory_staff`
  - warehouse/location/product/category/unit-of-measure read
  - inventory read
  - inventory reservation update
  - inbound receipt read
  - inbound receipt line create/read/update
  - batch read
  - stock adjustment create/read
  - stock movement read
  - storage create/read
- `staff`
  - category read
  - product read
  - warehouse read
  - location read
  - unit-of-measure read
  - business-partner read

## 6. Important Guardrails Beyond Permission

Permissions alone are not enough for these flows:
- stock adjustment approve/reject still needs service-level validation
  - currently only admin can approve/reject in `StockAdjustmentsServiceImpl`
- warehouse ownership checks must remain in service layer
- state-transition validation must stay in service layer
- quantity and inventory integrity rules must stay in service layer
- background jobs should stay scoped by owner, not by broad read permission

RBAC decides "who may attempt the action".
Service logic decides "whether the action is valid for current business state".

## 7. Current Gaps to Fix in Code

These are the main mismatches between current code and the target model:
- `POST /api/v1/auth/reset-password` should be public, but is currently covered by default `authenticated`
- several RBAC admin endpoints have `@PreAuthorize` commented out and currently fall back to generic `authenticated`
  - role update/delete/read users
  - role-permission endpoints
  - user-role delete/read
- many business endpoints still rely on `isAuthenticated()` or global `.anyRequest().authenticated()`
- current seed permissions in `V20260313_14__Seed_full_project_dev_data.sql` are not aligned with the real endpoint surface

## 8. Migration Template for ADMIN

Use section 3 as the source of truth for permission rows.

Minimal Flyway pattern:

```sql
-- 1. Ensure ROLE_ADMIN exists
INSERT INTO roles (id, code, name, description, is_default)
SELECT UUID(), 'ROLE_ADMIN', 'ADMIN', 'System administrator with full access', FALSE
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE code = 'ROLE_ADMIN'
);

-- 2. Insert every permission from section 3 into permissions table
-- Columns:
--   id, code, name, resource, action, description
-- Example row:
--   UUID(), 'PERM_ROLE_CREATE', 'role.create', 'ROLE', 'CREATE', 'Create role'

-- 3. Grant every existing permission to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE r.code = 'ROLE_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions rp
      WHERE rp.role_id = r.id
        AND rp.permission_id = p.id
  );
```

Notes:
- if your test runtime does not support `UUID()`, replace with pre-generated IDs
- do not keep the old dev seed set like `PERM_INV_READ`, `PERM_ORDER_READ`, `PERM_REPORT_EXPORT` once you move to the catalog in section 3
- after seeding, `ROLE_ADMIN` should no longer need business endpoint checks via `hasRole('ADMIN')`; permission checks are enough

## 9. Recommended Rollout Order

1. Fix public whitelist in `SecurityConfig`, especially `POST /api/v1/auth/reset-password`.
2. Seed the permission catalog from section 3.
3. Grant all seeded permissions to `ROLE_ADMIN`.
4. Replace business endpoint guards from role/authenticated checks to permission checks.
5. Keep all current service-layer validations intact.
6. Only after `ADMIN` is stable, define and seed `warehouse_manager`, `inventory_staff`, and `staff`.
