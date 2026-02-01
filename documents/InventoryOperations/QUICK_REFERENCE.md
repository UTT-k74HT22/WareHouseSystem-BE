# Quick Reference - Module 03 Correct Schema
## For Implementation Team

---

## ✅ Correct Table Names (USE THESE!)

| Module 03 Entity | Database Table Name | Status |
|------------------|---------------------|--------|
| Inbound Receipt | `inbound_receipts` | ✅ Use this |
| Inbound Receipt Line | `inbound_receipt_lines` | ✅ Use this |
| Outbound Shipment | `outbound_shipments` | ✅ Use this |
| Outbound Shipment Line | `outbound_shipment_lines` | ✅ Use this |
| Stock Movement | `stock_movements` | ✅ Use this |
| Inventory Adjustment | `inventory_adjustments` | ✅ Already exists |
| Stock Transfer | `stock_transfers` | ✅ Create new |
| Batch | `batches` | ✅ Already exists |
| Inventory | `inventory` | ✅ Already exists, add reserved_quantity |

---

## ✅ Correct Java Class Names

```java
// Entities
InboundReceipt.java
InboundReceiptLine.java
OutboundShipment.java
OutboundShipmentLine.java
StockMovement.java
InventoryAdjustment.java
StockTransfer.java

// Repositories
InboundReceiptRepository.java
InboundReceiptLineRepository.java
OutboundShipmentRepository.java
OutboundShipmentLineRepository.java
StockMovementRepository.java
InventoryAdjustmentRepository.java
StockTransferRepository.java

// Services
InboundReceiptService.java + InboundReceiptServiceImpl.java
OutboundShipmentService.java + OutboundShipmentServiceImpl.java
StockMovementService.java + StockMovementServiceImpl.java
InventoryAdjustmentService.java + InventoryAdjustmentServiceImpl.java
StockTransferService.java + StockTransferServiceImpl.java

// Controllers
InboundReceiptController.java
OutboundShipmentController.java
StockMovementController.java
InventoryAdjustmentController.java
StockTransferController.java
```

---

## ✅ Correct API Endpoints

```
# Inbound Operations
GET    /api/inbound-receipts
POST   /api/inbound-receipts
GET    /api/inbound-receipts/{id}
PUT    /api/inbound-receipts/{id}
DELETE /api/inbound-receipts/{id}
POST   /api/inbound-receipts/{id}/confirm
POST   /api/inbound-receipts/{id}/complete

# Outbound Operations
GET    /api/outbound-shipments
POST   /api/outbound-shipments
GET    /api/outbound-shipments/{id}
PUT    /api/outbound-shipments/{id}
DELETE /api/outbound-shipments/{id}
POST   /api/outbound-shipments/{id}/pick
POST   /api/outbound-shipments/{id}/complete-picking
POST   /api/outbound-shipments/{id}/ship

# Stock Movements
GET    /api/stock-movements
GET    /api/stock-movements/{id}
GET    /api/stock-movements/export

# Inventory Adjustments
GET    /api/inventory-adjustments
POST   /api/inventory-adjustments
GET    /api/inventory-adjustments/{id}
PUT    /api/inventory-adjustments/{id}
DELETE /api/inventory-adjustments/{id}
POST   /api/inventory-adjustments/{id}/approve
POST   /api/inventory-adjustments/{id}/reject
POST   /api/inventory-adjustments/{id}/complete

# Stock Transfers
GET    /api/stock-transfers
POST   /api/stock-transfers
GET    /api/stock-transfers/{id}
PUT    /api/stock-transfers/{id}
DELETE /api/stock-transfers/{id}
POST   /api/stock-transfers/{id}/confirm
POST   /api/stock-transfers/{id}/complete
```

---

## ✅ Correct Status ENUMs

### InboundReceiptStatus
```java
public enum InboundReceiptStatus {
    DRAFT,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
```

### OutboundShipmentStatus
```java
public enum OutboundShipmentStatus {
    DRAFT,
    PICKING,
    PICKED,
    SHIPPED,
    CANCELLED
}
```

### InventoryAdjustmentStatus
```java
public enum InventoryAdjustmentStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    COMPLETED
}
```

### StockTransferStatus
```java
public enum StockTransferStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
```

### StockMovementType
```java
public enum StockMovementType {
    INBOUND,
    OUTBOUND,
    ADJUSTMENT,
    TRANSFER,
    RETURN
}
```

---

## ✅ Key Database Differences from BA Doc

### 1. NO separate stock_reservations table
- Use `inventory.reserved_quantity` instead
- Available quantity = on_hand_quantity - reserved_quantity (computed column)

### 2. NO inventory_adjustment_lines table
- Each adjustment is a single record (one product, one location)
- To adjust multiple products → create multiple adjustment records

### 3. Batch tracking uses batches table
- `inbound_receipt_lines.batch_id` → FK to `batches` table
- Batch info (manufacturing_date, expiry_date) stored in `batches` table
- NOT stored as varchar fields in receipt lines

### 4. Purchase Orders & Sales Orders are REQUIRED
- `inbound_receipts.purchase_order_id` is NOT NULL
- `outbound_shipments.sales_order_id` is NOT NULL
- These are already defined in core schema

---

## ❌ WRONG Names (DO NOT USE!)

| ❌ WRONG | ✅ CORRECT |
|---------|-----------|
| `goods_receipts` | `inbound_receipts` |
| `goods_receipt_lines` | `inbound_receipt_lines` |
| `shipments` | `outbound_shipments` |
| `shipment_lines` | `outbound_shipment_lines` |
| `GoodsReceipt` class | `InboundReceipt` class |
| `Shipment` class | `OutboundShipment` class |
| `/api/goods-receipts` | `/api/inbound-receipts` |
| `/api/shipments` | `/api/outbound-shipments` |

---

## 📋 Flyway Migration Files to Create

```
V20260202_01__create_inbound_receipts.sql
V20260202_02__create_inbound_receipt_lines.sql
V20260202_03__create_outbound_shipments.sql
V20260202_04__create_outbound_shipment_lines.sql
V20260202_05__create_stock_transfers.sql
V20260202_06__alter_inventory_add_reserved_quantity.sql
```

---

## 🎯 Implementation Priority

### Phase 1: Core Tables (Week 1)
1. Create `inbound_receipts` table
2. Create `inbound_receipt_lines` table
3. Create `outbound_shipments` table
4. Create `outbound_shipment_lines` table
5. Alter `inventory` table (add reserved_quantity)

### Phase 2: Audit & Transfers (Week 2)
6. Verify `stock_movements` table exists
7. Create `stock_transfers` table

### Phase 3: Integrations (Week 3)
8. Verify `purchase_orders` table exists
9. Verify `sales_orders` table exists
10. Verify `batches` table exists

---

## 📞 Questions? Contact

- **Database Schema Issues**: Check `04_DATABASE_SCHEMA.md`
- **Business Logic Issues**: Check `BA_MODULE_03_INVENTORY_OPERATIONS.md`
- **Implementation Issues**: Check `MODULE_03_BASE.md`
- **Alignment Issues**: Check `SCHEMA_ALIGNMENT_SUMMARY.md`

---

**Last Updated**: February 01, 2026  
**Status**: ✅ Ready for Implementation
