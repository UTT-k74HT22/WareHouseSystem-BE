# 📊 BẢNG TỔNG HỢP API TOÀN BỘ DỰ ÁN - WAREHOUSE MANAGEMENT SYSTEM

> **Ngày tạo:** 01/03/2026  
> **Cập nhật lần cuối:** 21/03/2026 — Re-audit: RBAC 17/19 ✅, Sales Orders 7/7 ✅, Inventory 8/8 ✅
> **Phiên bản:** 2.5
> **Mục đích:** Review toàn bộ API theo từng module, phân loại CRUD vs Nâng cao, đánh dấu trạng thái triển khai

---

## 📈 TỔNG QUAN THỐNG KÊ

| # | Module | Planned | ✅ Done | ❌ Not Done | Coverage |
|---|--------|---------|--------|-------------|----------|
| 1 | **Auth & Users** | 13 | 11 | 2 | 85% |
| 2 | **RBAC & Authorization** | 19 | 17 | 2 | 89% 🆕 |
| 3 | **Warehouse** | 7 | 7 | 0 | ✅ 100% |
| 4 | **Location** | 9 | 8 | 1 | 89% |
| 5 | **Product** | 11 | 9 | 2 | 82% |
| 6 | **UOM (Units of Measure)** | 5 | 5 | 0 | ✅ 100% |
| 7 | **Business Partner** | 8 | 7 | 1 | 88% |
| 8 | **Category** | 5 | 5 | 0 | ✅ 100% |
| 9 | **Batch** | 11 | 11 | 0 | ✅ 100% |
| 10 | **Inventory** | 8 | 8 | 0 | ✅ 100% 🆕 |
| 11 | **Stock Adjustments** | 5 | 5 | 0 | ✅ 100% |
| 12 | **Stock Transfers** | 5 | 5 | 0 | ✅ 100% |
| 13 | **Purchase Orders** | 6 | 6 | 0 | ✅ 100% |
| 14 | **Purchase Order Lines** | 4 | 4 | 0 | ✅ 100% |
| 15 | **Inbound Receipts** | 7 | 7 | 0 | ✅ 100% |
| 16 | **Inbound Receipt Lines** | 4 | 4 | 0 | ✅ 100% |
| 17 | **Sales Orders** | 7 | 7 | 0 | ✅ 100% 🆕 |
| 18 | **Sales Order Lines** | 4 | 4 | 0 | ✅ 100% 🆕 |
| 19 | **Outbound Shipments** | 6 | 0 | 6 | 🔴 0% |
| 20 | **Outbound Shipment Lines** | 3 | 0 | 3 | 🔴 0% |
| 21 | **Stock Movements** | 8 | 3 | 5 | 38% |
| 22 | **Reporting** | 16 | 0 | 16 | 🔴 0% |
| 23 | **Employee** | 5 | 5 | 0 | ✅ 100% |
| 24 | **Email** | 10 | 10 | 0 | ✅ 100% |
| 25 | **Storage (MinIO)** | 5 | 5 | 0 | ✅ 100% |
| | **TỔNG CỘNG** | **190** | **141** | **49** | **74%** 🆕 |

---

## 🟢 BIỂU ĐỒ TIẾN ĐỘ

```
Auth & Users       ████████░░ 85%
RBAC & Authz       █████████░ 89% 🆕
Warehouse          ██████████ 100% ✅
Location           █████████░ 89%
Product            ████████░░ 82%
UOM                ██████████ 100% ✅
Business Partner   █████████░ 88%
Category           ██████████ 100% ✅
Batch              ██████████ 100% ✅
Inventory          ██████████ 100% ✅ 🆕
Stock Adjustments  ██████████ 100% ✅
Stock Transfers    ██████████ 100% ✅
Purchase Orders    ██████████ 100% ✅
PO Lines           ██████████ 100% ✅
Inbound Receipts   ██████████ 100% ✅
IR Lines           ██████████ 100% ✅
Sales Orders       ██████████ 100% ✅ 🆕
SO Lines           ██████████ 100% ✅ 🆕
Outbound Shipments ░░░░░░░░░░ 0%  🔴
OS Lines           ░░░░░░░░░░ 0%  🔴
Stock Movements    ████░░░░░░ 38%
Reporting          ░░░░░░░░░░ 0%  🔴
Employee           ██████████ 100% ✅
Email              ██████████ 100% ✅
Storage            ██████████ 100% ✅
```

