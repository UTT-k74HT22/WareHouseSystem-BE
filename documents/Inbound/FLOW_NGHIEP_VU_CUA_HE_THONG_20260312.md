# Flow nghiệp vụ của hệ thống
## Date: 2026-03-12
## Scope: Main inbound business flow and core entity relationships

## 1. Mục tiêu tài liệu

Tài liệu này dùng để giải thích bức tranh dữ liệu chính của module inbound trong dự án hiện tại.

Mục tiêu là giúp đọc entity theo đúng vai trò nghiệp vụ, không nhầm giữa:

- dữ liệu kế hoạch mua hàng
- dữ liệu nhận hàng thực tế
- dữ liệu tồn kho sau khi confirm
- dữ liệu truy vết batch và movement

Trong codebase hiện tại không có entity riêng tên `IO` hoặc `IOB`.
Nếu team đang gọi tắt theo nghiệp vụ, cặp gần nhất tương ứng trong code là:

- `IR` = `InboundReceipts`
- `IRL` = `InboundReceiptLines`

## 2. Quy ước viết tắt

| Viết tắt | Entity | Ý nghĩa |
|----------|--------|---------|
| `PO` | `PurchaseOrders` | Header của đơn mua hàng |
| `POL` | `PurchaseOrderLines` | Dòng hàng trong đơn mua |
| `IR` | `InboundReceipts` | Header của phiếu nhận hàng |
| `IRL` | `InboundReceiptLines` | Dòng nhận hàng thực tế |
| `Batch` | `Batch` | Lô hàng để truy xuất nguồn gốc |
| `Inventory` | `Inventory` | Tồn kho snapshot hiện tại |
| `Movement` | `StockMovements` | Nhật ký biến động kho |
| `Reservation` | `InventoryReservation` | Phần tồn kho đã bị giữ cho luồng khác |

## 3. Ba lớp dữ liệu cốt lõi

### 3.1. Lớp kế hoạch mua hàng

Lớp này trả lời câu hỏi: doanh nghiệp dự định mua gì, của ai, cho kho nào, giá bao nhiêu.

- `PO` là chứng từ header
- `POL` là danh sách sản phẩm và số lượng dự kiến phải nhận

### 3.2. Lớp thực thi nhận hàng

Lớp này trả lời câu hỏi: nhà cung cấp thực tế giao gì trong từng lần giao hàng.

- `IR` là header của một lần nhận hàng
- `IRL` là từng dòng hàng thực tế nhận được trong lần đó

### 3.3. Lớp tồn kho và truy vết

Lớp này trả lời câu hỏi: hàng đã vào kho chưa, đang nằm ở đâu, thuộc batch nào, có dùng được không.

- `Batch` giữ danh tính lô hàng
- `Inventory` giữ snapshot tồn kho hiện tại
- `StockMovements` giữ lịch sử biến động
- `InventoryReservation` giữ phần tồn đã được allocate

## 4. Vai trò của từng entity

| Entity | Vai trò nghiệp vụ | Field quan trọng | Quan hệ chính |
|--------|-------------------|------------------|---------------|
| `Products` | Master sản phẩm | `sku`, `name`, `status`, `requiresBatchTracking` | Được tham chiếu từ `POL`, `IRL`, `Batch`, `Inventory` |
| `Warehouses` | Kho tổng đích | `code`, `name`, `status` | `PO` và `IR` gắn với một warehouse |
| `Locations` | Vị trí lưu trữ trong kho | `warehouseId`, `code`, `name`, `type`, `status` | `IRL` chọn location, `Inventory` gắn với location |
| `PurchaseOrders` | Header mua hàng | `purchaseOrderNumber`, `supplierId`, `warehouseId`, `status`, `totalAmount` | Một `PO` có nhiều `POL`, nhiều `IR` |
| `PurchaseOrderLines` | Dòng mua hàng | `purchaseOrderId`, `productId`, `quantityOrdered`, `quantityReceived`, `unitPrice` | Một `POL` có thể được nhận bởi nhiều `IRL` qua nhiều đợt |
| `InboundReceipts` | Header nhận hàng thực tế | `receiptNumber`, `purchaseOrderId`, `warehouseId`, `status`, `confirmedAt`, `confirmedBy` | Một `IR` có nhiều `IRL` |
| `InboundReceiptLines` | Dòng nhận hàng thực tế | `inboundReceiptId`, `purchaseOrderLineId`, `productId`, `locationId`, `batchId`, `quantityReceived`, `qualityStatus` | Mỗi `IRL` map vào đúng một `POL` |
| `Batch` | Lô hàng để truy vết | `batchNumber`, `productId`, `manufacturingDate`, `expiryDate`, `status` | Một `Batch` thuộc một `Product`; nhiều dòng inventory có thể tham chiếu cùng batch nếu khác location |
| `Inventory` | Snapshot tồn kho theo dimension | `productId`, `warehouseId`, `locationId`, `batchId`, `onHandQuantity`, `quarantineQuantity`, `reservedQuantity` | Mỗi dòng là một dimension tồn kho duy nhất |
| `StockMovements` | Ledger biến động kho | `movementType`, `quantityChange`, `quantityBefore`, `quantityAfter`, `referenceType`, `referenceNumber` | Được sinh ra khi confirm receipt hoặc các flow stock khác |
| `InventoryReservation` | Phần tồn đang bị giữ | `inventoryId`, `quantity`, `orderLineId`, `status` | Gắn vào `Inventory` để trừ khỏi available |

