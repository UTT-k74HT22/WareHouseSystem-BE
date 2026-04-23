# Test Requirements - Category, Product, Batch

Tài liệu này mô tả yêu cầu kiểm thử API cho 3 module: Danh mục sản phẩm, Sản phẩm và Lô hàng. Nội dung bám theo code hiện tại trong `src/main/java/org/demo/whs`.

## 1. Phạm vi kiểm thử

| Module | Controller | Base endpoint | Quyền chính |
| --- | --- | --- | --- |
| Danh mục sản phẩm | `CategoryController` | `/api/v1/categories` | `PERM_CATEGORY_CREATE`, `PERM_CATEGORY_READ`, `PERM_CATEGORY_UPDATE` |
| Sản phẩm | `ProductController` | `/api/v1/products` | `PERM_PRODUCT_CREATE`, `PERM_PRODUCT_READ`, `PERM_PRODUCT_UPDATE`, `PERM_PRODUCT_DELETE` |
| Lô hàng | `BatchController` | `/api/v1/batches` | `PERM_BATCH_CREATE`, `PERM_BATCH_READ`, `PERM_BATCH_UPDATE` |

Ngoài phạm vi:
- Module Category hiện không có API xóa danh mục.
- Module Batch hiện không có API xóa lô hàng.
- SKU sản phẩm, mã danh mục và số lô đều do hệ thống sinh, client không được gửi các trường này khi tạo/cập nhật.

## 2. Dữ liệu chuẩn dùng khi test

Các Postman collections dùng biến môi trường để chạy được trên local dev. Nếu dùng dữ liệu seed Flyway hiện có, có thể dùng các giá trị sau:

| Biến | Giá trị mặc định | Ý nghĩa |
| --- | --- | --- |
| `baseUrl` | `http://localhost:8080` | URL backend |
| `authToken` | rỗng | JWT access token sau login |
| `categoryId` | `92000000-0000-0000-0000-000000000001` | Danh mục ACTIVE |
| `uomId` | `91000000-0000-0000-0000-000000000001` | Đơn vị tính hợp lệ |
| `productId` | `95000000-0000-0000-0000-000000000001` | Sản phẩm seed có batch tracking |
| `productSku` | `SKU-DEV-001` | SKU seed |
| `batchId` | `96000000-0000-0000-0000-000000000001` | Lô seed AVAILABLE |
| `warehouseId` | `93000000-0000-0000-0000-000000000001` | Kho seed |

Tài khoản login mặc định trong collection:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Nếu local database không có user này, cập nhật request `00 - Login` trong collection.

## 3. Kỹ thuật sinh test case

Số ca kiểm thử được sinh theo loại nghiệp vụ:

| Loại chức năng | Phương pháp chính | Ví dụ áp dụng |
| --- | --- | --- |
| CRUD đơn giản | Phân lớp tương đương | Create Category hợp lệ/thiếu tên/trùng tên |
| Trường số, phân trang, độ dài | Giá trị biên | `page=-1`, `size=0`, `size=101`, tên quá 100/200 ký tự |
| Nhiều điều kiện đầu vào | Bảng quyết định | Create Product phụ thuộc tên, category, UOM, SKU, stock levels |
| Nghiệp vụ trạng thái | Kiểm thử chuyển trạng thái | Batch `AVAILABLE -> QUARANTINE -> AVAILABLE` |
| Bảo mật | Permission testing | Chưa đăng nhập trả `401`, thiếu quyền trả `403` |
| Hồi quy nghiệp vụ | Regression testing | Generic batch status patch luôn bị chặn |

Với bảng quyết định, tổ hợp đầy đủ bằng tích số giá trị các biến đầu vào. Ví dụ Create Product có 5 điều kiện boolean thì đầy đủ là `2^5 = 32` rule. Bộ test rút gọn bằng cách giữ 1 happy path và mỗi nhóm lỗi chính 1 rule đại diện.

