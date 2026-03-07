# 📊 BẢNG TỔNG HỢP API TOÀN BỘ DỰ ÁN - WAREHOUSE MANAGEMENT SYSTEM

> **Ngày tạo:** 01/03/2026  
> **Cập nhật lần cuối:** 07/03/2026 — Đồng bộ lại trạng thái Inventory sau review code/Jira/PR  
> **Phiên bản:** 1.4  
> **Mục đích:** Review toàn bộ API theo từng module, phân loại CRUD vs Nâng cao, đánh dấu trạng thái triển khai

---

## 📈 TỔNG QUAN THỐNG KÊ

| # | Module | Planned | ✅ Done | ❌ Not Done | Coverage |
|---|--------|---------|--------|-------------|----------|
| 1 | **Auth & Users** | 9 | 7 | 2 | 78% |
| 2 | **Warehouse** | 7 | 6 | 1 | 86% |
| 3 | **Location** | 9 | 8 | 1 | 89% |
| 4 | **Product** | 11 | 9 | 2 | 82% |
| 5 | **UOM (Units of Measure)** | 5 | 5 | 0 | ✅ 100% |
| 6 | **Business Partner** | 8 | 6 | 2 | 75% |
| 7 | **Category** | 5 | 5 | 0 | ✅ 100% |
| 8 | **Batch** | 10 | 3 | 7 | 30% |
| 9 | **Inventory** | 8 | 3 | 5 | 38% |
| 10 | **Stock Adjustments** | 4 | 4 | 0 | ✅ 100% |
| 11 | **Stock Transfers** | 3 | 3 | 0 | 100% |
| 12 | **Purchase Orders** | 6 | 0 | 6 | 🔴 0% |
| 13 | **Purchase Order Lines** | 3 | 0 | 3 | 🔴 0% |
| 14 | **Inbound Receipts** | 7 | 0 | 7 | 🔴 0% |
| 15 | **Inbound Receipt Lines** | 3 | 0 | 3 | 🔴 0% |
| 16 | **Sales Orders** | 6 | 0 | 6 | 🔴 0% |
| 17 | **Sales Order Lines** | 3 | 0 | 3 | 🔴 0% |
| 18 | **Outbound Shipments** | 6 | 0 | 6 | 🔴 0% |
| 19 | **Outbound Shipment Lines** | 3 | 0 | 3 | 🔴 0% |
| 20 | **Stock Movements** | 8 | 2 | 6 | 25% |
| 21 | **Reporting** | 16 | 0 | 16 | 🔴 0% |
| 22 | **Employee** | 5 | 5 | 0 | ✅ 100% |
| 23 | **Email** | 10 | 10 | 0 | ✅ 100% |
| 24 | **Storage (MinIO)** | 5 | 5 | 0 | ✅ 100% |
| | **TỔNG CỘNG** | **160** | **80** | **80** | **50%** |

---

## 🟢 BIỂU ĐỒ TIẾN ĐỘ

```
Auth & Users       ████████░░ 78%
Warehouse          █████████░ 86%
Location           █████████░ 89%
Product            ████████░░ 82%
UOM                ██████████ 100% ✅
Business Partner   ████████░░ 75%
Category           ██████████ 100% ✅
Batch              ███░░░░░░░ 30%
Inventory          ████░░░░░░ 38%
Stock Adjustments  ██████████ 100% ✅
Stock Transfers    ██████████ 100% ✅
Purchase Orders    ░░░░░░░░░░ 0%  🔴
PO Lines           ░░░░░░░░░░ 0%  🔴
Inbound Receipts   ░░░░░░░░░░ 0%  🔴
IR Lines           ░░░░░░░░░░ 0%  🔴
Sales Orders       ░░░░░░░░░░ 0%  🔴
SO Lines           ░░░░░░░░░░ 0%  🔴
Outbound Shipments ░░░░░░░░░░ 0%  🔴
OS Lines           ░░░░░░░░░░ 0%  🔴
Stock Movements    ██░░░░░░░░ 25%
Reporting          ░░░░░░░░░░ 0%  🔴
Employee           ██████████ 100% ✅
Email              ██████████ 100% ✅
Storage            ██████████ 100% ✅
```

