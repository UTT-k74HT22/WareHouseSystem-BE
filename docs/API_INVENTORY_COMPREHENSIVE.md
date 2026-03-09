# 📊 BẢNG TỔNG HỢP API TOÀN BỘ DỰ ÁN - WAREHOUSE MANAGEMENT SYSTEM

> **Ngày tạo:** 01/03/2026  
> **Cập nhật lần cuối:** 09/03/2026 — Đồng bộ toàn bộ trạng thái từ code thực tế (controller scan)
> **Phiên bản:** 2.0
> **Mục đích:** Review toàn bộ API theo từng module, phân loại CRUD vs Nâng cao, đánh dấu trạng thái triển khai

---

## 📈 TỔNG QUAN THỐNG KÊ

| # | Module | Planned | ✅ Done | ❌ Not Done | Coverage |
|---|--------|---------|--------|-------------|----------|
| 1 | **Auth & Users** | 9 | 7 | 2 | 78% |
| 2 | **Warehouse** | 7 | 7 | 0 | ✅ 100% |
| 3 | **Location** | 9 | 8 | 1 | 89% |
| 4 | **Product** | 11 | 9 | 2 | 82% |
| 5 | **UOM (Units of Measure)** | 5 | 5 | 0 | ✅ 100% |
| 6 | **Business Partner** | 8 | 7 | 1 | 88% |
| 7 | **Category** | 5 | 5 | 0 | ✅ 100% |
| 8 | **Batch** | 10 | 4 | 6 | 40% |
| 9 | **Inventory** | 8 | 4 | 4 | 50% |
| 10 | **Stock Adjustments** | 5 | 5 | 0 | ✅ 100% |
| 11 | **Stock Transfers** | 5 | 5 | 0 | ✅ 100% |
| 12 | **Purchase Orders** | 6 | 6 | 0 | ✅ 100% |
| 13 | **Purchase Order Lines** | 3 | 3 | 0 | ✅ 100% |
| 14 | **Inbound Receipts** | 7 | 0 | 7 | 🔴 0% |
| 15 | **Inbound Receipt Lines** | 3 | 0 | 3 | 🔴 0% |
| 16 | **Sales Orders** | 7 | 0 | 7 | 🔴 0% |
| 17 | **Sales Order Lines** | 3 | 0 | 3 | 🔴 0% |
| 18 | **Outbound Shipments** | 6 | 0 | 6 | 🔴 0% |
| 19 | **Outbound Shipment Lines** | 3 | 0 | 3 | 🔴 0% |
| 20 | **Stock Movements** | 8 | 3 | 5 | 38% |
| 21 | **Reporting** | 16 | 0 | 16 | 🔴 0% |
| 22 | **Employee** | 5 | 5 | 0 | ✅ 100% |
| 23 | **Email** | 10 | 10 | 0 | ✅ 100% |
| 24 | **Storage (MinIO)** | 5 | 5 | 0 | ✅ 100% |
| | **TỔNG CỘNG** | **168** | **98** | **70** | **58%** |

---

## 🟢 BIỂU ĐỒ TIẾN ĐỘ

```
Auth & Users       ████████░░ 78%
Warehouse          ██████████ 100% ✅
Location           █████████░ 89%
Product            ████████░░ 82%
UOM                ██████████ 100% ✅
Business Partner   █████████░ 88%
Category           ██████████ 100% ✅
Batch              ████░░░░░░ 40%
Inventory          █████░░░░░ 50%
Stock Adjustments  ██████████ 100% ✅
Stock Transfers    ██████████ 100% ✅
Purchase Orders    ██████████ 100% ✅  🆕
PO Lines           ██████████ 100% ✅  🆕
Inbound Receipts   ░░░░░░░░░░ 0%  🔴
IR Lines           ░░░░░░░░░░ 0%  🔴
Sales Orders       ░░░░░░░░░░ 0%  🔴
SO Lines           ░░░░░░░░░░ 0%  🔴
Outbound Shipments ░░░░░░░░░░ 0%  🔴
OS Lines           ░░░░░░░░░░ 0%  🔴
Stock Movements    ████░░░░░░ 38%
Reporting          ░░░░░░░░░░ 0%  🔴
Employee           ██████████ 100% ✅
Email              ██████████ 100% ✅
Storage            ██████████ 100% ✅
```

---

## 🔄 Đồng Bộ Jira/GitHub (09/03/2026)

