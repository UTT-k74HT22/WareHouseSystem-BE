# Test Case Design - Product Module

---

## 1. API: Create Product (POST /api/v1/products)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tạo sản phẩm thành công | name="Laptop Dell XPS 15", categoryId="92000000-0000-0000-0000-000000000001", uomId="91000000-0000-0000-0000-000000000001", sellingPrice=25000000.00 | 1. POST http://localhost:8080/api/v1/products<br>2. Body: valid JSON with required fields<br>3. Send request | 201 Created, return product data with generated SKU | Pass |
| TC02 | Tạo sản phẩm thiếu trường bắt buộc | name="", categoryId="", uomId="" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON missing required fields<br>3. Send request | 400 Bad Request, validation errors | Fail |
| TC03 | Tạo sản phẩm định dạng giá không hợp lệ | name="Test Product", categoryId="92000000-0000-0000-0000-000000000001", uomId="91000000-0000-0000-0000-000000000001", sellingPrice="abc" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with invalid price format<br>3. Send request | 400 Bad Request, validation error for price | Fail |
| TC04 | Tạo sản phẩm trùng SKU | name="Duplicate Product", categoryId="92000000-0000-0000-0000-000000000001", uomId="91000000-0000-0000-0000-000000000001", sku="SKU-DEV-001" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with existing SKU<br>3. Send request | 409 Conflict, SKU already exists | Fail |
| TC21 | Tạo sản phẩm với tất cả trường tùy chọn | name="iPhone 15 Pro Max", categoryId="92000000-0000-0000-0000-000000000002", uomId="91000000-0000-0000-0000-000000000002", description="Điện thoại smartphone cao cấp", weight=0.5, dimensions="16x8x1", barcode="1234567890123", imageUrl="/products/iphone15.jpg", requiresBatchTracking=true, costPrice=20000000.00, sellingPrice=28000000.00 | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with all fields<br>3. Send request | 201 Created, return product with all fields | Pass |
| TC25 | Tạo sản phẩm định dạng kích thước không hợp lệ | name="Test Product", categoryId="92000000-0000-0000-0000-000000000001", uomId="91000000-0000-0000-0000-000000000001", dimensions="invalid" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with invalid dimensions format<br>3. Send request | 400 Bad Request, validation error for dimensions | Fail |

---

## 2. API: Get Product By ID (GET /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC05 | Lấy sản phẩm theo ID thành công | productId="95000000-0000-0000-0000-000000000001" | 1. GET http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Send request | 200 OK, return product data | Pass |
| TC06 | Lấy sản phẩm theo ID không tồn tại | productId="99999999-9999-9999-9999-999999999999" | 1. GET http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Send request | 404 Not Found | Fail |

---

## 3. API: Get Product By SKU (GET /api/v1/products/sku/{sku})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC07 | Lấy sản phẩm theo SKU thành công | sku="SKU-DEV-001" | 1. GET http://localhost:8080/api/v1/products/sku/SKU-DEV-001<br>2. Send request | 200 OK, return product data | Pass |
| TC08 | Lấy sản phẩm theo SKU không tồn tại | sku="NONEXISTENT" | 1. GET http://localhost:8080/api/v1/products/sku/NONEXISTENT<br>2. Send request | 404 Not Found | Fail |

---

## 4. API: Get All Products (GET /api/v1/products)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC09 | Lấy danh sách sản phẩm có phân trang | page=0, size=10 | 1. GET http://localhost:8080/api/v1/products?page=0&size=10<br>2. Send request | 200 OK, return paginated product list | Pass |
| TC20 | Lấy danh sách sản phẩm phân trang không hợp lệ | page=-1, size=0 | 1. GET http://localhost:8080/api/v1/products?page=-1&size=0<br>2. Send request | 400 Bad Request, validation error | Fail |

---

## 5. API: Update Product (PUT /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC10 | Cập nhật sản phẩm thành công | id="95000000-0000-0000-0000-000000000001", name="Dev Product 1 Updated", sellingPrice=15000.00 | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: valid JSON with updated fields<br>3. Send request | 200 OK, return updated product data | Pass |
| TC11 | Cập nhật sản phẩm không tồn tại | id="99999999-9999-9999-9999-999999999999" | 1. PUT http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Body: valid JSON<br>3. Send request | 404 Not Found | Fail |
| TC12 | Cập nhật sản phẩm lỗi validation | id="95000000-0000-0000-0000-000000000001", name="", sellingPrice=-10 | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: JSON with invalid fields<br>3. Send request | 400 Bad Request, validation errors | Fail |
| TC22 | Cập nhật sản phẩm thay đổi trạng thái | id="95000000-0000-0000-0000-000000000001", status="INACTIVE" | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: JSON with status field<br>3. Send request | 200 OK, return updated product with new status | Pass |

---

## 6. API: Delete Product (DELETE /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC13 | Xóa sản phẩm thành công | productId="95000000-0000-0000-0000-000000000005" | 1. DELETE http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000005<br>2. Send request | 200 OK, success message | Pass |
| TC14 | Xóa sản phẩm không tồn tại | productId="99999999-9999-9999-9999-999999999999" | 1. DELETE http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Send request | 404 Not Found | Fail |

---

## 7. API: Search Products (POST /api/v1/products/search)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC15 | Tìm kiếm sản phẩm với bộ lọc | searchText="Dev", categoryId="92000000-0000-0000-0000-000000000001", status="ACTIVE" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: JSON with search filters<br>3. Send request | 200 OK, return filtered product list | Pass |
| TC16 | Tìm kiếm sản phẩm không có kết quả | searchText="nonexistent123", categoryId="92000000-0000-0000-0000-000000000001" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: JSON with filters returning no results<br>3. Send request | 200 OK, empty product list | Pass |
| TC23 | Tìm kiếm sản phẩm theo SKU chính xác | sku="SKU-DEV-001" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: {"sku": "SKU-DEV-001"}<br>3. Send request | 200 OK, return product with matching SKU | Pass |
| TC24 | Tìm kiếm sản phẩm theo trạng thái | status="ACTIVE" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: {"status": "ACTIVE"}<br>3. Send request | 200 OK, return products with ACTIVE status | Pass |

---

## 8. API: Get Products By Category (GET /api/v1/products/category/{categoryId})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC17 | Lấy sản phẩm theo danh mục | categoryId="92000000-0000-0000-0000-000000000001", page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/category/92000000-0000-0000-0000-000000000001?page=0&size=10<br>2. Send request | 200 OK, return products in category | Pass |
| TC18 | Lấy sản phẩm theo danh mục không có | categoryId="92000000-0000-0000-0000-000000000099", page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/category/92000000-0000-0000-0000-000000000099?page=0&size=10<br>2. Send request | 200 OK, empty list | Pass |

---

## 9. API: Get Batch Tracking Products (GET /api/v1/products/batch-tracking)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC19 | Lấy sản phẩm theo dõi lô | page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/batch-tracking?page=0&size=10<br>2. Send request | 200 OK, return products with batch tracking enabled | Pass |