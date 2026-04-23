# Hướng dẫn Kiểm thử API Product với Postman

## Quy trình 5 bước chuẩn

> **Mục tiêu**: Hướng dẫn step-by-step kiểm thử API Product (CRUD) từ cơ bản đến automation

---

## Bước 1: Chuẩn bị môi trường

### 1.1 Yêu cầu hệ thống
- [ ] Postman đã cài đặt (Download: https://www.postman.com/downloads/)
- [ ] Ứng dụng WMS đang chạy (localhost:8080)
- [ ] Tài khoản test có quyền Product

### 1.2 Thiết lập Environment

1. Mở Postman → Click **Environments** → **New Environment**
2. Đặt tên: `WHS Local Dev`
3. Thêm các biến:

| Variable | Initial Value | Current Value | Description |
|----------|---------------|---------------|-------------|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` | API Base URL |
| `authToken` | `` | `` | JWT Token (sẽ được set từ login) |
| `productId` | `` | `` | ID sản phẩm test |
| `categoryId` | `CAT001` | `CAT001` | Category ID test |
| `uomId` | `UOM001` | `UOM001` | Unit of Measure ID test |

4. Click **Save**

### 1.3 Thiết lập Authentication

1. Tạo request Login mới:
   - **Method**: POST
   - **URL**: `{{baseUrl}}/api/v1/auth/login`
   - **Body**:
   ```json
   {
     "username": "admin",
     "password": "admin123"
   }
   ```

2. Thêm Script để lưu token:
   ```javascript
   // Tab Tests
   var jsonData = pm.response.json();
   pm.collectionVariables.set("authToken", "Bearer " + jsonData.data.token);
   ```

3. Send request → Xác nhận token được lưu vào biến

---

## Bước 2: Tạo Collection & Request

### 2.1 Tạo Collection

1. Click **Collections** → **New Collection**
2. Đặt tên: `WHS Product API`
3. Thêm mô tả: `API Test for Product Module - CRUD Operations`
4. Tab **Authorization**:
   - Type: `Bearer Token`
   - Token: `{{authToken}}`
5. Click **Save**

### 2.2 Tạo Folder Structure

Tạo các folder trong Collection:

```
WHS Product API
├── 01-Create
│   ├── Create Product - Success
│   ├── Create Product - Validation Error
│   └── Create Product - Duplicate SKU
├── 02-Read
│   ├── Get All Products
│   ├── Get Product By ID
│   └── Get Product By SKU
├── 03-Update
│   ├── Update Product - Success
│   └── Update Product - Not Found
├── 04-Delete
│   ├── Delete Product - Success
│   └── Delete Product - Not Found
└── 05-Validation
    ├── Missing Required Fields
    └── Invalid Data Types
```

### 2.3 Request Details

#### 2.3.1 CREATE - Tạo sản phẩm (Success)

| Field | Value |
|-------|-------|
| Method | POST |
| URL | `{{baseUrl}}/api/v1/products` |
| Tab Headers | Content-Type: application/json |
| Tab Body | |

```json
{
  "name": "Test Product API 001",
  "description": "Product for API testing",
  "category_id": "{{categoryId}}",
  "uom_id": "{{uomId}}",
  "weight": 1.5,
  "dimensions": "10x20x30",
  "min_stock_level": 10,
  "max_stock_level": 1000,
  "reorder_point": 50,
  "cost_price": 100.00,
  "selling_price": 150.00,
  "barcode": "TESTAPI123456",
  "requires_batch_tracking": false
}
```

#### 2.3.2 GET ALL - Lấy danh sách

| Field | Value |
|-------|-------|
| Method | GET |
| URL | `{{baseUrl}}/api/v1/products?page=0&size=10` |

#### 2.3.3 GET BY ID - Lấy theo ID

| Field | Value |
|-------|-------|
| Method | GET |
| URL | `{{baseUrl}}/api/v1/products/{{productId}}` |

#### 2.3.4 GET BY SKU - Lấy theo SKU

| Field | Value |
|-------|-------|
| Method | GET |
| URL | `{{baseUrl}}/api/v1/products/sku/{sku}` |

#### 2.3.5 UPDATE - Cập nhật

| Field | Value |
|-------|-------|
| Method | PUT |
| URL | `{{baseUrl}}/api/v1/products/{{productId}}` |
| Tab Body | |

```json
{
  "name": "Updated Test Product",
  "description": "Updated description",
  "category_id": "{{categoryId}}",
  "uom_id": "{{uomId}}",
  "weight": 2.0,
  "dimensions": "15x25x35",
  "min_stock_level": 20,
  "max_stock_level": 2000,
  "reorder_point": 100,
  "cost_price": 120.00,
  "selling_price": 180.00,
  "barcode": "TESTAPI123456UP",
  "requires_batch_tracking": true
}
```

#### 2.3.6 DELETE - Xóa

| Field | Value |
|-------|-------|
| Method | DELETE |
| URL | `{{baseUrl}}/api/v1/products/{{productId}}` |

---

## Bước 3: Manual Verification

### 3.1 Checklist kiểm tra Response

Với mỗi request, kiểm tra:

| Kiểm tra | Expected | Thực tế | Pass/Fail |
|----------|----------|---------|-----------|
| Status Code | 201/200/200/200 | | |
| Response Time | < 2000ms | | |
| Content-Type | application/json | | |
| Response Structure | Đúng schema | | |
| Data returned | Đầy đủ fields | | |

### 3.2 Response Schema mong đợi

**Base Response Structure:**
```json
{
  "status": "success",
  "message": "Product created successfully",
  "data": {
    "id": "PRD-xxx",
    "sku": "SKU-xxx",
    "name": "Test Product",
    "description": "...",
    "categoryId": "CAT001",
    "uomId": "UOM001",
    "weight": 1.5,
    "dimensions": "10x20x30",
    "minStockLevel": 10,
    "maxStockLevel": 1000,
    "reorderPoint": 50,
    "costPrice": 100.00,
    "sellingPrice": 150.00,
    "barcode": "TEST123456",
    "imageUrl": null,
    "requiresBatchTracking": false,
    "createdAt": "2026-04-08T10:00:00",
    "updatedAt": "2026-04-08T10:00:00"
  }
}
```

### 3.3 Error Response Schema

```json
{
  "status": "error",
  "message": "Product name is required",
  "errorCode": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "name",
      "message": "Product name is required"
    }
  ]
}
```

### 3.4 Test Cases cơ bản

| # | Test Case | Method | Expected |
|---|-----------|--------|----------|
| 1 | Tạo sản phẩm thành công | POST | 201 |
| 2 | Tạo sản phẩm thiếu tên | POST | 400 |
| 3 | Tạo sản phẩm thiếu category | POST | 400 |
| 4 | Tạo sản phẩm trùng SKU | POST | 400 |
| 5 | Lấy danh sách sản phẩm | GET | 200 |
| 6 | Lấy sản phẩm theo ID | GET | 200 |
| 7 | Lấy sản phẩm không tồn tại | GET | 404 |
| 8 | Cập nhật sản phẩm thành công | PUT | 200 |
| 9 | Cập nhật sản phẩm không tồn tại | PUT | 404 |
| 10 | Xóa sản phẩm thành công | DELETE | 200 |
| 11 | Xóa sản phẩm không tồn tại | DELETE | 404 |
| 12 | Lấy sản phẩm sau khi xóa | GET | 404 |

---

## Bước 4: Viết Test Scripts

### 4.1 CREATE - Tests Tab

```javascript
// ===========================================
// CREATE PRODUCT - SUCCESS TESTS
// ===========================================

