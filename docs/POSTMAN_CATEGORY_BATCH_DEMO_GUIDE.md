# Postman Demo Guide - Category & Batch

Tài liệu này bổ sung cho phần kiểm thử trong báo cáo `Nhóm-7-CNPM-Kho-dược-phẩm.docx`, tập trung vào 2 module:

- Danh mục sản phẩm: `CategoryController`, base endpoint `/api/v1/categories`.
- Lô hàng: `BatchController`, base endpoint `/api/v1/batches`.

## 1. Mục tiêu demo

| Nhóm test | Module | Mục tiêu |
| --- | --- | --- |
| Auto functional test | Category | Tạo danh mục, validation, phân trang/filter, chi tiết, cập nhật, đổi trạng thái |
| Auto functional test | Batch | Tạo lô, validation ngày/sản phẩm, query, cập nhật, chặn generic status, quarantine/release |
| Performance smoke test | Category | Đo response time các API đọc danh mục |
| Performance smoke test | Batch | Đo response time các API đọc/query lô: list, detail, expiring, FIFO, by-product, traceability |

## 2. Collection dùng cho demo

| File | Nội dung |
| --- | --- |
| `docs/postman/05_Category_API_Test.postman_collection.json` | Auto test chức năng module danh mục |
| `docs/postman/07_Batch_API_Test.postman_collection.json` | Auto test chức năng module lô hàng |
| `docs/postman/08_Category_Batch_Performance.postman_collection.json` | Performance smoke test cho API đọc Category/Batch |
| `docs/postman/WHS_Local_Dev.postman_environment.json` | Environment local, có alias biến cho cả `baseUrl/authToken` và `base_url/access_token` |

## 3. Điều kiện trước khi chạy

1. Backend đang chạy tại `http://localhost:8080`.
2. MySQL/Redis/RabbitMQ đã chạy nếu profile local cần các service này.
3. Database có seed data chuẩn từ Flyway:
   - `categoryId = 92000000-0000-0000-0000-000000000001`
   - `uomId = 91000000-0000-0000-0000-000000000001`
   - `seedProductId = 95000000-0000-0000-0000-000000000001`
   - `seedBatchId = 96000000-0000-0000-0000-000000000001`
   - `warehouseId = 93000000-0000-0000-0000-000000000001`
4. Tài khoản demo mặc định: `admin/admin123`.

## 4. Chạy bằng Postman Desktop

Import các file sau:

1. `docs/postman/WHS_Local_Dev.postman_environment.json`
2. `docs/postman/05_Category_API_Test.postman_collection.json`
3. `docs/postman/07_Batch_API_Test.postman_collection.json`
4. `docs/postman/08_Category_Batch_Performance.postman_collection.json`

Thứ tự chạy đề xuất:

1. Chọn environment `WHS Local Dev`.
2. Run collection `WHS Category API Test`.
3. Run collection `WHS Batch API Test`.
4. Với collection `WHS Category & Batch Performance`, chạy folder `00 - Setup` một lần.
5. Chạy folder `01 - Category Read Performance` với `Iterations = 10`, `Delay = 50ms`.
6. Chạy folder `02 - Batch Read Performance` với `Iterations = 10`, `Delay = 50ms`.

## 5. Chạy bằng Newman trên Windows PowerShell

```powershell
cd C:\WareHouseSystem\Application\whsBE
.\docs\newman\run-category-batch-demo.ps1
```

Tùy chỉnh số vòng performance:

```powershell
.\docs\newman\run-category-batch-demo.ps1 -Iterations 30 -DelayMs 100
```

Tùy chỉnh URL backend:

```powershell
.\docs\newman\run-category-batch-demo.ps1 -BaseUrl http://localhost:8081
```

Reports HTML được ghi vào:

```text
docs/newman/reports/
```

## 6. Tiêu chí pass/fail

Functional auto test pass khi:

- Happy path trả đúng `200` hoặc `201`.
- Negative case trả đúng `400` hoặc `404`.
- Response dùng wrapper `BaseResponse` với `success=true` ở happy path.
- Category không nhận `code` từ client.
- Batch không nhận `batch_number` từ client.
- Batch generic `PATCH /status` bị chặn.
- Batch workflow đi đúng `AVAILABLE -> QUARANTINE -> AVAILABLE`.

Performance smoke test pass khi:

- Không có HTTP `5xx`.
- Mỗi request đọc nằm dưới `perfReadMaxMs`, mặc định `1000ms`.
- Newman report không có assertion fail.
- Report có thể dùng để trình bày response time từng endpoint trong demo.

## 7. Mapping vào báo cáo

Có thể đưa vào chương kiểm thử:

| Nội dung báo cáo | Bằng chứng demo |
| --- | --- |
| Thiết kế test case | `docs/TEST_REQUIREMENTS_CATEGORY_PRODUCT_BATCH.md` |
| Kiểm thử tự động API | `05_Category_API_Test`, `07_Batch_API_Test` |
| Kiểm thử hiệu năng cơ bản | `08_Category_Batch_Performance` + Newman HTML report |
| Kết quả kiểm thử | Ảnh màn hình Postman/Newman report trong `docs/newman/reports/` |