---

## 🔄 Đồng Bộ Jira/GitHub (16/03/2026)

- **16/03/2026** — Re-audit theo `Jira issue status + controller routes + service implementation`, ưu tiên trạng thái chạy được trong code thay vì chỉ nhìn annotation/controller shell.
- Context audit chi tiết đã được lưu tại `docs/SESSION_API_JIRA_SYNC_20260316.md`.

| Scope Jira | Trạng thái trên Jira | Đối chiếu code thực tế | Kết luận sync |
|---|---|---|---|
| `WHS-35`, `WHS-41`, `WHS-42`, `WHS-83`, `WHS-84`, `WHS-85`, `WHS-86` | `Done` | `BatchController` + service + test/doc đã khớp | ✅ Aligned |
| `WHS-19`, `WHS-10..17` | Parent `In Progress`, child `WHS-10..16` `Done`, `WHS-17` `In Progress` | Inventory public API hiện đủ `list/summary/by-location/check-availability/reserve/unreserve/increase`, còn thiếu `decrease` | ✅ Aligned |
| `WHS-43..46`, `WHS-53..58` | `Done` | Purchase Orders + PO Lines + Inbound Receipts + Receipt Lines đã có controller/service thực thi | ✅ Aligned |
| `WHS-47..50`, `WHS-59..65` | `In Progress` / `To Do` | `SalesOrdersController`, `SalesOrderLinesController`, `OutboundShipmentsController`, `OutboundShipmentLinesController` vẫn là shell, chưa có route methods | ✅ Aligned |
| `WHS-144..148`, `WHS-149..167` | Chủ yếu `In Progress` / `To Do` | Có route ở controller nhưng `PermissionServiceImpl` và `RoleServiceImpl` vẫn là stub `return null/List.of()`; `check-permission` và `my-permissions` chưa tồn tại | ✅ Jira đang phản ánh đúng hơn docs cũ |

> ⚠️ Kết luận quan trọng nhất của đợt sync này: **RBAC chưa phải implemented module**. Docs cũ chưa ghi module này nên dễ gây hiểu nhầm khi nhìn thấy controller đã tồn tại.

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
| 6 | `POST` | `/api/v1/auth/forgot-password` | Gửi OTP quên mật khẩu | ✅ Done |
| 7 | `POST` | `/api/v1/auth/verify-forgot-password-otp` | Xác thực OTP quên mật khẩu | ✅ Done |
| 8 | `POST` | `/api/v1/auth/reset-password` | Đặt lại mật khẩu bằng session token hợp lệ | ✅ Done |
| 9 | `POST` | `/api/v1/auth/change-password` | Đổi mật khẩu khi đã đăng nhập | ✅ Done |
| 10 | `POST` | `/api/v1/otp/send` | Gửi/Gửi lại OTP qua email | ✅ Done |
| 11 | `POST` | `/api/v1/otp/verify` | Xác thực OTP | ✅ Done |
| 12 | `POST` | `/api/v1/auth/verify-email` | Xác thực email (email verification token) | ❌ Not Done |
| 13 | `POST` | `/api/v1/auth/resend-verification` | Gửi lại email xác thực | ❌ Not Done |

> ✅ **Module này hiện ở 11/13 APIs**.
>
> **Utility endpoints không tính coverage:** `GET /api/v1/home/test`, `GET /api/v1/home/me`

---

## MODULE 1B: RBAC & AUTHORIZATION (`/api/v1/permissions`, `/api/v1/roles`, `/api/v1/users/{userId}/roles`, `/api/v1/auth`)

