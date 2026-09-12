# WHS - Performance & Correctness Testing Plan

## Mục lục
1. [Tổng quan](#1-tổng-quan)
2. [Kiến trúc cần test](#2-kiến-trúc-cần-test)
3. [Thiết lập môi trường](#3-thiết-lập-môi-trường)
4. [Cài đặt công cụ](#4-cài-đặt-công-cụ)
5. [Chuẩn bị test data](#5-chuẩn-bị-test-data)
6. [Area 1 – Inventory Reserve Race Condition](#6-area-1--inventory-reserve-race-condition)
7. [Area 2 – Concurrent Stock Increase / Decrease](#7-area-2--concurrent-stock-increase--decrease)
8. [Area 3 – Authentication & Rate Limiting](#8-area-3--authentication--rate-limiting)
9. [Area 4 – Sales Order Concurrent Confirmation](#9-area-4--sales-order-concurrent-confirmation)
10. [Area 5 – RBAC Permission Enforcement](#10-area-5--rbac-permission-enforcement)
11. [Area 6 – Connection Pool & System Load](#11-area-6--connection-pool--system-load)
12. [Chạy tất cả bằng Newman](#12-chạy-tất-cả-bằng-newman)
13. [Metrics & Benchmark](#13-metrics--benchmark)
14. [Checklist xác nhận hệ thống OK](#14-checklist-xác-nhận-hệ-thống-ok)

---

## 1. Tổng quan

### Hệ thống WHS sử dụng các cơ chế chịu tải sau:
| Cơ chế | Vị trí | Mục đích |
|--------|--------|----------|
| Redisson Distributed Lock | `InventoryServiceImpl.reserve()` | Chống race condition khi reserve |
| Pessimistic Write Lock | `InventoryRepository.findBestSuitableForUpdate()` | Khóa DB row khi update stock |
| Optimistic Lock (`@Version`) | `Inventory` entity | Phát hiện concurrent modification |
| Bucket4j + Redis Rate Limit | `RateLimitFilter` | Chống brute force, abuse |
| HikariCP Connection Pool | `application.yml` | Pool 10 DB connections |
| Redis Permission Cache | `PermissionCacheService` | TTL 15 phút, giảm DB reads |
| RabbitMQ DLQ + Retry | Email, Background Job | Đảm bảo xử lý async |

### 6 Critical Test Areas:
```
Area 1: Inventory Reserve Race Condition      → Quan trọng nhất (overselling risk)
Area 2: Concurrent Stock Increase/Decrease    → Tính toán số học đúng đắn
Area 3: Authentication & Rate Limiting        → Security boundary
Area 4: Sales Order Concurrent Confirmation   → Idempotency
Area 5: RBAC Permission Enforcement           → Authorization
Area 6: Connection Pool & System Load         → Khả năng chịu tải tổng thể
```

---

## 2. Kiến trúc cần test

```
Client Request
     │
     ▼
[RateLimitFilter]        ← Bucket4j + Redis (per IP / per user)
     │
     ▼
[JwtAuthFilter]          ← Validate JWT, load permissions từ Redis cache
     │
     ▼
[Controller]             ← Input validation (@Valid)
     │
     ▼
[Service]
  ├── Redisson Lock      ← Distributed lock (10s wait, 30s hold)
  ├── @Transactional     ← DB transaction boundary
  └── PESSIMISTIC_WRITE  ← SELECT ... FOR UPDATE trên Inventory row
     │
     ▼
[MySQL] ─── [HikariCP: pool 5-10]
[Redis] ─── [Lettuce: pool 8]
```

---

## 3. Thiết lập môi trường

### 3.1 Khởi động services
```bash
# Từ thư mục gốc project
docker-compose up -d mysql redis rabbitmq minio

# Kiểm tra health
curl http://localhost:8080/actuator/health
```

### 3.2 Khởi động ứng dụng
```bash
cd /c/WareHouseSystem/Application/whsBE
mvn spring-boot:run
```

### 3.3 Xác nhận Flyway migration đã chạy
```bash
# Kiểm tra Flyway migration status
curl -s http://localhost:8080/actuator/health | python -m json.tool
# Kết quả mong đợi: "status": "UP"
```

---

## 4. Cài đặt công cụ

### 4.1 Newman (Postman CLI)
```bash
# Cài đặt Newman và reporter
npm install -g newman newman-reporter-html newman-reporter-htmlextra

# Kiểm tra cài đặt
newman --version
```

### 4.2 k6 (Load Testing - tùy chọn)
```bash
# Windows (scoop)
scoop install k6

# Hoặc download từ: https://k6.io/docs/get-started/installation/
k6 version
```

### 4.3 Postman Desktop
- Download: https://www.postman.com/downloads/
- Import các collection từ `docs/postman/`
- Import environment từ `docs/postman/WHS_Performance_Env.postman_environment.json`

---

## 5. Chuẩn bị test data

### BƯỚC 1: Login admin và lấy token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "Admin@123"}'
```
→ Copy `access_token` vào environment variable `access_token`

### BƯỚC 2: Tạo test product qua Swagger
- Mở http://localhost:8080/swagger-ui.html
- Vào **Product Management** → `POST /api/v1/products`
- Tạo sản phẩm: `{"name": "Test Product RC", "sku": "SKU-TEST-RC-001", ...}`
- Copy `id` vào environment variable `test_product_id`

### BƯỚC 3: Lấy warehouse ID
```bash
curl http://localhost:8080/api/v1/warehouses \
  -H "Authorization: Bearer {{access_token}}"
```
→ Copy `id` của warehouse đầu tiên vào `test_warehouse_id`

### BƯỚC 4: Lấy location ID (nếu cần)
```bash
curl "http://localhost:8080/api/v1/locations?warehouse_id={{test_warehouse_id}}" \
  -H "Authorization: Bearer {{access_token}}"
```
→ Copy `id` vào `test_location_id`

### BƯỚC 5: Lấy customer ID
```bash
curl "http://localhost:8080/api/v1/business-partners?type=CUSTOMER" \
  -H "Authorization: Bearer {{access_token}}"
```
→ Copy `id` vào `test_customer_id`

### BƯỚC 6: Cập nhật environment file
Mở `docs/postman/WHS_Performance_Env.postman_environment.json` và điền:
```json
{
  "test_product_id": "<product_id_vừa_tạo>",
  "test_warehouse_id": "<warehouse_id>",
  "test_location_id": "<location_id>",
  "test_customer_id": "<customer_id>"
}
```

---

## 6. Area 1 – Inventory Reserve Race Condition

**Risk:** Overselling - nhiều request cùng lúc reserve cùng 1 inventory  
**Cơ chế bảo vệ:** Redisson Lock (`lock:reserve:{orderLineId}`) + Pessimistic Write Lock  
**Collection:** `docs/postman/02_Inventory_ConcurrentTest.postman_collection.json`

### Kịch bản test:

#### Scenario A: Setup Stock
```
POST /api/v1/inventories/increase
Body: {
  "product_id": "{{test_product_id}}",
  "warehouse_id": "{{test_warehouse_id}}",
  "quantity": 10,
  "reference_type": "STOCK_ADJUSTMENT",
  "reference_number": "TEST-SETUP-{{timestamp}}"
}
Kết quả mong đợi: on_hand_quantity = 10
```

#### Scenario B: Tạo 15 Sales Orders (mỗi order 1 unit)
```
POST /api/v1/sales-orders (x15 lần)
Body mỗi lần: {
  "customer_id": "{{test_customer_id}}",
  "warehouse_id": "{{test_warehouse_id}}",
  "order_date": "{{today}}",
  "requested_delivery_date": "{{tomorrow}}",
  "currency": "VND",
  "lines": [{"product_id": "{{test_product_id}}", "quantity_ordered": 1, "unit_price": 100000}]
}
→ Lưu lại 15 order_id và order_line_id
```

#### Scenario C: Confirm 15 Orders đồng thời
```bash
# Chạy 15 Newman instance song song
for i in {1..15}; do
  newman run 02_Inventory_ConcurrentTest.postman_collection.json \
    -e WHS_Performance_Env.postman_environment.json \
    --folder "Confirm Order $i" &
done
wait
```

**Expected Results:**
```
✓ 10 orders confirmed thành công (status 200, CONFIRMED)
✓ 5 orders thất bại (status 409, error code INV_004 - Insufficient inventory)
✓ GET /api/v1/inventories/summary/{{test_product_id}}:
    reserved_quantity = 10
    available_quantity = 0
    on_hand_quantity = 10
✓ Không có negative stock
✓ StockMovements có đúng 10 RESERVE entries
```

#### Scenario D: Idempotency - Confirm cùng 1 order 2 lần đồng thời
```bash
# Gửi 2 request confirm order_id_1 cùng lúc
newman run ... --folder "Idempotency Test" &
newman run ... --folder "Idempotency Test" &
wait
# Expected: chỉ 1 reservation được tạo, không duplicate
```

### Step-by-step trong Postman Collection Runner:
1. Import `02_Inventory_ConcurrentTest.postman_collection.json`
2. Chạy folder **01-Setup** (1 lần)
3. Chạy folder **02-Create-Orders** với **Iterations = 15**
4. Chạy folder **03-Concurrent-Confirm** với **Delay = 0ms**
5. Chạy folder **04-Verify-Consistency** (1 lần)
6. Kiểm tra kết quả trong Test Results panel

---

## 7. Area 2 – Concurrent Stock Increase / Decrease

**Risk:** Concurrent modifications gây sai số tồn kho  
**Cơ chế bảo vệ:** `@Transactional` + Pessimistic Write Lock  
**Collection:** `docs/postman/02_Inventory_ConcurrentTest.postman_collection.json`

### Kịch bản test:

#### Scenario A: 20 concurrent INCREASE requests
```
POST /api/v1/inventories/increase (x20)
quantity = 1 mỗi request
reference_number phải UNIQUE mỗi lần (dùng timestamp + random)
Expected final: on_hand += 20
```

#### Scenario B: 10 concurrent DECREASE sau khi increase xong
```
POST /api/v1/inventories/decrease (x10)
quantity = 1 mỗi request
Expected: không negative stock, số học đúng
```

#### Verification:
```bash
# Sau khi chạy: GET inventory summary
# Expected: on_hand = (initial + 20 successful increases) - (10 successful decreases)
# Mọi StockMovement phải có: quantity_after = quantity_before + quantity_change
```

### Step-by-step Newman:
```bash
# Setup: stock = 0
newman run 02_Inventory_ConcurrentTest.postman_collection.json \
  -e WHS_Performance_Env.postman_environment.json \
  --folder "01-Setup-Zero-Stock"

# 20 concurrent increases
for i in $(seq 1 20); do
  newman run 02_Inventory_ConcurrentTest.postman_collection.json \
    -e WHS_Performance_Env.postman_environment.json \
    --folder "Increase-Stock" \
    --env-var "ref_suffix=$i" &
done
wait

# Verify
newman run 02_Inventory_ConcurrentTest.postman_collection.json \
  -e WHS_Performance_Env.postman_environment.json \
  --folder "Verify-Stock-Count"
```

---

## 8. Area 3 – Authentication & Rate Limiting

**Risk:** Brute force attack, token abuse  
**Cơ chế bảo vệ:** Bucket4j + Redis (fail-closed), per-IP rate limiting  
**Collection:** `docs/postman/01_Auth_RateLimit_Test.postman_collection.json`

### Rate limits cần test:
| Endpoint | Limit | Window | Type |
|----------|-------|--------|------|
| `POST /auth/login` | 5 req | 5 phút | Per IP |
| `POST /auth/register` | 3 req | 60 phút | Per IP |
| `POST /auth/refresh-token` | 10 req | 1 phút | Per User |
| `POST /auth/forgot-password` | 3 req | 15 phút | Per IP |
| Tất cả khác | 100 req | 1 phút | Default |

### Step-by-step:

#### Test 1: Login Rate Limit
```bash
# Gửi 7 request login liên tiếp nhanh nhất có thể
newman run 01_Auth_RateLimit_Test.postman_collection.json \
  -e WHS_Performance_Env.postman_environment.json \
  --folder "Login-Rate-Limit" \
  --iteration-count 7 \
  --delay-request 0

# Expected:
# Request 1-5: HTTP 200
# Request 6-7: HTTP 429 với header:
#   X-RateLimit-Remaining: 0
#   Retry-After: <seconds>
```

#### Test 2: Token Refresh Rate Limit
```bash
newman run 01_Auth_RateLimit_Test.postman_collection.json \
  -e WHS_Performance_Env.postman_environment.json \
  --folder "Refresh-Rate-Limit" \
  --iteration-count 12

# Expected:
# Request 1-10: HTTP 200 với new access_token
# Request 11-12: HTTP 429
```

#### Test 3: Token Validation
```
a) No Authorization header   → HTTP 401
b) "Bearer invalid_token"    → HTTP 401
c) "Bearer expired_token"    → HTTP 401  
d) "Bearer valid_token"      → HTTP 200
e) "Bearer valid_token" (wrong permission) → HTTP 403
```

#### Test 4: Default Rate Limit (100/60s)
```bash
# Gửi 105 request đến endpoint bất kỳ trong 60s
for i in $(seq 1 105); do
  curl -s -o /dev/null -w "%{http_code}\n" \
    http://localhost:8080/api/v1/inventories \
    -H "Authorization: Bearer {{access_token}}" &
done
wait
# Expected: request 101-105 trả về 429
```

---

## 9. Area 4 – Sales Order Concurrent Confirmation

**Risk:** Double-booking inventory, duplicate reservations  
**Cơ chế bảo vệ:** Redisson Lock + Idempotency check (`findByOrderLineId`)  
**Collection:** `docs/postman/03_SalesOrder_ConcurrentTest.postman_collection.json`

### Step-by-step:

#### Test 1: Tạo nhiều Sales Orders song song
```bash
# Tạo 10 orders đồng thời
for i in $(seq 1 10); do
  newman run 03_SalesOrder_ConcurrentTest.postman_collection.json \
    -e WHS_Performance_Env.postman_environment.json \
    --folder "Create-Draft-Order" \
    --env-var "order_suffix=$i" &
done
wait

# Expected:
# Tất cả 10 orders tạo thành công (HTTP 201)
# SO numbers đều UNIQUE (format SO-YYYYMM-XXXX)
# Status: DRAFT
# Inventory KHÔNG bị ảnh hưởng
```

#### Test 2: Confirm cùng 1 order 2 lần đồng thời
```bash
# Lấy order_id từ Test 1
ORDER_ID="<order_id_từ_step_1>"

# Confirm 2 lần song song
newman run ... --folder "Confirm-Order" --env-var "order_id=$ORDER_ID" &
newman run ... --folder "Confirm-Order" --env-var "order_id=$ORDER_ID" &
wait

# Expected:
# Chính xác 1 request thành công (HTTP 200, CONFIRMED)
# 1 request thất bại hoặc cũng trả về 200 (idempotent - cùng kết quả)
# Chỉ 1 InventoryReservation được tạo (không duplicate)
# reserved_quantity chỉ tăng 1 lần
```

#### Test 3: Confirm 15 orders với stock = 10
```
Setup: stock = 10 units
Create: 15 orders, mỗi order 1 unit
Confirm: 15 orders đồng thời

Expected:
  - 10 orders → CONFIRMED (stock đủ)
  - 5 orders → lỗi hoặc giữ DRAFT (không đủ stock)
  - Tổng reserved_quantity = 10
  - Không có negative stock
```

#### Test 4: Cancel và verify unreserve
```
Cancel một CONFIRMED order:
  PUT /api/v1/sales-orders/{id}/cancel
  
Expected:
  - Order status = CANCELED
  - reserved_quantity giảm đúng lượng
  - available_quantity tăng lại
  - StockMovement UNRESERVE được tạo
```

---

## 10. Area 5 – RBAC Permission Enforcement

**Risk:** Unauthorized access, privilege escalation  
**Cơ chế bảo vệ:** JWT + `@PreAuthorize` + Permission cache (Redis 15 phút)  
**Collection:** `docs/postman/04_RBAC_SecurityTest.postman_collection.json`

### Permissions cần test:
```
PERM_INVENTORY_READ              → GET /api/v1/inventories
PERM_INVENTORY_RESERVATION_UPDATE → POST /api/v1/inventories/reserve
PERM_INVENTORY_MUTATION_UPDATE   → POST /api/v1/inventories/increase|decrease
PERM_SALES_ORDER_CREATE          → POST /api/v1/sales-orders
PERM_SALES_ORDER_READ            → GET /api/v1/sales-orders
PERM_SALES_ORDER_UPDATE          → PUT /api/v1/sales-orders/{id}/confirm
```

### Step-by-step:

#### Test 1: Không có token
```bash
curl http://localhost:8080/api/v1/inventories
# Expected: HTTP 401
# Body: {"code": "UNAUTHORIZED", ...}
```

#### Test 2: Token sai / hết hạn
```bash
curl http://localhost:8080/api/v1/inventories \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.invalid.signature"
# Expected: HTTP 401
```

#### Test 3: Đúng token nhưng sai permission
```
1. Tạo user mới chỉ có PERM_SALES_ORDER_READ
2. Login → lấy token
3. Thử gọi POST /api/v1/inventories/reserve
Expected: HTTP 403
```

#### Test 4: Đúng permission → access thành công
```
User có PERM_INVENTORY_READ:
GET /api/v1/inventories → HTTP 200

User có PERM_SALES_ORDER_CREATE:
POST /api/v1/sales-orders → HTTP 201
```

#### Test 5: Permission Cache Consistency
```
1. User X có permission PERM_INVENTORY_READ
2. Login → lấy token → verify GET /inventories = 200
3. Admin revoke permission qua PUT /api/v1/role-permissions
4. Ngay lập tức (trước 15 phút): GET /inventories = 200 (cache chưa expire!)
5. Sau khi cache expire hoặc evict: GET /inventories = 403
```

**Lưu ý:** Cache TTL = 15 phút. Đây là trade-off giữa performance và security.
Để force evict: gọi admin API xóa cache hoặc restart Redis.

---

## 11. Area 6 – Connection Pool & System Load

**Risk:** DB connection pool exhaustion, request timeouts  
**Cơ chế:** HikariCP max-pool-size=10, connection-timeout=30s  
**Collection:** Dùng Newman với nhiều iteration đồng thời

### Step-by-step:

#### Test 1: Sustained Load (30 users, 60 giây)
```bash
# Chạy 30 Newman instance đồng thời trong 60 giây
for i in $(seq 1 30); do
  newman run 02_Inventory_ConcurrentTest.postman_collection.json \
    -e WHS_Performance_Env.postman_environment.json \
    --folder "Get-Inventories" \
    --iteration-count 10 \
    --delay-request 2000 &
done
wait
```

**Monitor trong quá trình test:**
```bash
# Terminal 1: HikariCP metrics
watch -n 2 'curl -s http://localhost:8080/actuator/metrics/hikaricp.connections.active | python -m json.tool'

# Terminal 2: Response times
watch -n 2 'curl -s http://localhost:8080/actuator/metrics/http.server.requests | python -m json.tool'
```

#### Test 2: Spike Test - Đột ngột 50 requests
```bash
# Gửi 50 requests cùng lúc
for i in $(seq 1 50); do
  curl -s -o /dev/null -w "%{http_code} %{time_total}\n" \
    http://localhost:8080/api/v1/inventories \
    -H "Authorization: Bearer {{access_token}}" &
done
wait
```

**Expected metrics:**
```
✓ HTTP 200 rate: >= 95%
✓ P50 response time: < 200ms
✓ P95 response time: < 1000ms
✓ P99 response time: < 2000ms
✓ HikariCP connection timeout: < 1% requests
✓ Không có HTTP 500
```

#### Test 3: Actuator metrics monitoring
```bash
# Kiểm tra connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
curl http://localhost:8080/actuator/metrics/hikaricp.connections.timeout

# Kiểm tra Redis
curl http://localhost:8080/actuator/metrics/spring.data.repository.invocations

# Kiểm tra response times
curl "http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/v1/inventories"
```

---

## 12. Chạy tất cả bằng Newman

### 12.1 Cấu trúc thư mục
```
docs/
├── PERFORMANCE_TEST_PLAN.md          ← File này
├── postman/
│   ├── WHS_Performance_Env.postman_environment.json
│   ├── 01_Auth_RateLimit_Test.postman_collection.json
│   ├── 02_Inventory_ConcurrentTest.postman_collection.json
│   ├── 03_SalesOrder_ConcurrentTest.postman_collection.json
│   └── 04_RBAC_SecurityTest.postman_collection.json
└── newman/
    ├── run-all-tests.sh               ← Script chạy tất cả
    ├── run-concurrent-inventory.sh    ← Script test race condition
    └── reports/                       ← HTML reports output
```

### 12.2 Chạy từng collection
```bash
# Area 3: Auth & Rate Limit
newman run docs/postman/01_Auth_RateLimit_Test.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/01_auth_report.html

# Area 1+2: Inventory
newman run docs/postman/02_Inventory_ConcurrentTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/02_inventory_report.html

# Area 4: Sales Order
newman run docs/postman/03_SalesOrder_ConcurrentTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/03_sales_order_report.html

# Area 5: RBAC
newman run docs/postman/04_RBAC_SecurityTest.postman_collection.json \
  -e docs/postman/WHS_Performance_Env.postman_environment.json \
  --reporters cli,html \
  --reporter-html-export docs/newman/reports/04_rbac_report.html
```

### 12.3 Chạy tất cả cùng lúc
```bash
bash docs/newman/run-all-tests.sh
```

---

## 13. Metrics & Benchmark

### Target metrics cho production-ready:
| Metric | Target | Critical Threshold |
|--------|--------|--------------------|
| P50 response time (GET) | < 100ms | > 500ms = FAIL |
| P95 response time (GET) | < 300ms | > 1000ms = FAIL |
| P50 response time (POST - write) | < 200ms | > 1000ms = FAIL |
| P95 response time (POST - write) | < 500ms | > 2000ms = FAIL |
| Error rate (5xx) | < 0.1% | > 1% = FAIL |
| Error rate (timeout) | < 0.5% | > 2% = FAIL |
| Race condition correctness | 100% | < 100% = FAIL |
| Rate limit enforcement | 100% | < 100% = FAIL |
| RBAC enforcement | 100% | < 100% = FAIL |

### HikariCP thresholds:
| Metric | Normal | Warning | Critical |
|--------|--------|---------|----------|
| Active connections | < 8 | 8-9 | = 10 (pool full) |
| Pending connections | 0 | 1-2 | > 3 |
| Connection timeout | 0 | - | > 0 |

---

## 14. Checklist xác nhận hệ thống OK

### Area 1 – Inventory Reserve Race Condition
- [ ] 15 orders reserve 10-unit stock: đúng 10 succeed, 5 fail
- [ ] Không có negative `on_hand_quantity`
- [ ] `reserved_quantity` = số orders confirmed × quantity/order
- [ ] Không có duplicate `InventoryReservation` cho cùng `order_line_id`
- [ ] `StockMovements` đủ số RESERVE entries

### Area 2 – Stock Operations
- [ ] 20 concurrent increase: tổng stock tăng đúng 20
- [ ] `reference_number` khác nhau không gây conflict
- [ ] `StockMovements.quantity_after = quantity_before + quantity_change`

### Area 3 – Auth & Rate Limit
- [ ] Login request thứ 6 trả về HTTP 429
- [ ] Refresh-token request thứ 11 trả về HTTP 429
- [ ] Invalid token → HTTP 401 (không phải 500)
- [ ] Rate limit reset sau đúng time window

### Area 4 – Sales Order
- [ ] Confirm cùng order 2 lần: chỉ 1 reservation được tạo
- [ ] SO numbers không trùng dù tạo đồng thời
- [ ] Cancel → stock được unreserve đúng

### Area 5 – RBAC
- [ ] No token → 401
- [ ] Wrong permission → 403
- [ ] Correct permission → 200
- [ ] Permission cache invalidation hoạt động

### Area 6 – System Load
- [ ] P95 response time < 1000ms dưới 30 concurrent users
- [ ] Không có HTTP 500 trong sustained load test
- [ ] HikariCP không bao giờ timeout
