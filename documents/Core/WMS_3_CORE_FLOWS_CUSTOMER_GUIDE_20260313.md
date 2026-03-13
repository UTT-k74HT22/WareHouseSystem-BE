# WMS 3 Core Business Flows
## Customer Guide

**Date:** 2026-03-13  
**System:** Warehouse Management System Backend  
**Audience:** Customer, Project Sponsor, Business Team, Delivery Team  
**Purpose:** Giải thích rõ 3 luồng nghiệp vụ cốt lõi của hệ thống kho theo cách dễ đọc, dễ trình bày, và bám sát trạng thái triển khai thực tế của dự án.

---

## 1. Executive Summary

Hệ thống hiện tại được xây dựng xoay quanh 3 flow nghiệp vụ cốt lõi:

1. **Inbound** - Nhập hàng vào kho từ nhà cung cấp
2. **Outbound** - Xuất hàng ra khỏi kho cho khách hàng
3. **Stock Adjustment** - Điều chỉnh tồn kho khi số liệu hệ thống và thực tế chênh lệch

Ba flow này tạo thành vòng đời chính của hàng hóa trong kho:

```text
Nhà cung cấp
    -> Purchase Order
    -> Inbound Receipt
    -> Inventory trong kho
    -> Sales Order
    -> Outbound Shipment
    -> Khách hàng

Khi số liệu lệch thực tế
    -> Stock Adjustment
    -> Inventory được điều chỉnh lại
```

### Tình trạng triển khai hiện tại

| Flow | Vai trò | Tình trạng |
|------|---------|------------|
| Inbound | Đưa hàng vào kho và tăng tồn | **Đã triển khai** |
| Outbound | Giữ hàng, xuất hàng và giảm tồn | **Đã phân tích và chuẩn bị cấu trúc, chưa hoàn thiện service nghiệp vụ chính** |
| Stock Adjustment | Điều chỉnh tồn kho và ghi nhận audit | **Đã triển khai** |

### Ghi chú về phạm vi

Ngoài 3 flow trên, hệ thống còn có **Stock Transfer** để chuyển hàng giữa các location trong cùng warehouse. Đây là luồng hỗ trợ nội bộ, không phải 1 trong 3 flow headline chính của báo cáo này.

---

## 2. Bức Tranh Tổng Quan Về Dữ Liệu

Để khách hàng dễ hiểu, có thể nhìn hệ thống qua 3 lớp dữ liệu:

### 2.1. Lớp kế hoạch

Đây là lớp ghi nhận doanh nghiệp **dự định làm gì**.

- `PurchaseOrders` và `PurchaseOrderLines` ghi nhận kế hoạch mua hàng
- `SalesOrders` và `SalesOrderLines` ghi nhận kế hoạch bán hàng

### 2.2. Lớp thực thi

Đây là lớp ghi nhận doanh nghiệp **đã xử lý hàng thực tế như thế nào**.

- `InboundReceipts` và `InboundReceiptLines` ghi nhận từng lần nhận hàng thực tế
- `OutboundShipments` và `OutboundShipmentLines` ghi nhận từng lần xuất hàng thực tế

### 2.3. Lớp tồn kho và truy vết

Đây là lớp trả lời câu hỏi **hàng hiện đang ở đâu và đã biến động ra sao**.

- `Inventory` là snapshot tồn kho hiện tại
- `InventoryReservation` là phần hàng đã được giữ cho luồng xuất
- `StockMovements` là lịch sử biến động tồn kho
- `StockAdjustments` là chứng từ điều chỉnh sai lệch tồn kho

---

## 3. Flow 1 - Inbound: Nhập Hàng Vào Kho

### 3.1. Mục tiêu nghiệp vụ

Flow Inbound dùng để quản lý toàn bộ quá trình:

1. Lập đơn mua hàng với nhà cung cấp
2. Ghi nhận từng lần giao hàng thực tế
3. Tăng tồn kho đúng theo hàng đã nhận
4. Theo dõi partial receipt, batch, location và chất lượng hàng
5. Ghi lại lịch sử biến động để kiểm soát và truy vết

### 3.2. Tình trạng triển khai

**Đã triển khai trong hệ thống hiện tại.**

Các phần đã hoạt động:

- Purchase Order CRUD
- Purchase Order confirmation
- Purchase Order Lines CRUD
- Inbound Receipt CRUD
- Inbound Receipt confirmation
- Inventory increase khi confirm receipt
- Batch/quarantine handling
- Stock movement recording cho inbound