## 5. Quan hệ dữ liệu cần nhớ

### 5.1. Header và line

- `1 PO -> nhiều POL`
- `1 IR -> nhiều IRL`

Đây là hai cặp header-line chính của inbound.

### 5.2. Quan hệ giữa PO và Receipt

- `1 PO -> nhiều IR`
- `1 POL -> nhiều IRL` theo thời gian

Điểm này cực kỳ quan trọng vì hệ thống cho phép partial receipt nhiều đợt.
Nghĩa là một line mua hàng có thể được nhận thành 2, 3 hoặc nhiều lần cho tới khi đủ số lượng.

### 5.3. Quan hệ giữa IRL và batch/location

- `IRL.productId` chỉ ra đang nhận sản phẩm nào
- `IRL.locationId` chỉ ra hàng được put vào vị trí nào
- `IRL.batchId` chỉ có ý nghĩa khi product yêu cầu batch tracking

`locationId` luôn phải thuộc cùng `warehouseId` với receipt.

### 5.4. Quan hệ giữa inventory và các dimension

Một dòng `Inventory` là tồn kho duy nhất theo tổ hợp:

```text
(product, warehouse, location, batch)
```

Điều này nghĩa là:

- cùng product nhưng khác warehouse là dòng tồn kho khác
- cùng warehouse nhưng khác location là dòng tồn kho khác
- cùng location nhưng khác batch là dòng tồn kho khác

## 6. Ý nghĩa các field số lượng

### 6.1. Ở Purchase Order Line

- `quantityOrdered` = số lượng đã đặt mua
- `quantityReceived` = số lượng đã nhận lũy kế của line đó qua tất cả các lần confirm receipt

Đây là nơi system theo dõi tiến độ fulfillment của PO.

### 6.2. Ở Inbound Receipt Line

- `quantityReceived` = số lượng của riêng lần nhận hiện tại

Nó là quantity theo event, không phải quantity lũy kế.

### 6.3. Ở Inventory

- `onHandQuantity` = stock vật lý đang có trong kho
- `quarantineQuantity` = phần stock vật lý bị chặn vì quality issue
- `reservedQuantity` = phần stock đã reserve cho luồng downstream

### 6.4. Available quantity

Trong implementation hiện tại:

```text
available = onHand - quarantine - reserved
```

Nghĩa là:

- hàng quarantine vẫn tồn tại vật lý
- nhưng không được xem là available để bán hoặc xuất

## 7. Ý nghĩa status

### 7.1. `PO.status`

- `DRAFT`: mới tạo, còn sửa được
- `CONFIRMED`: đã chốt để nhận hàng
- `PARTIALLY_RECEIVED`: đã nhận một phần
- `COMPLETED`: đã nhận đủ tất cả line
- `CANCELLED`: hủy đơn

### 7.2. `IR.status`

- `DRAFT`: mới tạo, chỉ là chứng từ chuẩn bị nhận hàng
- `CONFIRMED`: đã chốt, inventory đã tăng, movement đã ghi
- `CANCELLED`: hủy receipt

### 7.3. `IRL.qualityStatus`

- `PASS`: hàng đạt
- `QUARANTINE`: hàng có vấn đề chất lượng, cần cách ly

### 7.4. `Batch.status`

- `AVAILABLE`: lô dùng bình thường
- `QUARANTINE`: lô đang bị cách ly
- `EXPIRED`: lô hết hạn
- `RECALLED`: lô bị thu hồi

## 8. Luồng nghiệp vụ từ đầu đến cuối

### Bước 1: Tạo PO

Purchasing tạo `PO` và thêm nhiều `POL`.
Ở giai đoạn này hệ thống mới chỉ ghi nhận kế hoạch mua hàng.
Chưa có stock nào được tăng.

### Bước 2: Confirm PO

