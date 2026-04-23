# Câu hỏi phỏng vấn & Prep cho Postman API Testing

> **Mục tiêu**: Chuẩn bị câu hỏi để phòng khi test API Product - đảm bảo cover đủ các case và hiểu rõ behavior của hệ thống

---

## Phần 1: Câu hỏi về hiểu biết API

### 1.1 Về API Endpoint

| # | Câu hỏi | Đáp án mẫu | Ghi chú |
|---|---------|------------|---------|
| 1 | API endpoint `/api/v1/products` hỗ trợ những method nào? | GET, POST | |
| 2 | API endpoint `/api/v1/products/{id}` hỗ trợ những method nào? | GET, PUT, DELETE | |
| 3 | Làm sao để tìm sản phẩm theo SKU? | GET `/api/v1/products/sku/{sku}` | |
| 4 | Làm sao để tìm sản phẩm theo category? | GET `/api/v1/products/category/{categoryId}` | |
| 5 | API search products dùng method nào? | POST `/api/v1/products/search` | |
| 6 | Pagination có params nào? | `page` (0-based), `size` | |

### 1.2 Về Authentication/Authorization

| # | Câu hỏi | Đáp án mẫu |
|---|---------|------------|
| 1 | API này dùng loại authentication gì? | JWT Bearer Token |
| 2 | Header nào chứa token? | `Authorization: Bearer <token>` |
| 3 | API có require permission không? | Có, các quyền: PERM_PRODUCT_CREATE, PERM_PRODUCT_READ, PERM_PRODUCT_UPDATE, PERM_PRODUCT_DELETE |
| 4 | Nếu không có token thì response như thế nào? | 401 Unauthorized |
| 5 | Nếu có token nhưng không có quyền thì sao? | 403 Forbidden |

### 1.3 Về Data Validation

| # | Câu hỏi | Expected Answer |
|---|---------|-----------------|
| 1 | Trường nào là bắt buộc khi tạo product? | `name`, `category_id`, `uom_id` |
| 2 | SKU có bắt buộc không? | Không - tự động generate |
| 3 | Các trường number có validation gì? | Must be >= 0, có giới hạn digits |
| 4 | Dimensions format là gì? | `LxWxH` (e.g., 10x20x30) |
| 5 | Image URL format có giới hạn gì? | Max 255 chars, pattern: `^[a-zA-Z0-9/_\-.]*$` |

---

## Phần 2: Test Cases chuẩn bị

### 2.1 Happy Path Cases

| # | Test Case | Method | Endpoint | Expected |
|---|-----------|--------|----------|----------|
| 1 | Tạo sản phẩm thành công | POST | /api/v1/products | 201 Created |
| 2 | Lấy danh sách sản phẩm | GET | /api/v1/products | 200 OK |
| 3 | Lấy sản phẩm theo ID | GET | /api/v1/products/{id} | 200 OK |
| 4 | Lấy sản phẩm theo SKU | GET | /api/v1/products/sku/{sku} | 200 OK |
| 5 | Cập nhật sản phẩm thành công | PUT | /api/v1/products/{id} | 200 OK |
| 6 | Xóa sản phẩm thành công | DELETE | /api/v1/products/{id} | 200 OK |

### 2.2 Validation Error Cases

| # | Test Case | Method | Endpoint | Body | Expected |
|---|-----------|--------|----------|------|----------|
| 1 | Tạo không có tên | POST | /api/v1/products | {} | 400 Bad Request |
| 2 | Tạo tên empty | POST | /api/v1/products | {"name": ""} | 400 Bad Request |
| 3 | Tạo không có category | POST | /api/v1/products | {"name": "Test"} | 400 Bad Request |
| 4 | Tạo không có UOM | POST | /api/v1/products | {"name": "Test", "category_id": "CAT001"} | 400 Bad Request |
| 5 | Tạo costPrice âm | POST | /api/v1/products | {...,"cost_price": -1} | 400 Bad Request |
| 6 | Tạo sellingPrice âm | POST | /api/v1/products | {...,"selling_price": -1} | 400 Bad Request |
| 7 | Tạo minStock > maxStock | POST | /api/v1/products | {...,"min_stock_level": 100,"max_stock_level": 50} | ??? (cần verify) |
| 8 | Dimensions sai format | POST | /api/v1/products | {...,"dimensions": "10-20-30"} | 400 Bad Request |
| 9 | Image URL sai pattern | POST | /api/v1/products | {...,"image_url": "test<>file"} | 400 Bad Request |
| 10 | Name quá 200 ký tự | POST | /api/v1/products | {"name": "A".repeat(201)} | 400 Bad Request |

### 2.3 Not Found Cases

