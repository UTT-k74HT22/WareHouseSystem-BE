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

## 1. Master Data APIs

### Warehouses

#### List Warehouses
```http
GET /api/warehouses?status=ACTIVE&page=0&size=20

Response (200):
{
  "content": [
    {
      "id": 1,
      "code": "WH-MAIN",
      "name": "Main Warehouse",
      "address": "123 Main St",
      "city": "Hanoi",
      "country": "Vietnam",
      "type": "MAIN",
      "status": "ACTIVE",
      "manager": {
        "id": 5,
        "name": "John Manager"
      },
      "createdAt": "2026-01-15T10:00:00Z"
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

#### Create Warehouse
```http
POST /api/warehouses
Authorization: Bearer {token}
Permission: WAREHOUSE:CREATE

Request:
{
  "code": "WH-NORTH",
  "name": "North Warehouse",
  "address": "456 North Ave",
  "city": "Hanoi",
  "state": "Hanoi",
  "country": "Vietnam",
  "postalCode": "100000",
  "type": "SATELLITE",
  "managerId": 5
}

Response (201):
{
  "id": 6,
  "code": "WH-NORTH",
  "name": "North Warehouse",
  "status": "ACTIVE",
  "createdAt": "2026-01-21T10:30:00Z"
}
```

#### Get Warehouse by ID
```http
GET /api/warehouses/{id}

Response (200):
{
  "id": 1,
  "code": "WH-MAIN",
  "name": "Main Warehouse",
  "address": "123 Main St",
  "city": "Hanoi",
  "locations": [
    {
      "id": 1,
      "code": "A-01",
      "name": "Zone A - Row 1"
    }
  ]
}
```

#### Update Warehouse
```http
PUT /api/warehouses/{id}
Permission: WAREHOUSE:UPDATE

Request:
{
  "name": "Main Warehouse - Updated",
  "address": "123 Main St, Building B",
  "status": "ACTIVE"
}

Response (200):
{
  "id": 1,
  "code": "WH-MAIN",
  "name": "Main Warehouse - Updated",
  "updatedAt": "2026-01-21T10:35:00Z"
}
```

#### Delete Warehouse (Soft Delete)
```http
DELETE /api/warehouses/{id}
Permission: WAREHOUSE:DELETE

Response (204): No Content
```

---

### Products

#### List Products
```http
GET /api/products?status=ACTIVE&search=laptop&page=0&size=20

Response (200):
{
  "content": [
    {
      "id": 100,
      "sku": "PROD-LAPTOP-001",
      "name": "Dell Laptop XPS 15",
      "description": "High performance laptop",
      "uom": {
        "id": 1,
        "code": "PCS",
        "name": "Pieces"
      },
      "status": "ACTIVE",
      "minStockLevel": 10,
      "createdAt": "2026-01-10T08:00:00Z"
    }
  ]
}
```

#### Create Product
```http
POST /api/products
Permission: PRODUCT:CREATE

Request:
{
  "sku": "PROD-MOUSE-001",
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "uomId": 1,
  "category": "ELECTRONICS",
  "minStockLevel": 50,
  "maxStockLevel": 500
}

Response (201):
{
  "id": 101,
  "sku": "PROD-MOUSE-001",
  "name": "Wireless Mouse",
  "status": "ACTIVE",
  "createdAt": "2026-01-21T10:40:00Z"
}
```

#### Get Product by SKU
```http
GET /api/products/by-sku/{sku}

Response (200):
{
  "id": 100,
  "sku": "PROD-LAPTOP-001",
  "name": "Dell Laptop XPS 15",
  "currentStock": [
    {
      "warehouseId": 1,
      "warehouseName": "Main Warehouse",
      "onHandQuantity": 25,
      "reservedQuantity": 5,
      "availableQuantity": 20
    }
  ]
}
```

---

### Locations

#### List Locations by Warehouse
```http
GET /api/warehouses/{warehouseId}/locations

Response (200):
[
  {
    "id": 1,
    "code": "A-01",
    "name": "Zone A - Row 1",
    "zone": "A",
    "type": "STORAGE",
    "capacity": 1000,
    "currentUtilization": 750
  },
  {
    "id": 2,
    "code": "B-01",
    "name": "Zone B - Row 1",
    "zone": "B",
    "type": "PICKING",
    "capacity": 500,
    "currentUtilization": 300
  }
]
```

#### Create Location
```http
POST /api/warehouses/{warehouseId}/locations
Permission: LOCATION:CREATE

Request:
{
  "code": "C-01",
  "name": "Zone C - Row 1",
  "zone": "C",
  "type": "STORAGE",
  "capacity": 800
}

Response (201):
{
  "id": 10,
  "code": "C-01",
  "warehouseId": 1,
  "status": "ACTIVE"
}
```

---

## 2. Inventory APIs

#### Get Inventory Overview
```http
GET /api/inventory?productId=100&warehouseId=1

