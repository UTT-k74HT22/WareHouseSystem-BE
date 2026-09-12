# Sơ Đồ Thuật Toán - Quản Lý Kho WMS

## 1. Tổng Quan Luồng Quản Lý Kho

```mermaid
flowchart TB
    subgraph WMS["Hệ thống WMS"]
        subgraph NHAPKHO["Nhập kho"]
            NK1["Tạo phiếu nhập"]
            NK2["Xác nhận nhập kho"]
            NK3["Tăng tồn kho"]
            NK4["Cập nhật PO"]
        end

        subgraph TONKHO["Tồn kho"]
            TK1["Kiểm tra tồn"]
            TK2["Đặt hàng"]
            TK3["Reserve"]
            TK4["Tăng/Giảm"]
        end

        subgraph XUATKHO["Xuất kho"]
            TK5["Sales Order"]
            XK1["Tạo shipment"]
            XK2["Start Picking"]
            XK3["Mark as Packed"]
            XK4["Ship & Dispatch"]
            XK5["Giảm tồn kho"]
        end
    end
```

---

## 2. LUỒNG NHẬP KHO (Inbound Receipts)

### 2.1 Tổng quan luồng nhập kho

```mermaid
flowchart TB
    START(["Bắt đầu"]) --> V1["Kiểm tra PO\n(CONFIRMED / PARTIALLY_RECEIVED)"]
    V1 --> V2["Xác thực Warehouse"]
    V2 --> C1["Tạo phiếu nhập DRAFT\nGR-XXXXX"]
    C1 --> A1["Thêm line items"]
    A1 --> A2["Xác nhận nhập kho"]
    A2 --> C2{"Line items\nhợp lệ?"}
    C2 -->|Không| E1["Báo lỗi"]
    C2 -->|Có| V3["Kiểm tra sản phẩm\nACTIVE?"]
    V3 --> V4["Kiểm tra location\nACTIVE?"]
    V4 --> V5{"Quality Status\n= QUARANTINE?"}
    V5 -->|Có| V6["Kiểm tra batch\n(EXP/RECALLED?)"]
    V5 -->|Không| V7{"Batch tracking?"}
    V6 --> E2["Báo lỗi batch"]
    V7 -->|Có| V8["Validate batch"]
    V7 -->|Không| P1["Tăng inventory\n(onHand + qty)"]
    P1 --> P2{"Quarantine?"}
    P2 -->|Có| P3["Tăng quarantine qty"]
    P2 -->|Không| P4["Tăng location capacity"]
    P3 --> P5["Ghi StockMovement\n(INBOUND)"]
    P4 --> P5
    P5 --> P6["Cập nhật PO line\n(quantityReceived)"]
    P6 --> C3{"Tất cả line\nđã nhập?"}
    C3 -->|Không| S1["PO = PARTIALLY_RECEIVED"]
    C3 -->|Có| S2["PO = COMPLETED"]
    S1 --> F1["Cập nhật trạng thái\nphiếu nhập = CONFIRMED"]
    S2 --> F1
    F1 --> END(["Kết thúc"])
```

### 2.2 Chi tiết từng bước

#### Bước 1: Tạo phiếu nhập DRAFT

```mermaid
flowchart LR
    A["Request: purchaseOrderId"] --> V1
    V1["Tìm PurchaseOrder"] --> V2
    V2{"Status = CONFIRMED\nhoặc PARTIALLY_RECEIVED?"}
    V2 -->|Không| E["Throw Error: PO_001"]
    V2 -->|Có| V3
    V3["Lấy Warehouse"] --> V4
    V4["Generate receiptNumber\nGR-XXXXX"] --> C["Tạo InboundReceipts\nstatus = DRAFT"]
    C --> R["Return response"]
```

#### Bước 2: Xác nhận nhập kho (Confirm)