| # | Test Case | Method | Endpoint | Expected |
|---|-----------|--------|----------|----------|
| 1 | Lấy sản phẩm không tồn tại | GET | /api/v1/products/NOT_EXIST | 404 Not Found |
| 2 | Lấy theo SKU không tồn tại | GET | /api/v1/products/sku/NOT_EXIST | 404 Not Found |
| 3 | Cập nhật sản phẩm không tồn tại | PUT | /api/v1/products/NOT_EXIST | 404 Not Found |
| 4 | Xóa sản phẩm không tồn tại | DELETE | /api/v1/products/NOT_EXIST | 404 Not Found |

### 2.4 Permission Cases

| # | Test Case | Expected Behavior |
|---|-----------|-------------------|
| 1 | Gọi API không có token | 401 Unauthorized |
| 2 | Gọi API với token hết hạn | 401 Unauthorized |
| 3 | User không có PERM_PRODUCT_CREATE gọi POST | 403 Forbidden |
| 4 | User không có PERM_PRODUCT_READ gọi GET | 403 Forbidden |
| 5 | User không có PERM_PRODUCT_UPDATE gọi PUT | 403 Forbidden |
| 6 | User không có PERM_PRODUCT_DELETE gọi DELETE | 403 Forbidden |

### 2.5 Business Logic Cases

| # | Test Case | Expected Behavior |
|---|-----------|-------------------|
| 1 | Tạo sản phẩm trùng SKU (nếu cho phép) | ??? (cần clarify) |
| 2 | Tạo sản phẩm trùng name | Nên cho phép hay không? (cần clarify) |
| 3 | Lấy sản phẩm sau khi xóa | 404 Not Found |
| 4 | Cập nhật sản phẩm đã bị xóa | 404 Not Found |
| 5 | Cập nhật không đổi gì (empty body) | 200 OK hoặc 400? (cần verify) |
| 6 | Tạo sản phẩm với category không tồn tại | 400 Bad Request hoặc 404? |
| 7 | Tạo sản phẩm với UOM không tồn tại | 400 Bad Request hoặc 404? |

### 2.6 Pagination & Sorting Cases

| # | Test Case | Expected Behavior |
|---|-----------|-------------------|
| 1 | Page = 0 | OK - trang đầu tiên |
| 2 | Page = -1 | 400 Bad Request (nếu validate) hoặc 200 với empty |
| 3 | Size = 0 | 400 Bad Request hoặc 200 với empty |
| 4 | Size = 1000 | Có giới hạn max không? (cần clarify) |
| 5 | Page lớn hơn total pages | Return empty array |

### 2.7 Edge Cases

| # | Test Case | Expected Behavior |
|---|-----------|-------------------|
| 1 | Gọi API với ID = null | 404 hoặc 400? |
| 2 | Gọi API với ID = empty | 404 hoặc 400? |
| 3 | Gọi API với special characters trong ID | XSS protection? |
| 4 | Request body quá lớn | 413 Payload Too Large |
| 5 | Gọi API liên tục (rate limit) | 429 Too Many Requests |

---

## Phần 3: Câu hỏi prep cho Developer

### 3.1 Cần clarify từ Dev Team

```
1. SKU có unique constraint không? Có thể trùng được không?
2. Product Name có unique constraint không? 
3. Category ID và UOM ID khi không tồn tại thì trả về 400 hay 404?
4. Delete là hard delete hay soft delete?
   - Nếu soft delete thì có thể restore không?
   - Có API restore không?
5. Khi tạo product mới, ai gán ID? Client hay Server?
6. Có max limit cho size pagination không?
7. API có rate limiting không? Giới hạn bao nhiêu?
8. Error response có field "errors" array không?
9. Search API hỗ trợ những fields nào để filter?
10. Có API bulk create/update không?
```

### 3.2 Questions about Security

```
1. Input có được sanitize không? (XSS prevention)
2. Có SQL injection prevention không?
3. Category ID và UOM ID validation như thế nào?
   - Có kiểm tra tồn tại không?
   - Hay chỉ validate format?
4. JWT token expiration time là bao lâu?
5. Có refresh token không?
```

### 3.3 Questions about Performance

```
1. API có pagination mặc định không? (nếu không truyền size)
2. Có N+1 query problem không?
3. Có cache không? Redis?
4. GET by ID có dùng cache không?
5. Search API có full-text search không?
```

---

## Phần 4: Test Data chuẩn bị

### 4.1 Test Accounts

| Username | Password | Permissions |
|----------|----------|-------------|
| admin | admin123 | All |
| viewer | viewer123 | PERM_PRODUCT_READ only |
| creator | creator123 | PERM_PRODUCT_CREATE, PERM_PRODUCT_READ |
| updater | updater123 | PERM_PRODUCT_UPDATE, PERM_PRODUCT_READ |
| deleter | deleter123 | PERM_PRODUCT_DELETE, PERM_PRODUCT_READ |