- **09/03/2026** — Full code scan, đồng bộ trạng thái toàn bộ controller vs API plan.
- `PurchaseOrderLinesController.java` đã có 3/3 endpoints (Create, Update, Delete) → module `Purchase Order Lines` **100% Done** 🆕
- `PurchaseOrdersController.java` đã có 6/6 endpoints (CRUD + confirm) → module `Purchase Orders` **100% Done**.
- `BusinessPartnerController.java` đã có endpoint `GET /search` → cập nhật từ 6/8 lên 7/8.
- `WareHouseController.java` đã có `DELETE /{id}` → cập nhật từ 6/7 lên 7/7.
- `BatchController.java` đã có `PATCH /{id}/status` → cập nhật từ 3/10 lên 4/10.
- `InventoryController.java` đã có `POST /check-availability` (merged) → cập nhật từ 3/8 lên 4/8.
- `StockAdjustmentsController.java` đã có `GET /{id}` (untracked) → cập nhật count lên 5/5.
- `StockTransfersController.java` đã có `GET /{id}` + `PUT /{id}/cancel` (untracked) → cập nhật count lên 5/5.
- `StockMovementsController.java` đã có `GET /reference/{referenceType}/{referenceId}` → cập nhật lên 3/8.
- **Entity fixes applied:**
  - `InboundReceiptLines.java`: trailing space in `@Table(name)` fixed.
  - `InboundReceiptLines.java`: `batchId` changed from `nullable=false` to `nullable=true`.
- Đồng bộ trước đó (06–07/03/2026):
  - `WHS-70` đã `Done`; PR `#50` merge vào `develop`.
  - `WHS-87` added track `PUT /api/v1/stock-transfers/{id}/complete`.
  - `WHS-71..WHS-81` Done, `WHS-82` In Review.

---

# 📋 CHI TIẾT TỪNG MODULE

---

## MODULE 1: AUTH & USERS (`/api/v1/auth`, `/api/v1/otp`, `/api/v1/users`, `/api/v1/home`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/auth/register` | Đăng ký tài khoản mới | ✅ Done |
| 2 | `GET` | `/api/v1/users/{accountId}` | Lấy thông tin user theo ID | ✅ Done |
| 3 | `GET` | `/api/v1/users/managers` | Lấy danh sách managers | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `POST` | `/api/v1/auth/login` | Đăng nhập (rate-limited 5/5min) | ✅ Done |
| 5 | `POST` | `/api/v1/auth/refresh-token` | Làm mới token (rate-limited 10/min) | ✅ Done |
| 6 | `POST` | `/api/v1/otp/send` | Gửi/Gửi lại OTP qua email | ✅ Done |
| 7 | `POST` | `/api/v1/otp/verify` | Xác thực OTP | ✅ Done |
| 8 | `POST` | `/api/v1/auth/verify-email` | Xác thực email (email verification token) | ❌ Not Done |
| 9 | `POST` | `/api/v1/auth/resend-verification` | Gửi lại email xác thực | ❌ Not Done |

> **Utility endpoints đã triển khai:** `GET /api/v1/home/test` (health check), `GET /api/v1/home/me` (current user info)

---

## MODULE 2: MASTER DATA - WAREHOUSE (`/api/v1/warehouse`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/warehouse` | Tạo kho mới | ✅ Done |
| 2 | `GET` | `/api/v1/warehouse` | Danh sách kho (phân trang) | ✅ Done |
| 3 | `GET` | `/api/v1/warehouse/all` | Danh sách kho (không phân trang) | ✅ Done |
| 4 | `GET` | `/api/v1/warehouse/{id}` | Chi tiết kho theo ID | ✅ Done |
| 5 | `PUT` | `/api/v1/warehouse/{id}` | Cập nhật thông tin kho | ✅ Done |
| 6 | `DELETE` | `/api/v1/warehouse/{id}` | Xóa kho (soft delete) | ✅ Done 🆕 |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 7 | `PATCH` | `/api/v1/warehouse/{id}/status` | Đổi trạng thái kho (ACTIVE/INACTIVE/MAINTENANCE) | ✅ Done |

> ✅ **Module này đã hoàn thành 100%** (7/7 APIs)

---

