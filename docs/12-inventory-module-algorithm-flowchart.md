# Inventory Module - Algorithm Flowchart

## Overview
Module quản lý tồn kho trong hệ thống Warehouse Management System (WMS).

## Main Operations

```mermaid
flowchart TB
    subgraph CONTROLLER["InventoryController"]
        A1["GET /inventories"] --> S1
        A2["GET /summary/{productId}"] --> S2
        A3["GET /by-location"] --> S3
        A4["POST /check-availability"] --> S4
        A5["POST /reserve"] --> S5
        A6["POST /unreserve"] --> S6
        A7["POST /increase"] --> S7
        A8["POST /decrease"] --> S8
    end

    subgraph SERVICE["InventoryServiceImpl"]
        S1["getInventories"] --> S1_1["Validate pagination"]
        S1_1 --> S1_2["Query with Specification"]
        S1_2 --> S1_3["Fetch related entities"]
        S1_3 --> S1_4["Map to Response"]

        S2["getSummaryByProduct"] --> S2_1["Query aggregate from DB"]
        S2_1 --> S2_2["Return summary"]

        S3["getInventoryByLocation"] --> S3_1["Query projection by location"]
        S3_1 --> S3_2["Group by location"]
        S3_2 --> S3_3["Build response"]

        S4["checkAvailability"] --> S4_1["Validate product"]
        S4_1 --> S4_2["Validate warehouse optional"]
        S4_2 --> S4_3["Validate location optional"]
        S4_3 --> S4_4["Query available qty"]
        S4_4 --> S4_5["Compare with request"]
        S4_5 --> S4_6["Return availability"]

        S5["reserve"] --> S5_1["Validate orderLineId & quantity"]
        S5_1 --> S5_2["Fetch order line"]
        S5_2 --> S5_3["Acquire Redisson Lock"]
        S5_3 --> S5_4{"Existing reservation?"}
        S5_4 -->|Yes| S5_5["Return existing"]
        S5_4 -->|No| S5_6["Find best inventory"]
        S5_6 --> S5_7{"Inventory found?"}
        S5_7 -->|No| S5_8["Throw INV_004"]
        S5_7 -->|Yes| S5_9{"Enough available?"}
        S5_9 -->|No| S5_8
        S5_9 -->|Yes| S5_10["Update reserved quantity"]
        S5_10 --> S5_11["Create reservation record"]
        S5_11 --> S5_12["Record stock movement"]
        S5_12 --> S5_13["Return response"]

        S6["unreserve"] --> S6_1["Validate orderLineId & quantity"]
        S6_1 --> S6_2["Find reservation"]
        S6_2 --> S6_3{"Reservation exists?"}
        S6_3 -->|No| S6_4["Throw INV_001"]
        S6_3 -->|Yes| S6_5["Get inventory with lock"]
        S6_5 --> S6_6["Heal reserved if needed"]
        S6_6 --> S6_7["Update reserved quantity"]
        S6_7 --> S6_8["Record movement"]
        S6_8 --> S6_9{"Remaining qty > 0?"}
        S6_9 -->|Yes| S6_10["Update reservation"]
        S6_9 -->|No| S6_11["Delete reservation"]
        S6_10 --> S6_12["Return response"]
        S6_11 --> S6_12

        S7["increase"] --> S7_1["Validate quantity > 0"]
        S7_1 --> S7_2["Validate product"]
        S7_2 --> S7_3["Validate warehouse"]
        S7_3 --> S7_4["Validate location optional"]
        S7_4 --> S7_5["Validate batch optional"]
        S7_5 --> S7_6["Acquire Lock"]
        S7_6 --> S7_7["Find or create inventory"]
        S7_7 --> S7_8["Update onHand qty"]
        S7_8 --> S7_9["Record stock movement"]
        S7_9 --> S7_10["Return response"]

        S8["decrease"] --> S8_1["Validate quantity > 0"]
        S8_1 --> S8_2["Find inventory with lock"]
        S8_2 --> S8_3{"consumeReserved?"}
        S8_3 -->|Yes| S8_4["Validate & consume reserved"]
        S8_3 -->|No| S8_5["Check available qty"]
        S8_4 --> S8_6["Update onHand & reserved"]
        S8_5 --> S8_6
        S8_6 --> S8_7["Record movement"]
        S8_7 --> S8_8["Return response"]
    end

    CONTROLLER --> SERVICE
```

## 1. Get Inventories Flow

```mermaid
flowchart LR
    START["Start"] --> V1["Validate page size <= 100"]
    V1 --> Q1["Query with InventorySpecification"]
    Q1 --> E1["Fetch productMap"]
    E1 --> E2["Fetch warehouseMap"]
    E2 --> E3["Fetch locationMap"]
    E3 --> E4["Fetch batchMap"]
    E4 --> M1["Map to InventoryResponse"]
    M1 --> END["Return PageResponse"]
```

## 2. Reserve Inventory Flow