## 4. Module Danh mục sản phẩm

### 4.1. Endpoint cần kiểm thử

| STT | Method | Endpoint | Mục tiêu | Expected |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/v1/categories` | Tạo danh mục | `201 Created` |
| 2 | GET | `/api/v1/categories` | Lấy danh sách, filter status, phân trang | `200 OK` |
| 3 | GET | `/api/v1/categories/{id}` | Lấy chi tiết | `200 OK` hoặc `404` |
| 4 | PUT | `/api/v1/categories/{id}` | Cập nhật tên/mô tả | `200 OK` |
| 5 | PATCH | `/api/v1/categories/{id}/status` | Đổi trạng thái `ACTIVE/INACTIVE` | `200 OK` |

### 4.2. Quy tắc nghiệp vụ và validation

- `code` phải `null`; hệ thống tự sinh mã danh mục.
- `name` bắt buộc, không blank, tối đa 100 ký tự.
- `description` tối đa 255 ký tự.
- `status` bắt buộc khi tạo và đổi trạng thái.
- `id` trên path phải đúng định dạng UUID.
- Không cho tạo/cập nhật trùng tên danh mục.
- Update body rỗng hoặc không có trường có giá trị sẽ bị từ chối ở service.

### 4.3. Bảng test requirements

| ID | Kịch bản | Phương pháp | Input chính | Expected |
| --- | --- | --- | --- | --- |
| CAT-TC-01 | Login lấy token | Security setup | admin/admin123 | `200`, lưu `authToken` |
| CAT-TC-02 | Tạo danh mục hợp lệ | Happy path | name, description, status=`ACTIVE` | `201`, `success=true`, lưu `createdCategoryId` |
| CAT-TC-03 | Tạo danh mục có `code` | Equivalence invalid | body có `code` | `400` |
| CAT-TC-04 | Tạo thiếu tên | Equivalence invalid | name thiếu/blank | `400` |
| CAT-TC-05 | Tạo thiếu status | Equivalence invalid | không có `status` | `400` |
| CAT-TC-06 | Lấy danh sách ACTIVE | Happy path/filter | `status=ACTIVE&page=0&size=10` | `200`, data có `content` |
| CAT-TC-07 | Filter status không hợp lệ | Equivalence invalid | `status=DELETED` | `400` |
| CAT-TC-08 | Lấy chi tiết hợp lệ | Happy path | `createdCategoryId` hoặc `categoryId` | `200` |
| CAT-TC-09 | ID sai định dạng | Boundary/validation | `NOT-A-UUID` | `400` |
| CAT-TC-10 | ID không tồn tại | Error handling | UUID không tồn tại | `404` |
| CAT-TC-11 | Cập nhật hợp lệ | Happy path | name/description | `200` |
| CAT-TC-12 | Cập nhật có `code` | Equivalence invalid | body có `code` | `400` |
| CAT-TC-13 | Đổi trạng thái INACTIVE | State update | status=`INACTIVE` | `200`, status=`INACTIVE` |
| CAT-TC-14 | Đổi trạng thái thiếu status | Equivalence invalid | `{}` | `400` |

## 5. Module Sản phẩm

### 5.1. Endpoint cần kiểm thử

| STT | Method | Endpoint | Mục tiêu | Expected |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/v1/products` | Tạo sản phẩm | `201 Created` |
| 2 | GET | `/api/v1/products` | Lấy danh sách phân trang | `200 OK` |
| 3 | POST | `/api/v1/products/search` | Tìm kiếm/filter | `200 OK` |
| 4 | GET | `/api/v1/products/{id}` | Lấy chi tiết theo ID | `200 OK` hoặc `404` |
| 5 | GET | `/api/v1/products/sku/{sku}` | Lấy chi tiết theo SKU | `200 OK` hoặc `404` |
| 6 | PUT | `/api/v1/products/{id}` | Cập nhật sản phẩm | `200 OK` |
| 7 | DELETE | `/api/v1/products/{id}` | Xóa mềm sản phẩm | `200 OK` |
| 8 | GET | `/api/v1/products/category/{categoryId}` | Lấy theo danh mục | `200 OK` |
| 9 | GET | `/api/v1/products/batch-tracking` | Lấy sản phẩm cần theo dõi lô | `200 OK` |

