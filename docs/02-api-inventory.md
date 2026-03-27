# API Inventory (Current Codebase Snapshot)

Source scanned: `src/main/java/org/demo/whs/controller/**` with security context from `SecurityConfig`.

## 1) Security Baseline Used for Inventory

- Global rule: all endpoints require authentication unless explicitly permitted (`src/main/java/org/demo/whs/configuration/SecurityConfig.java:86-102`).
- Public endpoints: `/api/v1/auth/**`, Swagger docs, `/actuator/health`, `/actuator/info` (`SecurityConfig.java:88-95`).
- Additional method-level restrictions use `@PreAuthorize` (mainly `EmailController`, `HomeController`).

---

## 2) Endpoint Inventory by Module

### A. Authentication

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | User login, issue access + refresh tokens | `LoginRequest` (`@Valid`) | `BaseResponse<AuthResponse>` | **Public** (`permitAll`) + rate limit | `AuthController.java:42-56`, `SecurityConfig.java:88` |
| POST | `/api/v1/auth/refresh-token` | Exchange refresh token for new access token | `RefreshTokenRequest` (`@Valid`) | `BaseResponse<RefreshTokenResponse>` | **Public** (`permitAll`) + rate limit | `AuthController.java:65-77`, `SecurityConfig.java:88` |

### B. Home / Session

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| GET | `/api/v1/home/test` | Admin-only test endpoint | N/A | `String` | Authenticated + `hasRole('ADMIN')` | `HomeController.java:23-27` |
| GET | `/api/v1/home/me` | Return current authenticated username | N/A | `ResponseEntity<String>` | Authenticated | `HomeController.java:29-33`, `SecurityConfig.java:101` |

### C. Warehouse

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| GET | `/api/v1/warehouse/test` | Placeholder test | N/A | `String` | Authenticated | `WareHouseController.java:25-30` |
| POST | `/api/v1/warehouse` | Create warehouse | `CreateWarehouseRequest` (`@Valid`) | `BaseResponse<WareHouseResponse>` | Authenticated | `WareHouseController.java:38-44` |
| GET | `/api/v1/warehouse` | List warehouses (paged) | Query params `page,size` | `BaseResponse<PageResponse<WareHouseResponse>>` | Authenticated | `WareHouseController.java:53-59` |
| GET | `/api/v1/warehouse/all` | List all warehouses (no paging) | N/A | `BaseResponse<List<WareHouseResponse>>` | Authenticated | `WareHouseController.java:66-72` |
| GET | `/api/v1/warehouse/{id}` | Get warehouse detail | Path param `id` | `BaseResponse<WareHouseResponse>` | Authenticated | `WareHouseController.java:80-86` |
| PUT | `/api/v1/warehouse/{id}` | Update warehouse | `UpdateWarehouseRequest` (`@Valid`) | `BaseResponse<WareHouseResponse>` | Authenticated | `WareHouseController.java:95-103` |
| PATCH | `/api/v1/warehouse/{id}/status` | Change warehouse status | `ChangeStatusRequest` (`@Valid`) | `BaseResponse<WareHouseResponse>` | Authenticated | `WareHouseController.java:112-121` |

