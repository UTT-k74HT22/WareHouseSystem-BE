# WMS 3 Core Business Flows
## Executive Summary

**Date:** 2026-03-13  
**Audience:** Customer Meeting, Steering Committee, Demo Session, Slide Summary

---

## 1. Core Message

Hệ thống kho của dự án được xây dựng quanh 3 flow chính:

1. **Inbound** - quản lý nhập hàng từ nhà cung cấp vào kho
2. **Outbound** - quản lý giữ hàng và xuất hàng cho khách
3. **Stock Adjustment** - quản lý điều chỉnh sai lệch tồn kho

Ba flow này tương ứng với 3 câu hỏi lớn trong vận hành kho:

- Hàng vào kho như thế nào
- Hàng ra khỏi kho như thế nào
- Khi số liệu lệch thực tế thì xử lý như thế nào

---

## 2. Snapshot Trạng Thái Dự Án

| Flow | Mục tiêu | Tình trạng |
|------|----------|------------|
| Inbound | Tăng tồn kho từ hàng nhận thực tế | **Đã triển khai** |
| Outbound | Giữ hàng, xuất hàng, giảm tồn kho | **Đang hoàn thiện** |
| Stock Adjustment | Điều chỉnh tồn kho và audit | **Đã triển khai** |

---

## 3. Flow 1 - Inbound

### Mục tiêu

Quản lý từ lúc doanh nghiệp đặt mua hàng đến khi hàng thực sự được nhập vào kho.

### Chứng từ chính

- `Purchase Order`
- `Purchase Order Lines`
- `Inbound Receipt`
- `Inbound Receipt Lines`
- `Inventory`

### Logic nghiệp vụ ngắn gọn

```text
Tạo PO
-> Thêm line hàng
-> Confirm PO
-> Tạo Inbound Receipt
-> Nhập receipt lines
-> Confirm Receipt
-> Inventory tăng
-> PO cập nhật tiến độ nhận
```

### Điểm quan trọng

- `Confirm PO` chỉ chốt đơn mua, chưa tăng tồn kho
- `Confirm Inbound Receipt` mới là thời điểm tăng tồn thực tế
- hệ thống hỗ trợ nhận hàng nhiều đợt
- hàng quarantine vẫn vào kho vật lý nhưng không được xem là available stock

### Case thực tế

**Case A - Nhập đủ 1 lần**

- PO đặt 100 sản phẩm
- nhà cung cấp giao đủ 100
- confirm receipt
- inventory tăng 100

**Case B - Nhập 2 đợt**

- PO đặt 500 sản phẩm
- đợt 1 nhận 300
- đợt 2 nhận 200
- PO đi từ `CONFIRMED` sang `PARTIALLY_RECEIVED`, rồi `COMPLETED`

**Case C - Hàng lỗi khi nhận**

- nhận 50 sản phẩm
- 10 sản phẩm bị lỗi
- line lỗi được ghi quarantine
- tồn vật lý tăng nhưng phần quarantine không được bán

### Giá trị kinh doanh

- kiểm soát rõ giữa kế hoạch mua và hàng thực nhận
- hỗ trợ partial receipt
- có truy vết batch, location, movement

---

## 4. Flow 2 - Outbound

### Mục tiêu

Quản lý từ lúc khách đặt hàng đến khi hàng thực sự được giao ra khỏi kho.

### Chứng từ chính

- `Sales Order`
- `Sales Order Lines`
- `Outbound Shipment`
- `Outbound Shipment Lines`
- `InventoryReservation`

### Logic nghiệp vụ mục tiêu

```text
Tạo Sales Order
-> Confirm Sales Order
-> Reserve hàng
-> Tạo Shipment
-> Picking / Packing
-> Confirm Shipment
-> Inventory giảm
-> SO cập nhật tiến độ giao
```

### Điểm quan trọng

- `Confirm Sales Order` nên là bước giữ hàng
- `Confirm Shipment` mới là bước giảm tồn kho vật lý
- hệ thống đã có nền tảng `checkAvailability`, `reserve`, `unreserve`
- service outbound chính vẫn đang được hoàn thiện

### Case thực tế

**Case A - Giao đủ 1 lần**

- khách đặt 20 sản phẩm
- hệ thống reserve 20
- shipment giao đủ 20
- inventory giảm 20

**Case B - Giao theo 2 chuyến**

- khách đặt 100 kiện
- shipment 1 giao 60
- shipment 2 giao 40
- SO đi từ `CONFIRMED` sang `PARTIALLY_SHIPPED`, rồi `COMPLETED`

**Case C - Khách hủy trước khi giao**

- SO đã confirm và đã reserve
- chưa ship thực tế
- hủy SO
- hệ thống unreserve hàng

### Giá trị kinh doanh

- tránh oversell
- tách rõ bước giữ hàng và bước giao hàng
- hỗ trợ giao nhiều đợt

---

## 5. Flow 3 - Stock Adjustment

### Mục tiêu

Điều chỉnh tồn kho khi hệ thống và thực tế không còn khớp nhau.

### Chứng từ chính

- `Stock Adjustment`
- `Inventory`
- `Stock Movements`

### Logic nghiệp vụ ngắn gọn

```text
Tạo adjustment
-> Hệ thống tính chênh lệch
-> Auto-approve hoặc chờ duyệt
-> Apply inventory
-> Ghi movement adjustment
```

### Điểm quan trọng

- không cho quantity sau điều chỉnh bị âm
- không cho thấp hơn phần hàng đang reserve
- adjustment nhạy cảm hoặc chênh lệch lớn có thể phải duyệt
- khi approve, hệ thống re-check lại inventory để tránh duyệt trên dữ liệu cũ

### Case thực tế

**Case A - Kiểm kê lệch**

- hệ thống ghi 100
- thực tế còn 97
- tạo adjustment về 97

**Case B - Hàng hỏng**

- phát hiện 20 đơn vị hỏng
- tạo adjustment giảm tồn
- có audit trail ghi reason

**Case C - Sai số do lỗi hệ thống**

- tích hợp gây lệch số lượng
- adjustment phải chờ duyệt
- admin xem xét rồi approve

### Giá trị kinh doanh

- hỗ trợ kiểm kê và reconciliation
- tăng minh bạch nội bộ
- kiểm soát chặt các thay đổi nhạy cảm

---

## 6. Một Câu Tóm Tắt Cho Khách Hàng

> Hệ thống đang quản lý đầy đủ phần nhập kho và điều chỉnh kho, đồng thời đã chuẩn bị xong nền tảng nghiệp vụ cho xuất kho. Khi hoàn thiện outbound, hệ thống sẽ khép kín toàn bộ vòng đời hàng hóa từ lúc mua vào, lưu trữ, giữ hàng, giao hàng, đến điều chỉnh sai lệch và truy vết lịch sử biến động.