## MODULE 2: MASTER DATA - LOCATION (`/api/v1/locations`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/locations` | Tạo vị trí mới | ✅ Done |
| 2 | `GET` | `/api/v1/locations` | Danh sách vị trí (phân trang) | ✅ Done |
| 3 | `GET` | `/api/v1/locations/{id}` | Chi tiết vị trí theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/locations/{id}` | Cập nhật vị trí | ✅ Done |
| 5 | `DELETE` | `/api/v1/locations/{id}` | Xóa vị trí (soft delete) | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `GET` | `/api/v1/locations/warehouse/{warehouseId}` | Danh sách vị trí theo kho | ✅ Done |
| 7 | `GET` | `/api/v1/locations/search` | Tìm kiếm (code, name, zone, type, status) | ✅ Done |
| 8 | `PATCH` | `/api/v1/locations/{id}/status` | Đổi trạng thái vị trí | ✅ Done |
| 9 | `POST` | `/api/v1/locations/bulk` | Import CSV hàng loạt | ❌ Not Done |

---

## MODULE 2: MASTER DATA - PRODUCT (`/api/v1/products`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/products` | Tạo sản phẩm mới | ✅ Done |
| 2 | `GET` | `/api/v1/products` | Danh sách sản phẩm (phân trang) | ✅ Done |
| 3 | `GET` | `/api/v1/products/{id}` | Chi tiết sản phẩm theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/products/{id}` | Cập nhật sản phẩm | ✅ Done |
| 5 | `DELETE` | `/api/v1/products/{id}` | Xóa sản phẩm (soft delete) | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `GET` | `/api/v1/products/sku/{sku}` | Tìm sản phẩm theo SKU | ✅ Done |
| 7 | `POST` | `/api/v1/products/search` | Tìm kiếm nâng cao (filters + phân trang) | ✅ Done |
| 8 | `GET` | `/api/v1/products/category/{categoryId}` | Sản phẩm theo danh mục | ✅ Done |
| 9 | `GET` | `/api/v1/products/batch-tracking` | Sản phẩm cần theo dõi lô | ✅ Done |
| 10 | `POST` | `/api/v1/products/import` | Import sản phẩm từ Excel (async) | ❌ Not Done |
| 11 | `GET` | `/api/v1/products/export` | Export sản phẩm ra Excel (async) | ❌ Not Done |

> **Lưu ý:** API import/export cần tích hợp RabbitMQ cho xử lý async.

---

## MODULE 2: MASTER DATA - UOM (`/api/v1/units-of-measure`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/units-of-measure` | Tạo đơn vị đo mới | ✅ Done |
| 2 | `GET` | `/api/v1/units-of-measure` | Danh sách tất cả UOM | ✅ Done |
| 3 | `GET` | `/api/v1/units-of-measure/{id}` | Chi tiết UOM theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/units-of-measure/{id}` | Cập nhật UOM | ✅ Done |
| 5 | `DELETE` | `/api/v1/units-of-measure/{id}` | Xóa UOM (hard delete, kiểm tra product liên kết) | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

## MODULE 2: MASTER DATA - BUSINESS PARTNER (`/api/v1/business-partners`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/business-partners` | Tạo đối tác kinh doanh | ✅ Done |
| 2 | `GET` | `/api/v1/business-partners` | Danh sách tất cả đối tác | ✅ Done |
| 3 | `GET` | `/api/v1/business-partners/{id}` | Chi tiết đối tác theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/business-partners/{id}` | Cập nhật đối tác | ✅ Done |
| 5 | `DELETE` | `/api/v1/business-partners/{id}` | Xóa đối tác (soft delete) | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PATCH` | `/api/v1/business-partners/{id}/status` | Đổi trạng thái đối tác | ✅ Done |
| 7 | `GET` | `/api/v1/business-partners/search` | Tìm kiếm (code, name, type, status) | ✅ Done 🆕 |
| 8 | `GET` | `/api/v1/business-partners/type/{type}` | Lọc theo loại (SUPPLIER/CUSTOMER/BOTH) | ❌ Not Done |

---