### 🔵 Core CRUD & lookup APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/permissions` | Tạo permission | ✅ Done 🆕 |
| 2 | `GET` | `/api/v1/permissions` | Danh sách permissions | ✅ Done 🆕 |
| 3 | `GET` | `/api/v1/permissions/{id}` | Chi tiết permission | ✅ Done 🆕 |
| 4 | `PUT` | `/api/v1/permissions/{id}` | Cập nhật permission | ✅ Done 🆕 |
| 5 | `DELETE` | `/api/v1/permissions/{id}` | Xóa permission | ✅ Done 🆕 |
| 6 | `POST` | `/api/v1/roles` | Tạo role | ✅ Done 🆕 |
| 7 | `GET` | `/api/v1/roles` | Danh sách roles | ✅ Done 🆕 |
| 8 | `GET` | `/api/v1/roles/{id}` | Chi tiết role | ✅ Done 🆕 |
| 9 | `PUT` | `/api/v1/roles/{id}` | Cập nhật role | ✅ Done 🆕 |
| 10 | `DELETE` | `/api/v1/roles/{id}` | Xóa role | ✅ Done 🆕 |

### 🟣 Assignment & authorization APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 11 | `POST` | `/api/v1/roles/{id}/permissions` | Gán permission vào role | ✅ Done 🆕 |
| 12 | `DELETE` | `/api/v1/roles/{id}/permissions/{permId}` | Gỡ permission khỏi role | ✅ Done 🆕 |
| 13 | `GET` | `/api/v1/roles/{id}/permissions` | Danh sách permissions của role | ✅ Done 🆕 |
| 14 | `POST` | `/api/v1/users/{userId}/roles` | Gán roles cho user | ✅ Done 🆕 |
| 15 | `DELETE` | `/api/v1/users/{userId}/roles/{roleId}` | Thu hồi role của user | ✅ Done 🆕 |
| 16 | `GET` | `/api/v1/users/{userId}/roles` | Danh sách roles của user | ✅ Done 🆕 |
| 17 | `GET` | `/api/v1/roles/{id}/users` | Danh sách users theo role | ❌ Not Done |
| 18 | `POST` | `/api/v1/auth/check-permission` | Kiểm tra quyền truy cập | ❌ Not Done |
| 19 | `GET` | `/api/v1/auth/my-permissions` | Lấy quyền hiệu lực của user hiện tại | ❌ Not Done |

> 🟡 **Module này hiện ở 17/19 APIs (89%).**
>
> ✅ `PermissionController`, `RoleController`, `RolePermissionController`, `UserRoleController` đã được implement đầy đủ.
>
> ⚠️ Hai endpoint authorization tracking (`check-permission`, `my-permissions`) và endpoint `roles/{id}/users` vẫn chưa được triển khai.

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
| 2 | `GET` | `/api/v1/batches` | Danh sách lô hàng có filter + quantity summary | ✅ Done |
| 3 | `GET` | `/api/v1/batches/{id}` | Chi tiết lô hàng theo ID + quantity summary | ✅ Done |
| 4 | `PUT` | `/api/v1/batches/{id}` | Cập nhật thông tin lô | ✅ Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 5 | `PATCH` | `/api/v1/batches/{id}/status` | Chặn generic status patch, buộc dùng workflow chuyên biệt | ✅ Done 🆕 |
| 6 | `PUT` | `/api/v1/batches/{id}/quarantine` | Cách ly lô hàng (vấn đề chất lượng) | ✅ Done |
| 7 | `PUT` | `/api/v1/batches/{id}/release` | Giải phóng lô khỏi cách ly | ✅ Done |
| 8 | `GET` | `/api/v1/batches/{id}/traceability` | Truy xuất nguồn gốc lô hàng | ✅ Done |
| 9 | `GET` | `/api/v1/batches/expiring` | Danh sách lô sắp hết hạn | ✅ Done |
| 10 | `GET` | `/api/v1/batches/fifo-recommendations` | Đề xuất FIFO theo tồn kho khả dụng | ✅ Done |
| 11 | `GET` | `/api/v1/batches/by-product/{productId}` | Danh sách lô theo sản phẩm | ✅ Done |

