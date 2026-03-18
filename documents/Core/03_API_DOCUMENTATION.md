# API Documentation - Warehouse Management System
## RESTful API Specification

---

## 📋 Mục Lục
1. [API Standards](#api-standards)
2. [Authentication](#authentication)
3. [Error Handling](#error-handling)
4. [Module APIs](#module-apis)
5. [Request/Response Examples](#requestresponse-examples)

---

## ���� API Standards

### Base URL
```
Development:  http://localhost:8080/api
Production:   https://api.wms.com/api
```

### HTTP Methods Convention
| Method | Purpose | Idempotent |
|--------|---------|------------|
| `GET` | Retrieve resources | ✅ Yes |
| `POST` | Create new resources | ❌ No |
| `PUT` | Update entire resource | ✅ Yes |
| `PATCH` | Partial update | ❌ No |
| `DELETE` | Delete resource | ✅ Yes |

### Response Status Codes
| Code | Meaning | Usage |
|------|---------|-------|
| `200` | OK | Successful GET, PUT, DELETE |
| `201` | Created | Successful POST |
| `204` | No Content | Successful DELETE with no response body |
| `400` | Bad Request | Validation error |
| `401` | Unauthorized | Missing or invalid token |
| `403` | Forbidden | Insufficient permissions |
| `404` | Not Found | Resource not found |
| `409` | Conflict | Business rule violation |
| `422` | Unprocessable Entity | Semantic validation error |
| `429` | Too Many Requests | Rate limit exceeded |
| `500` | Internal Server Error | Server error |

### Common Headers
```http
# Request Headers
Authorization: Bearer {access_token}
Content-Type: application/json
Accept: application/json
X-Request-ID: {uuid}

# Response Headers
Content-Type: application/json
X-Request-ID: {uuid}
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642780800
```

### Pagination
```http
GET /api/products?page=0&size=20&sort=name,asc

Response:
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalPages": 10,
  "totalElements": 200,
  "first": true,
  "last": false
}
```

### Filtering
```http
GET /api/products?status=ACTIVE&category=ELECTRONICS&name=laptop
GET /api/inventory?productId=123&warehouseId=1&minQuantity=10
```

---

## 🔐 Authentication

### 1. Login
**Endpoint:** `POST /api/auth/login`

**Request:**
```json
{
  "username": "admin",
  "password": "Admin@123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "roles": ["ADMIN"]
  }
}
```

**Error (401 Unauthorized):**
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/api/auth/login"
}
```

**Rate Limit:** 5 requests per minute per IP

---

### 2. Refresh Token
**Endpoint:** `POST /api/auth/refresh`

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

### 3. Logout
**Endpoint:** `POST /api/auth/logout`

**Headers:**
```http
Authorization: Bearer {access_token}
```

**Response (200 OK):**
```json
{
  "message": "Logout successful"
}
```

---

### 4. Get Current User
**Endpoint:** `GET /api/auth/me`

**Headers:**
```http
Authorization: Bearer {access_token}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+84123456789",
    "avatarUrl": "https://..."
  },
  "roles": [
    {
      "id": 1,
      "name": "ADMIN",
      "permissions": ["PRODUCT:READ", "PRODUCT:WRITE", "..."]
    }
  ]
}
```

---

## ❌ Error Handling

### Standard Error Response Format
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "errors": [
    {
      "field": "sku",
      "message": "SKU must not be blank",
      "rejectedValue": ""
    },
    {
      "field": "name",
      "message": "Name must be between 3 and 100 characters",
      "rejectedValue": "AB"
    }
  ]
}
```

### Common Error Scenarios

#### 1. Validation Error (400)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid request parameters",
  "errors": [
    {
      "field": "quantity",
      "message": "Quantity must be positive",
      "rejectedValue": -10
    }
  ]
}
```

#### 2. Unauthorized (401)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

#### 3. Forbidden (403)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You don't have permission to access this resource"
}
```

#### 4. Not Found (404)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Product with SKU 'PROD-001' not found"
}
```

#### 5. Business Rule Violation (409)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient stock",
  "details": {
    "productId": 123,
    "requested": 100,
    "available": 50
  }
}
```