## MODULE 2: MASTER DATA - CATEGORY (`/api/v1/categories`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/categories` | Tạo danh mục sản phẩm | ✅ Done |
| 2 | `GET` | `/api/v1/categories` | Danh sách danh mục (phân trang, filter status) | ✅ Done |
| 3 | `GET` | `/api/v1/categories/{id}` | Chi tiết danh mục theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/categories/{id}` | Cập nhật danh mục | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 5 | `PATCH` | `/api/v1/categories/{id}/status` | Đổi trạng thái danh mục (ACTIVE/INACTIVE) | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

## MODULE 3: BATCH MANAGEMENT (`/api/v1/batches`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/batches` | Tạo lô hàng mới | ✅ Done |
| 2 | `GET` | `/api/v1/batches` | Danh sách lô hàng (phân trang) | ✅ Done |
| 3 | `GET` | `/api/v1/batches/{id}` | Chi tiết lô hàng theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/batches/{id}` | Cập nhật thông tin lô | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 5 | `PATCH` | `/api/v1/batches/{id}/status` | Đổi trạng thái lô (AVAILABLE/QUARANTINED/EXPIRED/DEPLETED) | ✅ Done 🆕 |
| 6 | `PUT` | `/api/v1/batches/{id}/quarantine` | Cách ly lô hàng (vấn đề chất lượng) | ❌ Not Done |
| 7 | `PUT` | `/api/v1/batches/{id}/release` | Giải phóng lô khỏi cách ly | ❌ Not Done |
| 8 | `GET` | `/api/v1/batches/{id}/traceability` | Truy xuất nguồn gốc lô hàng | ❌ Not Done |
| 9 | `GET` | `/api/v1/batches/expiring` | Danh sách lô sắp hết hạn | ❌ Not Done |
| 10 | `GET` | `/api/v1/batches/by-product/{productId}` | Danh sách lô theo sản phẩm | ❌ Not Done |

---

## MODULE 4: INVENTORY MANAGEMENT (`/api/v1/inventories`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `GET` | `/api/v1/inventories` | Danh sách tồn kho (filters) | ✅ Done |
| 2 | `GET` | `/api/v1/inventories/summary/{productId}` | Tổng hợp tồn kho theo sản phẩm | ✅ Done |
| 3 | `GET` | `/api/v1/inventories/by-location` | Tồn kho nhóm theo vị trí | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `POST` | `/api/v1/inventories/check-availability` | Kiểm tra khả dụng tồn kho | ✅ Done 🆕 |
| 5 | `POST` | `/api/v1/inventories/reserve` | Đặt trước tồn kho cho đơn hàng | ❌ Not Done |
| 6 | `POST` | `/api/v1/inventories/unreserve` | Giải phóng tồn kho đã đặt trước | ❌ Not Done |
| 7 | `POST` | `/api/v1/inventories/increase` | Tăng tồn kho (từ inbound) | ❌ Not Done |
| 8 | `POST` | `/api/v1/inventories/decrease` | Giảm tồn kho (từ outbound) | ❌ Not Done |

> 🟡 `increase` sẽ được triển khai nội bộ (internal service) cùng module Inbound Confirm Receipt.
>
> ⚠️ `reserve`/`unreserve`/`decrease` sẽ được triển khai cùng module Outbound.

---

## MODULE 4: STOCK ADJUSTMENTS (`/api/v1/stock-adjustments`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/stock-adjustments` | Tạo điều chỉnh tồn kho | ✅ Done |
| 2 | `GET` | `/api/v1/stock-adjustments` | Danh sách điều chỉnh (filter: status, product, warehouse, dates) | ✅ Done |
| 3 | `GET` | `/api/v1/stock-adjustments/{id}` | Chi tiết điều chỉnh theo ID | ✅ Done 🆕 |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `PUT` | `/api/v1/stock-adjustments/{id}/approve` | Phê duyệt điều chỉnh | ✅ Done |
| 5 | `PUT` | `/api/v1/stock-adjustments/{id}/reject` | Từ chối điều chỉnh | ✅ Done |

> ✅ **Module này đã hoàn thành 100%** (5/5 APIs)

---

## MODULE 4: STOCK TRANSFERS (`/api/v1/stock-transfers`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/stock-transfers` | Tạo chuyển kho nội bộ | ✅ Done |
| 2 | `GET` | `/api/v1/stock-transfers` | Danh sách chuyển kho | ✅ Done |
| 3 | `GET` | `/api/v1/stock-transfers/{id}` | Chi tiết chuyển kho theo ID | ✅ Done 🆕 |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `PUT` | `/api/v1/stock-transfers/{id}/complete` | Hoàn tất phiếu chuyển kho | ✅ Done |
| 5 | `PUT` | `/api/v1/stock-transfers/{id}/cancel` | Hủy phiếu chuyển kho | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (5/5 APIs)

---