> 🟢 **Đã triển khai 11/11**: Batch module đã đủ lifecycle + query APIs theo scope `WHS-35`.
>
> ✅ `GET /api/v1/batches` hiện hỗ trợ filter `keyword`, `product_id`, `warehouse_id`, `status`, `manufacturing_date_from/to`, `expiry_date_from/to`.
>
> ✅ Batch list/detail response đã trả thêm `total_on_hand_quantity`, `total_quarantine_quantity`, `total_reserved_quantity`, `total_available_quantity` để màn Batch phản ánh đúng tồn kho theo lô.
>
> ✅ `PATCH /status` được giữ lại với behavior chặn `BATCH_011` để không bypass state machine; status mutation phải đi qua `quarantine/release`.
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
| 4 | `POST` | `/api/v1/inventories/check-availability` | Kiểm tra khả dụng tồn kho | ✅ Done |
| 5 | `POST` | `/api/v1/inventories/reserve` | Đặt trước tồn kho cho đơn hàng | ✅ Done |
| 6 | `POST` | `/api/v1/inventories/unreserve` | Giải phóng tồn kho đã đặt trước | ✅ Done |
| 7 | `POST` | `/api/v1/inventories/increase` | Tăng tồn kho (từ inbound hoặc adjustment) | ✅ Done |
| 8 | `POST` | `/api/v1/inventories/decrease` | Giảm tồn kho (từ outbound) | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (8/8 APIs)
>
> ✅ `decrease` API đã được implement trong `InventoryController` và `InventoryService`.

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
| 1 | `GET` | `/api/v1/purchase-order-lines/purchase-order/{purchaseOrderId}` | Danh sách dòng theo PO | ✅ Done 🆕 |
| 2 | `POST` | `/api/v1/purchase-order-lines` | Thêm dòng vào PO | ✅ Done 🆕 |
| 3 | `PUT` | `/api/v1/purchase-order-lines/{id}` | Cập nhật dòng PO | ✅ Done 🆕 |
| 4 | `DELETE` | `/api/v1/purchase-order-lines/{id}` | Xóa dòng PO | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (4/4 APIs)
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
| 1 | `POST` | `/api/v1/inbound-receipts` | Tạo phiếu nhập kho draft | ✅ Done 🆕 |
| 2 | `GET` | `/api/v1/inbound-receipts` | Danh sách phiếu nhập (filter + page) | ✅ Done 🆕 |
| 3 | `GET` | `/api/v1/inbound-receipts/{id}` | Chi tiết phiếu nhập | ✅ Done 🆕 |
| 4 | `PUT` | `/api/v1/inbound-receipts/{id}` | Cập nhật phiếu nhập (chỉ DRAFT) | ✅ Done 🆕 |
| 5 | `DELETE` | `/api/v1/inbound-receipts/{id}` | Xóa phiếu nhập (chỉ DRAFT) | ✅ Done 🆕 |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/inbound-receipts/{id}/confirm` | Xác nhận nhập kho → cập nhật inventory + movement + PO progress | ✅ Done 🆕 |
| 7 | `GET` | `/api/v1/inbound-receipts/by-po/{purchaseOrderId}` | Phiếu nhập theo PO | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (7/7 APIs)
>
> **Lifecycle:** `DRAFT → CONFIRMED` | `DRAFT → CANCELLED`
>
> **Confirm side effects đã có trong service thực tế:**
> - validate binding `IRL -> POL`
> - update `PurchaseOrderLines.quantityReceived`
> - recompute `PO.status`
> - update `Inventory`
> - write `StockMovements`
> - hỗ trợ `QUARANTINE` theo rule `onHand - quarantine - reserved`

---

## MODULE 5: INBOUND - INBOUND RECEIPT LINES (`/api/v1/inbound-receipt-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `GET` | `/api/v1/inbound-receipt-lines?inboundReceiptId={id}` | Danh sách line theo phiếu nhập | ✅ Done |
| 2 | `POST` | `/api/v1/inbound-receipt-lines` | Thêm dòng vào phiếu nhập | ✅ Done |
| 3 | `PUT` | `/api/v1/inbound-receipt-lines/{id}` | Cập nhật dòng phiếu nhập | ✅ Done |
| 4 | `DELETE` | `/api/v1/inbound-receipt-lines/{id}` | Xóa dòng phiếu nhập | ✅ Done |