#### 6. Rate Limit (429)
```json
{
  "timestamp": "2026-01-21T10:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 60 seconds",
  "retryAfter": 60
}
```

---

## 📦 Module APIs

---

### 📦 Module 1: Authentication & RBAC

#### Auth APIs (`/api/auth`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/auth/login` | Đăng nhập | Public |
| `POST` | `/api/auth/refresh` | Refresh token | Public |
| `POST` | `/api/auth/logout` | Đăng xuất | isAuthenticated |
| `GET`  | `/api/auth/me` | Lấy thông tin user hiện tại | isAuthenticated |

#### User APIs (`/api/users`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/users` | Tạo user mới | ADMIN |
| `GET`  | `/api/users` | Danh sách users (phân trang) | ADMIN |
| `GET`  | `/api/users/{id}` | Chi tiết user | ADMIN |
| `PUT`  | `/api/users/{id}` | Cập nhật user | ADMIN |
| `DELETE`| `/api/users/{id}` | Xóa user | ADMIN |

#### Role APIs (`/api/roles`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/roles` | Tạo role mới | ADMIN |
| `GET`  | `/api/roles` | Danh sách roles | ADMIN |
| `GET`  | `/api/roles/{id}` | Chi tiết role | ADMIN |
| `PUT`  | `/api/roles/{id}` | Cập nhật role | ADMIN |
| `DELETE`| `/api/roles/{id}` | Xóa role | ADMIN |

#### Permission APIs (`/api/permissions`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `GET`  | `/api/permissions` | Danh sách permissions | ADMIN |

#### User Role APIs (`/api/user-roles`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/user-roles` | Gán role cho user | ADMIN |
| `GET`  | `/api/user-roles/user/{userId}` | Lấy roles của user | ADMIN |
| `GET`  | `/api/user-roles/role/{roleId}` | Lấy users của role | ADMIN |
| `DELETE`| `/api/user-roles` | Xóa role của user | ADMIN |

#### Role Permission APIs (`/api/role-permissions`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/role-permissions` | Gán permission cho role | ADMIN |
| `GET`  | `/api/role-permissions/role/{roleId}` | Lấy permissions của role | ADMIN |
| `DELETE`| `/api/role-permissions` | Xóa permission của role | ADMIN |

#### Employee APIs (`/api/employees`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/employees` | Tạo employee | ADMIN |
| `GET`  | `/api/employees` | Danh sách employees | isAuthenticated |
| `GET`  | `/api/employees/{id}` | Chi tiết employee | isAuthenticated |
| `PUT`  | `/api/employees/{id}` | Cập nhật employee | ADMIN |
| `DELETE`| `/api/employees/{id}` | Xóa employee | ADMIN |

#### OTP APIs (`/api/otps`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/otps/send` | Gửi OTP | Public |
| `POST` | `/api/otps/verify` | Xác minh OTP | Public |
| `POST` | `/api/otps/refresh` | Làm mới OTP | Public |

---

### 📦 Module 2: Master Data (Dữ liệu chính)

#### Warehouse APIs (`/api/warehouses`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/warehouses` | Tạo warehouse | ADMIN |
| `GET`  | `/api/warehouses` | Danh sách warehouses | isAuthenticated |
| `GET`  | `/api/warehouses/{id}` | Chi tiết warehouse | isAuthenticated |
| `PUT`  | `/api/warehouses/{id}` | Cập nhật warehouse | ADMIN |
| `DELETE`| `/api/warehouses/{id}` | Xóa warehouse | ADMIN |
| `GET`  | `/api/warehouses/{id}/locations` | Danh sách locations | isAuthenticated |

#### Location APIs (`/api/locations`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `GET`  | `/api/locations` | Danh sách locations | isAuthenticated |
| `GET`  | `/api/locations/{id}` | Chi tiết location | isAuthenticated |

#### Product APIs (`/api/products`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/products` | Tạo sản phẩm | ADMIN/MANAGER |
| `GET`  | `/api/products` | Danh sách sản phẩm | isAuthenticated |
| `GET`  | `/api/products/{id}` | Chi tiết sản phẩm | isAuthenticated |
| `GET`  | `/api/products/by-sku/{sku}` | Tìm theo SKU | isAuthenticated |
| `PUT`  | `/api/products/{id}` | Cập nhật sản phẩm | ADMIN/MANAGER |
| `DELETE`| `/api/products/{id}` | Xóa sản phẩm | ADMIN |