// Test: Status code 201 Created
pm.test("Should return 201 when product is created", function() {
    pm.response.to.have.status(201);
});

// Test: Response status is success
pm.test("Should return success status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("success");
});

// Test: Response message
pm.test("Should return success message", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.equals("Product created successfully");
});

// Test: Product ID is generated
pm.test("Should generate product ID", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.id).to.be.ok;
    pm.expect(jsonData.data.id).to.not.be.empty;
});

// Test: SKU is auto-generated
pm.test("Should auto-generate SKU", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.sku).to.be.ok;
    pm.expect(jsonData.data.sku).to.not.be.empty;
});

// Test: Product name matches request
pm.test("Should return correct product name", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.name).to.equals("Test Product API 001");
});

// Test: Product category is set
pm.test("Should set category correctly", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.categoryId).to.equals(pm.environment.get("categoryId"));
});

// Test: Product prices are set
pm.test("Should set prices correctly", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.costPrice).to.equals("100.00");
    pm.expect(jsonData.data.sellingPrice).to.equals("150.00");
});

// Test: Save product ID to variable
pm.test("Should save product ID for other tests", function() {
    var jsonData = pm.response.json();
    pm.collectionVariables.set("productId", jsonData.data.id);
    pm.collectionVariables.set("productSku", jsonData.data.sku);
});