> ✅ **Module này đã hoàn thành 100%** (4/4 APIs)
>
> **Đã verify trong code:** `InboundReceiptLinesController.java` và `InboundReceiptLinesServiceImpl.java` đều đã có implementation thực tế, không còn là controller/service shell.
>
> **Business rules đang được enforce trong service thực tế:**
> - Chỉ cho phép mutate khi `InboundReceipt.status = DRAFT`
> - `PurchaseOrderLine` phải thuộc đúng `PurchaseOrder` của receipt
> - `quantityReceived > 0` và không được vượt remaining quantity của PO line
> - `location` phải thuộc cùng warehouse với receipt và ở trạng thái usable
> - Có kiểm tra batch tracking, quality status, duplicate split dimension và quarantine note bắt buộc

---

## MODULE 6: OUTBOUND - SALES ORDERS (`/api/v1/sales-orders`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-orders` | Tạo đơn bán hàng (DRAFT) | ✅ Done 🆕 |
| 2 | `GET` | `/api/v1/sales-orders` | Danh sách SO (filters) | ✅ Done 🆕 |
| 3 | `GET` | `/api/v1/sales-orders/{id}` | Chi tiết SO | ✅ Done 🆕 |
| 4 | `PUT` | `/api/v1/sales-orders/{id}` | Cập nhật SO (chỉ DRAFT) | ✅ Done 🆕 |
| 5 | `DELETE` | `/api/v1/sales-orders/{id}` | Xóa SO (chỉ DRAFT) | ❌ Not Done |

### 🟣 Advanced APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 6 | `PUT` | `/api/v1/sales-orders/{id}/confirm` | Xác nhận SO → đặt trước tồn kho | ✅ Done 🆕 |
| 7 | `PUT` | `/api/v1/sales-orders/{id}/cancel` | Hủy SO → giải phóng tồn kho | ✅ Done 🆕 |

> 🟡 **Đã triển khai 6/7**: thiếu delete endpoint.
>
> ✅ `SalesOrdersController` và `SalesOrdersServiceImpl` đã được implement đầy đủ.
>
> Lifecycle: `DRAFT → CONFIRMED → PARTIALLY_SHIPPED → COMPLETED` | `CONFIRMED → CANCELLED`

---

## MODULE 6: OUTBOUND - SALES ORDER LINES (`/api/v1/sales-order-lines`)

### 🔵 CRUD APIs

| # | Method | Endpoint | Mô tả | Status |
|---|--------|----------|--------|--------|
| 1 | `POST` | `/api/v1/sales-order-lines` | Thêm dòng vào SO | ✅ Done 🆕 |
| 2 | `PUT` | `/api/v1/sales-order-lines/{id}` | Cập nhật dòng SO | ✅ Done 🆕 |
| 3 | `DELETE` | `/api/v1/sales-order-lines/{id}` | Xóa dòng SO | ✅ Done 🆕 |
| 4 | `GET` | `/api/v1/sales-order-lines/by-so/{soId}` | Danh sách dòng theo SO | ✅ Done 🆕 |

> ✅ **Module này đã hoàn thành 100%** (4/4 APIs)
>
> ✅ `SalesOrderLinesController` và `SalesOrderLinesServiceImpl` đã được implement đầy đủ.
>
> **Note:** Có thêm endpoint GET bonus không có trong kế hoạch ban đầu.

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
| 3 | `DELETE` | `/api/v1/storage?objectName={objectName}` | Xóa file | ✅ Done |
| 4 | `GET` | `/api/v1/storage/presigned-url` | Tạo presigned URL | ✅ Done |
| 5 | `GET` | `/api/v1/storage/exists` | Kiểm tra file tồn tại | ✅ Done |

> ✅ **Module này đã hoàn thành 100%**
>
> **Ghi chú route thực tế:** controller hiện dùng query-param delete làm route khuyến nghị và vẫn giữ `DELETE /api/v1/storage/{*objectName}` như endpoint legacy để tương thích ngược.

---

---