### 5.2. Quy tắc nghiệp vụ và validation

- `sku` phải `null`; hệ thống tự sinh SKU.
- `name` bắt buộc, tối đa 200 ký tự.
- `category_id` bắt buộc và category phải tồn tại, đang `ACTIVE`.
- `uom_id` bắt buộc và UOM phải tồn tại.
- `weight > 0` nếu gửi.
- `dimensions` đúng format `LxWxH`, ví dụ `10x20x30`.
- Các mức tồn và giá không âm.
- `max_stock_level >= min_stock_level`.
- `reorder_point` phải nằm trong khoảng min-max nếu các trường liên quan có giá trị.
- Không được tắt `requires_batch_tracking` nếu sản phẩm đã có tồn kho theo lô.
- Delete là soft delete, chuyển trạng thái sản phẩm sang `INACTIVE`.

### 5.3. Bảng quyết định rút gọn - Create Product

| Rule | Tên hợp lệ | Category ACTIVE | UOM tồn tại | Không gửi SKU | Stock level hợp lệ | Expected |
| --- | --- | --- | --- | --- | --- | --- |
| R1 | Có | Có | Có | Có | Có | `201 Created` |
| R2 | Không | Có | Có | Có | Có | `400` |
| R3 | Có | Không | Có | Có | Có | `404/400` |
| R4 | Có | Có | Không | Có | Có | `404` |
| R5 | Có | Có | Có | Không | Có | `400` |
| R6 | Có | Có | Có | Có | Không | `400` |

### 5.4. Bảng test requirements

| ID | Kịch bản | Phương pháp | Input chính | Expected |
| --- | --- | --- | --- | --- |
| PRD-TC-01 | Login lấy token | Security setup | admin/admin123 | `200`, lưu `authToken` |
| PRD-TC-02 | Tạo sản phẩm hợp lệ | Happy path | category, UOM, name, prices, stock levels | `201`, lưu `createdProductId`, `createdProductSku` |
| PRD-TC-03 | Tạo có `sku` | Decision table R5 | body có `sku` | `400` |
| PRD-TC-04 | Tạo thiếu name | Decision table R2 | name thiếu/blank | `400` |
| PRD-TC-05 | Tạo thiếu category | Decision table R3 | không có `category_id` | `400` |
| PRD-TC-06 | Tạo thiếu UOM | Decision table R4 | không có `uom_id` | `400` |
| PRD-TC-07 | `max < min` | Boundary/business | min=100, max=50 | `400` |
| PRD-TC-08 | `reorder < min` | Boundary/business | min=10, reorder=5 | `400` |
| PRD-TC-09 | `dimensions` sai format | Validation | `10-20-30` | `400` |
| PRD-TC-10 | Lấy danh sách | Happy path | page=0,size=10 | `200` |
| PRD-TC-11 | Phân trang âm | Boundary | page=-1 | `400` |
| PRD-TC-12 | Lấy theo ID | Happy path | `createdProductId` | `200` |
| PRD-TC-13 | Lấy theo SKU | Happy path | `createdProductSku` | `200` |
| PRD-TC-14 | ID không tồn tại | Error handling | `NOT_EXIST` | `404` |
| PRD-TC-15 | Search theo name/status | Filter | name + status | `200` |
| PRD-TC-16 | Cập nhật hợp lệ | Happy path | name, selling price | `200` |
| PRD-TC-17 | Cập nhật giá âm | Boundary | selling_price=-1 | `400` |
| PRD-TC-18 | Xóa mềm sản phẩm | State update | `createdProductId` | `200` |

