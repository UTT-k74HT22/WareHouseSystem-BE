# Hướng dẫn Test Race Condition cho Inventory

## Tổng quan

Tài liệu này hướng dẫn test các race condition có thể xảy ra trong module Inventory:

- **Increase**: Tăng tồn kho (nhập hàng, điều chỉnh tăng)
- **Decrease**: Giảm tồn kho (xuất hàng, điều chỉnh giảm)
- **Reserve**: Đặt trước tồn kho cho đơn hàng
- **Unreserve**: Hủy đặt trước

## Cơ chế bảo vệ hiện tại

| Operation | Cơ chế | Lock Key |
|-----------|--------|----------|
| Increase | Redisson Lock + DB Unique Constraint | `lock:inventory:reference:{type}:{id}` |
| Decrease | Redisson Lock + DB Unique Constraint | `lock:inventory:reference:{type}:{id}` |
| Reserve | Redisson Lock | `lock:reserve:{orderLineId}` |
| Unreserve | @Version (Optimistic Locking) | - |

---

## Các Test Cases Race Condition

### 1. Concurrent Increase Requests (Tăng tồn kho đồng thời)

**Mục tiêu**: Khi nhiều request tăng tồn kho cùng lúc, tổng tồn kho phải bằng tổng các lần tăng.

**Scenario**:
```
Initial: onHand = 100
Request 1: +50
Request 2: +30
Request 3: +20
Expected: onHand = 200
```

**Test Steps**:
1. Tạo inventory ban đầu với onHand = 100
2. Gửi 3 request increase đồng thời
3. Verify tổng onHand = 200

### 2. Concurrent Decrease Requests (Giảm tồn kho đồng thời)

**Mục tiêu**: Khi nhiều request giảm tồn kho cùng lúc, tổng tồn kho phải đúng.

**Scenario**:
```
Initial: onHand = 100
Request 1: -30
Request 2: -40
Request 3: -20
Expected: onHand = 10
```

**Test Steps**:
1. Tạo inventory ban đầu với onHand = 100
2. Gửi 3 request decrease đồng thời
3. Verify tổng onHand = 10

### 3. Concurrent Decrease - Insufficient Stock (Giảm quá tồn kho)

**Mục tiêu**: Khi giảm nhiều hơn tồn kho hiện có, chỉ có request đầu tiên thành công.

**Scenario**:
```
Initial: onHand = 50
Request 1: -30
Request 2: -30
Request 3: -30
Expected: 1 success, 2 fail (insufficient stock)
```

**Test Steps**:
1. Tạo inventory ban đầu với onHand = 50
2. Gửi 3 request decrease đồng thời với tổng > 50
3. Verify: 1 request thành công, 2 request thất bại với lỗi "insufficient stock"

### 4. Concurrent Reserve Requests (Đặt trước đồng thời)

**Mục tiêu**: Khi nhiều request đặt trước cùng lúc, tổng reserved không vượt quá available.

**Scenario**:
```
Initial: onHand = 100, reserved = 0, available = 100
Request 1: reserve 60
Request 2: reserve 50
Expected: 1 success, 1 fail (insufficient available)
```

### 5. Concurrent Reserve & Decrease (Đặt trước và giảm đồng thời)

**Mục tiêu**: Test race giữa reserve và decrease.

**Scenario**:
```
Initial: onHand = 100, reserved = 0, available = 100
Request A: reserve 50
Request B: decrease 60 (not consumeReserved)
Expected: A or B success, other fails
```

### 6. Distributed Lock Contention (Tranh chấp Lock)

**Mục tiêu**: Khi một request đang giữ lock, request khác phải chờ hoặc báo lỗi timeout.

**Scenario**:
1. Request 1 bắt đầu increase (giữ lock)
2. Request 2 cố gắng decrease cùng reference
3. Verify Request 2 phải chờ hoặc báo lỗi timeout

### 7. Optimistic Locking Test (Version Conflict)

**Mục tiêu**: Khi update inventory mà version không khớp, phải báo lỗi.

**Test Steps**:
1. Đọc inventory để lấy version
2. Update inventory với version cũ (giả định)
3. Verify: 409 Conflict hoặc retry

---

## Postman Collection Structure

```
WHS Inventory Race Condition
├── 00-Setup
│   ├── Login
│   └── Create Test Inventory
├── 01-Concurrent-Increase
│   ├── Increase - 3 Concurrent Requests
│   └── Verify Total
├── 02-Concurrent-Decrease
│   ├── Decrease - 3 Concurrent Requests
│   └── Verify Total
├── 03-Insufficient-Stock
│   ├── Decrease Over Stock - 3 Concurrent
│   └── Verify Results
├── 04-Concurrent-Reserve
│   ├── Reserve - 2 Concurrent Requests
│   └── Verify Results
├── 05-Lock-Contention
│   ├── Increase with Delay (Hold Lock)
│   └── Concurrent Decrease (Timeout)
└── 06-Optimistic-Locking
    └── Version Conflict Test
```

---

## Cấu hình Test Data

| Field | Giá trị test | Mô tả |
|-------|-------------|-------|
| productId | PROD_RC_001 | Sản phẩm test |
| warehouseId | WHS_RC_001 | Kho test |
| locationId | LOC_RC_001 | Vị trí test |
| batchId | (auto) | Batch (optional) |
| initialQty | 100 | Số lượng ban đầu |