## MODULE 5: INBOUND - PURCHASE ORDERS (`/api/v1/purchase-orders`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/purchase-orders` | Tạo đơn mua hàng (DRAFT) | ✅ Done 🆕 |
| 2 | `GET` | `/api/v1/purchase-orders` | Danh sách PO (filters: number, supplier, warehouse, status, dates) | ✅ Done 🆕 |
| 3 | `GET` | `/api/v1/purchase-orders/{id}` | Chi tiết PO | ✅ Done 🆕 |
| 4 | `PUT` | `/api/v1/purchase-orders/{id}` | Cập nhật PO (chỉ DRAFT) | ✅ Done 🆕 |
| 5 | `DELETE` | `/api/v1/purchase-orders/{id}` | Xóa PO (chỉ DRAFT) | ✅ Done 🆕 |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/purchase-orders/{id}/confirm` | Xác nhận PO → CONFIRMED | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (6/6 APIs)
>
> **Lifecycle:** `DRAFT → CONFIRMED → PARTIALLY_RECEIVED → COMPLETED` | `DRAFT → CANCELLED`
>
> **Filters:** purchaseOrderNumber, supplierId, warehouseId, status, orderDateFrom/To, expectedDeliveryDateFrom/To
>
> **Sort fields:** createdAt, updatedAt, purchaseOrderNumber, orderDate, expectedDeliveryDate, status

---

## MODULE 5: INBOUND - PURCHASE ORDER LINES (`/api/v1/purchase-order-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/purchase-order-lines` | Thêm dòng vào PO | ✅ Done 🆕 |
| 2 | `PUT` | `/api/v1/purchase-order-lines/{id}` | Cập nhật dòng PO | ✅ Done 🆕 |
| 3 | `DELETE` | `/api/v1/purchase-order-lines/{id}` | Xóa dòng PO | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (3/3 APIs)
>
> **Business Rules:**
> - Chỉ thao tác trên PO có trạng thái `DRAFT`
> - Tự động tính `lineTotal = quantityOrdered × unitPrice`
> - Auto-increment `lineNumber` khi tạo mới
> - Không cho phép trùng `productId` trong cùng một PO
> - Validate `quantityOrdered > 0` và `unitPrice >= 0`
> - Pessimistic locking để tránh race condition
>
> **Request Fields (Create):**
> ```json
> {
>   "purchase_order_id": "string (required)",
>   "product_id": "string (required)",
>   "quantity_ordered": "decimal (required, min: 0.01)",
>   "unit_price": "decimal (required, min: 0.00)",
>   "notes": "string (optional, max: 500 chars)"
> }
> ```
>
> **Request Fields (Update):**
> ```json
> {
>   "product_id": "string (optional)",
>   "quantity_ordered": "decimal (optional, min: 0.01)",
>   "unit_price": "decimal (optional, min: 0.00)",
>   "notes": "string (optional, max: 500 chars)"
> }
> ```
>
> **Response Fields:**
> ```json
> {
>   "id": "string",
>   "purchase_order_id": "string",
>   "product_id": "string",
>   "line_number": "integer",
>   "quantity_ordered": "decimal",
>   "quantity_received": "decimal",
>   "unit_price": "decimal",
>   "line_total": "decimal",
>   "notes": "string",
>   "created_at": "yyyy-MM-dd HH:mm:ss",
>   "updated_at": "yyyy-MM-dd HH:mm:ss"
> }
> ```

---

## MODULE 5: INBOUND - INBOUND RECEIPTS (`/api/v1/inbound-receipts`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/inbound-receipts` | Tạo phiếu nhập kho | ❌ Not Done |
| 2 | `GET` | `/api/v1/inbound-receipts` | Danh sách phiếu nhập | ❌ Not Done |
| 3 | `GET` | `/api/v1/inbound-receipts/{id}` | Chi tiết phiếu nhập | ❌ Not Done |
| 4 | `PUT` | `/api/v1/inbound-receipts/{id}` | Cập nhật phiếu nhập (chỉ DRAFT) | ❌ Not Done |
| 5 | `DELETE` | `/api/v1/inbound-receipts/{id}` | Xóa phiếu nhập (chỉ DRAFT) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/inbound-receipts/{id}/confirm` | Xác nhận nhập kho → cập nhật inventory | ❌ Not Done |
| 7 | `GET` | `/api/v1/inbound-receipts/by-po/{poId}` | Phiếu nhập theo PO | ❌ Not Done |

> 🔴 **Controller rỗng** — Module tiếp theo cần triển khai

---

## MODULE 5: INBOUND - INBOUND RECEIPT LINES (`/api/v1/inbound-receipt-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/inbound-receipt-lines` | Thêm dòng vào phiếu nhập | ❌ Not Done |
| 2 | `PUT` | `/api/v1/inbound-receipt-lines/{id}` | Cập nhật dòng phiếu nhập | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/inbound-receipt-lines/{id}` | Xóa dòng phiếu nhập | ❌ Not Done |