### 3.3. Các chứng từ chính

| Chứng từ / Dữ liệu | Vai trò nghiệp vụ |
|--------------------|-------------------|
| `Purchase Order (PO)` | Đơn mua hàng tổng |
| `Purchase Order Line (POL)` | Từng dòng hàng trong đơn mua |
| `Inbound Receipt (IR)` | Một lần nhận hàng thực tế |
| `Inbound Receipt Line (IRL)` | Từng dòng hàng thực tế đã nhận |
| `Inventory` | Số lượng đang có trong kho |
| `Stock Movement` | Nhật ký biến động tăng kho |

### 3.4. Trạng thái chính

#### Purchase Order

```text
DRAFT
  -> CONFIRMED
  -> PARTIALLY_RECEIVED
  -> COMPLETED
  -> CANCELLED
```

Ý nghĩa:

- `DRAFT`: đơn mới tạo, còn sửa được
- `CONFIRMED`: đơn đã chốt để bắt đầu nhận hàng
- `PARTIALLY_RECEIVED`: đã nhận một phần
- `COMPLETED`: đã nhận đủ
- `CANCELLED`: hủy đơn

#### Inbound Receipt

```text
DRAFT
  -> CONFIRMED
  -> CANCELLED
```

Ý nghĩa:

- `DRAFT`: phiếu nhận hàng đang chuẩn bị
- `CONFIRMED`: hàng đã vào kho, inventory đã tăng
- `CANCELLED`: phiếu bị hủy

### 3.5. Luồng xử lý chi tiết

#### Bước 1 - Tạo Purchase Order

Người dùng tạo PO với các thông tin chính:

- nhà cung cấp
- kho đích
- ngày đặt hàng
- ngày giao dự kiến
- điều khoản thanh toán
- ghi chú

Ở bước này, hệ thống mới chỉ ghi nhận **kế hoạch mua hàng**.  
**Chưa có bất kỳ thay đổi nào đối với tồn kho.**

#### Bước 2 - Tạo Purchase Order Lines

Người dùng thêm từng line sản phẩm vào PO:

- sản phẩm
- số lượng đặt
- đơn giá
- thành tiền

Hệ thống kiểm soát:

- chỉ PO ở trạng thái `DRAFT` mới được thêm/sửa/xóa line
- một sản phẩm không được lặp lại nhiều lần trong cùng một PO
- số lượng đặt phải lớn hơn 0
- đơn giá không được âm

#### Bước 3 - Confirm Purchase Order

Khi người dùng confirm PO:

- hệ thống chốt trạng thái PO thành `CONFIRMED`
- hệ thống tính lại `subTotal`, `taxAmount`, `totalAmount`
- PO không còn là chứng từ nháp

Điểm rất quan trọng cần trình bày rõ với khách:

> **Confirm PO không làm tăng tồn kho.**

PO chỉ là chứng từ kế hoạch mua hàng đã được phê duyệt nội bộ.  
Tồn kho chỉ tăng khi hàng thực tế được nhận và `Inbound Receipt` được confirm.

#### Bước 4 - Tạo Inbound Receipt

Bộ phận receiving tạo một `Inbound Receipt` từ PO đã confirm.

Receipt phải bám theo:

- đúng `purchaseOrderId`
- đúng `warehouseId` của PO

Hệ thống cho phép tạo receipt từ PO ở trạng thái:

- `CONFIRMED`
- `PARTIALLY_RECEIVED`

Điều này giúp hỗ trợ **nhận hàng nhiều đợt**.

#### Bước 5 - Tạo Inbound Receipt Lines

Mỗi `Inbound Receipt Line` là một sự kiện nhận hàng thực tế:

- nhận sản phẩm nào
- nhận bao nhiêu
- put vào location nào
- thuộc batch nào
- chất lượng là `PASS` hay `QUARANTINE`
- có ghi chú hay không

#### Bước 6 - Confirm Inbound Receipt

Đây là bước làm thay đổi tồn kho thực tế.

Khi confirm receipt, hệ thống thực hiện trong một transaction:

1. khóa receipt và purchase order để tránh race condition
2. kiểm tra receipt còn ở trạng thái `DRAFT`
3. kiểm tra receipt có line
4. kiểm tra từng receipt line map đúng vào đúng purchase order line
5. chặn nhận vượt phần số lượng còn lại của PO line
6. kiểm tra product còn active
7. kiểm tra location thuộc đúng warehouse
8. xử lý batch nếu sản phẩm yêu cầu batch tracking
9. tăng inventory theo đúng dimension `(product, warehouse, location, batch)`
10. ghi stock movement loại `INBOUND`
11. cộng dồn `quantityReceived` trên purchase order line
12. tính lại trạng thái PO là `PARTIALLY_RECEIVED` hoặc `COMPLETED`
13. đổi receipt sang trạng thái `CONFIRMED`

### 3.6. Tác động dữ liệu sau khi confirm receipt

| Dữ liệu | Tác động |
|--------|----------|
| `Inventory.onHandQuantity` | Tăng lên |
| `Inventory.quarantineQuantity` | Tăng nếu line là `QUARANTINE` |
| `PurchaseOrderLine.quantityReceived` | Cộng dồn theo từng đợt nhận |
| `PurchaseOrder.status` | Tự động chuyển `PARTIALLY_RECEIVED` hoặc `COMPLETED` |
| `InboundReceipt.status` | Chuyển sang `CONFIRMED` |
| `StockMovements` | Sinh bản ghi `INBOUND` để audit |

### 3.7. Quy tắc nghiệp vụ quan trọng

| Quy tắc | Ý nghĩa kinh doanh |
|--------|---------------------|
| Chỉ PO draft mới được sửa | Chống thay đổi chứng từ sau khi đã chốt |
| Chỉ receipt draft mới được confirm | Chống confirm lặp |
| Không được nhận vượt số lượng còn lại | Bảo vệ tính đúng của đơn mua |
| Location phải cùng warehouse với receipt | Bảo vệ tính nhất quán dữ liệu kho |
| Product batch-tracked phải có batch | Đảm bảo truy xuất nguồn gốc |
| Line quarantine phải có ghi chú | Phục vụ kiểm soát chất lượng |
| Available = onHand - quarantine - reserved | Hàng cách ly và hàng đã giữ không được xuất bán |

### 3.8. Các case thực tế

#### Case 1 - Nhập đủ trong 1 lần giao

**Bối cảnh:** Công ty đặt mua 100 chiếc adapter từ nhà cung cấp A.

Luồng thực tế:

1. Purchasing tạo PO 100 chiếc
2. PO được confirm
3. Nhà cung cấp giao đủ 100 chiếc trong 1 lần
4. Warehouse tạo 1 receipt, nhập 100 chiếc vào location A-01
5. Receipt được confirm

Kết quả:

- Inventory tăng 100
- PO line nhận đủ 100
- PO chuyển `COMPLETED`
- Có 1 stock movement loại `INBOUND`

#### Case 2 - Nhập một phần qua nhiều đợt

**Bối cảnh:** Công ty đặt mua 500 thùng nguyên liệu nhưng nhà cung cấp giao làm 2 đợt.

Luồng thực tế:

1. Tạo PO 500 thùng
2. Confirm PO
3. Đợt 1 giao 300 thùng, hệ thống tạo Receipt 1 và confirm
4. Đợt 2 giao 200 thùng, hệ thống tạo Receipt 2 và confirm

Kết quả sau đợt 1:

- Inventory tăng 300
- PO line đã nhận 300 / 500
- PO chuyển `PARTIALLY_RECEIVED`

Kết quả sau đợt 2:

- Inventory tăng thêm 200
- PO line đã nhận đủ 500 / 500
- PO chuyển `COMPLETED`

#### Case 3 - Hàng nhập bị lỗi, cần quarantine

**Bối cảnh:** Nhận 50 hộp thuốc, trong đó 10 hộp có dấu hiệu móp méo bao bì.

Luồng thực tế:

1. Receipt line bình thường ghi nhận phần đạt chất lượng
2. Receipt line lỗi được gắn `qualityStatus = QUARANTINE`
3. Nếu sản phẩm có batch tracking, batch có thể được chuyển về trạng thái `QUARANTINE`
4. Receipt được confirm

Kết quả:

- `onHand` vẫn tăng vì hàng đã vào kho vật lý
- `quarantineQuantity` tăng tương ứng với phần hàng lỗi
- phần hàng lỗi không được tính là available để xuất bán

### 3.9. Giá trị mang lại cho khách hàng

