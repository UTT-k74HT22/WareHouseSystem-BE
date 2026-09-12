# WHS - Quick Start: Performance & Correctness Testing

Hướng dẫn từng bước chi tiết để chạy tất cả performance tests.

---

## BƯỚC 1: Cài đặt công cụ

```bash
# Cài Newman (Postman CLI)
npm install -g newman newman-reporter-html newman-reporter-htmlextra

# Kiểm tra
newman --version
# Output: newman/6.x.x
```

---

## BƯỚC 2: Khởi động ứng dụng

```bash
# Terminal 1: Start services
docker-compose up -d

# Terminal 2: Start Spring Boot
cd C:/WareHouseSystem/Application/whsBE
mvn spring-boot:run

# Kiểm tra health
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP",...}
```

---

## BƯỚC 3: Tạo dữ liệu test ban đầu

### 3a. Mở Swagger
```
http://localhost:8080/swagger-ui.html
```

### 3b. Login để lấy token cho Swagger
```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}' | python -m json.tool
```
→ Copy `access_token`, paste vào Authorize button trên Swagger UI

### 3c. Tạo test product
**Swagger** → Product Management → `POST /api/v1/products`
```json
{
  "name": "Test Product RC",
  "sku": "SKU-RC-001",
  "category_id": "<any_category_id>",
  "uom_id": "<any_uom_id>",
  "selling_price": 100000,
  "cost_price": 80000
}
```
→ Copy `id` từ response (dạng UUID)

### 3d. Lấy warehouse ID
```bash
curl -s http://localhost:8080/api/v1/warehouses?page=0&size=1 \
  -H "Authorization: Bearer <access_token>" | python -m json.tool
```
→ Copy `id` của warehouse đầu tiên

### 3e. Lấy customer ID
```bash
curl -s "http://localhost:8080/api/v1/business-partners?page=0&size=1" \
  -H "Authorization: Bearer <access_token>" | python -m json.tool
```
→ Copy `id` của customer đầu tiên

---

## BƯỚC 4: Cập nhật environment file

Mở file: `docs/postman/WHS_Performance_Env.postman_environment.json`

Điền các giá trị sau:
```json
{
  "test_product_id": "<UUID_product_vừa_tạo>",
  "test_warehouse_id": "<UUID_warehouse>",
  "test_customer_id": "<UUID_customer>"
}
```

---

## BƯỚC 5: Import vào Postman Desktop

1. Mở Postman
2. Import → Files → Chọn tất cả files trong `docs/postman/`:
   - `WHS_Performance_Env.postman_environment.json`
   - `01_Auth_RateLimit_Test.postman_collection.json`
   - `02_Inventory_ConcurrentTest.postman_collection.json`
   - `03_SalesOrder_ConcurrentTest.postman_collection.json`
   - `04_RBAC_SecurityTest.postman_collection.json`
3. Chọn environment **"WHS Performance Test Environment"** (góc trên bên phải)

---

## BƯỚC 6: Chạy tests

### Option A: Postman Collection Runner (Có giao diện)

1. Click vào collection → **Run collection**
2. Chọn environment: **WHS Performance Test Environment**
3. Chọn folder muốn chạy
4. Đặt Iterations và Delay như hướng dẫn trong từng folder

#### Quick Run – Auth Rate Limit:
- Collection: `01_Auth_RateLimit_Test`
- Folder: `01 - Login Rate Limit Test`
- **Iterations: 7, Delay: 0ms**
- Expected: Request 1-5 → 200, Request 6-7 → 429 ✓

#### Quick Run – Inventory Concurrent:
- Collection: `02_Inventory_ConcurrentTest`
- Folder: `00 - Setup & Auth` → Run once
- Folder: `01 - Setup Initial Stock` → Run once
- Folder: `02 - Create Sales Orders for Race Condition` → **Iterations: 15**
- Folder: `03 - Confirm Single Order` → **Iterations: 1** (pick an order ID)
- Folder: `06 - Verify Consistency` → Run once

#### Quick Run – RBAC Security:
- Collection: `04_RBAC_SecurityTest`
- Run All folders
- **Iterations: 1**

### Option B: Newman CLI (Tự động hóa)

