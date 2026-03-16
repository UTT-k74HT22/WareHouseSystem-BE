# BA Document - Outbound Shipment Lines (OBL) Module
## Business Analysis - Phase 6 Child Module

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Outbound Shipment Lines (OBL) |
| **Parent Phase** | Phase 6: Outbound Operations |
| **Version** | 1.0 |
| **Date** | March 16, 2026 |
| **Status** | Ready for Development |
| **Author** | Business Analyst |
| **Parent Jira** | [WHS-50](https://jira.example.com/browse/WHS-50) - Outbound Shipment Lines API Completion |
| **Jira Task** | [WHS-65](https://jira.example.com/browse/WHS-65) - Implement outbound shipment lines APIs |

---

## 1. Overview

### 1.1 Vị trí trong luồng Outbound

```
Sales Order (SO)
    │
    ├── 1:N ──▶ Sales Order Lines (SOL)
    │                │
    │                └── quantity_ordered, quantity_shipped
    │
    └── 1:N ──▶ Outbound Shipments (OB)
                        │
                        ├── 1:N ──▶ Outbound Shipment Lines (OBL)
                        │                │
                        │                ├── sales_order_line_id ──► SOL ◄── CRITICAL
                        │                ├── location_id
                        │                ├── batch_id
                        │                └── quantity_shipped
                        │
                        └── status: DRAFT → PICKING → PACKED → SHIPPED
```

### 1.2 Chain Flow: SO → SOL → OB → OBL

```mermaid
flowchart LR
    subgraph "Sales Order"
        SO[Sales Order]
        SOL[Sales Order Lines]
    end
    
    subgraph "Outbound"
        OB[Outbound Shipment]
        OBL[Outbound Shipment Lines]
    end
    
    SO -->|1:N| SOL
    SO -->|1:N| OB
    OB -->|1:N| OBL
    OBL -->|references| SOL
```

---

## 2. Entity Definition

### 2.1 OutboundShipmentLines Entity

```java
// Entity: OutboundShipmentLines
@Table(name = "outbound_shipment_lines")
public class OutboundShipmentLines extends BaseEntity {
    
    @Column(name = "outbound_shipment_id", nullable = false)
    private String outboundShipmentId;
    
    @Column(name = "sales_order_line_id", nullable = false)
    private String salesOrderLineId;  // CRITICAL: References SOL
    
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @Column(name = "batch_id")
    private String batchId;  // Nullable, for batch-tracked products
    
    @Column(name = "location_id", nullable = false)
    private String locationId;
    
    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;
    
    @Column(name = "quantity_shipped", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantityShipped;
    
    @Column(name = "picked_at")
    private LocalDateTime pickedAt;
    
    @Column(name = "picked_by")
    private String pickedBy;
    
    @Column(name = "notes")
    private String notes;
}
```

### 2.2 Relationships

| Relationship | Type | Description |
|--------------|------|-------------|
| outbound_shipment_id → OutboundShipments | N:1 | OBL belongs to one OB |
| sales_order_line_id → SalesOrderLines | N:1 | OBL references one SOL |
| product_id → Products | N:1 | OBL item is a product |
| batch_id → Batches | N:1 | Optional, for FIFO |
| location_id → Locations | N:1 | Pick location |

### 2.3 Unique Constraints

| Constraint | Columns | Description |
|------------|---------|-------------|
| UK | `(outbound_shipment_id, line_number)` | Each OB has unique line numbers |
| FK | `outbound_shipment_id` → `outbound_shipments(id)` | CASCADE delete |
| FK | `sales_order_line_id` → `sales_order_lines(id)` | RESTRICT delete |
| FK | `location_id` → `locations(id)` | RESTRICT delete |

---

## 3. State Machine

### 3.1 OBL Status (Derived from Parent OB)

```mermaid
stateDiagram-v2
    [*] --> NEW
    
    note right of NEW
        Created as part of shipment creation
        or added to DRAFT shipment
    end note
    
    NEW --> PICKED: Pick confirmed
    
    note right of PICKED
        Staff picked this line
        Location & qty verified
    end note
    
    PICKED --> SHIPPED: Shipment confirmed
    
    PICKED --> CANCELLED: Shipment cancelled
    NEW --> CANCELLED: Shipment cancelled
    
    SHIPPED --> [*]
    CANCELLED --> [*]
```

### 3.2 OBL vs OB vs SOL Synchronization

| OB Status | OBL Status | SOL Impact |
|-----------|------------|------------|
| DRAFT | NEW | No impact |
| PICKING | NEW/PICKED | No impact |
| PACKED | PICKED | No impact |
| SHIPPED | SHIPPED | SOL.quantity_shipped += OBL.quantity_shipped |
| CANCELLED | CANCELLED | No impact (no revert needed) |

---

## 4. Business Flows

### 4.1 Flow: OBL Creation

```mermaid
sequenceDiagram
    actor WS as Warehouse Staff
    participant C as OutboundShipmentLinesController
    participant S as OutboundShipmentLinesService
    participant OB_R as OutboundShipmentsRepository
    participant SOL_R as SalesOrderLinesRepository
    participant OBL_R as OutboundShipmentLinesRepository
    participant LOC_R as LocationsRepository
    
    WS->>C: POST /api/v1/outbound-shipment-lines
    C->>S: createLine(request)
    
    rect rgb(240, 248, 255)
    Note over S: Validate Shipment exists and DRAFT
    S->>OB_R: findById(shipmentId)
    OB_R-->>S: Shipment Entity
    
    alt Shipment not found
        S-->>C: throw NotFoundException(OUT_009)
    end
    
    alt Shipment status != DRAFT
        S-->>C: throw BadRequestException(OUT_010)
    end
    end
    
    rect rgb(255, 248, 240)
    Note over S: Validate SOL reference
    S->>SOL_R: findById(salesOrderLineId)
    SOL_R-->>S: SOL Entity
    
    alt SOL not found
        S-->>C: throw NotFoundException(OUT_008)
    end
    
    S->>S: remaining = SOL.quantityOrdered - SOL.quantityShipped
    
    alt quantityShipped > remaining
        S-->>C: throw BadRequestException(OUT_013)
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: Validate Location
    S->>LOC_R: findById(locationId)
    
    alt Location not found
        S-->>C: throw BadRequestException(OUT_XXX)
    end
    
    S->>S: location belongs to shipment.warehouseId
    
    alt Location not in warehouse
        S-->>C: throw BadRequestException(OUT_015)
    end
    end
    
    rect rgb(240, 240, 255)
    Note over S: Create Line
    S->>OBL_R: findMaxLineNumber(shipmentId)
    OBL_R-->>S: maxLineNumber
    
    S->>S: lineNumber = maxLineNumber + 1
    S->>OBL_R: save(line)
    end
    
    S-->>C: BaseResponse(line)
    C-->>WS: 201 Created
```

### 4.2 Flow: OBL Confirm (Shipment Confirm)

```mermaid
sequenceDiagram
    participant System
    participant OBL_R as OutboundShipmentLinesRepository
    participant INV_R as InventoryRepository
    participant SOL_R as SalesOrderLinesRepository
    participant MOV_S as StockMovementsService
    
    rect rgb(240, 248, 255)
    Note over System: Called from Shipment Confirm
    loop For each OBL
        System->>OBL_R: findByShipmentId(shipmentId)
    end
    end
    
    rect rgb(255, 240, 240)
    Note over System: Inventory Decrease
    loop For each OBL
        System->>INV_R: findByProductWarehouseLocationBatch
        INV_R-->>System: Inventory
        
        alt inventory.onHand < OBL.quantityShipped
            System-->>: throw BadRequestException(OUT_014)
        end
        
        System->>System: onHand -= quantityShipped
        System->>System: reserved -= quantityShipped
        
        System->>MOV_S: writeMovement(OUTBOUND, reference=OBL)
    end
    end
    
    rect rgb(240, 240, 255)
    Note over System: Update SOL
    loop For each OBL
        System->>SOL_R: findById(OBL.salesOrderLineId)
        SOL_R-->>System: SOL
        
        System->>System: SOL.quantityShipped += OBL.quantityShipped
        System->>SOL_R: save(SOL)
    end
    end
```

---

## 5. Critical Integration: OBL → SOL

### 5.1 Data Flow

```
OBL.quantity_shipped ──────────────────────► SOL.quantity_shipped (ACCUMULATED)
      │
      │ On Shipment Confirm:
      │ 1. Decrease Inventory (onHand, reserved)
      │ 2. Write Stock Movement (OUTBOUND)
      │ 3. Update SOL.quantity_shipped
      │ 4. Recompute SOL status
      │ 5. Recompute SO status
      │
      └───────────────────────────► SO status:
                                      - All shipped → COMPLETED
                                      - Some shipped → PARTIALLY_SHIPPED
```

### 5.2 Validation Rules

| Rule | Description | Error Code |
|------|-------------|------------|
| BR-OBL-01 | OBL must reference valid SOL | OUT_008 |
| BR-OBL-02 | OBL.quantity_shipped ≤ SOL.remaining | OUT_013 |
| BR-OBL-03 | Location must belong to shipment warehouse | OUT_015 |
| BR-OBL-04 | OBL can only be created for DRAFT shipment | OUT_010 |
| BR-OBL-05 | OBL can only be updated for DRAFT shipment | OUT_010 |
| BR-OBL-06 | OBL can only be deleted for DRAFT shipment | OUT_010 |
| BR-OBL-07 | Inventory must have sufficient onHand quantity | OUT_014 |

### 5.3 Remaining Quantity Calculation

```java
// For each SOL line:
remainingQuantity = SOL.quantityOrdered - SOL.quantityShipped

// For each OBL creation/update:
OBL.quantityShipped ≤ remainingQuantity

// Example:
SOL.quantityOrdered = 100
SOL.quantityShipped = 30  // Already shipped 30
remainingQuantity = 100 - 30 = 70  // Can ship max 70 more
```

---

## 6. API Specification

### 6.1 Outbound Shipment Lines APIs

| # | Method | Endpoint | Description | Jira Task | Parent |
|---|--------|----------|-------------|-----------|--------|
| 1 | `POST` | `/api/v1/outbound-shipment-lines` | Add line to shipment | [WHS-65](https://jira.example.com/browse/WHS-65) | WHS-50 |
| 2 | `PUT` | `/api/v1/outbound-shipment-lines/{id}` | Update line | [WHS-65](https://jira.example.com/browse/WHS-65) | WHS-50 |
| 3 | `DELETE` | `/api/v1/outbound-shipment-lines/{id}` | Delete line | [WHS-65](https://jira.example.com/browse/WHS-65) | WHS-50 |

### 6.2 API Request/Response Models

#### 6.2.1 Create Line Request
```json
{
  "outboundShipmentId": "uuid",
  "salesOrderLineId": "uuid",
  "productId": "uuid",
  "locationId": "uuid",
  "batchId": "uuid (optional)",
  "quantityShipped": 10.00,
  "notes": "Optional notes"
}
```

#### 6.2.2 Update Line Request
```json
{
  "locationId": "uuid",
  "batchId": "uuid (optional)",
  "quantityShipped": 15.00,
  "notes": "Updated notes"
}
```

#### 6.2.3 Response
```json
{
  "id": "uuid",
  "outboundShipmentId": "uuid",
  "salesOrderLineId": "uuid",
  "productId": "uuid",
  "locationId": "uuid",
  "batchId": "uuid or null",
  "lineNumber": 1,
  "quantityShipped": 10.00,
  "pickedAt": "timestamp or null",
  "pickedBy": "uuid or null",
  "notes": "..."
}
```

---

## 7. Integration Points

### 7.1 With Outbound Shipments (Parent)

| Operation | Impact on OBL |
|-----------|---------------|
| Shipment Created | OBL can be added (if DRAFT) |
| Shipment Pick | OBL status unchanged |
| Shipment Confirm | OBL affects SOL, writes movements |
| Shipment Cancel | OBL not affected (no revert) |

### 7.2 With Sales Order Lines (Critical Reference)

| Operation | OBL Action | SOL Impact |
|-----------|------------|------------|
| OBL Create | Validate remaining qty | - |
| OBL Update | Validate remaining qty | - |
| OBL Confirm | Update SOL.quantity_shipped | Accumulate shipped qty |

### 7.3 With Inventory

| OBL Operation | Inventory Operation | Effect |
|---------------|---------------------|--------|
| Shipment Confirm | `decrease()` | onHand -= qty, reserved -= qty |

### 7.4 With Stock Movements

| Source | Movement Type | Reference | Quantity |
|--------|--------------|-----------|----------|
| OBL Confirm | OUTBOUND | OUTBOUND_SHIPMENT | quantity_shipped |

---

## 8. Error Codes

| Code | Description | HTTP |
|------|-------------|------|
| OBL_001 | Shipment not found | 404 |
| OBL_002 | Shipment is not in DRAFT status | 400 |
| OBL_003 | Sales order line not found | 400 |
| OBL_004 | Quantity exceeds remaining | 400 |
| OBL_005 | Location not found | 400 |
| OBL_006 | Location does not belong to warehouse | 400 |
| OBL_007 | Insufficient inventory at location | 400 |
| OBL_008 | Shipment line not found | 404 |

---

## 9. End-to-End Flow: Complete Chain

### 9.1 Full Chain: SO → SOL → OB → OBL

```mermaid
sequenceDiagram
    participant SM as Sales Manager
    participant WS as Warehouse Staff
    participant Picker
    participant SO_S as SalesOrdersService
    participant OB_S as OutboundShipmentsService
    participant INV_S as InventoryService
    
    Note over SM,WS: Step 1: Create Sales Order
    SM->>SO_S: POST /sales-orders (with lines)
    SO_S-->>SM: SO created (DRAFT)
    
    Note over SM,WS: Step 2: Confirm Sales Order
    SM->>SO_S: PUT /sales-orders/{id}/confirm
    SO_S->>INV_S: reserve() for each SOL
    SO_S-->>SM: SO CONFIRMED
    
    Note over SM,WS: Step 3: Create Outbound Shipment
    WS->>OB_S: POST /outbound-shipments (from SO)
    OB_S-->>WS: Shipment created (DRAFT)
    
    Note over Picker,WS: Step 4: Add Shipment Lines
    WS->>OB_S: POST /outbound-shipment-lines (reference SOL)
    OB_S-->>WS: OBL created
    
    Note over Picker,WS: Step 5: Start Picking
    Picker->>OB_S: PUT /outbound-shipments/{id}/pick
    OB_S-->>Picker: Shipment PICKING
    
    Note over Picker,WS: Step 6: Confirm Shipment
    Picker->>OB_S: PUT /outbound-shipments/{id}/confirm
    OB_S->>INV_S: decrease() for each OBL
    OB_S->>INV_S: write OUTBOUND movement
    OB_S->>SO_S: update SOL.quantity_shipped
    OB_S->>SO_S: recompute SO status
    OB_S-->>Picker: Shipment SHIPPED
```

---

## 10. Acceptance Criteria

### 10.1 Create Line
- [ ] OBL created with correct line_number (auto-increment)
- [ ] OBL references valid SOL via sales_order_line_id
- [ ] quantity_shipped ≤ SOL remaining quantity
- [ ] location belongs to shipment warehouse
- [ ] Cannot create line for non-DRAFT shipment

### 10.2 Update Line
- [ ] OBL updated with new values
- [ ] quantity_shipped ≤ SOL remaining quantity
- [ ] Cannot update line if shipment not DRAFT

### 10.3 Delete Line
- [ ] OBL deleted from database
- [ ] Cannot delete line if shipment not DRAFT

### 10.4 Shipment Confirm
- [ ] Decreases inventory (onHand, reserved) per OBL
- [ ] Writes OUTBOUND movement per OBL
- [ ] Updates SOL.quantity_shipped correctly
- [ ] Recomputes SOL and SO status correctly
- [ ] Atomic transaction (all or nothing)

---

## 11. Comparison: OBL vs IBL (Inbound)

| Aspect | Outbound Shipment Lines (OBL) | Inbound Receipt Lines (IBL) |
|--------|-------------------------------|----------------------------|
| Parent | Outbound Shipments | Inbound Receipts |
| References | Sales Order Lines | Purchase Order Lines |
| Location | Source location (pick from) | Destination location (receive to) |
| Batch | Optional (for FIFO) | Optional (for tracking) |
| Quantity | quantity_shipped | quantityReceived |
| Inventory Effect | decrease (onHand, reserved) | increase (onHand) |
| Movement Type | OUTBOUND | INBOUND |
| Status Update | Updates SOL.quantity_shipped | Updates POL.quantityReceived |

---

## 12. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Mar 16, 2026 | BA | Initial OBL business analysis |

---

**Document Status:** ✅ Ready for Development  
**Related Documents:**
- [BA_PHASE6_OUTBOUND_OPERATIONS_COMPLETE.md](./BA_PHASE6_OUTBOUND_OPERATIONS_COMPLETE.md) - Parent document
- [BA_PHASE6_SALES_ORDER_LINES.md](./BA_PHASE6_SALES_ORDER_LINES.md) - SOL module
- [DB_MODULE_06_OUTBOUND.md](./DB_MODULE_06_OUTBOUND.md) - Database schema