> 🔴 **Controller rỗng**

---

## MODULE 6: OUTBOUND - SALES ORDERS (`/api/v1/sales-orders`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-orders` | Tạo đơn bán hàng (DRAFT) | ❌ Not Done |
| 2 | `GET` | `/api/v1/sales-orders` | Danh sách SO (filters) | ❌ Not Done |
| 3 | `GET` | `/api/v1/sales-orders/{id}` | Chi tiết SO | ❌ Not Done |
| 4 | `PUT` | `/api/v1/sales-orders/{id}` | Cập nhật SO (chỉ DRAFT) | ❌ Not Done |
| 5 | `DELETE` | `/api/v1/sales-orders/{id}` | Xóa SO (chỉ DRAFT) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/sales-orders/{id}/confirm` | Xác nhận SO → đặt trước tồn kho | ❌ Not Done |
| 7 | `PUT` | `/api/v1/sales-orders/{id}/cancel` | Hủy SO → giải phóng tồn kho | ❌ Not Done |

> 🔴 **Controller rỗng** — Lifecycle: `DRAFT → CONFIRMED → PARTIALLY_SHIPPED → COMPLETED` | `CONFIRMED → CANCELLED`

---

## MODULE 6: OUTBOUND - SALES ORDER LINES (`/api/v1/sales-order-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-order-lines` | Thêm dòng vào SO | ❌ Not Done |
| 2 | `PUT` | `/api/v1/sales-order-lines/{id}` | Cập nhật dòng SO | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/sales-order-lines/{id}` | Xóa dòng SO | ❌ Not Done |

> 🔴 **Controller rỗng**

---

## MODULE 6: OUTBOUND - OUTBOUND SHIPMENTS (`/api/v1/outbound-shipments`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/outbound-shipments` | Tạo lô xuất hàng | ❌ Not Done |
| 2 | `GET` | `/api/v1/outbound-shipments` | Danh sách lô xuất | ❌ Not Done |
| 3 | `GET` | `/api/v1/outbound-shipments/{id}` | Chi tiết lô xuất | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `PUT` | `/api/v1/outbound-shipments/{id}/pick` | Đánh dấu đang picking | ❌ Not Done |
| 5 | `PUT` | `/api/v1/outbound-shipments/{id}/confirm` | Xác nhận xuất kho → giảm tồn kho | ❌ Not Done |
| 6 | `GET` | `/api/v1/outbound-shipments/{id}/pick-list` | In danh sách pick (PDF) | ❌ Not Done |

> 🔴 **Controller rỗng** — Lifecycle: `DRAFT → PICKING → PACKED → SHIPPED` | `DRAFT → CANCELLED`

---