---

## 🔄 Đồng Bộ Jira/GitHub (06/03/2026)

- `WHS-70` đã `Done`; PR `#50` đã merge vào `develop` lúc `2026-03-04 15:00:05 UTC` (`22:00:05 ICT`).
- Đã cập nhật `Done` cho cụm task liên quan được gộp bởi PR `#50`: `WHS-21`, `WHS-22`, `WHS-23`, `WHS-24`, `WHS-25`, `WHS-26`, `WHS-27`.
- Đã bổ sung `WHS-87` (`To Do`) ngày `06/03/2026` để track riêng `PUT /api/v1/stock-transfers/{id}/complete`, do đây là API stock-changing quan trọng nhưng trước đó chưa có subtask Jira tương ứng dưới `WHS-25`.
- Review ngày `07/03/2026` xác nhận `GET /api/v1/inventories/summary/{productId}` và `GET /api/v1/inventories/by-location` đã có trên `develop`; `WHS-13` vẫn đang ở trạng thái `IN REVIEW` qua PR `#74`, chưa tính là Done.
- Snapshot re-check cuối cùng (09:04 ICT, 05/03/2026): `WHS-71..WHS-81` đã `Done` (PR đã merge), chỉ còn `WHS-82` ở `IN REVIEW` do PR `#70` còn `Open`.
- `WHS-51` vẫn `In Progress`; `WHS-64`, `WHS-66` vẫn `To Do` do chưa đủ endpoint theo AC (đặc biệt `by-product`, `by-batch`, traceability/analytics).

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
| 6 | `DELETE` | `/api/v1/warehouse/{id}` | Xóa kho (soft delete) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 7 | `PATCH` | `/api/v1/warehouse/{id}/status` | Đổi trạng thái kho (ACTIVE/INACTIVE/MAINTENANCE) | ✅ Done |

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
| 7 | `GET` | `/api/v1/business-partners/search` | Tìm kiếm (code, name, type, status) | ❌ Not Done |
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

> ✅ **Module này đã hoàn thành 100%** (WHS-5 → WHS-9 trong epic WHS-18)
> 
> **Phân quyền hiện tại:** `POST/PUT/PATCH` yêu cầu `ADMIN`, `GET` cho `USER` hoặc `ADMIN`.

---

## MODULE 3: BATCH MANAGEMENT (`/api/v1/batches`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/batches` | Tạo lô hàng mới | ✅ Done |
| 2 | `GET` | `/api/v1/batches` | Danh sách lô hàng (filters: product, status, dates) | ✅ Done |
| 3 | `GET` | `/api/v1/batches/{id}` | Chi tiết lô hàng theo ID | ✅ Done |
| 4 | `PUT` | `/api/v1/batches/{id}` | Cập nhật thông tin lô | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 5 | `PUT` | `/api/v1/batches/{id}/quarantine` | Cách ly lô hàng (vấn đề chất lượng) | ❌ Not Done |
| 6 | `PUT` | `/api/v1/batches/{id}/release` | Giải phóng lô khỏi cách ly | ❌ Not Done |
| 7 | `GET` | `/api/v1/batches/{id}/traceability` | Truy xuất nguồn gốc lô hàng | ❌ Not Done |
| 8 | `GET` | `/api/v1/batches/expiring` | Danh sách lô sắp hết hạn | ❌ Not Done |
| 9 | `GET` | `/api/v1/batches/fifo-recommendations` | Gợi ý FIFO (lô cũ nhất trước) | ❌ Not Done |
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
| 4 | `POST` | `/api/v1/inventories/check-availability` | Kiểm tra khả dụng tồn kho | ❌ Not Done |
| 5 | `POST` | `/api/v1/inventories/reserve` | Đặt trước tồn kho cho đơn hàng | ❌ Not Done |
| 6 | `POST` | `/api/v1/inventories/unreserve` | Giải phóng tồn kho đã đặt trước | ❌ Not Done |
| 7 | `POST` | `/api/v1/inventories/increase` | Tăng tồn kho (từ inbound) | ❌ Not Done |
| 8 | `POST` | `/api/v1/inventories/decrease` | Giảm tồn kho (từ outbound) | ❌ Not Done |