- Kiểm soát rõ ràng giữa **đơn mua kế hoạch** và **hàng đã nhận thực tế**
- Hỗ trợ nhận hàng nhiều đợt
- Quản lý batch và quality status
- Tăng tồn kho có kiểm soát
- Có đầy đủ audit trail để kiểm kê và truy vết

---

## 4. Flow 2 - Outbound: Xuất Hàng Ra Khỏi Kho

### 4.1. Mục tiêu nghiệp vụ

Flow Outbound dùng để quản lý:

1. tiếp nhận nhu cầu bán hàng
2. giữ hàng cho đơn đã xác nhận
3. chuẩn bị xuất hàng thực tế
4. giảm tồn kho khi hàng thực sự rời kho
5. ghi nhận lịch sử xuất hàng để truy vết

### 4.2. Tình trạng triển khai

**Flow này hiện đang ở giai đoạn phân tích và chuẩn bị cấu trúc, chưa hoàn thiện service nghiệp vụ chính.**

Những gì đã có:

- migration bảng outbound
- entity cho sales orders và outbound shipments
- enum trạng thái
- nền tảng inventory để `checkAvailability`, `reserve`, `unreserve`
- thiết kế nghiệp vụ đã được mô tả trong tài liệu BA

Những gì chưa hoàn thiện:

- service nghiệp vụ cho sales order
- service nghiệp vụ cho shipment
- endpoint CRUD/confirm/cancel chính thức cho outbound
- transaction giảm tồn kho khi shipment được confirm

### 4.3. Các chứng từ chính

| Chứng từ / Dữ liệu | Vai trò nghiệp vụ |
|--------------------|-------------------|
| `Sales Order (SO)` | Đơn bán hàng tổng |
| `Sales Order Line (SOL)` | Từng dòng sản phẩm khách đặt mua |
| `Outbound Shipment (OS)` | Một lệnh xuất / đợt giao hàng thực tế |
| `Outbound Shipment Line (OSL)` | Từng dòng hàng thực tế được xuất |
| `InventoryReservation` | Phần tồn kho đã được giữ cho đơn bán |
| `Stock Movement` | Nhật ký biến động xuất kho |

### 4.4. Trạng thái mục tiêu

#### Sales Order

```text
DRAFT
  -> CONFIRMED
  -> PARTIALLY_SHIPPED
  -> COMPLETED
  -> CANCELLED
```

#### Outbound Shipment

```text
DRAFT
  -> PICKING
  -> PACKED
  -> SHIPPED
  -> CANCELLED
```

### 4.5. Luồng nghiệp vụ đề xuất cho hệ thống

#### Bước 1 - Tạo Sales Order

Bộ phận sales tạo SO với các thông tin:

- khách hàng
- warehouse xuất hàng
- ngày giao dự kiến
- các dòng sản phẩm và số lượng

Ở bước này:

- hệ thống ghi nhận nhu cầu bán
- chưa giữ hàng
- chưa giảm tồn kho

#### Bước 2 - Confirm Sales Order

Khi SO được confirm, hệ thống nên:

1. kiểm tra đủ tồn khả dụng cho tất cả các line
2. nếu thiếu 1 line thì fail toàn bộ transaction
3. nếu đủ thì reserve hàng cho tất cả các line
4. chuyển SO sang `CONFIRMED`

Điểm quan trọng:

> `Confirm Sales Order` nên là bước **giữ hàng**, chưa phải bước **giảm tồn kho vật lý**.

Điều này giúp hệ thống tránh tình trạng một đơn hàng đã chốt nhưng hàng bị đơn khác lấy mất.

#### Bước 3 - Tạo Outbound Shipment

Từ một SO đã confirm, bộ phận kho tạo `Outbound Shipment`.

Shipment là chứng từ thực thi thực tế:

- giao hàng đợt nào
- lấy hàng từ location nào
- lấy batch nào
- ship số lượng bao nhiêu

Một SO có thể sinh ra nhiều shipment nếu cần giao nhiều đợt.

#### Bước 4 - Picking và Packing

Trong vận hành thực tế, bộ phận kho có thể đi qua các bước:

- `PICKING`: đang lấy hàng từ vị trí lưu trữ
- `PACKED`: đã đóng gói xong

Đây là giai đoạn xử lý nội bộ trước khi hàng rời kho.

#### Bước 5 - Confirm Shipment

Đây là thời điểm nên làm thay đổi tồn kho vật lý.

Khi shipment được confirm là `SHIPPED`, hệ thống nên:

