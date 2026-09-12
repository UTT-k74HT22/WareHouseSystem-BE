# Hướng dẫn Setup & Import Postman Collection

> **Mục tiêu**: Hướng dẫn import và chạy API Test Collection cho Product Module

---

## Bước 1: Cài đặt công cụ

### 1.1 Cài đặt Postman

| OS | Link Download | Yêu cầu |
|----|---------------|---------|
| Windows | https://www.postman.com/downloads/ | Windows 10+ |
| macOS | https://www.postman.com/downloads/ | macOS 10.15+ |
| Linux | https://www.postman.com/downloads/ | Ubuntu 18.04+ |

### 1.2 Cài đặt Node.js (cho Newman)

```bash
# Kiểm tra đã cài chưa
node --version
npm --version

# Nếu chưa cài, download từ https://nodejs.org/
# Khuyên dùng LTS version
```

### 1.3 Cài đặt Newman

```bash
# Cài đặt global
npm install -g newman

# Verify cài đặt
newman --version
```

### 1.4 Cài đặt Newman HTML Reporter

```bash
npm install -g newman-reporter-html
```

---

## Bước 2: Import Collection

### 2.1 Import Collection (JSON)

1. Mở Postman
2. Click **Import** button (góc trên bên trái)
3. Drag & drop file `WHS_Product_API.json` vào ô import
4. Hoặc click **Upload Files** và chọn file
5. Click **Import**

### 2.2 Tạo Environment

1. Click **Environments** (sidebar bên phải)
2. Click **+** để tạo environment mới
3. Đặt tên: `WHS Local Dev`
4. Thêm các biến:

| Variable | Initial Value | Current Value | Description |
|----------|---------------|---------------|-------------|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` | API Base URL |
| `authToken` | `` | `` | JWT Token |
| `productId` | `` | `` | ID sản phẩm test |
| `productSku` | `` | `` | SKU sản phẩm test |
| `categoryId` | `CAT001` | `CAT001` | Category ID test |
| `uomId` | `UOM001` | `UOM001` | UOM ID test |

5. Click **Save**

### 2.3 Chọn Environment

Trong dropdown góc trên bên phải, chọn `WHS Local Dev`

---

## Bước 3: Chạy Tests

### 3.1 Chạy thủ công (Manual)

#### Cách 1: Từng request
1. Mở Collection `WHS Product API`
2. Click request **00-Setup > Login**
3. Click **Send**
4. Xác nhận token được lưu vào biến `authToken`
5. Tiếp tục với các request khác theo thứ tự

#### Cách 2: Collection Runner
1. Click **WHS Product API** collection
2. Click **Run** button
3. Cấu hình:
   - **Iterations**: 1
   - **Delay**: 500ms
   - **Data**: (để trống)
4. Click **Run WHS Product API**
5. Xem kết quả trong tab **Run Results**

### 3.2 Chạy bằng Newman (CLI)

```bash
# Di chuyển vào thư mục docs/postman
cd docs/postman

# Chạy collection
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json

# Chạy với HTML report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html

# Chạy với JSON report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r json

# Chạy với cả hai và xuất file
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html,json --reporter-html-export report.html --reporter-json-export report.json
```

### 3.3 Newman Options Thường dùng

| Option | Mô tả | Ví dụ |
|--------|-------|-------|
| `-e` | Chỉ định environment | `-e WHS_Local_Dev.postman_environment.json` |
| `-r` | Reporter type | `-r html,json` |
| `--reporter-html-export` | Xuất HTML report | `--reporter-html-export report.html` |
| `--reporter-json-export` | Xuất JSON report | `--reporter-json-export report.json` |
| `-n` | Số iterations | `-n 10` |
| `--request-timeout` | Timeout per request (ms) | `--request-timeout 60000` |
| `--insecure` | Bỏ qua SSL verification | `--insecure` |
| `-v` | Verbose output | `-v` |
| `--folder` | Chạy folder cụ thể | `--folder "01-Create"` |

---

## Bước 4: Xuất kết quả

### 4.1 Từ Postman

1. Sau khi chạy xong Runner
2. Click **Export Results**
3. Chọn định dạng (JSON/CSV)
4. Lưu file

### 4.2 Từ Newman

```bash
# Kết quả sẽ hiển thị trên terminal

# Hoặc xuất ra file
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html,json

# File sẽ được lưu vào thư mục hiện tại
# - newman/
#   - collection-json-report.json
#   - collection-html-report.html
```

---

## Bước 5: Troubleshooting

### 5.1 Lỗi thường gặp

| Lỗi | Nguyên nhân | Cách fix |
|-----|-------------|----------|
| `401 Unauthorized` | Token không đúng | Chạy lại Login, kiểm tra credentials |
| `Connection refused` | App không chạy | Start ứng dụng trước |
| `ReferenceError: productId is not defined` | Chưa chạy Create | Chạy theo thứ tự folder |
| `ECONNREFUSED` | Wrong port | Kiểm tra `baseUrl` trong Environment |

### 5.2 Kiểm tra biến

```javascript
// Trong Postman Console
console.log("productId:", pm.collectionVariables.get("productId"))
console.log("authToken:", pm.collectionVariables.get("authToken"))
```

Để mở Postman Console: View > Show Postman Console

### 5.3 Debug Network

1. Mở Postman Console (View > Show Postman Console)
2. Gửi request
3. Xem chi tiết trong Console tab

---

## Checklists

### ☐ Setup Environment
- [ ] Đã cài Postman
- [ ] Đã cài Node.js
- [ ] Đã cài Newman
- [ ] Đã import Collection
- [ ] Đã tạo Environment
- [ ] Đã config đúng biến

### ☐ Chạy Tests
- [ ] App đang chạy trên localhost:8080
- [ ] Đã login thành công
- [ ] Đã chạy Create
- [ ] Đã chạy Read
- [ ] Đã chạy Update
- [ ] Đã chạy Delete
- [ ] Đã verify sau Delete

### ☐ Xuất kết quả
- [ ] Đã export kết quả
- [ ] Đã review failed tests
- [ ] Đã ghi lại bugs

---

## Quick Commands

```bash
# Quick start
cd docs/postman
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json

# Full report
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -r html,json --reporter-html-export report.html --reporter-json-export report.json

# Verbose
newman run WHS_Product_API.json -e WHS_Local_Dev.postman_environment.json -v
```

---

**Version**: 1.0  
**Last Updated**: 2026-04-08