```bash
# Chạy từng collection
cd C:/WareHouseSystem/Application/whsBE

# Area 3: Auth & Rate Limit
newman run docs/postman/01_Auth_RateLimit_Test.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/01_auth.html

# Area 1+2: Inventory
newman run docs/postman/02_Inventory_ConcurrentTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/02_inventory.html

# Area 4: Sales Order
newman run docs/postman/03_SalesOrder_ConcurrentTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/03_sales.html

# Area 5: RBAC
newman run docs/postman/04_RBAC_SecurityTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/04_rbac.html
```

### Option B2: Demo riêng module danh mục + lô hàng

```powershell
# Windows PowerShell
cd C:/WareHouseSystem/Application/whsBE
.\docs\newman\run-category-batch-demo.ps1 -Iterations 10 -DelayMs 50
```

Script này chạy:
- `05_Category_API_Test.postman_collection.json`
- `07_Batch_API_Test.postman_collection.json`
- `08_Category_Batch_Performance.postman_collection.json`

Report HTML được lưu tại `docs/newman/reports/`.

### Option C: Chạy tất cả với 1 lệnh

```bash
# Trên Git Bash / WSL / Linux
bash docs/newman/run-all-tests.sh
```

### Option D: Race Condition Test (Concurrent)

```bash
# Test 15 concurrent orders với 10 units stock
bash docs/newman/run-concurrent-inventory.sh 15

# Load test: 30 users trong 60 giây
bash docs/newman/run-load-test.sh 30 60
```

---

## BƯỚC 7: Xem kết quả

### Newman HTML Reports
Reports được lưu tại: `docs/newman/reports/`
Mở file `.html` bằng browser để xem:
- Pass/Fail summary
- Request/Response details
- Response time charts

### Postman Collection Runner
- Panel bên phải hiển thị test results
- Green = PASS, Red = FAIL
- Click vào từng request để xem details

### Console logs quan trọng cần kiểm tra:
```
INVENTORY CONSISTENCY CHECK:
  On Hand: 10
  Reserved: 10
  Available: 0
  Formula check: onHand(10) - quarantine(0) - reserved(10) = 0 | reported available: 0
  ✓ All correct

RACE CONDITION RESULTS:
  Total confirms attempted: 15
  Successful: 10          ← Đúng (= stock available)
  Failed (conflict): 5    ← Đúng (= attempts - stock)
  Total: 15
```

---

## Troubleshooting

| Vấn đề | Nguyên nhân | Giải pháp |
|--------|-------------|-----------|
| `access_token` rỗng | Chưa chạy Login request | Chạy folder `00-Setup` trước |
| `test_product_id = FILL_ME_IN` | Chưa điền env | Cập nhật environment file |
| HTTP 404 trên inventory summary | Product chưa có stock | Chạy `01-Setup-Initial-Stock` |
| HTTP 400 on reserve | `order_line_id` không tồn tại | Chạy `02-Create-Sales-Orders` trước |
| Rate limit triggered sớm | Test chạy quá nhanh | Đây là expected behavior |
| Port 8080 không respond | App chưa khởi động | `mvn spring-boot:run` |
| Redis connection refused | Redis chưa chạy | `docker-compose up -d redis` |

---

## Checklist Final

Sau khi chạy tất cả tests, kiểm tra:

- [ ] Auth: Login rate limit (429 sau request thứ 6) ✓
- [ ] Auth: Invalid token → 401 (không phải 500) ✓
- [ ] Inventory: Không có negative stock sau concurrent ops ✓
- [ ] Inventory: reserved_quantity = số orders confirmed × qty ✓
- [ ] Inventory: available = onHand - quarantine - reserved ✓
- [ ] Inventory: StockMovements audit trail đầy đủ ✓
- [ ] SalesOrder: SO numbers không trùng dù tạo đồng thời ✓
- [ ] SalesOrder: Double-confirm = idempotent (1 reservation) ✓
- [ ] RBAC: No token → 401 ✓
- [ ] RBAC: Wrong permission → 403 ✓
- [ ] RBAC: Correct permission → 200 ✓
- [ ] Load: P95 response time < 1000ms dưới 30 users ✓
- [ ] Load: Không có HTTP 500 ✓