#### Category APIs (`/api/categories`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/categories` | Tạo category | ADMIN |
| `GET`  | `/api/categories` | Danh sách categories | USER/ADMIN |
| `GET`  | `/api/categories/{id}` | Chi tiết category | USER/ADMIN |
| `PUT`  | `/api/categories/{id}` | Cập nhật category | ADMIN |
| `PATCH`| `/api/categories/{id}/status` | Cập nhật trạng thái | ADMIN |

#### Business Partner APIs (`/api/business-partners`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/business-partners` | Tạo đối tác | ADMIN/MANAGER |
| `GET`  | `/api/business-partners` | Danh sách đối tác | isAuthenticated |
| `GET`  | `/api/business-partners/{id}` | Chi tiết đối tác | isAuthenticated |
| `PUT`  | `/api/business-partners/{id}` | Cập nhật đối tác | ADMIN/MANAGER |
| `DELETE`| `/api/business-partners/{id}` | Xóa đối tác | ADMIN |

#### Units of Measure APIs (`/api/units-of-measure`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/units-of-measure` | Tạo UoM | ADMIN |
| `GET`  | `/api/units-of-measure` | Danh sách UoM | isAuthenticated |
| `GET`  | `/api/units-of-measure/{id}` | Chi tiết UoM | isAuthenticated |
| `PUT`  | `/api/units-of-measure/{id}` | Cập nhật UoM | ADMIN |
| `DELETE`| `/api/units-of-measure/{id}` | Xóa UoM | ADMIN |

---

### 📦 Module 3: Batch Management (Quản lý lô hàng)