### D. Location

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| POST | `/api/v1/locations` | Create location | `CreateLocationRequest` (`@Valid`) | `BaseResponse<LocationResponse>` | Authenticated | `LocationController.java:37-47` |
| GET | `/api/v1/locations` | List locations (paged) | Query params `page,size` | `BaseResponse<PageResponse<LocationResponse>>` | Authenticated | `LocationController.java:56-66` |
| GET | `/api/v1/locations/{id}` | Get location detail | Path param `id` | `BaseResponse<LocationResponse>` | Authenticated | `LocationController.java:74-83` |
| GET | `/api/v1/locations/warehouse/{warehouseId}` | List by warehouse | Path/query params | `BaseResponse<PageResponse<LocationResponse>>` | Authenticated | `LocationController.java:93-106` |
| GET | `/api/v1/locations/search` | Filter/search locations | Query params -> `SearchLocationRequest` (constructed manually) | `BaseResponse<PageResponse<LocationResponse>>` | Authenticated | `LocationController.java:121-163` |
| PUT | `/api/v1/locations/{id}` | Update location | `UpdateLocationRequest` (`@Valid`) | `BaseResponse<LocationResponse>` | Authenticated | `LocationController.java:172-182` |
| PATCH | `/api/v1/locations/{id}/status` | Change location status | `ChangeLocationStatusRequest` (`@Valid`) | `BaseResponse<LocationResponse>` | Authenticated | `LocationController.java:191-202` |
| DELETE | `/api/v1/locations/{id}` | Soft-delete location | Path param `id` | `BaseResponse<Void>` | Authenticated | `LocationController.java:210-218` |

### E. Product

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| POST | `/api/v1/products` | Create product | `CreateProductRequest` (`@Valid`) | `BaseResponse<ProductResponse>` | Authenticated | `ProductController.java:40-48` |
| PUT | `/api/v1/products/{id}` | Update product | `UpdateProductRequest` (`@Valid`) | `BaseResponse<ProductResponse>` | Authenticated | `ProductController.java:59-66` |
| GET | `/api/v1/products/{id}` | Get product by id | Path param `id` | `BaseResponse<ProductResponse>` | Authenticated | `ProductController.java:76-81` |
| GET | `/api/v1/products/sku/{sku}` | Get product by SKU | Path param `sku` | `BaseResponse<ProductResponse>` | Authenticated | `ProductController.java:91-96` |
| GET | `/api/v1/products` | List products (paged) | Query params `page,size` | `BaseResponse<PageResponse<ProductResponse>>` | Authenticated | `ProductController.java:107-114` |
| POST | `/api/v1/products/search` | Search/filter products | `SearchProductRequest` (**no `@Valid`**) | `BaseResponse<PageResponse<ProductResponse>>` | Authenticated | `ProductController.java:126-134` |
| DELETE | `/api/v1/products/{id}` | Soft-delete product | Path param `id` | `BaseResponse<Void>` | Authenticated | `ProductController.java:144-149` |
| GET | `/api/v1/products/category/{categoryId}` | List by category | Path/query params | `BaseResponse<PageResponse<ProductResponse>>` | Authenticated | `ProductController.java:161-169` |
| GET | `/api/v1/products/batch-tracking` | List products requiring batch tracking | Query params | `BaseResponse<PageResponse<ProductResponse>>` | Authenticated | `ProductController.java:180-187` |

### F. Units of Measure

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| POST | `/api/v1/units-of-measure` | Create UOM | `UnitsOfMeasureRequest` (`@Valid`) | `BaseResponse<UnitsOfMeasureResponse>` | Authenticated | `UnitsOfMeasureController.java:30-36` |
| GET | `/api/v1/units-of-measure` | List UOM | N/A | `BaseResponse<List<UnitsOfMeasureResponse>>` | Authenticated | `UnitsOfMeasureController.java:43-49` |
| GET | `/api/v1/units-of-measure/{id}` | Get UOM detail | Path param `id` | `BaseResponse<UnitsOfMeasureResponse>` | Authenticated | `UnitsOfMeasureController.java:57-63` |
| PUT | `/api/v1/units-of-measure/{id}` | Update UOM | `UpdateUnitsOfMeasureRequest` (`@Valid`) | `BaseResponse<UnitsOfMeasureResponse>` | Authenticated | `UnitsOfMeasureController.java:72-80` |
| DELETE | `/api/v1/units-of-measure/{id}` | Delete UOM | Path param `id` | `BaseResponse<Void>` | Authenticated | `UnitsOfMeasureController.java:89-95` |