1. trừ `onHandQuantity`
2. đồng thời trừ `reservedQuantity`
3. cập nhật `quantityShipped` trên sales order line
4. chuyển trạng thái SO thành `PARTIALLY_SHIPPED` hoặc `COMPLETED`
5. ghi `StockMovement` loại `OUTBOUND`

### 4.6. Tác động dữ liệu mục tiêu của outbound

| Dữ liệu | Tác động mong muốn |
|--------|--------------------|
| `Inventory.reservedQuantity` | Tăng khi confirm SO |
| `Inventory.onHandQuantity` | Giảm khi confirm Shipment |
| `Inventory.reservedQuantity` | Giảm tương ứng khi shipment được ship hoặc khi đơn bị hủy |
| `SalesOrderLine.quantityShipped` | Cộng dồn theo từng shipment |
| `SalesOrder.status` | Tự động chuyển `PARTIALLY_SHIPPED` hoặc `COMPLETED` |
| `StockMovements` | Sinh `RESERVE`, `UNRESERVE`, `OUTBOUND` |

### 4.7. Quy tắc nghiệp vụ nên trình bày với khách

| Quy tắc | Ý nghĩa kinh doanh |
|--------|---------------------|
| Confirm SO phải kiểm tra đủ hàng | Tránh nhận đơn nhưng không có khả năng giao |
| Reservation là all-or-nothing | Tránh giữ hàng dở dang cho đơn |
| Shipment mới là sự kiện xuất kho vật lý | Tách rõ bước giữ hàng và bước giao hàng |
| Có thể giao nhiều đợt | Hỗ trợ split shipment |
| Hủy đơn phải unreserve | Trả hàng lại available stock |

### 4.8. Các case thực tế

#### Case 1 - Xuất đủ trong một lần giao

**Bối cảnh:** Khách hàng B đặt 20 máy quét mã vạch và công ty có đủ hàng.

Luồng mục tiêu:

1. Sales tạo SO 20 máy
2. SO được confirm, hệ thống reserve 20 máy
3. Kho tạo shipment và pick đủ 20 máy
4. Shipment được confirm là `SHIPPED`

Kết quả mong muốn:

- `reservedQuantity` tăng ở lúc confirm SO
- `onHandQuantity` giảm ở lúc ship
- SO chuyển `COMPLETED`
- có movement `RESERVE` và `OUTBOUND`

#### Case 2 - Giao hàng thành 2 đợt

**Bối cảnh:** Khách đặt 100 kiện, nhưng theo lịch giao hàng sẽ gửi 60 kiện trước và 40 kiện sau.

Luồng mục tiêu:

1. SO 100 kiện được confirm
2. Hệ thống reserve đủ 100 kiện
3. Shipment 1 giao 60 kiện
4. Shipment 2 giao 40 kiện

Kết quả mong muốn:

- sau shipment 1, SO là `PARTIALLY_SHIPPED`
- sau shipment 2, SO là `COMPLETED`
- hệ thống vẫn kiểm soát toàn bộ hàng đã được giữ cho đơn ngay từ đầu

#### Case 3 - Hủy đơn trước khi giao

**Bối cảnh:** Khách đã đặt hàng, đơn đã confirm, nhưng hủy trước khi kho xuất.

Luồng mục tiêu:

1. SO được confirm và đã reserve hàng
2. Chưa có shipment active hoặc chưa ship
3. Người dùng hủy SO

Kết quả mong muốn:

- hệ thống unreserve phần hàng đã giữ
- available stock được trả lại
- SO chuyển `CANCELLED`

### 4.9. Giá trị mang lại cho khách hàng

- kiểm soát được việc giữ hàng cho đơn đã chốt
- phân biệt rõ giữa đặt hàng, giữ hàng và giao hàng
- hỗ trợ giao nhiều đợt
- giảm rủi ro oversell
- truy vết được lịch sử giao hàng theo shipment và batch

---

## 5. Flow 3 - Stock Adjustment: Điều Chỉnh Tồn Kho

### 5.1. Mục tiêu nghiệp vụ

Flow Stock Adjustment dùng để xử lý các tình huống mà số liệu hệ thống không còn khớp với thực tế vận hành:

- kiểm kê thấy lệch
- hàng hỏng hoặc mất mát
- sai sót thao tác trước đó
- lỗi tích hợp hoặc lỗi hệ thống

Mục tiêu của flow là:

1. điều chỉnh snapshot tồn kho về đúng thực tế
2. kiểm soát quyền hạn điều chỉnh
3. lưu dấu vết đầy đủ để audit sau này

### 5.2. Tình trạng triển khai

**Đã triển khai trong hệ thống hiện tại.**

Các phần đã có:

- tạo adjustment request
- auto-approve hoặc pending approval theo rule
- approve / reject
- cập nhật inventory khi được phép
- ghi stock movement adjustment

### 5.3. Các chứng từ chính

| Chứng từ / Dữ liệu | Vai trò nghiệp vụ |
|--------------------|-------------------|
| `Stock Adjustment` | Yêu cầu điều chỉnh tồn kho |
| `Inventory` | Dòng tồn kho bị điều chỉnh |
| `Stock Movement` | Nhật ký tăng/giảm do điều chỉnh |

### 5.4. Trạng thái chính

```text
PENDING_APPROVAL
  -> APPROVED
  -> REJECTED
```

Trong một số trường hợp, adjustment có thể được auto-approve ngay khi tạo, thay vì đi qua bước chờ duyệt.

### 5.5. Luồng xử lý chi tiết

#### Bước 1 - Tạo yêu cầu điều chỉnh

Người dùng chọn dòng inventory cần điều chỉnh và nhập:

- `inventoryId`
- `quantityAfter`
- `reason`
- `notes`

Hệ thống tự lấy:

- `quantityBefore` từ inventory hiện tại
- `adjustmentQuantity = quantityAfter - quantityBefore`

#### Bước 2 - Kiểm tra hợp lệ

Hệ thống chặn các trường hợp:

- `quantityAfter` âm
- `quantityAfter` nhỏ hơn phần `reservedQuantity`
- chênh lệch bằng 0

Điều này bảo vệ tính đúng của tồn kho và tránh adjustment vô nghĩa.

#### Bước 3 - Xác định có cần phê duyệt hay không

Hệ thống dùng role, reason và độ lớn chênh lệch để quyết định:

- auto-approve
- hoặc chuyển `PENDING_APPROVAL`

Ý nghĩa kinh doanh:

- adjustment nhỏ, ít nhạy cảm có thể xử lý nhanh
- adjustment nhạy cảm hoặc lớn phải có kiểm soát phê duyệt

#### Bước 4 - Auto-apply hoặc chờ duyệt

Nếu adjustment không cần duyệt:

- inventory được cập nhật ngay
- movement được ghi ngay
- adjustment ở trạng thái `APPROVED`

Nếu cần duyệt:

- inventory chưa thay đổi
- adjustment được lưu ở `PENDING_APPROVAL`

#### Bước 5 - Approve adjustment

Khi người có quyền phê duyệt approve:

1. hệ thống khóa adjustment và inventory
2. kiểm tra adjustment còn ở trạng thái `PENDING_APPROVAL`
3. kiểm tra inventory chưa bị thay đổi kể từ lúc adjustment được tạo
4. nếu dữ liệu vẫn hợp lệ, cập nhật inventory
5. ghi stock movement adjustment
6. chuyển adjustment sang `APPROVED`

Đây là điểm kiểm soát rất quan trọng vì tránh việc duyệt dựa trên snapshot đã cũ.

#### Bước 6 - Reject adjustment

Nếu adjustment không hợp lệ hoặc không được chấp thuận:

- adjustment chuyển `REJECTED`
- lưu lý do từ chối
- inventory không bị thay đổi

### 5.6. Tác động dữ liệu

| Dữ liệu | Tác động |
|--------|----------|
| `Inventory.onHandQuantity` | Được set về `quantityAfter` khi adjustment được áp dụng |
| `StockAdjustment.status` | Chuyển `APPROVED` hoặc `REJECTED` |
| `StockMovements` | Sinh `ADJUSTMENT_INCREASE` hoặc `ADJUSTMENT_DECREASE` |

### 5.7. Quy tắc nghiệp vụ quan trọng

| Quy tắc | Ý nghĩa kinh doanh |
|--------|---------------------|
| Không cho quantityAfter âm | Không cho tồn kho âm |
| Không cho quantityAfter thấp hơn reserved | Không phá vỡ đơn hàng đang giữ hàng |
| Không cho adjustment bằng 0 | Tránh chứng từ vô nghĩa |
| Adjustment nhạy cảm cần duyệt | Tăng kiểm soát nội bộ |
| Approve phải re-check inventory snapshot | Tránh điều chỉnh trên dữ liệu cũ |