// Test: Response time is acceptable
pm.test("Response time should be under 2 seconds", function() {
    pm.expect(pm.response.responseTime).to.be.below(2000);
});

// Test: Content-Type header
pm.test("Should return application/json", function() {
    pm.response.to.have.header("Content-Type", /application\/json/);
});
```

### 4.2 GET BY ID - Tests Tab

```javascript
// ===========================================
// GET PRODUCT BY ID - TESTS
// ===========================================

pm.test("Should return 200 when product is found", function() {
    pm.response.to.have.status(200);
});

pm.test("Should return success status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("success");
});

pm.test("Should return correct product ID", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.id).to.equals(pm.collectionVariables.get("productId"));
});

pm.test("Should return product name", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.name).to.be.ok;
});

pm.test("Should return product SKU", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.sku).to.be.ok;
});

pm.test("Should return product prices", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.costPrice).to.be.ok;
    pm.expect(jsonData.data.sellingPrice).to.be.ok;
});

pm.test("Should return audit fields", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.createdAt).to.be.ok;
    pm.expect(jsonData.data.updatedAt).to.be.ok;
});
```

### 4.3 GET ALL - Tests Tab

```javascript
// ===========================================
// GET ALL PRODUCTS - TESTS
// ===========================================

pm.test("Should return 200 OK", function() {
    pm.response.to.have.status(200);
});

pm.test("Should return success status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("success");
});

pm.test("Should return paginated data", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.ok;
    pm.expect(jsonData.data.content).to.be.an("array");
});

pm.test("Should return page metadata", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.page).to.equals(0);
    pm.expect(jsonData.data.size).to.equals(10);
});

pm.test("Should return total elements", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.totalElements).to.be.a("number");
});

pm.test("Should return total pages", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.totalPages).to.be.a("number");
});

pm.test("Content array should be present", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.content).to.be.ok;
});
```

### 4.4 UPDATE - Tests Tab

```javascript
// ===========================================
// UPDATE PRODUCT - TESTS
// ===========================================

pm.test("Should return 200 OK", function() {
    pm.response.to.have.status(200);
});

pm.test("Should return success status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("success");
});

pm.test("Should return success message", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.equals("Product updated successfully");
});

pm.test("Should return updated name", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.name).to.equals("Updated Test Product");
});

pm.test("Should return updated prices", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.costPrice).to.equals("120.00");
    pm.expect(jsonData.data.sellingPrice).to.equals("180.00");
});

pm.test("Should return same product ID", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.id).to.equals(pm.collectionVariables.get("productId"));
});

pm.test("Should update batch tracking flag", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data.requiresBatchTracking).to.equals(true);
});
```

### 4.5 DELETE - Tests Tab

```javascript
// ===========================================
// DELETE PRODUCT - TESTS
// ===========================================

pm.test("Should return 200 OK", function() {
    pm.response.to.have.status(200);
});

pm.test("Should return success status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("success");
});

pm.test("Should return success message", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.message).to.equals("Product deleted successfully");
});

pm.test("Should return null data", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.null;
});
```

### 4.6 VALIDATION TESTS

#### 4.6.1 Missing Required Fields

```javascript
// POST với body rỗng
pm.test("Should return 400 when required fields missing", function() {
    pm.response.to.have.status(400);
});

pm.test("Should return error status", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("error");
});
```

#### 4.6.2 Not Found

```javascript
// GET với ID không tồn tại
pm.test("Should return 404 when product not found", function() {
    pm.response.to.have.status(404);
});

pm.test("Should return error message", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.status).to.equals("error");
});
```

---

## Bước 5: Automation & Reporting

### 5.1 Chạy Manual với Collection Runner

1. Click **WHS Product API** collection
2. Click **Run** button
3. Cấu hình Runner:

| Setting | Value |
|---------|-------|
| Iterations | 1 |
| Delay | 500ms |
| Log Responses | All |
| Data | None |

4. Click **Run WHS Product API**
5. Xem kết quả trong tab **Run Results**

### 5.2 Chạy Newman (Command Line)

#### 5.2.1 Cài đặt Newman

```bash
# Cài đặt Node.js nếu chưa có
# Download từ https://nodejs.org/