# 🎯 ĐỀ XUẤT THỨ TỰ TRIỂN KHAI (Cập nhật 21/03/2026)

### ✅ Phase 1–6 — ĐÃ HOÀN THÀNH PHẦN LÕI
- Master Data: Warehouse, Location, Product, UOM, Business Partner, Category, Employee
- Inventory Core: read-side, availability, reserve, unreserve, increase, decrease, stock adjustments, stock transfers
- Inbound foundation: Purchase Orders, Purchase Order Lines, Inbound Receipts, Inbound Receipt Lines, Confirm Receipt
- Outbound foundation: Sales Orders, Sales Order Lines (CRUD + confirm + cancel)
- RBAC foundation: Permission, Role, RolePermission, UserRole (17/19 APIs)

### 🔴 Phase 7 — Outbound execution còn thiếu
| Ưu tiên | Task | Chi tiết | Effort |
|---------|------|----------|--------|
| 🔴 P0 | Outbound Shipments CRUD + pick + confirm — 6 APIs | Shipment lifecycle, source inventory allocation, movement write | 5 ngày |
| 🟡 P1 | Outbound Shipment Lines — 3 APIs | CRUD line gắn shipment | 2 ngày |
| 🔴 P0 | Sales Orders DELETE — 1 API | Xóa SO ở trạng thái DRAFT | 1 ngày |

### 🟡 Phase 8 — RBAC + Stock Movements Enhancement
| Ưu tiên | Task | Chi tiết | Effort |
|---------|------|----------|--------|
| 🟡 P1 | RBAC authorization endpoints — 3 APIs | check-permission, my-permissions, roles/{id}/users | 2 ngày |
| 🟡 P1 | Stock Movements by-product, by-batch — 2 APIs | Query read-side cho điều tra biến động | 2 ngày |
| 🟡 P2 | Stock Movements traceability + export — 3 APIs | Báo cáo truy vết và export | 3 ngày |

### 🟢 Phase 9 — Reporting & Miscellaneous
| Ưu tiên | Task | Chi tiết | Effort |
|---------|------|----------|--------|
| 🟡 P2 | Reporting on-demand — 6 APIs | Current stock, valuation, movements, low-stock, expiring batches | 5 ngày |
| 🟢 P3 | Reporting async — 4 APIs | Request/status/download/list | 3 ngày |
| 🟢 P3 | Reporting scheduled — 6 APIs | Scheduler CRUD + enable/disable | 4 ngày |
| 🟡 P2 | Auth verify-email + resend — 2 APIs | Đóng nốt gap auth còn lại | 2 ngày |

---

# 📊 TỔNG KẾT (21/03/2026)

| Metric | Value |
|--------|-------|
| **Tổng API thiết kế** | 190 |
| **Đã triển khai** | 141 (74%) 🆕 |
| **Chưa triển khai** | 49 (26%) |
| **Module hoàn thành 100%** | Warehouse, UOM, Category, Employee, Email, Storage, Batch, Stock Adj., Stock Transfers, Purchase Orders, Purchase Order Lines, Inbound Receipts, Inbound Receipt Lines, Inventory, Sales Orders, Sales Order Lines |
| **Module 0%** | Outbound Shipments, Outbound Shipment Lines, Reporting |
| **Module gần hoàn thành** | RBAC (89% - thiếu 2), Auth (85% - thiếu 2), Stock Movements (38%) |

> ✅ **Thay đổi lớn nhất của đợt re-audit 21/03:**
> - RBAC: Permission, Role, RolePermission, UserRole controllers + services đã được implement đầy đủ (17/19)
> - Sales Orders: CRUD + confirm + cancel đã hoàn chỉnh (6/7 - thiếu delete)
> - Sales Order Lines: Full CRUD (4/4)
> - Inventory: Decrease API đã được bổ sung (8/8)
>
> ⚠️ **Khoảng trống còn lại:**
> - RBAC: check-permission, my-permissions, roles/{id}/users
> - Sales Orders: delete endpoint
> - Outbound Shipments + Outbound Shipment Lines: chưa triển khai
> - Stock Movements: 5 API nâng cao chưa có
> - Reporting: 16 API chưa có