#### Batch APIs (`/api/batches`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/batches` | Tạo batch mới | isAuthenticated |
| `GET`  | `/api/batches` | Danh sách batches (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/batches/{id}` | Chi tiết batch | isAuthenticated |
| `PUT`  | `/api/batches/{id}` | Cập nhật batch | isAuthenticated |
| `PATCH`| `/api/batches/{id}/status` | Đổi status batch | isAuthenticated |
| `PUT`  | `/api/batches/{id}/quarantine` | Chuyển sang QUARANTINE | isAuthenticated |
| `PUT`  | `/api/batches/{id}/release` | Release từ QUARANTINE | isAuthenticated |
| `GET`  | `/api/batches/{id}/traceability` | Truy xuất nguồn gốc batch | isAuthenticated |
| `GET`  | `/api/batches/expiring` | Danh sách batch sắp hết hạn | isAuthenticated |
| `GET`  | `/api/batches/fifo-recommendations` | FIFO recommendations | isAuthenticated |
| `GET`  | `/api/batches/by-product/{productId}` | Batches theo sản phẩm | isAuthenticated |

---

### 📦 Module 4: Inventory Management (Quản lý tồn kho)

#### Inventory APIs (`/api/inventories`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `GET`  | `/api/inventories` | Danh sách tồn kho (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/inventories/summary/{productId}` | Tổng tồn kho theo sản phẩm | isAuthenticated |
| `GET`  | `/api/inventories/by-location` | Tồn kho theo vị trí | isAuthenticated |
| `POST` | `/api/inventories/check-availability` | Kiểm tra tồn kho khả dụng | isAuthenticated |
| `POST` | `/api/inventories/reserve` | Đặt trước tồn kho | isAuthenticated |
| `POST` | `/api/inventories/unreserve` | Giải phóng tồn kho đã đặt | isAuthenticated |
| `POST` | `/api/inventories/increase` | Tăng tồn kho | isAuthenticated |
| `POST` | `/api/inventories/decrease` | Giảm tồn kho | isAuthenticated |

#### Stock Adjustments APIs (`/api/stock-adjustments`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/stock-adjustments` | Tạo yêu cầu điều chỉnh | isAuthenticated |
| `GET`  | `/api/stock-adjustments` | Danh sách điều chỉnh (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/stock-adjustments/{id}` | Chi tiết điều chỉnh | isAuthenticated |
| `PUT`  | `/api/stock-adjustments/{id}/approve` | Phê duyệt điều chỉnh | isAuthenticated |
| `PUT`  | `/api/stock-adjustments/{id}/reject` | Từ chối điều chỉnh | isAuthenticated |

#### Stock Transfers APIs (`/api/stock-transfers`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/stock-transfers` | Tạo chuyển kho | isAuthenticated |
| `GET`  | `/api/stock-transfers` | Danh sách chuyển kho (phân trang) | isAuthenticated |
| `GET`  | `/api/stock-transfers/{id}` | Chi tiết chuyển kho | isAuthenticated |
| `PUT`  | `/api/stock-transfers/{id}/complete` | Hoàn thành chuyển kho | isAuthenticated |
| `PUT`  | `/api/stock-transfers/{id}/cancel` | Hủy chuyển kho | isAuthenticated |

#### Stock Movements APIs (`/api/stock-movements`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `GET`  | `/api/stock-movements` | Danh sách movements (phân trang) | isAuthenticated |
| `GET`  | `/api/stock-movements/{id}` | Chi tiết movement | isAuthenticated |
| `GET`  | `/api/stock-movements/reference/{referenceType}/{referenceId}` | Movements theo reference | isAuthenticated |

---

### 📦 Module 5: Inbound Operations (Nhập kho)

#### Purchase Order APIs (`/api/purchase-orders`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/purchase-orders` | Tạo PO (DRAFT) | isAuthenticated |
| `GET`  | `/api/purchase-orders` | Danh sách POs (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/purchase-orders/{id}` | Chi tiết PO | isAuthenticated |
| `PUT`  | `/api/purchase-orders/{id}` | Cập nhật PO (DRAFT) | isAuthenticated |
| `DELETE`| `/api/purchase-orders/{id}` | Xóa PO (DRAFT) | isAuthenticated |
| `PUT`  | `/api/purchase-orders/{id}/confirm` | Confirm PO | isAuthenticated |

#### Purchase Order Lines APIs (`/api/purchase-order-lines`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/purchase-order-lines` | Thêm dòng PO | isAuthenticated |
| `GET`  | `/api/purchase-order-lines/purchase-order/{purchaseOrderId}` | Danh sách lines theo PO | isAuthenticated |
| `PUT`  | `/api/purchase-order-lines/{id}` | Cập nhật dòng PO | isAuthenticated |
| `DELETE`| `/api/purchase-order-lines/{id}` | Xóa dòng PO | isAuthenticated |

#### Inbound Receipt APIs (`/api/inbound-receipts`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/inbound-receipts` | Tạo phiếu nhập (DRAFT) | isAuthenticated |
| `GET`  | `/api/inbound-receipts` | Danh sách phiếu nhập (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/inbound-receipts/by-po/{purchaseOrderId}` | Phiếu nhập theo PO | isAuthenticated |
| `GET`  | `/api/inbound-receipts/{id}` | Chi tiết phiếu nhập | isAuthenticated |
| `PUT`  | `/api/inbound-receipts/{id}` | Cập nhật phiếu nhập (DRAFT) | isAuthenticated |
| `DELETE`| `/api/inbound-receipts/{id}` | Xóa phiếu nhập (DRAFT) | isAuthenticated |
| `PUT`  | `/api/inbound-receipts/{id}/confirm` | Confirm phiếu nhập | isAuthenticated |

#### Inbound Receipt Lines APIs (`/api/inbound-receipt-lines`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/inbound-receipt-lines` | Thêm dòng phiếu nhập | isAuthenticated |
| `GET`  | `/api/inbound-receipt-lines?inboundReceiptId={id}` | Danh sách lines theo phiếu | isAuthenticated |
| `PUT`  | `/api/inbound-receipt-lines/{id}` | Cập nhật dòng phiếu nhập | isAuthenticated |
| `DELETE`| `/api/inbound-receipt-lines/{id}` | Xóa dòng phiếu nhập | isAuthenticated |

---

### 📦 Module 6: Outbound Operations (Xuất kho) - CHƯA TRIỂN KHAI

#### Sales Order APIs (`/api/sales-orders`) - ❌ CHƯA IMPLEMENT
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/sales-orders` | Tạo SO (DRAFT) | ADMIN/MANAGER |
| `GET`  | `/api/sales-orders` | Danh sách SOs (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/sales-orders/{id}` | Chi tiết SO | isAuthenticated |
| `PUT`  | `/api/sales-orders/{id}` | Cập nhật SO (DRAFT) | ADMIN/MANAGER |
| `DELETE`| `/api/sales-orders/{id}` | Xóa SO (DRAFT) | ADMIN/MANAGER |
| `PUT`  | `/api/sales-orders/{id}/confirm` | Confirm SO (reserve stock) | ADMIN/MANAGER |
| `PUT`  | `/api/sales-orders/{id}/cancel` | Cancel SO (unreserve stock) | ADMIN/MANAGER |

#### Sales Order Lines APIs (`/api/sales-order-lines`) - ❌ CHƯA IMPLEMENT
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/sales-order-lines` | Thêm dòng SO | ADMIN/MANAGER |
| `GET`  | `/api/sales-order-lines/by-so/{soId}` | Danh sách lines theo SO | isAuthenticated |
| `PUT`  | `/api/sales-order-lines/{id}` | Cập nhật dòng SO | ADMIN/MANAGER |
| `DELETE`| `/api/sales-order-lines/{id}` | Xóa dòng SO | ADMIN/MANAGER |

#### Outbound Shipment APIs (`/api/outbound-shipments`) - ❌ CHƯA IMPLEMENT
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/outbound-shipments` | Tạo shipment (DRAFT) | isAuthenticated |
| `GET`  | `/api/outbound-shipments` | Danh sách shipments (phân trang, filter) | isAuthenticated |
| `GET`  | `/api/outbound-shipments/{id}` | Chi tiết shipment | isAuthenticated |
| `PUT`  | `/api/outbound-shipments/{id}/pick` | Bắt đầu picking | isAuthenticated |
| `PUT`  | `/api/outbound-shipments/{id}/confirm` | Confirm shipment (decrease stock) | isAuthenticated |
| `GET`  | `/api/outbound-shipments/{id}/pick-list` | Lấy pick list (FIFO) | isAuthenticated |

#### Outbound Shipment Lines APIs (`/api/outbound-shipment-lines`) - ❌ CHƯA IMPLEMENT
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/outbound-shipment-lines` | Thêm dòng shipment | isAuthenticated |
| `PUT`  | `/api/outbound-shipment-lines/{id}` | Cập nhật dòng shipment | isAuthenticated |
| `DELETE`| `/api/outbound-shipment-lines/{id}` | Xóa dòng shipment | isAuthenticated |

---

### 📦 Module 7: Notifications (Thông báo)

#### Email APIs (`/api/emails`)
| Method | Endpoint | Mô tả | Quyền |
|--------|----------|--------|-------|
| `POST` | `/api/emails/send` | Gửi email | isAuthenticated |
| `POST` | `/api/emails/send-batch` | Gửi email batch | isAuthenticated |

---

## 📊 Tổng kết

### Số lượng API theo Module

| Module | Số APIs | Trạng thái |
|--------|---------|------------|
| Auth & RBAC | ~30 | ✅ Đã implement |
| Master Data | ~35 | ✅ Đã implement |
| Batch Management | 11 | ✅ Đã implement |
| Inventory Management | 17 | ✅ Đã implement |
| Inbound Operations | 18 | ✅ Đã implement |
| **Outbound Operations** | **19** | **❌ CHƯA IMPLEMENT** |
| Notifications | 2 | ✅ Đã implement |
| **TỔNG CỘNG** | **~132** | **113 đã implement** |

### Các API còn thiếu (chưa implement)
- **Module Outbound**: 19 APIs cần triển khai

---

## 📝 Ghi chú

1. **Tất cả APIs đều yêu cầu xác thực** (trừ `/api/auth/login`, `/api/auth/refresh`, `/api/otps/*`)
2. **Response format chuẩn**: `{ "success": true, "message": "...", "data": {...} }`
3. **Tham khảo Swagger UI**: `http://localhost:8080/swagger-ui.html` để xem chi tiết
4. **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

---

**Cập nhật lần cuối:** 17/03/2026  
**Phiên bản:** 2.0  
**Ghi chú:** Cập nhật dựa trên code đã implement thực tế

