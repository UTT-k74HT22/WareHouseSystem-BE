# BA Document - Sales Order (SO) Module
## Business Analysis - Phase 6 Parent Module

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Sales Order (SO) |
| **Parent Phase** | Phase 6: Outbound Operations |
| **Version** | 1.0 |
| **Date** | March 16, 2026 |
| **Status** | Ready for Development |
| **Author** | Business Analyst |
| **Parent Jira** | [WHS-47](https://jira.example.com/browse/WHS-47) - Sales Orders API Completion |
| **Jira Tasks** | [WHS-59](https://jira.example.com/browse/WHS-59) - SO CRUD APIs, [WHS-60](https://jira.example.com/browse/WHS-60) - SO Confirm/Cancel APIs |

---

## 1. Overview

### 1.1 Vị trí trong luồng Outbound

```
Sales Order (SO) ◄── Parent Module
    │
    ├── 1:N ──▶ Sales Order Lines (SOL) ◄── [WHS-61]
    │                │
    │                └── Mỗi SOL: product, qty, price, shipped
    │
    └── 1:N ──▶ Outbound Shipments (OB) ◄── [WHS-62, WHS-63]
                     │
                     └── 1:N ──▶ Outbound Shipment Lines (OBL) ◄── [WHS-65]
```

### 1.2 Flow triển khai

```
Sales Order (SO) ──▶ Sales Order Lines (SOL) ──▶ Outbound Shipments (OB) ──▶ Outbound Shipment Lines (OBL)
      [WHS-59]            [WHS-61]                    [WHS-62, WHS-63]             [WHS-65]
      [WHS-60]
```

---

## 2. Entity Definition

### 2.1 SalesOrders Entity

```java
// Entity: SalesOrders
@Table(name = "sales_orders")
public class SalesOrders extends BaseEntity {
    
    @Column(name = "so_number", nullable = false, unique = true)
    private String soNumber;  // Format: SO-YYYY-NNNN
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;  // FK to business_partners (type = CUSTOMER)
    
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;  // FK to warehouses
    
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;
    
    @Column(name = "requested_delivery_date", nullable = false)
    private LocalDate requestedDeliveryDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SalesOrdersStatus status;
    
    @Column(name = "sub_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;  // Computed from lines
    
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;  // Computed from lines
    
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;  // Computed: subTotal + taxAmount
    
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false)
    private CurrencyType currency;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "confirmed_by")
    private String confirmedBy;
}
```

### 2.2 Status Enum

```java
public enum SalesOrdersStatus {
    DRAFT,              // Mới tạo, có thể edit
    CONFIRMED,          // Đã xác nhận, đã reserve tồn kho
    PARTIALLY_SHIPPED, // Đã giao một phần
    COMPLETED,          // Hoàn tất (tất cả đã giao)
    CANCELLED           // Đã hủy
}
```

### 2.3 Unique Constraints

| Constraint | Columns | Description |
|------------|---------|-------------|
| UK | `so_number` | Unique per system |

### 2.4 Foreign Keys

| FK | References | On Delete |
|----|------------|-----------|
| customer_id | business_partners(id) | RESTRICT |
| warehouse_id | warehouses(id) | RESTRICT |
| confirmed_by | accounts(id) | SET NULL |

---

## 3. State Machine

### 3.1 Sales Order Lifecycle

```mermaid
stateDiagram-v2
    [*] --> DRAFT
    
    DRAFT --> CONFIRMED: Confirm (reserve stock)
    DRAFT --> CANCELLED: Cancel
    
    CONFIRMED --> PARTIALLY_SHIPPED: Partial shipment confirmed
    CONFIRMED --> COMPLETED: All shipments confirmed
    
    PARTIALLY_SHIPPED --> COMPLETED: Remaining shipments confirmed
    PARTIALLY_SHIPPED --> PARTIALLY_SHIPPED: Additional shipment
    
    note right of PARTIALLY_SHIPPED
        Cannot cancel when
        PARTIALLY_SHIPPED
    end note
    
    CANCELLED --> [*]
    COMPLETED --> [*]
```

### 3.2 State Transition Rules

| From State | To State | Trigger | Pre-condition | Side Effect |
|------------|----------|---------|---------------|-------------|
| DRAFT | CONFIRMED | /confirm | DRAFT only | reserve inventory |
| DRAFT | CANCELLED | /cancel | DRAFT only | none |
| CONFIRMED | PARTIALLY_SHIPPED | shipment confirm | - | update shipped qty |
| CONFIRMED | COMPLETED | shipment confirm | all lines shipped | - |
| PARTIALLY_SHIPPED | COMPLETED | shipment confirm | all lines shipped | - |
| PARTIALLY_SHIPPED | PARTIALLY_SHIPPED | shipment confirm | partial | - |

---

## 4. Business Flows

### 4.1 Flow: Create Sales Order

```mermaid
sequenceDiagram
    actor SM as Sales Manager
    participant C as SalesOrdersController
    participant S as SalesOrdersService
    participant SO_R as SalesOrdersRepository
    participant BP_R as BusinessPartnersRepository
    participant WH_R as WarehouseRepository
    participant SOL_R as SalesOrderLinesRepository
    
    SM->>C: POST /api/v1/sales-orders
    C->>S: createSalesOrder(request)
    
    rect rgb(240, 248, 255)
    Note over S: 1. Validate Customer
    S->>BP_R: findById(customerId)
    BP_R-->>S: Customer
    
    alt Customer not found
        S-->>C: throw NotFoundException(OUT_007)
    end
    
    alt Customer status != ACTIVE
        S-->>C: throw BadRequestException(OUT_002)
    end
    
    alt Customer type != CUSTOMER
        S-->>C: throw BadRequestException(OUT_002)
    end
    end
    
    rect rgb(255, 248, 240)
    Note over S: 2. Validate Warehouse
    S->>WH_R: findById(warehouseId)
    WH_R-->>S: Warehouse
    
    alt Warehouse not found or inactive
        S-->>C: throw BadRequestException(OUT_XXX)
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: 3. Validate Lines
    alt lines empty
        S-->>C: throw BadRequestException(OUT_001)
    end
    
    loop For each line
        S->>S: validate product exists and active
        S->>S: validate quantity > 0
        S->>S: validate unitPrice >= 0
    end
    end
    
    rect rgb(240, 240, 255)
    Note over S: 4. Compute Totals
    loop For each line
        S->>S: lineTotal = quantityOrdered * unitPrice
    end
    S->>S: subTotal = sum(lineTotal)
    S->>S: taxAmount = subTotal * taxRate (or 0)
    S->>S: totalAmount = subTotal + taxAmount
    end
    
    rect rgb(255, 255, 240)
    Note over S: 5. Generate SO Number
    S->>S: generateSoNumber()  // SO-YYYY-NNNN
    end
    
    rect rgb(240, 248, 240)
    Note over S: 6. Persist
    S->>SO_R: save(so with status=DRAFT)
    SO_R-->>S: saved SO
    
    loop For each line
        S->>S: create SOL entity
        S->>SOL_R: save(line)
    end
    end
    
    S-->>C: BaseResponse(so)
    C-->>SM: 201 Created
```

### 4.2 Flow: Update Sales Order (DRAFT only)

```mermaid
sequenceDiagram
    actor SM as Sales Manager
    participant C as SalesOrdersController
    participant S as SalesOrdersService
    participant SO_R as SalesOrdersRepository
    
    SM->>C: PUT /api/v1/sales-orders/{id}
    C->>S: updateSalesOrder(id, request)
    
    rect rgb(240, 248, 255)
    Note over S: Load & Validate
    S->>SO_R: findById(id)
    SO_R-->>S: SO Entity
    
    alt SO not found
        S-->>C: throw NotFoundException(OUT_007)
    end
    
    alt SO status != DRAFT
        S-->>C: throw BadRequestException(OUT_XXX)
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: Update Fields
    S->>S: update fields (dates, notes, customer, warehouse)
    S->>SO_R: save(so)
    end
    
    S-->>C: BaseResponse(updated SO)
    C-->>SM: 200 OK
```

**Lưu Ý:** Update SO không bao gồm thay đổi lines. Lines được quản lý qua SOL APIs riêng.

### 4.3 Flow: Delete Sales Order (DRAFT only)

```mermaid
sequenceDiagram
    actor SM as Sales Manager
    participant C as SalesOrdersController
    participant S as SalesOrdersService
    participant SO_R as SalesOrdersRepository
    
    SM->>C: DELETE /api/v1/sales-orders/{id}
    C->>S: deleteSalesOrder(id)
    
    rect rgb(240, 248, 255)
    Note over S: Load & Validate
    S->>SO_R: findById(id)
    SO_R-->>S: SO Entity
    
    alt SO not found
        S-->>C: throw NotFoundException(OUT_007)
    end
    
    alt SO status != DRAFT
        S-->>C: throw BadRequestException(OUT_XXX)
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: Delete
    S->>SO_R: delete(so)  // CASCADE deletes SOL
    end
    
    S-->>C: BaseResponse(null)
    C-->>SM: 204 No Content
```

### 4.4 Flow: Confirm Sales Order (RESERVE)

```mermaid
sequenceDiagram
    actor SM as Sales Manager
    participant C as SalesOrdersController
    participant S as SalesOrdersService
    participant SO_R as SalesOrdersRepository
    participant SOL_R as SalesOrderLinesRepository
    participant INV_S as InventoryService
    
    SM->>C: PUT /api/v1/sales-orders/{id}/confirm
    C->>S: confirmSalesOrder(id)
    
    rect rgb(240, 248, 255)
    Note over S: 1. Load & Validate
    S->>SO_R: findById(id)
    SO_R-->>S: SO Entity
    
    alt SO not found
        S-->>C: throw NotFoundException(OUT_007)
    end
    
    alt SO status != DRAFT
        S-->>C: throw BadRequestException(OUT_004)
    end
    end
    
    rect rgb(255, 248, 240)
    Note over S: 2. Check Availability (ALL OR NOTHING)
    S->>SOL_R: findBySalesOrderId(id)
    
    loop For each SOL
        S->>INV_S: checkAvailability(productId, warehouseId, qty)
        INV_S-->>S: available
        
        alt available < requested
            S-->>C: throw BadRequestException(OUT_003)
        end
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: 3. Reserve Stock (ALL OR NOTHING)
    S->>SOL_R: findBySalesOrderId(id)
    
    loop For each SOL
        S->>INV_S: reserve(productId, warehouseId, locationId?, batchId?, qty, reference)
        INV_S-->>S: reservation result
    end
    end
    
    rect rgb(240, 240, 255)
    Note over S: 4. Update Status
    S->>S: status = CONFIRMED
    S->>S: confirmedAt = now()
    S->>S: confirmedBy = currentUser
    S->>SO_R: save(so)
    end
    
    S-->>C: BaseResponse(confirmed SO)
    C-->>SM: 200 OK
```

### 4.5 Flow: Cancel Sales Order (UNRESERVE)

```mermaid
sequenceDiagram
    actor SM as Sales Manager
    participant C as SalesOrdersController
    participant S as SalesOrdersService
    participant SO_R as SalesOrdersRepository
    participant SOL_R as SalesOrderLinesRepository
    participant SH_R as OutboundShipmentsRepository
    participant INV_S as InventoryService
    
    SM->>C: PUT /api/v1/sales-orders/{id}/cancel
    C->>S: cancelSalesOrder(id)
    
    rect rgb(240, 248, 255)
    Note over S: 1. Load & Validate
    S->>SO_R: findById(id)
    SO_R-->>S: SO Entity
    
    alt SO not found
        S-->>C: throw NotFoundException(OUT_007)
    end
    
    alt SO status != CONFIRMED
        S-->>C: throw BadRequestException(OUT_006)
    end
    end
    
    rect rgb(255, 248, 240)
    Note over S: 2. Check No Active Shipments
    S->>SH_R: findBySalesOrderId(id)
    
    loop For each shipment
        S->>S: check shipment status
        
        alt status in [PICKING, PACKED, SHIPPED]
            S-->>C: throw BadRequestException(OUT_005)
        end
    end
    end
    
    rect rgb(240, 255, 240)
    Note over S: 3. Unreserve Stock
    S->>SOL_R: findBySalesOrderId(id)
    
    loop For each SOL
        S->>S: remaining = SOL.quantityOrdered - SOL.quantityShipped
        
        alt remaining > 0
            S->>INV_S: unreserve(productId, warehouseId, remaining, reference)
        end
    end
    end
    
    rect rgb(240, 240, 255)
    Note over S: 4. Update Status
    S->>S: status = CANCELLED
    S->>SO_R: save(so)
    end
    
    S-->>C: BaseResponse(cancelled SO)
    C-->>SM: 200 OK
```

---

## 5. API Specification

### 5.1 Sales Orders APIs

| # | Method | Endpoint | Description | Jira Task | Auth |
|---|--------|----------|-------------|-----------|------|
| 1 | `POST` | `/api/v1/sales-orders` | Create SO (DRAFT) | [WHS-59](https://jira.example.com/browse/WHS-59) | ADMIN, MANAGER |
| 2 | `GET` | `/api/v1/sales-orders` | List SOs (paginated + filters) | [WHS-59](https://jira.example.com/browse/WHS-59) | isAuthenticated |
| 3 | `GET` | `/api/v1/sales-orders/{id}` | Get SO details | [WHS-59](https://jira.example.com/browse/WHS-59) | isAuthenticated |
| 4 | `GET` | `/api/v1/sales-orders/{id}/lines` | Get SO lines | [WHS-59](https://jira.example.com/browse/WHS-59) | isAuthenticated |
| 5 | `PUT` | `/api/v1/sales-orders/{id}` | Update SO (DRAFT only) | [WHS-59](https://jira.example.com/browse/WHS-59) | ADMIN, MANAGER |
| 6 | `DELETE` | `/api/v1/sales-orders/{id}` | Delete SO (DRAFT only) | [WHS-59](https://jira.example.com/browse/WHS-59) | ADMIN, MANAGER |
| 7 | `PUT` | `/api/v1/sales-orders/{id}/confirm` | Confirm → reserve | [WHS-60](https://jira.example.com/browse/WHS-60) | ADMIN, MANAGER |
| 8 | `PUT` | `/api/v1/sales-orders/{id}/cancel` | Cancel → unreserve | [WHS-60](https://jira.example.com/browse/WHS-60) | ADMIN, MANAGER |

### 5.2 Query Parameters (List API)

| Parameter | Type | Description |
|-----------|------|-------------|
| `soNumber` | String | Filter by SO number |
| `customerId` | UUID | Filter by customer |
| `warehouseId` | UUID | Filter by warehouse |
| `status` | String | Filter by status |
| `orderDateFrom` | Date | Filter from order date |
| `orderDateTo` | Date | Filter to order date |
| `requestedDeliveryDateFrom` | Date | Filter from delivery date |
| `requestedDeliveryDateTo` | Date | Filter to delivery date |
| `page` | Integer | Page number (0-based) |
| `size` | Integer | Page size |
| `sort` | String | Sort field (createdAt, updatedAt, soNumber, orderDate, requestedDeliveryDate, status) |

### 5.3 Request/Response Models

#### 5.3.1 Create Request
```json
{
  "customerId": "uuid",
  "warehouseId": "uuid",
  "orderDate": "2026-03-16",
  "requestedDeliveryDate": "2026-03-23",
  "currency": "USD",
  "notes": "Optional notes",
  "lines": [
    {
      "productId": "uuid",
      "quantityOrdered": 10.00,
      "unitPrice": 25.50,
      "notes": "Optional"
    }
  ]
}
```

#### 5.3.2 Response
```json
{
  "id": "uuid",
  "soNumber": "SO-2026-0001",
  "customerId": "uuid",
  "warehouseId": "uuid",
  "orderDate": "2026-03-16",
  "requestedDeliveryDate": "2026-03-23",
  "status": "DRAFT",
  "subTotal": 255.00,
  "taxAmount": 0.00,
  "totalAmount": 255.00,
  "currency": "USD",
  "notes": "...",
  "confirmedAt": null,
  "confirmedBy": null,
  "createdAt": "2026-03-16T10:00:00",
  "updatedAt": "2026-03-16T10:00:00",
  "lines": [...]
}
```

---

## 6. Business Rules

### 6.1 Creation Rules

| BR-ID | Rule | Description |
|-------|------|-------------|
| BR-SO-01 | SO Number Format | `SO-YYYY-NNNN`, auto-increment per year |
| BR-SO-02 | Customer Required | Must exist, type=CUSTOMER, status=ACTIVE |
| BR-SO-03 | Warehouse Required | Must exist, status=ACTIVE |
| BR-SO-04 | At Least One Line | SO must have at least one SOL |
| BR-SO-05 | Line Valid Product | Each line must reference active product |
| BR-SO-06 | Line Quantity Positive | quantityOrdered > 0 |
| BR-SO-07 | Line Price Non-negative | unitPrice >= 0 |
| BR-SO-08 | Totals Computed | subTotal, taxAmount, totalAmount computed from lines |

### 6.2 Update Rules

| BR-ID | Rule | Description |
|-------|------|-------------|
| BR-SO-09 | DRAFT Only | Only DRAFT SO can be updated |
| BR-SO-10 | No Line Change | Lines managed via SOL APIs |

### 6.3 Delete Rules

| BR-ID | Rule | Description |
|-------|------|-------------|
| BR-SO-11 | DRAFT Only | Only DRAFT SO can be deleted |
| BR-SO-12 | Cascade Delete | Deleting SO deletes all SOL |

### 6.4 Confirm Rules

| BR-ID | Rule | Description |
|-------|------|-------------|
| BR-SO-13 | DRAFT Only | Only DRAFT SO can be confirmed |
| BR-SO-14 | All-or-Nothing | Reserve must succeed for ALL lines or rollback |
| BR-SO-15 | Availability Check | Check available stock before reserve |
| BR-SO-16 | Exclude Quarantine | Available excludes QUARANTINE batches |
| BR-SO-17 | Immutable After Confirm | CONFIRMED SO cannot be edited |

### 6.5 Cancel Rules

| BR-ID | Rule | Description |
|-------|------|-------------|
| BR-SO-18 | CONFIRMED Only | Only CONFIRMED SO can be cancelled |
| BR-SO-19 | No Active Shipment | Cannot cancel if has PICKING/PACKED/SHIPPED shipments |
| BR-SO-20 | All-or-Nothing | Unreserve must succeed for ALL remaining or rollback |
| BR-SO-21 | Audit Trail | Cancelled SO retained for audit |

---

## 7. Error Codes

| Code | Description | HTTP |
|------|-------------|------|
| OUT_001 | Sales order must have at least one line | 400 |
| OUT_002 | Customer not found or not active | 400 |
| OUT_003 | Insufficient stock for product {sku}: available={x}, requested={y} | 400 |
| OUT_004 | Can only confirm DRAFT orders | 400 |
| OUT_005 | Cannot cancel SO with active shipments | 400 |
| OUT_006 | Can only cancel CONFIRMED orders | 400 |
| OUT_007 | Sales order not found | 404 |
| OUT_XXX | Warehouse not found or inactive | 400 |

---

## 8. Integration Points

### 8.1 With Sales Order Lines (SOL)

| SO Operation | SOL Impact |
|--------------|------------|
| Create SO | Create SOLs in same request |
| Update SO | No impact (lines via SOL APIs) |
| Delete SO | CASCADE delete all SOL |
| Confirm SO | Reserve per SOL |
| Cancel SO | Unreserve remaining per SOL |

### 8.2 With Outbound Shipments (OB)

| SO Status | OB Impact |
|-----------|-----------|
| CONFIRMED | Can create shipment |
| PARTIALLY_SHIPPED | Can create additional shipment |
| COMPLETED | Cannot create shipment |
| CANCELLED | Cannot create shipment |

### 8.3 With Inventory

| SO Operation | Inventory Operation | Trigger |
|--------------|-------------------|---------|
| Confirm | `reserve()` | Called per SOL |
| Cancel | `unreserve()` | Called per SOL with remaining qty |

---

## 9. Acceptance Criteria

### 9.1 Create
- [ ] SO created with generated soNumber (SO-YYYY-NNNN)
- [ ] Customer validated (exists, type=CUSTOMER, ACTIVE)
- [ ] Warehouse validated (exists, ACTIVE)
- [ ] At least one line required
- [ ] subTotal computed from lines (qty × price)
- [ ] taxAmount and totalAmount computed
- [ ] Status = DRAFT by default
- [ ] Lines created with quantityShipped = 0

### 9.2 Read
- [ ] List with pagination and filters works
- [ ] Get by ID returns SO with all lines
- [ ] Get by ID returns 404 for non-existent SO

### 9.3 Update
- [ ] Only DRAFT SO can be updated
- [ ] Cannot update lines via this endpoint (use SOL APIs)
- [ ] Updated SO returns 200

### 9.4 Delete
- [ ] Only DRAFT SO can be deleted
- [ ] Deleting SO cascades to delete SOL
- [ ] Deleted SO returns 204

### 9.5 Confirm
- [ ] Only DRAFT SO can be confirmed
- [ ] Availability check for ALL lines before reserve
- [ ] Reserve for ALL lines in single transaction
- [ ] Rollback if any line fails
- [ ] Status changes to CONFIRMED
- [ ] confirmedAt and confirmedBy populated

### 9.6 Cancel
- [ ] Only CONFIRMED SO can be cancelled
- [ ] Check no active shipments before unreserve
- [ ] Unreserve for ALL remaining quantities
- [ ] Status changes to CANCELLED
- [ ] Audit trail preserved

---

## 10. Comparison: SO vs PO (Inbound)

| Aspect | Sales Order (SO) | Purchase Order (PO) |
|--------|------------------|-------------------|
| Direction | Outbound (xuất kho) | Inbound (nhập kho) |
| Partner Type | Customer | Supplier |
| Lifecycle | DRAFT→CONFIRMED→PARTIALLY_SHIPPED→COMPLETED | DRAFT→CONFIRMED→PARTIALLY_RECEIVED→COMPLETED |
| On Confirm | Reserve inventory | No immediate effect |
| On Receipt/Ship | decrease inventory | increase inventory |
| Lines | Sales Order Lines | Purchase Order Lines |
| Reference Doc | Outbound Shipments | Inbound Receipts |
| Status | PARTIALLY_SHIPPED | PARTIALLY_RECEIVED |

---

## 11. Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Mar 16, 2026 | BA | Initial SO business analysis |

---

**Document Status:** ✅ Ready for Development  
**Related Documents:**
- [BA_PHASE6_OUTBOUND_OPERATIONS_COMPLETE.md](./BA_PHASE6_OUTBOUND_OPERATIONS_COMPLETE.md) - Parent document
- [BA_PHASE6_SALES_ORDER_LINES.md](./BA_PHASE6_SALES_ORDER_LINES.md) - SOL module
- [BA_PHASE6_OUTBOUND_SHIPMENT_LINES.md](./BA_PHASE6_OUTBOUND_SHIPMENT_LINES.md) - OBL module
- [DB_MODULE_06_OUTBOUND.md](./DB_MODULE_06_OUTBOUND.md) - Database schema
