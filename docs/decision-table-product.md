# Bảng Quyết Định - Module Product

---

## 1. Tạo Sản Phẩm (POST /api/v1/products)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 | TC05 |
|------------------------|------|------|------|------|------|
| **C1**: Các trường bắt buộc hợp lệ | T | F | T | T | T |
| **C2**: Định dạng giá hợp lệ | T | T | F | T | T |
| **C3**: SKU không trùng | T | T | T | F | T |
| **C4**: min_stock_level >= 0 | T | T | T | T | F |
| **A1**: 201 Created | X | | | | |
| **A2**: 400 Bad Request | | X | X | | X |
| **A3**: 409 Conflict | | | | X | |

---

## 2. Lấy Sản Phẩm Theo ID (GET /api/v1/products/{id})

| Điều Kiện / Test Case | TC01 | TC02 | TC03 |
|------------------------|------|------|------|
| **C1**: ID tồn tại | T | F | T |
| **A1**: 200 OK | X | | X |
| **A4**: 404 Not Found | | X | |

---

## 3. Lấy Sản Phẩm Theo SKU (GET /api/v1/products/sku/{sku})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: SKU tồn tại | T | F |
| **A1**: 200 OK | X | |
| **A4**: 404 Not Found | | X |

---

## 4. Lấy Tất Cả Sản Phẩm (GET /api/v1/products)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 |
|------------------------|------|------|------|
| **C1**: page >= 0 | T | T | F |
| **C2**: có dữ liệu | T | F | - |
| **A1**: 200 OK | X | | |
| **A6**: 200 OK + empty | | X | |
| **A2**: 400 Bad Request | | | X |

---

## 5. Cập Nhật Sản Phẩm (PUT /api/v1/products/{id})

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 |
|------------------------|------|------|------|------|
| **C1**: ID tồn tại | T | F | T | T |
| **C2**: name hợp lệ | T | - | F | T |
| **C3**: price > 0 | T | - | F | T |
| **C4**: status hợp lệ | T | - | - | T |
| **A1**: 200 OK | X | | | X |
| **A2**: 400 Bad Request | | | X | |
| **A4**: 404 Not Found | | X | | |

---

## 6. Xóa Sản Phẩm (DELETE /api/v1/products/{id})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: ID tồn tại | T | F |
| **A5**: 200 OK | X | |
| **A4**: 404 Not Found | | X |

---

## 7. Tìm Kiếm Sản Phẩm (POST /api/v1/products/search)

| Điều Kiện / Test Case | TC01 | TC02 | TC03 | TC04 |
|------------------------|------|------|------|------|
| **C1**: Bộ lọc có kết quả | T | F | T | T |
| **A1**: 200 OK + list | X | | X | X |
| **A6**: 200 OK + empty | | X | | |

---

## 8. Lấy Sản Phẩm Theo Danh Mục (GET /api/v1/products/category/{categoryId})

| Điều Kiện / Test Case | TC01 | TC02 |
|------------------------|------|------|
| **C1**: Danh mục có sản phẩm | T | F |
| **A1**: 200 OK | X | |
| **A6**: 200 OK + empty | | X |

---

## 9. Lấy Sản Phẩm Theo Dõi Lô (GET /api/v1/products/batch-tracking)

| Điều Kiện / Test Case | TC01 |
|------------------------|------|
| **C1**: requiresBatchTracking = true | Filtered |
| **A1**: 200 OK | X |

---

## Chi Tiết Điều Kiện

| Điều Kiện | Mô Tả |
|-----------|-------|
| C1 | Các trường bắt buộc (name, categoryId, uomId) có mặt và hợp lệ |
| C2 | sellingPrice là số hợp lệ và > 0 |
| C3 | sku không tồn tại trong database |
| C4 | min_stock_level >= 0 (nếu được cung cấp) |

---

## Các Hành Động

| Hành Động | Mô Tả |
|-----------|-------|
| A1 | 200/201 OK - Thành công |
| A2 | 400 Bad Request - Lỗi validation |
| A3 | 409 Conflict - SKU đã tồn tại |
| A4 | 404 Not Found - Không tìm thấy tài nguyên |
| A5 | 200 OK - Xóa thành công |
| A6 | 200 OK - Kết quả rỗng |