### 5.8. Các case thực tế

#### Case 1 - Kiểm kê thấy thiếu hàng

**Bối cảnh:** Hệ thống đang ghi nhận 100 thùng, nhưng kiểm kê thực tế chỉ còn 97 thùng.

Luồng thực tế:

1. Tạo adjustment với `quantityAfter = 97`
2. Hệ thống tính chênh lệch `-3`
3. Tùy role và reason, adjustment được auto-approve hoặc chờ duyệt

Kết quả:

- inventory được điều chỉnh còn 97
- có movement `ADJUSTMENT_DECREASE`

#### Case 2 - Phát hiện hàng hỏng sau kiểm tra chất lượng

**Bối cảnh:** Có 20 chai hóa chất bị hỏng, không thể bán tiếp.

Luồng thực tế:

1. Chọn inventory hiện tại
2. Tạo adjustment giảm số lượng
3. Ghi reason là `DAMAGE`
4. Sau khi được phê duyệt, hệ thống giảm on-hand

Kết quả:

- tồn kho hệ thống phản ánh đúng số hàng còn dùng được
- có chứng từ audit giải thích nguyên nhân

#### Case 3 - Lỗi hệ thống hoặc nghi ngờ thất thoát

**Bối cảnh:** Sau một sự cố tích hợp, số lượng trên hệ thống tăng sai 50 đơn vị.

Luồng thực tế:

1. Người dùng tạo adjustment để đưa tồn kho về đúng số thực tế
2. Vì đây là lý do nhạy cảm hoặc chênh lệch lớn, adjustment có thể phải chờ duyệt
3. Người có quyền xem xét và approve hoặc reject

Kết quả:

- có lớp kiểm soát phê duyệt
- tránh việc người dùng tự ý sửa số lượng lớn mà không có giám sát

### 5.9. Giá trị mang lại cho khách hàng

- kiểm soát tốt chênh lệch tồn kho
- hỗ trợ kiểm kê định kỳ và reconciliation
- có lịch sử điều chỉnh minh bạch
- tăng mức độ an toàn khi xử lý các thay đổi nhạy cảm

---

## 6. So Sánh 3 Flow Chính

| Tiêu chí | Inbound | Outbound | Stock Adjustment |
|---------|---------|----------|------------------|
| Mục tiêu | Đưa hàng vào kho | Đưa hàng ra khỏi kho | Sửa sai lệch tồn kho |
| Chứng từ kế hoạch | PO | SO | Không có lớp kế hoạch riêng |
| Chứng từ thực thi | Inbound Receipt | Outbound Shipment | Stock Adjustment |
| Khi nào đổi tồn vật lý | Khi confirm receipt | Khi confirm shipment | Khi apply/approve adjustment |
| Có reservation không | Không | Có | Không |
| Có movement audit không | Có | Có theo thiết kế | Có |
| Tình trạng hiện tại | Đã triển khai | Đang hoàn thiện | Đã triển khai |

---

## 7. Thông Điệp Nên Trình Bày Với Khách Hàng

Nếu cần nói ngắn gọn, dễ hiểu, có thể dùng thông điệp sau:

> Hệ thống quản lý kho của dự án được xây dựng theo 3 flow cốt lõi: nhập hàng, xuất hàng và điều chỉnh tồn kho.
>
> Flow nhập hàng đã hỗ trợ đầy đủ từ đơn mua đến phiếu nhận và cập nhật tồn kho thực tế.  
> Flow điều chỉnh tồn kho đã hỗ trợ cả kiểm soát phê duyệt và audit trail.  
> Flow xuất hàng đã có đầy đủ thiết kế nghiệp vụ và nền tảng reservation trong inventory, và đang được hoàn thiện để khép kín vòng đời hàng hóa từ nhà cung cấp đến khách hàng.

---

## 8. Kết Luận

Ba flow này phản ánh đúng cách một kho vận hành trong thực tế:

- **Inbound** để hàng vào kho có kiểm soát
- **Outbound** để giữ hàng và giao hàng cho khách
- **Stock Adjustment** để sửa lệch giữa hệ thống và thực tế

Với tình trạng hiện tại của dự án:

- phần nhập kho và điều chỉnh kho đã đủ cơ sở để demo nghiệp vụ thật
- phần xuất kho đã có kiến trúc đúng hướng và nên là ưu tiên tiếp theo để hoàn thiện trọn vòng đời hàng hóa