```mermaid
flowchart TB
    START --> V1["Validate orderLineId not blank"]
    V1 --> V2["Validate quantity > 0"]
    V2 --> V3["Fetch SalesOrderLines"]
    V3 --> V4["Validate productId match"]
    V4 --> L1["Acquire Redisson Lock\nlock:reserve:{orderLineId}"]
    L1 --> Q1["Find existing reservation"]
    Q1 --> C1{"Existing?"}
    C1 -->|Yes| R1["Return existing"]
    C1 -->|No| Q2["Find best suitable inventory"]
    Q2 --> C2{"Found?"}
    C2 -->|No| E1["Throw INV_004\nInsufficient stock"]
    C2 -->|Yes| C3{"Available >= Request?"}
    C3 -->|No| E1
    C3 -->|Yes| U1["Update reservedQuantity"]
    U1 --> C["Create InventoryReservation"]
    C --> M["Record StockMovement\nType: RESERVE"]
    M --> RET["Return InventoryReserveResponse"]
```

## 3. Unreserve Inventory Flow

```mermaid
flowchart TB
    START --> V1["Validate orderLineId"]
    V1 --> V2["Validate quantity > 0"]
    V2 --> Q1["Find reservation by orderLineId"]
    Q1 --> C1{"Found?"}
    C1 -->|No| E1["Throw INV_001"]
    C1 -->|Yes| V3["Validate product & warehouse match"]
    V3 --> Q2["Find inventory for update"]
    Q2 --> C2{"Reserved >= Unreserve?"}
    C2 -->|No| E2["Throw INV_002\nData inconsistency"]
    C2 -->|Yes| U1["Update reservedQuantity\n(reserved - unreserve)"]
    U1 --> M["Record StockMovement\nType: UNRESERVE"]
    M --> C3{"Remaining > 0?"}
    C3 -->|Yes| U2["Save reservation"]
    C3 -->|No| D["Delete reservation"]
    U2 --> RET["Return response"]
    D --> RET
```

## 4. Increase Inventory Flow

```mermaid
flowchart TB
    START --> V1["Validate quantity > 0"]
    V1 --> V2["Validate product exists"]
    V2 --> V3["Validate warehouse exists"]
    V3 --> V4{"locationId provided?"}
    V4 -->|Yes| V5["Validate location belongs to warehouse"]
    V4 -->|No| V6{"batchId provided?"}
    V6 -->|Yes| V7["Validate batch belongs to product"]
    V5 --> V6
    V7 --> L1["Build lock key\nlock:inventory:reference:{type}:{id}"]
    L1 --> L2["Acquire Redisson Lock"]
    L2 --> Q1["Find or create inventory"]
    Q1 --> U1["Update onHandQuantity\n(onHand + quantity)"]
    U1 --> M["Record StockMovement\nType: INCREASE"]
    M --> RET["Return InventoryResponse"]
```

## 5. Decrease Inventory Flow

```mermaid
flowchart TB
    START --> V1["Validate quantity > 0"]
    V1 --> Q1["Find inventory for update"]
    Q1 --> C1{"consumeReserved?"}
    C1 -->|Yes| R1["Handle reservation logic"]
    C1 -->|No| C2{"Available >= Request?"}
    R1 --> C2
    C2 -->|No| E1["Throw INV_004\nInsufficient stock"]
    C2 -->|Yes| U1["Update quantities"]
    U1 --> M["Record StockMovement\nType: DECREASE"]
    M --> RET["Return response"]
```

## 6. Check Availability Flow

```mermaid
flowchart TB
    START --> V1["Validate product exists"]
    V1 --> V2{"warehouseId provided?"}
    V2 -->|Yes| V3["Validate warehouse exists"]
    V2 -->|No| V4{"locationId provided?"}
    V3 --> V5["location belongs to warehouse?"]
    V4 --> V5
    V5 --> Q1["Query available quantity"]
    Q1 --> C1{"Available >= Request?"}
    C1 -->|Yes| R1["isAvailable = true"]
    C1 -->|No| R2["isAvailable = false"]
    R1 --> RET["Return CheckAvailabilityResponse"]
    R2 --> RET
```

## Key Components

| Component | Description |
|-----------|-------------|
| Redisson Lock | Distributed lock for concurrent inventory updates |
| InventorySpecification | JPA Specification for dynamic filtering |
| StockMovements | Audit trail for all inventory changes |
| InventoryReservation | Track reserved stock per order line |

## Error Codes

| Code | Description |
|------|-------------|
| INV_001 | Inventory not found |
| INV_002 | Data inconsistency |
| INV_004 | Insufficient stock |
| PROD_001 | Product not found |
| WHS_001 | Warehouse not found |
| LOC_001 | Location not found |
| COM_001 | Bad request |
| COM_009 | Lock acquisition failed |
| COM_010 | Lock interrupted |

## Database Locking Strategy

1. **Pessimistic Locking**: Use `FOR UPDATE` via repository methods
2. **Redisson Distributed Lock**: Prevent race conditions across instances
3. **Idempotency**: Use referenceType + referenceId to prevent duplicate movements