> 🟡 **Đã có 3 endpoint read-side** (`GET /api/v1/inventories`, `GET /api/v1/inventories/summary/{productId}`, `GET /api/v1/inventories/by-location`).
> 
> ⚠️ `POST /api/v1/inventories/check-availability` đang ở trạng thái `IN REVIEW` qua PR `#74`, chưa merge vào `develop`.
> 
> 🔴 Các stock utility APIs còn thiếu: `reserve`, `unreserve`, `increase`, `decrease`.

---

## MODULE 4: STOCK ADJUSTMENTS (`/api/v1/stock-adjustments`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/stock-adjustments` | Tạo điều chỉnh tồn kho | ✅ Done |
| 2 | `GET` | `/api/v1/stock-adjustments` | Danh sách điều chỉnh | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 3 | `PUT` | `/api/v1/stock-adjustments/{id}/approve` | Phê duyệt điều chỉnh | ✅ Done |
| 4 | `PUT` | `/api/v1/stock-adjustments/{id}/reject` | Từ chối điều chỉnh | ✅ Done |

> ✅ **Đã hoàn thành theo Jira:** `WHS-21`, `WHS-22`, `WHS-23`, `WHS-24` (sync 05/03/2026).  
> ℹ️ Có thêm endpoint đã triển khai trong code: `GET /api/v1/stock-adjustments/{id}`.

---

## MODULE 4: STOCK TRANSFERS (`/api/v1/stock-transfers`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/stock-transfers` | Tạo chuyển kho nội bộ | ✅ Done |
| 2 | `GET` | `/api/v1/stock-transfers` | Danh sách chuyển kho | ✅ Done |
| 3 | `PUT` | `/api/v1/stock-transfers/{id}/complete` | Hoàn tất phiếu chuyển kho | ✅ Done |

> ℹ️ Jira scope của parent `WHS-25` hiện gồm: `WHS-26`, `WHS-27`, `WHS-87`.  
> ℹ️ `GET /api/v1/stock-transfers/{id}` và `PUT /api/v1/stock-transfers/{id}/cancel` đã có trong code nhưng chưa được tách thành subtask Jira riêng.

---