### 4.2 Test Data IDs

| Type | ID | Description |
|------|-----|-------------|
| Category | CAT001 | Test Category 1 |
| Category | CAT002 | Test Category 2 |
| UOM | UOM001 | Unit - Piece |
| UOM | UOM002 | Unit - Box |
| UOM | UOM003 | Unit - Kg |

### 4.3 Sample Create Request

```json
{
  "name": "Test Product API 001",
  "description": "Product for API testing",
  "category_id": "CAT001",
  "uom_id": "UOM001",
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

---

## Phần 5: Verification Checklist

### 5.1 Trước khi bắt đầu test

```
☐ Xác nhận ứng dụng đang chạy
☐ Xác nhận login thành công và có token
☐ Xác nhận có quyền cần thiết
☐ Chuẩn bị test data (category, uom)
☐ Backup database nếu cần
```

### 5.2 Trong khi test

```
☐ Verify status code đúng
☐ Verify response body đúng schema
☐ Verify data được lưu đúng
☐ Verify các edge cases
☐ Ghi lại tất cả bugs found
```

### 5.3 Sau khi test

```
☐ Verify sản phẩm đã tạo có thể get được
☐ Verify sản phẩm sau update đúng
☐ Verify sản phẩm sau delete không get được
☐ Cleanup test data
☐ Report bugs nếu có
```

---

## Phần 6: Expected Responses

### 6.1 Success Responses

**POST /api/v1/products - 201 Created**
```json
{
  "status": "success",
  "message": "Product created successfully",
  "data": {
    "id": "PRD-xxx",
    "sku": "SKU-xxx",
    "name": "Test Product API 001",
    ...
  }
}
```

**GET /api/v1/products/{id} - 200 OK**
```json
{
  "status": "success",
  "message": null,
  "data": {
    "id": "PRD-xxx",
    "sku": "SKU-xxx",
    ...
  }
}
```

**PUT /api/v1/products/{id} - 200 OK**
```json
{
  "status": "success",
  "message": "Product updated successfully",
  "data": { ... }
}
```

**DELETE /api/v1/products/{id} - 200 OK**
```json
{
  "status": "success",
  "message": "Product deleted successfully",
  "data": null
}
```

### 6.2 Error Responses

**400 Bad Request - Validation**
```json
{
  "status": "error",
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Product name is required"
    }
  ]
}
```

**401 Unauthorized**
```json
{
  "status": "error",
  "message": "Unauthorized",
  "errorCode": "UNAUTHORIZED"
}
```

**403 Forbidden**
```json
{
  "status": "error",
  "message": "Access denied",
  "errorCode": "FORBIDDEN"
}
```

**404 Not Found**
```json
{
  "status": "error",
  "message": "Product not found",
  "errorCode": "NOT_FOUND"
}
```

---

## Phần 7: Quick Reference Commands

### 7.1 Newman Commands

```bash
# Chạy tất cả test
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json

# Chạy 1 request cụ thể
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json --folder "01-Create"

# Chạy với report chi tiết
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html,json --reporter-html-export report.html

# Chạy với verbose
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -v

# Timeout
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json --request-timeout 60000
```

### 7.2 Postman Scripts Quick Ref

```javascript
// Lấy giá trị
pm.environment.get("baseUrl")
pm.collectionVariables.get("productId")

// Set giá trị
pm.environment.set("token", "value")
pm.collectionVariables.set("productId", "value")

// Assert
pm.expect(actual).to.equals(expected)
pm.response.to.have.status(200)
pm.response.to.have.header("Content-Type")

// Log
console.log("Value:", value)
```

---

## Checklists Review

### ☐ Đã cover Happy Path
- [ ] CREATE thành công
- [ ] GET ALL thành công  
- [ ] GET BY ID thành công
- [ ] GET BY SKU thành công
- [ ] UPDATE thành công
- [ ] DELETE thành công

### ☐ Đã cover Validation Errors
- [ ] Missing required fields
- [ ] Invalid format
- [ ] Out of range values
- [ ] Wrong data types

### ☐ Đã cover Error Cases
- [ ] Not Found (404)
- [ ] Unauthorized (401)
- [ ] Forbidden (403)

### ☐ Đã cover Edge Cases
- [ ] Empty inputs
- [ ] Special characters
- [ ] Large payloads

### ☐ Đã verify business logic
- [ ] Data persists correctly
- [ ] Soft delete behavior
- [ ] Category/UOM validation

---

**Version**: 1.0  
**Last Updated**: 2026-04-08  
**Purpose**: Interview Prep & Test Case Planning