## 6. Module Lô hàng

### 6.1. Endpoint cần kiểm thử

| STT | Method | Endpoint | Mục tiêu | Expected |
| --- | --- | --- | --- | --- |
| 1 | POST | `/api/v1/batches` | Tạo lô cho sản phẩm có batch tracking | `201 Created` |
| 2 | GET | `/api/v1/batches` | Danh sách có filter | `200 OK` |
| 3 | GET | `/api/v1/batches/{id}` | Chi tiết lô | `200 OK` hoặc `404` |
| 4 | PUT | `/api/v1/batches/{id}` | Cập nhật ngày/mã lô NCC/ghi chú | `200 OK` |
| 5 | PATCH | `/api/v1/batches/{id}/status` | Chặn đổi trạng thái generic | `400` |
| 6 | PUT | `/api/v1/batches/{id}/quarantine` | Cách ly lô | `200 OK` hoặc business error |
| 7 | PUT | `/api/v1/batches/{id}/release` | Giải phóng lô cách ly | `200 OK` |
| 8 | GET | `/api/v1/batches/{id}/traceability` | Truy xuất nguồn gốc | `200 OK` |
| 9 | GET | `/api/v1/batches/expiring` | Lô sắp hết hạn | `200 OK` |
| 10 | GET | `/api/v1/batches/fifo-recommendations` | Gợi ý FIFO | `200 OK` |
| 11 | GET | `/api/v1/batches/by-product/{productId}` | Lô theo sản phẩm | `200 OK` |

### 6.2. Quy tắc nghiệp vụ và validation

- `batch_number` phải `null`; hệ thống tự sinh số lô.
- `product_id` bắt buộc.
- Product phải tồn tại và `requires_batch_tracking=true`.
- `manufacturing_date` bắt buộc và không được ở tương lai.
- `expiry_date` không được nhỏ hơn `manufacturing_date`.
- Khi tạo thành công, batch ở trạng thái `AVAILABLE`.
- Generic `PATCH /status` luôn bị chặn; phải dùng workflow chuyên biệt.
- Chỉ batch `AVAILABLE` mới được quarantine.
- Quarantine cần `reason`, tối đa 500 ký tự.
- Chỉ batch `QUARANTINE` mới được release.
- Release cần `release_notes`, tối đa 1000 ký tự.
- Batch hết hạn hoặc recalled không được release.
- Search date range phải hợp lệ: `from <= to`.

### 6.3. Bảng chuyển trạng thái batch

| Trạng thái hiện tại | Hành động | Điều kiện | Expected |
| --- | --- | --- | --- |
| `AVAILABLE` | Quarantine | Có reason, không có reserved stock, không expired/recalled | `QUARANTINE` |
| `AVAILABLE` | Quarantine | Thiếu reason | `400` |
| `QUARANTINE` | Quarantine lại | Bất kỳ | Business error |
| `QUARANTINE` | Release | Có release notes, chưa hết hạn | `AVAILABLE` |
| `QUARANTINE` | Release | Thiếu release notes | `400` |
| `AVAILABLE` | Release | Không ở `QUARANTINE` | Business error |
| Bất kỳ | Generic `PATCH /status` | Bất kỳ | `400`, bị chặn |

### 6.4. Bảng test requirements

