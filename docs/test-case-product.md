# Test Case Design - Product Module

---

## 1. API: Create Product (POST /api/v1/products)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tạo sản phẩm thành công | name="Laptop Dell XPS 15", categoryId="92000000-0000-0000-0000-000000000001", uomId="91000000-0000-0000-0000-000000000001", sellingPrice=25000000.00 | 1. POST http://localhost:8080/api/v1/products<br>2. Body: valid JSON with required fields<br>3. Send request | 201 Created, return product data with generated SKU | Passed |
| TC02 | Tạo sản phẩm thiếu trường bắt buộc | name="", categoryId="", uomId="" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON missing required fields<br>3. Send request | 400 Bad Request, validation error | Failed |
| TC03 | Tạo sản phẩm định dạng giá không hợp lệ | sellingPrice="abc" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with invalid price format<br>3. Send request | 400 Bad Request, validation error | Failed |
| TC04 | Tạo sản phẩm trùng SKU | sku="SKU-DEV-001" | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with existing SKU<br>3. Send request | 409 Conflict, SKU already exists | Failed |
| TC05 | Tồn kho âm | min_stock_level=-1 | 1. POST http://localhost:8080/api/v1/products<br>2. Body: JSON with min_stock_level < 0<br>3. Send request | 400 Bad Request | Failed |

---

## 2. API: Get Product By ID (GET /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Lấy sản phẩm theo ID thành công | productId="95000000-0000-0000-0000-000000000001" | 1. GET http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Send request | 200 OK, return product data | Passed |
| TC02 | Lấy sản phẩm theo ID không tồn tại | productId="99999999-9999-9999-9999-999999999999" | 1. GET http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Send request | 404 Not Found | Failed |

---

## 3. API: Get Product By SKU (GET /api/v1/products/sku/{sku})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Lấy sản phẩm theo SKU thành công | sku="SKU-DEV-001" | 1. GET http://localhost:8080/api/v1/products/sku/SKU-DEV-001<br>2. Send request | 200 OK, return product data | Passed |
| TC02 | Lấy sản phẩm theo SKU không tồn tại | sku="NONEXISTENT" | 1. GET http://localhost:8080/api/v1/products/sku/NONEXISTENT<br>2. Send request | 404 Not Found | Failed |

---

## 4. API: Get All Products (GET /api/v1/products)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Lấy toàn bộ danh sách | | 1. GET http://localhost:8080/api/v1/products<br>2. Send request | 200 OK, return list | Passed |
| TC02 | Không có dữ liệu | no data | 1. GET http://localhost:8080/api/v1/products<br>2. Send request | 200 OK, empty list | Passed |
| TC03 | Tìm kiếm phân trang âm | page=-1 | 1. GET http://localhost:8080/api/v1/products?page=-1<br>2. Send request | 400 Bad Request | Failed |

---

## 5. API: Update Product (PUT /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Cập nhật sản phẩm thành công | id="95000000-0000-0000-0000-000000000001", name="Dev Product 1 Updated", sellingPrice=15000.00 | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: valid JSON with updated fields<br>3. Send request | 200 OK, return updated product data | Passed |
| TC02 | Cập nhật sản phẩm không tồn tại | id="99999999-9999-9999-9999-999999999999" | 1. PUT http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Body: valid JSON<br>3. Send request | 404 Not Found | Failed |
| TC03 | Cập nhật sản phẩm lỗi validation | id="95000000-0000-0000-0000-000000000001", name="", sellingPrice=-10 | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: JSON with invalid fields<br>3. Send request | 400 Bad Request, validation errors | Failed |
| TC04 | Cập nhật sản phẩm thay đổi trạng thái | id="95000000-0000-0000-0000-000000000001", status="INACTIVE" | 1. PUT http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000001<br>2. Body: JSON with status field<br>3. Send request | 200 OK, return updated product with new status | Passed |

---

## 6. API: Delete Product (DELETE /api/v1/products/{id})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Xóa sản phẩm thành công | productId="95000000-0000-0000-0000-000000000005" | 1. DELETE http://localhost:8080/api/v1/products/95000000-0000-0000-0000-000000000005<br>2. Send request | 200 OK, success message | Passed |
| TC02 | Xóa sản phẩm không tồn tại | productId="99999999-9999-9999-9999-999999999999" | 1. DELETE http://localhost:8080/api/v1/products/99999999-9999-9999-9999-999999999999<br>2. Send request | 404 Not Found | Failed |

---

## 7. API: Search Products (POST /api/v1/products/search)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Tìm kiếm sản phẩm với bộ lọc | searchText="Dev", categoryId="92000000-0000-0000-0000-000000000001", status="ACTIVE" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: JSON with search filters<br>3. Send request | 200 OK, return filtered product list | Passed |
| TC02 | Tìm kiếm sản phẩm không có kết quả | searchText="nonexistent123", categoryId="92000000-0000-0000-0000-000000000001" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: JSON with filters returning no results<br>3. Send request | 200 OK, empty product list | Passed |
| TC03 | Tìm kiếm sản phẩm theo SKU chính xác | sku="SKU-DEV-001" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: {"sku": "SKU-DEV-001"}<br>3. Send request | 200 OK, return product with matching SKU | Passed |
| TC04 | Tìm kiếm sản phẩm theo trạng thái | status="ACTIVE" | 1. POST http://localhost:8080/api/v1/products/search?page=0&size=10<br>2. Body: {"status": "ACTIVE"}<br>3. Send request | 200 OK, return products with ACTIVE status | Passed |

---

## 8. API: Get Products By Category (GET /api/v1/products/category/{categoryId})

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Lấy sản phẩm theo danh mục | categoryId="92000000-0000-0000-0000-000000000001", page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/category/92000000-0000-0000-0000-000000000001?page=0&size=10<br>2. Send request | 200 OK, return products in category | Passed |
| TC02 | Lấy sản phẩm theo danh mục không có | categoryId="92000000-0000-0000-0000-000000000099", page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/category/92000000-0000-0000-0000-000000000099?page=0&size=10<br>2. Send request | 200 OK, empty list | Passed |

---

## 9. API: Get Batch Tracking Products (GET /api/v1/products/batch-tracking)

| Test Case ID | Description | Test Data | Test Steps | Expected Response | Test Result |
|--------------|-------------|-----------|------------|-------------------|-------------|
| TC01 | Lấy sản phẩm theo dõi lô | page=0, size=10 | 1. GET http://localhost:8080/api/v1/products/batch-tracking?page=0&size=10<br>2. Send request | 200 OK, return products with batch tracking enabled | Passed |