## MODULE 6: OUTBOUND - OUTBOUND SHIPMENT LINES (`/api/v1/outbound-shipment-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/outbound-shipment-lines` | Thêm dòng vào lô xuất | ❌ Not Done |
| 2 | `PUT` | `/api/v1/outbound-shipment-lines/{id}` | Cập nhật dòng lô xuất | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/outbound-shipment-lines/{id}` | Xóa dòng lô xuất | ❌ Not Done |

> 🔴 **Controller rỗng**

---

## MODULE 7: STOCK MOVEMENTS (`/api/v1/stock-movements`)

### 🔵 CRUD APIs (Read-only - Immutable records)

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `GET` | `/api/v1/stock-movements` | Danh sách biến động kho (phân trang) | ✅ Done |
| 2 | `GET` | `/api/v1/stock-movements/{id}` | Chi tiết biến động | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 3 | `GET` | `/api/v1/stock-movements/reference/{referenceType}/{referenceId}` | Biến động theo tham chiếu | ✅ Done 🆕 |
| 4 | `GET` | `/api/v1/stock-movements/by-product/{productId}` | Lịch sử biến động theo sản phẩm | ❌ Not Done |
| 5 | `GET` | `/api/v1/stock-movements/by-batch/{batchId}` | Lịch sử biến động theo lô | ❌ Not Done |
| 6 | `GET` | `/api/v1/stock-movements/traceability/forward/{batchId}` | Truy xuất xuôi (lô → khách hàng) | ❌ Not Done |
| 7 | `GET` | `/api/v1/stock-movements/traceability/backward/{orderId}` | Truy xuất ngược (khách → nhà cung cấp) | ❌ Not Done |
| 8 | `GET` | `/api/v1/stock-movements/export` | Export Excel/PDF | ❌ Not Done |

> 🟡 **Đã triển khai 3/8**: list + detail + reference lookup.

---

## MODULE 8: REPORTING (`/api/v1/reports`)

> ⚠️ **Chưa có controller nào được tạo!** Cần tạo `ReportController.java`

### 🟣 On-Demand Reports

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/reports/current-stock` | Báo cáo tồn kho hiện tại | ❌ Not Done |
| 2 | `POST` | `/api/v1/reports/stock-valuation` | Báo cáo giá trị tồn kho | ❌ Not Done |
| 3 | `POST` | `/api/v1/reports/movements` | Báo cáo biến động kho | ❌ Not Done |
| 4 | `POST` | `/api/v1/reports/batch-traceability` | Báo cáo truy xuất lô hàng | ❌ Not Done |
| 5 | `POST` | `/api/v1/reports/low-stock` | Báo cáo tồn kho thấp | ❌ Not Done |
| 6 | `POST` | `/api/v1/reports/expiring-batches` | Báo cáo lô sắp hết hạn | ❌ Not Done |

### 🟣 Async Reports

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 7 | `POST` | `/api/v1/reports/async/request` | Yêu cầu báo cáo async | ❌ Not Done |
| 8 | `GET` | `/api/v1/reports/async/{requestId}` | Kiểm tra trạng thái báo cáo | ❌ Not Done |
| 9 | `GET` | `/api/v1/reports/async/{requestId}/download` | Tải báo cáo hoàn thành | ❌ Not Done |
| 10 | `GET` | `/api/v1/reports/async/my-requests` | Danh sách yêu cầu báo cáo của tôi | ❌ Not Done |

### 🟣 Scheduled Reports

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 11 | `POST` | `/api/v1/reports/schedules` | Tạo lịch báo cáo tự động | ❌ Not Done |
| 12 | `GET` | `/api/v1/reports/schedules` | Danh sách lịch báo cáo | ❌ Not Done |
| 13 | `PUT` | `/api/v1/reports/schedules/{id}` | Cập nhật lịch | ❌ Not Done |
| 14 | `DELETE` | `/api/v1/reports/schedules/{id}` | Xóa lịch | ❌ Not Done |
| 15 | `PUT` | `/api/v1/reports/schedules/{id}/enable` | Bật lịch | ❌ Not Done |
| 16 | `PUT` | `/api/v1/reports/schedules/{id}/disable` | Tắt lịch | ❌ Not Done |

---

## MODULE: EMPLOYEE (`/api/v1/employees`)

| # | Method | Endpoint | Mô tả | Auth | Status |
|---|--------|----------|--------|------|--------|
| 1 | `POST` | `/api/v1/employees` | Tạo nhân viên mới | `ADMIN` | ✅ Done |
| 2 | `GET` | `/api/v1/employees` | Danh sách nhân viên (phân trang, filter) | `ADMIN`, `MANAGER` | ✅ Done |
| 3 | `GET` | `/api/v1/employees/{id}` | Chi tiết nhân viên theo ID | `ADMIN` | ✅ Done |
| 4 | `PUT` | `/api/v1/employees/{id}` | Cập nhật thông tin WMS/HR | `ADMIN` | ✅ Done |
| 5 | `DELETE` | `/api/v1/employees/{id}` | Xóa mềm nhân viên → TERMINATED | `ADMIN` | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