| ID | Kịch bản | Phương pháp | Input chính | Expected |
| --- | --- | --- | --- | --- |
| BAT-TC-01 | Login lấy token | Security setup | admin/admin123 | `200`, lưu `authToken` |
| BAT-TC-02 | Tạo lô hợp lệ | Happy path | product tracking lô, manufacturing date, expiry date | `201`, status=`AVAILABLE`, lưu `createdBatchId` |
| BAT-TC-03 | Tạo có `batch_number` | Equivalence invalid | body có `batch_number` | `400` |
| BAT-TC-04 | Tạo thiếu product | Equivalence invalid | không có `product_id` | `400` |
| BAT-TC-05 | Product không tồn tại | Error handling | UUID không tồn tại | `404` |
| BAT-TC-06 | Manufacturing date ở tương lai | Boundary/date | future date | `400` |
| BAT-TC-07 | Expiry < manufacturing | Boundary/date | expiry trước manufacturing | `400` |
| BAT-TC-08 | Lấy danh sách filter | Happy path/filter | status/product/date/page/size | `200` |
| BAT-TC-09 | Date range sai | Boundary/date | `from > to` | `400` |
| BAT-TC-10 | Lấy chi tiết | Happy path | `createdBatchId` | `200` |
| BAT-TC-11 | Cập nhật hợp lệ | Happy path | supplier batch, notes | `200` |
| BAT-TC-12 | Generic patch status | Regression | `PATCH /status` | `400` |
| BAT-TC-13 | Quarantine thiếu reason | Validation | `{}` | `400` |
| BAT-TC-14 | Quarantine hợp lệ | State transition | reason | `200`, status=`QUARANTINE` |
| BAT-TC-15 | Release thiếu notes | Validation | `{}` | `400` |
| BAT-TC-16 | Release hợp lệ | State transition | release_notes | `200`, status=`AVAILABLE` |
| BAT-TC-17 | Expiring batches | Query | threshold_days, warehouse_id | `200` |
| BAT-TC-18 | FIFO recommendation | Query | product_id, warehouse_id, limit | `200` |
| BAT-TC-19 | Batches by product | Query | product_id | `200` |
| BAT-TC-20 | Traceability | Query | batch id | `200` |

## 7. Tiêu chí pass/fail

Một test case được tính là Pass khi:
- HTTP status đúng expected.
- Response theo wrapper `BaseResponse`: `success=true` cho happy path, `success=false` cho lỗi nghiệp vụ/validation.
- Dữ liệu trả về đúng trường quan trọng: `id`, `code`, `sku`, `batch_number`, `status`.
- Với create/update/delete/state transition, trạng thái sau thao tác đúng với nghiệp vụ.
- Với negative case, hệ thống không ghi dữ liệu sai và trả lỗi ổn định.

Một test case được tính là Fail khi:
- Status code khác expected.
- Response thiếu wrapper hoặc thiếu `data`/`field_errors` quan trọng.
- API cho phép dữ liệu vi phạm quy tắc nghiệp vụ.
- API tạo/cập nhật được mã do hệ thống phải tự sinh (`code`, `sku`, `batch_number`) từ client input.
- State transition không hợp lệ vẫn được thực hiện.

## 8. Postman collections sinh kèm

| Module | File |
| --- | --- |
| Danh mục | `docs/postman/05_Category_API_Test.postman_collection.json` |
| Sản phẩm | `docs/postman/06_Product_API_Test.postman_collection.json` |
| Lô hàng | `docs/postman/07_Batch_API_Test.postman_collection.json` |
| Hiệu năng danh mục + lô hàng | `docs/postman/08_Category_Batch_Performance.postman_collection.json` |

Khuyến nghị chạy theo thứ tự:
1. Import environment `docs/postman/WHS_Local_Dev.postman_environment.json`.
2. Chạy `05_Category_API_Test` để có `createdCategoryId`.
3. Chạy `06_Product_API_Test` để có `createdProductId` và `createdProductSku`.
4. Chạy `07_Batch_API_Test` để có `createdBatchId`.
5. Chạy `08_Category_Batch_Performance` folder `00 - Setup` một lần, sau đó chạy folder Category/Batch performance với nhiều iterations.

Các collection vẫn có biến fallback seed nên có thể chạy độc lập nếu database đã seed dữ liệu chuẩn.

Trên Windows PowerShell có thể chạy nhanh phần demo danh mục + lô hàng bằng:

```powershell
.\docs\newman\run-category-batch-demo.ps1 -Iterations 10 -DelayMs 50
```