Response (200):
{
  "content": [
    {
      "id": 500,
      "product": {
        "id": 100,
        "sku": "PROD-LAPTOP-001",
        "name": "Dell Laptop XPS 15"
      },
      "warehouse": {
        "id": 1,
        "code": "WH-MAIN",
        "name": "Main Warehouse"
      },
      "location": {
        "id": 1,
        "code": "A-01"
      },
      "batch": {
        "id": 50,
        "batchNumber": "BATCH-2026-001",
        "expiryDate": "2027-01-15"
      },
      "onHandQuantity": 25,
      "reservedQuantity": 5,
      "availableQuantity": 20,
      "lastUpdated": "2026-01-21T09:00:00Z"
    }
  ]
}
```

#### Check Available Stock
```http
GET /api/inventory/available?productId=100&warehouseId=1

Response (200):
{
  "productId": 100,
  "productSku": "PROD-LAPTOP-001",
  "warehouseId": 1,
  "totalOnHand": 25,
  "totalReserved": 5,
  "totalAvailable": 20,
  "byLocation": [
    {
      "locationId": 1,
      "locationCode": "A-01",
      "available": 20
    }
  ]
}
```

#### Inventory Adjustment
```http
POST /api/inventory/adjust
Permission: INVENTORY:ADJUST

Request:
{
  "productId": 100,
  "warehouseId": 1,
  "locationId": 1,
  "adjustmentType": "MANUAL",
  "quantityChange": -2,
  "reason": "DAMAGE",
  "notes": "2 units damaged during inspection"
}

Response (201):
{
  "id": 1001,
  "adjustmentNumber": "ADJ-20260121-001",
  "status": "PENDING_APPROVAL",
  "createdAt": "2026-01-21T10:45:00Z",
  "createdBy": {
    "id": 1,
    "username": "admin"
  }
}
```

#### Low Stock Alert
```http
GET /api/inventory/low-stock

Response (200):
[
  {
    "productId": 105,
    "productSku": "PROD-KEYBOARD-001",
    "productName": "Mechanical Keyboard",
    "currentStock": 8,
    "minStockLevel": 20,
    "deficit": 12,
    "status": "CRITICAL"
  }
]
```

---

## 3. Inbound APIs

#### Create Purchase Order
```http
POST /api/purchase-orders
Permission: PURCHASE_ORDER:CREATE

Request:
{
  "supplierId": 10,
  "warehouseId": 1,
  "expectedDeliveryDate": "2026-01-25",
  "notes": "Urgent order",
  "lines": [
    {
      "productId": 100,
      "quantity": 50,
      "unitPrice": 1000.00
    },
    {
      "productId": 101,
      "quantity": 100,
      "unitPrice": 25.00
    }
  ]
}

Response (201):
{
  "id": 5001,
  "orderNumber": "PO-20260121-001",
  "status": "DRAFT",
  "totalAmount": 52500.00,
  "createdAt": "2026-01-21T10:50:00Z"
}
```

#### Confirm Purchase Order
```http
PUT /api/purchase-orders/{id}/confirm
Permission: PURCHASE_ORDER:CONFIRM

Response (200):
{
  "id": 5001,
  "orderNumber": "PO-20260121-001",
  "status": "CONFIRMED",
  "confirmedAt": "2026-01-21T10:55:00Z"
}
```

#### Create Inbound Receipt
```http
POST /api/inbound-receipts
Permission: INBOUND:CREATE

Request:
{
  "purchaseOrderId": 5001,
  "warehouseId": 1,
  "receivedDate": "2026-01-21",
  "lines": [
    {
      "purchaseOrderLineId": 10001,
      "productId": 100,
      "locationId": 1,
      "receivedQuantity": 50,
      "batchNumber": "BATCH-2026-050",
      "manufactureDate": "2026-01-15",
      "expiryDate": "2027-01-15"
    }
  ]
}

Response (201):
{
  "id": 6001,
  "receiptNumber": "INB-20260121-001",
  "status": "DRAFT",
  "createdAt": "2026-01-21T11:00:00Z"
}
```

#### Confirm Inbound Receipt (Increase Stock)
```http
PUT /api/inbound-receipts/{id}/confirm
Permission: INBOUND:CONFIRM

Response (200):
{
  "id": 6001,
  "receiptNumber": "INB-20260121-001",
  "status": "CONFIRMED",
  "confirmedAt": "2026-01-21T11:05:00Z",
  "stockUpdated": true,
  "affectedProducts": [
    {
      "productId": 100,
      "quantityReceived": 50,
      "newStockLevel": 75
    }
  ]
}
```

---

## 4. Outbound APIs

#### Create Sales Order
```http
POST /api/sales-orders
Permission: SALES_ORDER:CREATE

Request:
{
  "customerId": 20,
  "warehouseId": 1,
  "deliveryDate": "2026-01-23",
  "lines": [
    {
      "productId": 100,
      "quantity": 10,
      "unitPrice": 1200.00
    }
  ]
}