```mermaid
flowchart TB
    START --> V1["Load receipt + lines\nwith LOCK"]
    V1 --> V2{"Status = DRAFT?"}
    V2 -->|Không| E1["Throw: COM_001"]
    V2 -->|Có| V3["Load PO + lines"]
    V3 --> V4{"PO status hợp lệ?"}
    V4 -->|Không| E2["Throw: COM_001"]
    V4 -->|Có| V5["Validate receipt lines\nvs PO lines"]
    V5 --> V6{"Tổng nhập <=\nremaining?"}
    V6 -->|Không| E3["Throw: POL_006"]
    V6 -->|Có| L1["Loop through lines"]
    
    L1 --> V7["Validate product\nACTIVE?"]
    V7 --> V8["Validate location\nACTIVE/MAINTENANCE?"]
    V8 --> V9{"Quality = QUARANTINE\n& no notes?"}
    V9 -->|Có| E4["Throw: COM_001"]
    V9 -->|Không| V10{"Batch tracking?"}
    V10 -->|Có| V11["Validate batch"]
    V10 -->|Không| P1["Tăng inventory"]
    
    P1 --> P2{"Quarantine?"}
    P2 -->|Có| P3["Tăng quarantine qty"]
    P2 -->|Không| P4["Tăng location capacity"]
    P3 --> M1["Ghi StockMovement\nINBOUND"]
    P4 --> M1
    M1 --> P5["Update PO line\nquantityReceived"]
    P5 --> L2{"Next line?"}
    L2 -->|Có| L1
    L2 -->|Không| S1["Recompute PO status"]
    S1 --> U1["Save all updates"]
    U1 --> U2["Update receipt\nstatus = CONFIRMED"]
    U2 --> END
```

### 2.3 Các trạng thái phiếu nhập

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Tạo mới
    DRAFT --> CONFIRMED : Xác nhận
    CONFIRMED --> [*]
    DRAFT --> CANCELLED : Hủy (nếu chưa confirm)
```

---

## 3. LUỒNG XUẤT KHO (Outbound Shipments)

### 3.1 Tổng quan luồng xuất kho

```mermaid
flowchart TB
    START(["Sales Order\nđã CONFIRMED"]) --> C1["Tạo Shipment\nDRAFT"]
    C1 --> A1["Thêm shipment lines\n(linked to SO lines)"]
    A1 --> T1["Start Picking\nDRAFT → PICKING"]
    T1 --> V1["Resolve Picking Location"]
    V1 --> L1["Loop through lines"]
    L1 --> R1["Find Reservation\n(by orderLineId)"]
    R1 --> R2{"Reserved >= Shipped?"}
    R2 -->|Không| E1["Throw: INV_004"]
    R2 -->|Có| M1["Move: Storage → Picking\n(consumeReserved=false)"]
    M1 --> U1["Update line: locationId\n= picking location"]
    U1 --> L2{"Next line?"}
    L2 -->|Có| L1
    L2 -->|Không| S1["Update status = PICKING"]
    S1 --> T2["Mark as Packed\nPICKING → PACKED"]
    T2 --> V2["Resolve Packing Location"]
    V2 --> L3["Loop through lines"]
    L3 --> M2["Move: Picking → Packing"]
    M2 --> U2["Update line: locationId"]
    U2 --> L4{"Next line?"}
    L4 -->|Có| L3
    L4 -->|Không| S2["Update status = PACKED"]
    S2 --> T3["Ship\nPACKED → STAGING"]
    T3 --> V3["Resolve Staging Location"]
    V3 --> L5["Loop through lines"]
    L5 --> M3["Move: Packing → Staging"]
    M3 --> U3["Update line: locationId"]
    U3 --> L6{"Next line?"}
    L6 -->|Có| L5
    L6 -->|Không| S3["Update status = STAGING"]
    S3 --> T4["Confirm Dispatch\nSTAGING → SHIPPED"]
    T4 --> L7["Loop through lines"]
    L7 --> D1["Decrease inventory\n(consumeReserved=true)"]
    D1 --> C2["Delete reservation"]
    C2 --> U4["Update SO line\nquantityShipped"]
    U4 --> L8{"Next line?"}
    L8 -->|Có| L7
    L8 -->|Không| S4["Update status = SHIPPED"]
    S4 --> U5["Update Sales Order\nstatus"]
    U5 --> END(["Kết thúc"])