### G. User

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| GET | `/api/v1/users/managers` | List users with manager role | N/A | `BaseResponse<List<AccountResponse>>` | Authenticated | `UserController.java:33-39` |
| GET | `/api/v1/users/{accountId}` | Get user by account ID | Path param `accountId` | `BaseResponse<AccountResponse>` | Authenticated | `UserController.java:47-53` |

### H. Email

| Method | Path | Purpose | Request DTO | Response DTO | Auth Requirement | Evidence |
|---|---|---|---|---|---|---|
| POST | `/api/v1/emails/send` | Send sync/async email | `SendEmailRequest` (`@Valid`) | `EmailLogResponse` | Authenticated + `hasRole('ADMIN')` | `EmailController.java:43-59` |
| GET | `/api/v1/emails/{id}` | Get email log by id | Path param `id` | `EmailLogResponse` | Authenticated + `@PreAuthorize("hasAuthority('ADMIN')")` | `EmailController.java:66-73` |
| GET | `/api/v1/emails` | List email logs (paged) | Query params `page,size,sortBy,sortDir` | `Page<EmailLogResponse>` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:80-97` |
| GET | `/api/v1/emails/status/{status}` | Filter logs by status | Path/query params | `Page<EmailLogResponse>` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:104-116` |
| GET | `/api/v1/emails/type/{type}` | Filter logs by type | Path/query params | `Page<EmailLogResponse>` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:123-135` |
| GET | `/api/v1/emails/recipient/{email}` | Filter logs by recipient | Path/query params | `Page<EmailLogResponse>` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:142-154` |
| POST | `/api/v1/emails/{id}/retry` | Retry failed email | Path param `id` | `EmailLogResponse` | **Authenticated only** (no method-level role check) | `EmailController.java:161-167`, `SecurityConfig.java:101` |
| GET | `/api/v1/emails/statistics` | Email statistics | N/A | `Map<String,Long>` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:175-182` |
| POST | `/api/v1/emails/process-pending` | Trigger pending processing | N/A | `String` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:189-196` |
| POST | `/api/v1/emails/retry-failed` | Trigger bulk retry | N/A | `String` | Authenticated + `hasAuthority('ADMIN')` | `EmailController.java:203-210` |

### I. Placeholder / Base-path-only Controllers (No concrete endpoints yet)

- `/api/v1/categories` (`CategoryController`)
- `/api/v1/inventories` (`InventoryController`)
- `/api/v1/inbound-receipts` (`InboundReceiptsController`)
- `/api/v1/inbound-receipt-lines` (`InboundReceiptLinesController`)
- `/api/v1/outbound-shipments` (`OutboundShipmentsController`) ✅ Done (8/9 APIs)
- `/api/v1/outbound-shipment-lines` (`OutboundShipmentLinesController`) ✅ Done (5/5 APIs)
- `/api/v1/purchase-orders` (`PurchaseOrdersController`)
- `/api/v1/purchase-order-lines` (`PurchaseOrderLinesController`)
- `/api/v1/sales-orders` (`SalesOrdersController`)
- `/api/v1/sales-order-lines` (`SalesOrderLinesController`)
- `/api/v1/stock-adjustments` (`StockAdjustmentsController`)
- `/api/v1/stock-movements` (`StockMovementsController`)
- `/api/v1/stock-transfers` (`StockTransfersController`)
- `/api/v1/business-partner` has only one placeholder endpoint: `GET /api/v1/business-partner` (`BusinessPartnerController.java:19-24`)
- `/api/v1/batches` has one simple endpoint: `GET /api/v1/batches` returning static text (`BatchController.java:21-24`)

---

## 3) Quick DTO / Validation Coverage Notes

- Good coverage: most write endpoints use `@RequestBody @Valid` with field-level constraints in DTO classes.
- Partial gap: `POST /api/v1/products/search` accepts `SearchProductRequest` without `@Valid` (`ProductController.java:126-129`), and search DTO has no constraints.
- Query/path parameter validation is mostly missing (`@Min` is used in some product paging fields only).