# Cài đặt Newman global
npm install -g newman
```

#### 5.2.2 Export Collection

1. Click **WHS Product API** collection
2. Click **Export**
3. Chọn **Collection v2.1**
4. Lưu thành `WHS_Product_API.json`

#### 5.2.3 Export Environment

1. Click **WHS Local Dev** environment
2. Click **Export**
3. Lưu thành `WHS_Local_Dev.postman_environment.json`

#### 5.2.4 Chạy Newman

```bash
# Chạy cơ bản
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json

# Chạy với HTML report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html

# Chạy với JSON report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r json

# Chạy với cả hai
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html,json
```

#### 5.2.5 Newman Options

```bash
# Chạy nhiều lần
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -n 10

# Timeout per request
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json --request-timeout 60000

# Disable SSL verification
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json --insecure

# Chạy với proxy
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json --proxy "http://proxy:8080"
```

### 5.3 GitHub Actions Integration

Tạo file `.github/workflows/postman-tests.yml`:

```yaml
name: API Tests

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  postman-tests:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
      
      - name: Install Newman
        run: npm install -g newman newman-reporter-html
      
      - name: Start Application
        run: |
          chmod +x mvnw
          ./mvnw spring-boot:run &
        shell: bash
      
      - name: Wait for application
        run: sleep 30
      
      - name: Run Postman Tests
        run: |
          newman run docs/postman/WHS_Product_API.json 
          -e docs/postman/WHS_Local_Dev.postman_environment.json 
          -r html,json
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: postman-test-results
          path: newman/
```

### 5.4 Scheduled Runs (Postman Pro/Enterprise)

1. Click **WHS Product API** collection
2. Click **Schedule Runs**
3. Cấu hình:
   - **Frequency**: Daily/Weekly
   - **Time**: 09:00
   - **Environment**: WHS Local Dev
4. Click **Schedule**

---

## Phụ lục

### A. Cấu trúc Response Types

#### Success Response
```json
{
  "status": "success",
  "message": "Operation description",
  "data": { ... }
}
```

#### Paginated Response
```json
{
  "status": "success",
  "message": null,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10
  }
}
```

#### Error Response
```json
{
  "status": "error",
  "message": "Error description",
  "errorCode": "ERROR_CODE",
  "errors": []
}
```

### B. HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Thành công |
| 201 | Created - Tạo mới thành công |
| 400 | Bad Request - Validation error |
| 401 | Unauthorized - Thiếu token |
| 403 | Forbidden - Không có quyền |
| 404 | Not Found - Không tìm thấy |
| 500 | Internal Server Error - Lỗi server |

### C. Error Codes

| Code | Description |
|------|-------------|
| VALIDATION_ERROR | Lỗi validation dữ liệu |
| NOT_FOUND | Không tìm thấy tài nguyên |
| DUPLICATE | Tài nguyên đã tồn tại |
| UNAUTHORIZED | Chưa xác thực |
| FORBIDDEN | Không có quyền |
| INTERNAL_ERROR | Lỗi nội bộ |

### D. Quick Reference

```bash
# Run tất cả test
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json

# Run với HTML report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html --reporter-html-export report.html

# Run với verbose output
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -v
```

---

## Checklists

### ☐ Trước khi test
- [ ] Ứng dụng đang chạy trên localhost:8080
- [ ] Đã login và lấy được token
- [ ] Đã tạo Collection và Environment
- [ ] Đã tạo các request CRUD
- [ ] Đã thêm test scripts

### ☐ Khi test
- [ ] Chạy từng request thủ công
- [ ] Verify response đúng expected
- [ ] Kiểm tra status codes
- [ ] Kiểm tra error cases

### ☐ Sau khi test
- [ ] Chạy Collection Runner
- [ ] Export kết quả
- [ ] Review failed tests
- [ ] Ghi lại bugs nếu có

---

**Version**: 1.0  
**Last Updated**: 2026-04-08  
**Author**: QA Team