```

### 3.2 Chi tiết từng bước

#### Start Picking (DRAFT → PICKING)

```mermaid
flowchart TB
    START --> V1["Find shipment\nwith LOCK"]
    V1 --> V2{"Status = DRAFT?"}
    V2 -->|Không| E1["Throw: COM_001"]
    V2 -->|Có| V3["Has lines?"]
    V3 -->|Không| E2["Throw: COM_001"]
    V3 -->|Có| V4["Resolve Picking Location"]
    V4 --> L1["For each line"]
    L1 --> R1["Find reservation\n(by orderLineId)"]
    R1 --> R2{"Found?"}
    R2 -->|Không| E3["Throw: INV_001"]
    R2 -->|Có| C1{"Reserved >= Shipped?"}
    C1 -->|Không| E4["Throw: INV_004"]
    C1 -->|Có| M1["moveInventory:\nStorage → Picking\nconsumeReserved=false"]
    M1 --> U1["Update line:\n- locationId = picking\n- batchId = reservation.batchId\n- pickedAt = now\n- pickedBy = actorId"]
    L1 --> L2{"Next line?"}
    L2 -->|Có| L1
    L2 -->|Không| S1["Update shipment:\nstatus = PICKING"]
    S1 --> END
```

#### Confirm Dispatch (STAGING → SHIPPED)

```mermaid
flowchart TB
    START --> V1["Find shipment\nwith LOCK"]
    V1 --> V2{"Status = STAGING?"}
    V2 -->|Không| E1["Throw: COM_001"]
    V2 -->|Có| V3["Has lines?"]
    V3 -->|Không| E2["Throw: COM_001"]
    V3 -->|Có| L1["For each line"]
    L1 --> R1["Find reservation"]
    R1 --> R2{"Found?"}
    R2 -->|Không| E3["Throw: INV_001"]
    R2 -->|Có| D1["inventoryService.decrease:\n- consumeReserved=true\n- orderLineId"]
    D1 --> D2{"Over shipped?"}
    D2 -->|Có| E4["Throw: COM_001"]
    D2 -->|Không| C1["Delete reservation"]
    C1 --> U1["Update line: locationId = null"]
    L1 --> L2{"Next line?"}
    L2 -->|Có| L1
    L2 -->|Không| U2["Update shipment:\n- status = SHIPPED\n- shippedAt = now"]
    U2 --> U3["Update SO line:\nquantityShipped += shipped"]
    U3 --> U4{"All shipped?"}
    U4 -->|Có| S1["SO = COMPLETED"]
    U4 -->|Không| S2["SO = PARTIALLY_SHIPPED"]
    S1 --> END
    S2 --> END
```

### 3.3 Các trạng thái Shipment

```mermaid
stateDiagram-v2
    [*] --> DRAFT : Tạo mới
    DRAFT --> PICKING : Start Picking
    PICKING --> PACKED : Mark as Packed
    PACKED --> STAGING : Ship
    STAGING --> SHIPPED : Confirm Dispatch
    DRAFT --> CANCELLED : Cancel
    PICKING --> CANCELLED : Cancel
    PACKED --> CANCELLED : Cancel
    STAGING --> CANCELLED : Cancel
    SHIPPED --> [*]
    CANCELLED --> [*]
```

---

## 4. LUỒNG TỒN KHO (Inventory)

### 4.1 Tổng quan các thao tác

```mermaid
flowchart TB
    subgraph INVENTORY_OPS
        Q1["Get Inventories\n(Pagination + Filter)"]
        Q2["Get Summary\n(by Product)"]
        Q3["Get by Location\n(Group by location)"]
        Q4["Check Availability"]
        R1["Reserve"]
        U1["Unreserve"]
        I1["Increase"]
        D1["Decrease"]
    end
```

### 4.2 Luồng Reserve (Đặt hàng)

```mermaid
flowchart TB
    START --> V1["Validate orderLineId\n& quantity > 0"]
    V1 --> V2["Find SalesOrderLine"]
    V2 --> V3{"Product match?"}
    V3 -->|Không| E1["Throw: COM_001"]
    V3 -->|Có| L1["Acquire Redisson Lock\nlock:reserve:{orderLineId}"]
    L1 --> Q1["Find existing reservation"]
    Q1 --> E2{"Found?"}
    E2 -->|Có| R1["Return existing"]
    E2 -->|Không| Q2["Find best suitable inventory"]
    Q2 --> E3{"Found?"}
    E3 -->|Không| E4["Throw: INV_004\nInsufficient stock"]
    E3 -->|Có| C1{"Available >= Request?"}
    C1 -->|Không| E4
    C1 -->|Có| U1["Update inventory:\nreserved += quantity"]
    U1 --> C2["Create reservation record"]
    C2 --> M1["Record StockMovement\nType: RESERVE"]
    M1 --> RET["Return response"]