Khi `PO` được confirm, nó trở thành chứng từ hợp lệ để bộ phận receiving tạo receipt.
Từ đây `PO` không còn là phiếu nháp nữa.

### Bước 3: Tạo draft receipt

Receiving tạo `IR` từ `PO`.
Receipt phải bám đúng `purchaseOrderId` và `warehouseId` của PO.
Tạo receipt vẫn chưa được phép tăng inventory.

### Bước 4: Nhập receipt lines

Receiving thêm `IRL` cho từng mặt hàng thực tế đã giao:

- nhận bao nhiêu
- cất ở location nào
- batch nào
- quality là `PASS` hay `QUARANTINE`
- có notes hay không

### Bước 5: Confirm receipt

Đây là transaction quan trọng nhất của inbound.
Khi confirm:

- validate toàn bộ `IRL`
- validate line phải map đúng vào `POL`
- reject nếu nhận vượt phần remaining của `POL`
- resolve batch nếu product có batch tracking
- find/create `Inventory` theo dimension `(product, warehouse, location, batch)`
- tăng tồn kho
- ghi `StockMovements`
- cộng dồn `POL.quantityReceived`
- recompute lại `PO.status`
- set `IR.status = CONFIRMED`

## 9. Quarantine flow phải hiểu thế nào

Khi `IRL.qualityStatus = QUARANTINE`:

- hàng vẫn được xem là đã nhận
- `POL.quantityReceived` vẫn tăng
- `Inventory.onHandQuantity` vẫn tăng vì hàng đã vào kho vật lý
- `Inventory.quarantineQuantity` cũng tăng
- `available` phải không được tính phần này
- `Batch.status` phải đi về `QUARANTINE` nếu batch hợp lệ

Tư duy đúng là:

`quarantine` không phải là "không nhập kho", mà là "đã nhập kho nhưng không được sử dụng tự do".

## 10. Ví dụ nghiệp vụ dễ nhớ

Giả sử có một `POL` đặt mua `100` sản phẩm.

### Đợt nhận 1

- tạo `IR-001`
- có `IRL-001`, `quantityReceived = 30`, `qualityStatus = PASS`

Sau confirm:

- `POL.quantityReceived = 30`
- `PO.status = PARTIALLY_RECEIVED`
- `Inventory.onHand += 30`
- `Inventory.quarantine = 0`

### Đợt nhận 2

- tạo `IR-002`
- có `IRL-002`, `quantityReceived = 20`, `qualityStatus = QUARANTINE`

Sau confirm:

- `POL.quantityReceived = 50`
- `PO.status` vẫn là `PARTIALLY_RECEIVED`
- `Inventory.onHand += 20`
- `Inventory.quarantine += 20`
- phần 20 này không được tính vào available

### Đợt nhận 3

- tạo `IR-003`
- có `IRL-003`, `quantityReceived = 50`, `qualityStatus = PASS`

Sau confirm:

- `POL.quantityReceived = 100`
- `PO.status = COMPLETED`

## 11. Những nhầm lẫn hay gặp

### Nhầm 1: `IRL.quantityReceived` và `POL.quantityReceived` là một

Sai.

- `IRL.quantityReceived` là quantity của một lần nhận
- `POL.quantityReceived` là quantity lũy kế của line mua hàng

### Nhầm 2: `Batch` là tồn kho

Sai.

`Batch` chỉ là identity của lô hàng.
Tồn kho thật sự nằm ở `Inventory`.

### Nhầm 3: `onHand` là số dùng được

Sai.

`onHand` là số đang có vật lý.
Số dùng được là `available = onHand - quarantine - reserved`.

### Nhầm 4: Tạo receipt là đã tăng tồn

Sai.

Chỉ `confirm receipt` mới tăng inventory và ghi stock movement.

## 12. Mental model ngắn gọn để nhớ

Nếu phải nhớ một câu duy nhất thì là:

`PO/POL` quản lý thứ doanh nghiệp dự định mua, `IR/IRL` quản lý thứ nhà cung cấp thực tế giao, còn `Batch/Inventory/Movement` quản lý hàng đã vào kho ra sao và hiện ở trạng thái nào.

## 13. Tài liệu liên quan

- `FLOW_NGHIEP_VU_ONBOARDING_TOM_TAT_20260312.md`
- `WHS-55_PURCHASE_ORDER_LINES_BA_DESIGN_20260309.md`
- `WHS-56_INBOUND_RECEIPTS_CRUD_DESIGN_20260309.md`
- `WHS-57_CONFIRM_RECEIPT_DESIGN_20260309.md`
- `BA_MODULE_05_INBOUND_OPERATIONS.md`