Response (201):
{
  "id": 7001,
  "orderNumber": "SO-20260121-001",
  "status": "DRAFT",
  "totalAmount": 12000.00
}
```

#### Confirm Sales Order (Reserve Stock)
```http
PUT /api/sales-orders/{id}/confirm
Permission: SALES_ORDER:CONFIRM

Response (200):
{
  "id": 7001,
  "orderNumber": "SO-20260121-001",
  "status": "CONFIRMED",
  "stockReserved": true,
  "reservedItems": [
    {
      "productId": 100,
      "quantityReserved": 10
    }
  ]
}
```

#### Create Outbound Shipment
```http
POST /api/outbound-shipments
Permission: OUTBOUND:CREATE

Request:
{
  "salesOrderId": 7001,
  "warehouseId": 1,
  "shipmentDate": "2026-01-22",
  "lines": [
    {
      "salesOrderLineId": 12001,
      "productId": 100,
      "locationId": 1,
      "quantityToShip": 10,
      "batchId": 50
    }
  ]
}

Response (201):
{
  "id": 8001,
  "shipmentNumber": "SHIP-20260121-001",
  "status": "DRAFT"
}
```

#### Confirm Shipment (Decrease Stock)
```http
PUT /api/outbound-shipments/{id}/confirm
Permission: OUTBOUND:CONFIRM

Response (200):
{
  "id": 8001,
  "shipmentNumber": "SHIP-20260121-001",
  "status": "SHIPPED",
  "shippedAt": "2026-01-22T14:00:00Z",
  "stockUpdated": true
}
```

---

## 5. Reporting APIs

#### Generate Inventory Report
```http
POST /api/reports/inventory
Permission: REPORT:GENERATE

Request:
{
  "reportType": "INVENTORY_SNAPSHOT",
  "filters": {
    "warehouseIds": [1, 2],
    "productIds": [100, 101],
    "includeZeroStock": false
  },
  "format": "PDF"
}

Response (202 Accepted):
{
  "jobId": "report-job-12345",
  "status": "PENDING",
  "estimatedTime": 30
}
```

#### Check Report Status
```http
GET /api/reports/jobs/{jobId}

Response (200):
{
  "jobId": "report-job-12345",
  "status": "COMPLETED",
  "format": "PDF",
  "fileSize": 524288,
  "downloadUrl": "/api/reports/jobs/report-job-12345/download",
  "createdAt": "2026-01-21T11:10:00Z",
  "completedAt": "2026-01-21T11:10:25Z"
}
```

#### Download Report
```http
GET /api/reports/jobs/{jobId}/download

Response (200):
Content-Type: application/pdf
Content-Disposition: attachment; filename="inventory-report-20260121.pdf"

[Binary PDF Data]
```

---

## 6. Excel Import APIs

#### Import Products
```http
POST /api/imports/products
Content-Type: multipart/form-data
Permission: IMPORT:EXECUTE

Request:
- file: products.xlsx

Response (202 Accepted):
{
  "jobId": "import-job-67890",
  "status": "PENDING",
  "fileName": "products.xlsx",
  "totalRows": 150
}
```

#### Check Import Status
```http
GET /api/imports/jobs/{jobId}

Response (200):
{
  "jobId": "import-job-67890",
  "status": "COMPLETED_WITH_ERRORS",
  "totalRows": 150,
  "successfulRows": 145,
  "failedRows": 5,
  "startedAt": "2026-01-21T11:15:00Z",
  "completedAt": "2026-01-21T11:16:30Z"
}
```

#### Get Import Errors
```http
GET /api/imports/jobs/{jobId}/errors

Response (200):
[
  {
    "rowNumber": 25,
    "field": "sku",
    "error": "SKU 'PROD-001' already exists",
    "value": "PROD-001"
  },
  {
    "rowNumber": 78,
    "field": "uomId",
    "error": "UOM with ID 999 not found",
    "value": "999"
  }
]
```

---

## 📱 WebSocket Notifications

### Connect to WebSocket
```javascript
const socket = new SockJS('http://localhost:8080/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({
  'Authorization': 'Bearer ' + accessToken
}, function(frame) {
  // Subscribe to notifications
  stompClient.subscribe('/user/queue/notifications', function(message) {
    const notification = JSON.parse(message.body);
    console.log('Received notification:', notification);
  });
});
```

### Notification Format
```json
{
  "eventType": "INBOUND_COMPLETED",
  "timestamp": "2026-01-21T11:05:00Z",
  "message": "Inbound receipt #INB-20260121-001 completed",
  "severity": "INFO",
  "data": {
    "receiptId": 6001,
    "receiptNumber": "INB-20260121-001",
    "warehouseId": 1,
    "totalItems": 50
  },
  "actionUrl": "/inbound-receipts/6001"
}
```

---

**Cập nhật lần cuối:** 21/01/2026  
**Version:** 1.0