```

### 4.3 Luồng Increase (Tăng tồn)

```mermaid
flowchart TB
    START --> V1["Validate quantity > 0"]
    V1 --> V2["Validate product exists"]
    V2 --> V3["Validate warehouse exists"]
    V3 --> V4{"locationId?"}
    V4 -->|Có| V5["location belongs to warehouse?"]
    V4 -->|Không| V6{"batchId?"}
    V5 --> V6
    V6 -->|Có| V7["batch belongs to product?"]
    V6 -->|Không| L1["Acquire Lock\n(refType + refId)"]
    V7 --> L1
    L1 --> Q1["Find or create inventory"]
    Q1 --> U1["Update onHandQuantity"]
    U1 --> M1["Record StockMovement\nType: INCREASE"]
    M1 --> RET["Return response"]
```

### 4.4 Luồng Decrease (Giảm tồn)

```mermaid
flowchart TB
    START --> V1["Validate quantity > 0"]
    V1 --> Q1["Find inventory\nwith LOCK"]
    Q1 --> C1{"consumeReserved?"}
    C1 -->|Có| R1["Validate reservation\n& quantity"]
    C1 -->|Không| C2{"Available >= Request?"}
    R1 --> C2
    C2 -->|Không| E1["Throw: INV_004"]
    C2 -->|Có| U1["Update quantities"]
    U1 --> M1["Record StockMovement\nType: DECREASE"]
    M1 --> RET["Return response"]
```

---

## 5. Mô hình tương tác giữa các luồng

```mermaid
flowchart LR
    subgraph SALES["Sales Order"]
        SO1["Sales Order\nCONFIRMED"]
    end

    subgraph RESERVE["Reserve"]
        R1["Reserve Inventory"]
    end

    subgraph OUTBOUND["Outbound"]
        O1["Create Shipment"]
        O2["Start Picking"]
        O3["Packed"]
        O4["Shipped"]
    end

    subgraph PO["Purchase Order"]
        PO1["PO CONFIRMED"]
    end

    subgraph INBOUND["Inbound"]
        I1["Inbound Receipt"]
        I2["Confirm → Increase"]
    end

    subgraph INV["Inventory"]
        INV1["On-Hand"]
        INV2["Reserved"]
    end

    SO1 --> R1
    R1 --> INV2
    INV2 --> O1
    O1 --> O2
    O2 --> O3
    O3 --> O4
    O4 --> D1["Decrease Inventory"]
    D1 --> INV1
    
    PO1 --> I1
    I1 --> I2
    I2 --> INV1
```

---

## 6. Error Codes

| Mã | Mô tả |
|----|-------|
| COM_001 | Bad request |
| COM_004 | Not found |
| COM_006 | Invalid page number |
| COM_007 | Invalid page size |
| COM_008 | Page size > 100 |
| COM_009 | Lock acquisition failed |
| COM_010 | Lock interrupted |
| INV_001 | Inventory not found |
| INV_002 | Data inconsistency |
| INV_004 | Insufficient stock |
| PROD_001 | Product not found |
| WHS_001 | Warehouse not found |
| LOC_001 | Location not found |
| PO_001 | Purchase order not found |
| PO_002 | PO lines not found |
| POL_006 | Receipt > remaining PO |
| BATCH_001 | Batch not found |
| BATCH_012 | Batch status invalid |

---

## 7. Chiến lược Locking

1. **Redisson Distributed Lock**: Ngăn race condition giữa các instances
2. **Pessimistic Lock (FOR UPDATE)**: Đảm bảo serializable transaction
3. **Idempotency**: Dùng referenceType + referenceId để tránh duplicate stock movement

---

## 8. Stock Movement Types

| Loại | Mô tả |
|------|-------|
| INBOUND | Nhập kho |
| OUTBOUND | Xuất kho |
| RESERVE | Đặt hàng |
| UNRESERVE | Hủy đặt hàng |
| INCREASE | Tăng tồn |
| DECREASE | Giảm tồn |
| INTERNAL_MOVE | Di chuyển nội bộ |