## MODULE 5: INBOUND - PURCHASE ORDERS (`/api/v1/purchase-orders`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/purchase-orders` | Tạo đơn mua hàng | ❌ Not Done |
| 2 | `GET` | `/api/v1/purchase-orders` | Danh sách PO (filters) | ❌ Not Done |
| 3 | `GET` | `/api/v1/purchase-orders/{id}` | Chi tiết PO | ❌ Not Done |
| 4 | `PUT` | `/api/v1/purchase-orders/{id}` | Cập nhật PO (chỉ DRAFT) | ❌ Not Done |
| 5 | `DELETE` | `/api/v1/purchase-orders/{id}` | Xóa PO (chỉ DRAFT) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/purchase-orders/{id}/confirm` | Xác nhận PO → chính thức | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 5: INBOUND - PURCHASE ORDER LINES (`/api/v1/purchase-order-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/purchase-order-lines` | Thêm dòng vào PO | ❌ Not Done |
| 2 | `PUT` | `/api/v1/purchase-order-lines/{id}` | Cập nhật dòng PO | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/purchase-order-lines/{id}` | Xóa dòng PO | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

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

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 5: INBOUND - INBOUND RECEIPT LINES (`/api/v1/inbound-receipt-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/inbound-receipt-lines` | Thêm dòng vào phiếu nhập | ❌ Not Done |
| 2 | `PUT` | `/api/v1/inbound-receipt-lines/{id}` | Cập nhật dòng phiếu nhập | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/inbound-receipt-lines/{id}` | Xóa dòng phiếu nhập | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 6: OUTBOUND - SALES ORDERS (`/api/v1/sales-orders`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-orders` | Tạo đơn bán hàng | ❌ Not Done |
| 2 | `GET` | `/api/v1/sales-orders` | Danh sách SO | ❌ Not Done |
| 3 | `GET` | `/api/v1/sales-orders/{id}` | Chi tiết SO | ❌ Not Done |
| 4 | `PUT` | `/api/v1/sales-orders/{id}` | Cập nhật SO (chỉ DRAFT) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 5 | `PUT` | `/api/v1/sales-orders/{id}/confirm` | Xác nhận SO → đặt trước tồn kho | ❌ Not Done |
| 6 | `PUT` | `/api/v1/sales-orders/{id}/cancel` | Hủy SO → giải phóng tồn kho | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 6: OUTBOUND - SALES ORDER LINES (`/api/v1/sales-order-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-order-lines` | Thêm dòng vào SO | ❌ Not Done |
| 2 | `PUT` | `/api/v1/sales-order-lines/{id}` | Cập nhật dòng SO | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/sales-order-lines/{id}` | Xóa dòng SO | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

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

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 6: OUTBOUND - OUTBOUND SHIPMENT LINES (`/api/v1/outbound-shipment-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/outbound-shipment-lines` | Thêm dòng vào lô xuất | ❌ Not Done |
| 2 | `PUT` | `/api/v1/outbound-shipment-lines/{id}` | Cập nhật dòng lô xuất | ❌ Not Done |
| 3 | `DELETE` | `/api/v1/outbound-shipment-lines/{id}` | Xóa dòng lô xuất | ❌ Not Done |

> 🔴 **Controller rỗng hoàn toàn**

---

## MODULE 7: STOCK MOVEMENTS (`/api/v1/stock-movements`)

### 🔵 CRUD APIs (Read-only - Immutable records)

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `GET` | `/api/v1/stock-movements` | Danh sách biến động kho (filters) | ✅ Done |
| 2 | `GET` | `/api/v1/stock-movements/{id}` | Chi tiết biến động | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 3 | `GET` | `/api/v1/stock-movements/by-product/{productId}` | Lịch sử biến động theo sản phẩm | ❌ Not Done |
| 4 | `GET` | `/api/v1/stock-movements/by-batch/{batchId}` | Lịch sử biến động theo lô | ❌ Not Done |
| 5 | `GET` | `/api/v1/stock-movements/traceability/forward/{batchId}` | Truy xuất xuôi (lô → khách hàng) | ❌ Not Done |
| 6 | `GET` | `/api/v1/stock-movements/traceability/backward/{orderId}` | Truy xuất ngược (khách → nhà cung cấp) | ❌ Not Done |
| 7 | `GET` | `/api/v1/stock-movements/analytics` | Phân tích biến động kho | ❌ Not Done |
| 8 | `GET` | `/api/v1/stock-movements/export` | Export Excel/PDF | ❌ Not Done |

> 🟡 **Đã triển khai một phần**: list + detail + `GET /api/v1/stock-movements/reference/{referenceType}/{referenceId}`.  
> 🔴 Các API `by-product`, `by-batch`, traceability/analytics/export vẫn chưa có.

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

> 🔴 **Chưa có ReportController.java** — Cần tạo mới hoàn toàn.

---

## MODULE: EMPLOYEE (`/api/v1/employees`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Auth | Status |
|---|--------|----------|--------|------|--------|
| 1 | `POST` | `/api/v1/employees` | Tạo nhân viên mới (tạo Account + UserProfile + Employee trong 1 transaction) | `ADMIN` | ✅ Done |
| 2 | `GET` | `/api/v1/employees` | Danh sách nhân viên (phân trang, filter theo `keyword`, `status`, `warehouseId`) | `ADMIN`, `MANAGER` | ✅ Done |
| 3 | `GET` | `/api/v1/employees/{id}` | Chi tiết nhân viên theo ID | `ADMIN` | ✅ Done |
| 4 | `PUT` | `/api/v1/employees/{id}` | Cập nhật thông tin WMS/HR (partial update) | `ADMIN` | ✅ Done |
| 5 | `DELETE` | `/api/v1/employees/{id}` | Xóa mềm nhân viên → chuyển trạng thái `TERMINATED` | `ADMIN` | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

#### 📝 Ghi chú chi tiết

**`POST /api/v1/employees`**
- Tự động tạo `Account` (username + hashed password), gán `Role`, tạo `UserProfile`, tạo `Employee` trong **1 transaction**
- Validate: `username` unique, `employee_code` unique, `role` phải tồn tại
- Nhân viên mới luôn có `status = ACTIVE`; `warehouse_id` không gán lúc tạo

**`GET /api/v1/employees`**
- Filter: `keyword` (tìm theo `employee_code`, `department`, `position`), `status` (default: `ACTIVE`), `warehouseId`
- Phân trang: default `size=10`, `sort=createdAt,DESC`, tối đa 100 records/page
- Dùng **batch loading** UserProfile để tránh N+1 query

**`PUT /api/v1/employees/{id}`**
- Chỉ cập nhật các trường WMS/HR: `department`, `position`, `hire_date`, `termination_date`, `salary_grade`, `warehouse_id`
- **Không thể** thay đổi `username`, `email`, `first_name`, `last_name` qua API này
- Khi `warehouse_id` được gửi: kiểm tra kho tồn tại và có trạng thái `ACTIVE`

**`DELETE /api/v1/employees/{id}`**
- Soft delete: set `status = TERMINATED`, không xóa dữ liệu vật lý
- Tự động set `termination_date = today` nếu chưa có
- Không thể terminate nhân viên đã ở trạng thái `TERMINATED` (`400 EMP_005`)

#### ⚠️ APIs chưa triển khai — Đề xuất bổ sung

| # | Method | Endpoint | Mô tả | Ưu tiên |
|---|--------|----------|--------|---------|
| 6 | `PATCH` | `/api/v1/employees/{id}/status` | Đổi trạng thái `ACTIVE` ↔ `ON_LEAVE` | 🟡 Medium |
| 7 | `GET` | `/api/v1/employees/warehouse/{warehouseId}` | Nhân viên theo kho (không phân trang) | 🟡 Medium |
| 8 | `GET` | `/api/v1/employees/code/{employeeCode}` | Tìm theo mã nhân viên | 🟢 Low |

> 📄 **Tài liệu chi tiết:** `documents/Auth/EMPLOYEE_API_DOCUMENTATION.md`

---

## MODULE: EMAIL (`/api/v1/emails`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/emails/send` | Gửi email (sync hoặc async) | ✅ Done |
| 2 | `GET` | `/api/v1/emails` | Danh sách email logs (phân trang) | ✅ Done |
| 3 | `GET` | `/api/v1/emails/{id}` | Chi tiết email log | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `GET` | `/api/v1/emails/status/{status}` | Email logs theo trạng thái | ✅ Done |
| 5 | `GET` | `/api/v1/emails/type/{type}` | Email logs theo loại | ✅ Done |
| 6 | `GET` | `/api/v1/emails/recipient/{email}` | Email logs theo người nhận | ✅ Done |
| 7 | `POST` | `/api/v1/emails/{id}/retry` | Gửi lại email thất bại | ✅ Done |
| 8 | `GET` | `/api/v1/emails/statistics` | Thống kê email | ✅ Done |
| 9 | `POST` | `/api/v1/emails/process-pending` | Xử lý email chờ (manual trigger) | ✅ Done |
| 10 | `POST` | `/api/v1/emails/retry-failed` | Gửi lại tất cả email thất bại | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

## MODULE: STORAGE / MinIO (`/api/v1/storage`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/storage/upload` | Upload 1 file | ✅ Done |
| 2 | `POST` | `/api/v1/storage/upload/batch` | Upload nhiều file (max 10) | ✅ Done |
| 3 | `DELETE` | `/api/v1/storage/{objectName}` | Xóa file | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 4 | `GET` | `/api/v1/storage/presigned-url` | Tạo presigned URL | ✅ Done |
| 5 | `GET` | `/api/v1/storage/exists` | Kiểm tra file tồn tại | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**

---

---

# 🎯 ĐỀ XUẤT THỨ TỰ TRIỂN KHAI

### Phase 1 — Hoàn thiện Master Data (Ưu tiên cao nhất)
| Ưu tiên | Task | Effort |
|---------|------|--------|
| ✅ Done | ~~Category CRUD (5 APIs) — Module nền tảng, Product phụ thuộc~~ | — |
| 🟡 P1 | Warehouse DELETE (soft delete) | 0.5 ngày |
| 🟡 P1 | Business Partner search + filter by type | 1 ngày |
| ✅ Done | ~~Employee CRUD còn lại (4 APIs)~~ — **Đã hoàn thành** | — |

### Phase 2 — Batch Management
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🟡 P1 | Batch update — 1 API còn lại của CRUD | 1 ngày |
| 🟡 P1 | Batch quarantine/release — 2 APIs | 1-2 ngày |
| 🟡 P2 | Batch traceability, expiring, FIFO — 4 APIs | 3 ngày |

### Phase 3 — Inventory Core
| Ưu tiên | Task | Effort |
|---------|------|--------|
| ✅ Done | ~~Inventory summary + by-location — 2 read-side APIs~~ | — |
| 🔴 P0 | check-availability, reserve, unreserve — 3 APIs | 3 ngày |
| 🟡 P1 | increase, decrease — 2 APIs (internal service) | 2 ngày |
| ✅ Done | ~~Stock Adjustments CRUD + approve/reject — 4 APIs~~ | — |
| ✅ Done | ~~Stock Transfers CRUD — 2 APIs~~ | — |

### Phase 4 — Inbound Operations
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🔴 P0 | Purchase Orders CRUD + confirm — 6 APIs | 4 ngày |
| 🔴 P0 | PO Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Inbound Receipts CRUD + confirm — 7 APIs | 5 ngày |
| 🟡 P1 | IR Lines CRUD — 3 APIs | 2 ngày |

### Phase 5 — Outbound Operations
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🔴 P0 | Sales Orders CRUD + confirm + cancel — 6 APIs | 4 ngày |
| 🟡 P1 | SO Lines CRUD — 3 APIs | 2 ngày |
| 🔴 P0 | Outbound Shipments CRUD + pick + confirm — 6 APIs | 5 ngày |
| 🟡 P1 | OS Lines CRUD — 3 APIs | 2 ngày |

### Phase 6 — Stock Movements & Reporting
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🔴 P0 | Stock Movements còn thiếu (6 APIs): by-product, by-batch, traceability, analytics, export | 4-5 ngày |
| 🟡 P2 | Reporting on-demand — 6 APIs | 5 ngày |
| 🟢 P3 | Reporting async — 4 APIs | 3 ngày |
| 🟢 P3 | Reporting scheduled — 6 APIs | 4 ngày |

### Phase 7 — Auth nâng cao
| Ưu tiên | Task | Effort |
|---------|------|--------|
| 🟡 P2 | verify-email, resend-verification — 2 APIs | 2 ngày |
| 🟡 P2 | Product import/export (async RabbitMQ) — 2 APIs | 3 ngày |

---

# 📊 TỔNG KẾT

| Metric | Value |
|--------|-------|
| **Tổng API thiết kế** | 160 |
| **Đã triển khai** | 80 (50%) |
| **Chưa triển khai** | 80 (50%) |
| **Module hoàn thành 100%** | UOM, Email, Storage, Employee, Category, **Stock Adjustments, Stock Transfers** |
| **Module 0%** | PO, PO Lines, Inbound Receipts, IR Lines, SO, SO Lines, Outbound Shipments, OS Lines, Reporting |
| **Entities đã có (DB migration)** | ✅ Tất cả 30 entities đã có migration |
| **Controllers đã tạo (shell)** | ✅ 26 controllers (8 controllers chưa có method API) |

> 💡 **Điểm mạnh:** Nền tảng tốt — DB schema, entities, auth, rate-limiting, email, storage đã hoàn thiện; module điều chỉnh/chuyển kho đã có API vận hành chính.  
> ✅ **Employee module:** Hoàn thành 100% (5/5 APIs) — tạo, danh sách, chi tiết, cập nhật, xóa mềm.  
> ⚠️ **Điểm yếu:** Inbound/Outbound/Reporting chưa triển khai; Inventory và Stock Movements mới ở mức partial.