---

## API Endpoints

### Increase Inventory
```
POST {{baseUrl}}/api/v1/inventories/increase
Content-Type: application/json
Authorization: {{authToken}}

{
  "productId": "{{productId}}",
  "warehouseId": "{{warehouseId}}",
  "locationId": "{{locationId}}",
  "batchId": null,
  "quantity": 50,
  "referenceType": "STOCK_ADJUSTMENT",
  "referenceId": "SA-{{$timestamp}}",
  "referenceNumber": "SA-{{$timestamp}}",
  "reason": "Test increase"
}
```

### Decrease Inventory
```
POST {{baseUrl}}/api/v1/inventories/decrease
Content-Type: application/json
Authorization: {{authToken}}

{
  "productId": "{{productId}}",
  "warehouseId": "{{warehouseId}}",
  "locationId": "{{locationId}}",
  "batchId": null,
  "quantity": 30,
  "consumeReserved": false,
  "referenceType": "STOCK_ADJUSTMENT",
  "referenceId": "SA-{{$timestamp}}",
  "referenceNumber": "SA-{{$timestamp}}",
  "reason": "Test decrease"
}
```

### Reserve Inventory
```
POST {{baseUrl}}/api/v1/inventories/reserve
Content-Type: application/json
Authorization: {{authToken}}

{
  "productId": "{{productId}}",
  "warehouseId": "{{warehouseId}}",
  "locationId": null,
  "batchId": null,
  "quantity": 50,
  "orderLineId": "ORDLINE-{{$timestamp}}"
}
```

---

## Expected Results

### Test 1: Concurrent Increase
| Request | quantity | Expected onHand |
|---------|----------|-----------------|
| Initial | 100 | 100 |
| Increase 1 | +50 | 150 |
| Increase 2 | +30 | 180 |
| Increase 3 | +20 | 200 |

### Test 2: Concurrent Decrease
| Request | quantity | Expected onHand |
|---------|----------|-----------------|
| Initial | 100 | 100 |
| Decrease 1 | -30 | 70 |
| Decrease 2 | -40 | 30 |
| Decrease 3 | -20 | 10 |

### Test 3: Insufficient Stock
| Request | quantity | Result | Final onHand |
|---------|----------|--------|--------------|
| Initial | 100 | - | 100 |
| Decrease 1 | -30 | Success | 70 |
| Decrease 2 | -30 | Fail (409) | 70 |
| Decrease 3 | -30 | Fail (409) | 70 |

### Test 4: Concurrent Reserve
| Request | quantity | Result | Reserved |
|---------|----------|--------|----------|
| Initial | 100 | - | 0 |
| Reserve 1 | 60 | Success | 60 |
| Reserve 2 | 50 | Fail (409) | 60 |

---

## Cách chạy Test

### Cách 1: Manual với Runner

1. Import collection vào Postman
2. Chạy request "00-Setup" trước
3. Mở Collection Runner
4. Chọn folder cần test
5. Set iterations = 1
6. Run và xem kết quả

### Cách 2: Newman CLI (Parallel Execution)

```bash
# Cài đặt concurrently nếu chưa có
npm install -g concurrently

# Chạy 3 request increase đồng thời
concurrently \
  "newman run WHS_Inventory_RaceCondition.json --folder 'Increase-1'" \
  "newman run WHS_Inventory_RaceCondition.json --folder 'Increase-2'" \
  "newman run WHS_Inventory_RaceCondition.json --folder 'Increase-3'"
```

### Cách 3: Script tự động

Tạo file `test-race-condition.js`:

```javascript
const newman = require('newman');

const options = {
    collection: './WHS_Inventory_RaceCondition.json',
    environment: './WHS_Local_Dev.postman_environment.json',
    reporters: ['json'],
    reporter: {
        json: {
            export: './race-test-results.json'
        }
    }
};

console.log('Running race condition tests...');
newman.run(options, (err, summary) => {
    if (err) {
        console.error('Error:', err);
        process.exit(1);
    }
    console.log('Tests completed');
    console.log('Run summary:', summary.run.stats);
});
```

---

## Checklists

### ☐ Pre-Test
- [ ] Ứng dụng đang chạy
- [ ] Đã login và có token
- [ ] Đã tạo test inventory với số lượng ban đầu
- [ ] Đã hiểu cách check kết quả

### ☐ Test Execution
- [ ] Test concurrent increase
- [ ] Test concurrent decrease
- [ ] Test insufficient stock
- [ ] Test concurrent reserve
- [ ] Test lock contention

### ☐ Post-Test
- [ ] Verify tổng tồn kho đúng
- [ ] Ghi lại bugs nếu có
- [ ] Cleanup test data

---

## Troubleshooting

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| 409 Conflict | Lock không acquire được | Kiểm tra Redisson đang chạy |
| 500 Internal Error | Lỗi database | Kiểm tra logs |
| onHand không đúng | Race condition | Báo bug cho Dev |
| Timeout | Lock chờ quá lâu | Tăng timeout hoặc báo bug |

---

**Version**: 1.0  
**Last Updated**: 2026-04-08