## MODULE: EMAIL (`/api/v1/emails`)

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/emails/send` | Gửi email | ✅ Done |
| 2 | `GET` | `/api/v1/emails` | Danh sách email logs | ✅ Done |
| 3 | `GET` | `/api/v1/emails/{id}` | Chi tiết email log | ✅ Done |
| 4 | `GET` | `/api/v1/emails/status/{status}` | Logs theo trạng thái | ✅ Done |
| 5 | `GET` | `/api/v1/emails/type/{type}` | Logs theo loại | ✅ Done |
| 6 | `GET` | `/api/v1/emails/recipient/{email}` | Logs theo người nhận | ✅ Done |
| 7 | `POST` | `/api/v1/emails/{id}/retry` | Gửi lại email thất bại | ✅ Done |
| 8 | `GET` | `/api/v1/emails/statistics` | Thống kê email | ✅ Done |
| 9 | `POST` | `/api/v1/emails/process-pending` | Xử lý email chờ | ✅ Done |
| 10 | `POST` | `/api/v1/emails/retry-failed` | Gửi lại tất cả thất bại | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

## MODULE: STORAGE / MinIO (`/api/v1/storage`)

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/storage/upload` | Upload 1 file | ✅ Done |
| 2 | `POST` | `/api/v1/storage/upload/batch` | Upload nhiều file | ✅ Done |
| 3 | `DELETE` | `/api/v1/storage/{objectName}` | Xóa file | ✅ Done |
| 4 | `GET` | `/api/v1/storage/presigned-url` | Tạo presigned URL | ✅ Done |
| 5 | `GET` | `/api/v1/storage/exists` | Kiểm tra file tồn tại | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

---

# 🎯 ĐỀ XUẤT THỨ TỰ TRIỂN KHAI (Cập nhật 09/03/2026)

### ✅ Phase 1–3 — ĐÃ HOÀN THÀNH
- Master Data (Warehouse, Location, Product, UOM, Business Partner, Category, Employee)
- Inventory Core (Stock Adjustments, Stock Transfers, Inventory read-side)
- Purchase Orders (CRUD + confirm)

### 🔴 Phase 4 — Inbound Chain (ĐANG TRIỂN KHAI)
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🔴 P0 | PO Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Inbound Receipts CRUD + by-po — 6 APIs | 3 ngày |
| 🔴 P0 | Inbound Receipt Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Confirm Receipt + inventory increase + stock movement — 1 API (core) | 3 ngày |

### 🟡 Phase 5 — Outbound Chain
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🔴 P0 | Sales Orders CRUD + confirm + cancel — 7 APIs | 4 ngày |
| 🟡 P1 | SO Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Outbound Shipments CRUD + pick + confirm — 6 APIs | 5 ngày |
| 🟡 P1 | OS Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Inventory reserve/unreserve/decrease (internal) | 3 ngày |

### 🟡 Phase 6 — Batch + Stock Movements Enhancement
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🟡 P1 | Batch update + quarantine/release — 3 APIs | 2 ngày |
| 🟡 P2 | Batch traceability, expiring, by-product — 3 APIs | 2 ngày |
| 🟡 P1 | Stock Movements by-product, by-batch — 2 APIs | 2 ngày |
| 🟡 P2 | Stock Movements traceability + export — 3 APIs | 3 ngày |

### 🟢 Phase 7 — Reporting & Miscellaneous
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🟡 P2 | Reporting on-demand — 6 APIs | 5 ngày |
| 🟢 P3 | Reporting async — 4 APIs | 3 ngày |
| 🟢 P3 | Reporting scheduled — 6 APIs | 4 ngày |
| 🟡 P2 | Auth verify-email + resend — 2 APIs | 2 ngày |

---

# 📊 TỔNG KẾT (09/03/2026)

| Metric | Value |
|--------|-------|
| **Tổng API thiết kế** | 168 |
| **Đã triển khai** | 95 (57%) |
| **Chưa triển khai** | 73 (43%) |
| **Module hoàn thành 100%** | Warehouse, UOM, Category, Employee, Email, Storage, Stock Adj., Stock Transfers, **Purchase Orders** |
| **Module 0%** | PO Lines, Inbound Receipts, IR Lines, SO, SO Lines, Outbound Shipments, OS Lines, Reporting |
| **Module tiếp theo** | **Inbound Chain** (PO Lines → Receipts → Receipt Lines → Confirm) |

> 💡 **Tiến bộ so với v1.4:** +15 API mới, PO module hoàn thành, nhiều endpoint untracked đã ghi nhận.
>
> ✅ **Nền tảng vững:** DB schema, entities, auth, rate-limiting, email, storage, PO sẵn sàng cho Inbound chain.
>
> ⚠️ **Entity fixes applied:** `InboundReceiptLines` trailing space + batchId nullable.
