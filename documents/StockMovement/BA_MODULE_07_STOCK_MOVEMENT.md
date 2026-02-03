# BA Document - Module 7: Stock Movement & Audit Trail
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Stock Movement & Audit Trail |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 🎯 Business Context

### Current Pain Points

1. **No Audit Trail**: Cannot track who changed inventory and when
2. **Missing Traceability**: Cannot trace product from supplier to customer
3. **Compliance Issues**: Cannot provide movement history for regulatory audits
4. **Reconciliation Problems**: Discrepancies between physical and system stock cannot be investigated
5. **No Root Cause Analysis**: Cannot identify where shrinkage occurs

### Business Value

✅ **Complete Traceability** - Track every movement from receipt to shipment  
✅ **Regulatory Compliance** - Provide full audit trail for inspections  
✅ **Shrinkage Detection** - Identify where stock losses occur  
✅ **Dispute Resolution** - Prove delivery to customers with batch traceability  
✅ **Quality Control** - Trace defective batches back to supplier  
✅ **Inventory Reconciliation** - Understand all quantity changes  

---

## 📦 Module Overview

### Core Entity: stock_movements

**Purpose:** Immutable log of every inventory change

**Created by:**
- Inbound receipt confirmation (INBOUND)
- Outbound shipment confirmation (OUTBOUND)
- Stock adjustments (ADJUSTMENT_INCREASE, ADJUSTMENT_DECREASE)
- Stock transfers (TRANSFER_OUT, TRANSFER_IN)
- Order confirmation (RESERVE)
- Order cancellation (UNRESERVE)

### Movement Types

| Type | Description | Quantity Change | Triggered By |
|------|-------------|-----------------|--------------|
| INBOUND | Goods received | + | Inbound receipt confirmation |
| OUTBOUND | Goods shipped | - | Outbound shipment confirmation |
| ADJUSTMENT_INCREASE | Manual increase | + | Stock adjustment (approved) |
| ADJUSTMENT_DECREASE | Manual decrease | - | Stock adjustment (approved) |
| TRANSFER_OUT | Moved from location | - (from source) | Stock transfer |
| TRANSFER_IN | Moved to location | + (to destination) | Stock transfer |
| RESERVE | Stock allocated to order | 0 (reserved +) | Sales order confirmation |
| UNRESERVE | Allocation released | 0 (reserved -) | Order cancellation |

---

## 📋 Features

### Feature 1: Automatic Movement Recording

**US-MOV-01**: As a System, I want to automatically create stock movement records for every inventory change, so that complete audit trail is maintained.

**How it works:**
1. Any operation that changes inventory calls `stockMovementService.recordMovement()`
2. System creates immutable movement record with:
   - Movement type
   - Product, warehouse, location, batch
   - Quantity change (+ or -)
   - Quantity before and after
   - Reference (PO number, SO number, adjustment number, etc.)
   - Timestamp and user
3. Record saved in transaction with inventory change (atomic)

**Business Rules:**
- BR-MOV-01: Movement records are immutable (insert-only, never update/delete)
- BR-MOV-02: Movement creation in same transaction as inventory change
- BR-MOV-03: Movement must reference source document

---

### Feature 2: Movement History Query

**US-MOV-02**: As an Inventory Controller, I want to view all movements for a product, so that I can understand stock changes.

**US-MOV-03**: As an Auditor, I want to filter movements by date range, type, and user, so that I can investigate specific transactions.

**UC-MOV-01: View Movement History**
- Filter by:
  - Product (required)
  - Warehouse (optional)
  - Location (optional)
  - Batch (optional)
  - Movement type (optional)
  - Date range (optional)
  - User (optional)
- Display:
  - Timestamp
  - Movement type
  - Quantity change
  - Quantity before/after
  - Reference document (clickable link)
  - User who triggered
  - Notes
- Sort by: timestamp DESC (newest first)
- Export to Excel/PDF

**Business Rules:**
- BR-MOV-04: Movement history accessible to users with VIEW_MOVEMENTS permission
- BR-MOV-05: Movement records retained indefinitely (no deletion)

---

### Feature 3: Traceability Reports

**US-MOV-04**: As a Compliance Officer, I want to trace a batch from supplier to all customers who received it (forward trace), so that I can execute targeted recalls.

**US-MOV-05**: As a Quality Manager, I want to trace a batch backward to the original supplier, so that I can investigate quality issues.

**UC-MOV-02: Forward Traceability (Batch → Customers)**
- Input: Batch ID
- Output:
  - Inbound receipt (where received, from which supplier, PO reference)
  - All inventory locations where batch is/was stored
  - All shipments that included this batch
  - Customers who received (customer name, shipment date, quantity, SO reference)
- Use case: Product recall

**UC-MOV-03: Backward Traceability (Customer → Batch → Supplier)**
- Input: Sales Order or Customer
- Output:
  - Which batches were shipped
  - For each batch: supplier, receipt date, PO number
- Use case: Customer complaint investigation

**Business Rules:**
- BR-MOV-06: Forward trace shows all downstream customers
- BR-MOV-07: Backward trace shows original supplier
- BR-MOV-08: Traceability includes all intermediate movements (transfers, adjustments)

---

### Feature 4: Movement Analytics

**US-MOV-06**: As a Warehouse Manager, I want to see movement summary by type and period, so that I can analyze warehouse activity.

**UC-MOV-04: Movement Analytics Dashboard**
- Show in selected period (week/month/quarter):
  - Total inbound quantity and transactions
  - Total outbound quantity and transactions
  - Total adjustments (separate increase/decrease)
  - Total transfers
  - Top 10 products by movement frequency
  - Movement trends (line chart)
- Filter by warehouse

**Business Rules:**
- BR-MOV-09: Analytics based on movement_date, not created_at
- BR-MOV-10: Exclude RESERVE/UNRESERVE from quantity analytics (no physical movement)

---

## 🔗 API Impact Summary

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | /api/stock-movements | List movements with filters | VIEWER |
| GET | /api/stock-movements/{id} | Get movement details | VIEWER |
| GET | /api/stock-movements/by-product/{productId} | Product movement history | VIEWER |
| GET | /api/stock-movements/by-batch/{batchId} | Batch movement history | VIEWER |
| GET | /api/stock-movements/traceability/forward/{batchId} | Forward trace report | COMPLIANCE_OFFICER |
| GET | /api/stock-movements/traceability/backward/{orderId} | Backward trace report | COMPLIANCE_OFFICER |
| GET | /api/stock-movements/analytics | Movement analytics | WAREHOUSE_MANAGER |
| GET | /api/stock-movements/export | Export to Excel/PDF | VIEWER |

---

## 💾 Database Impact

See [DB_MODULE_07_STOCK_MOVEMENT.md](./DB_MODULE_07_STOCK_MOVEMENT.md)

### Table: stock_movements

**Key Fields:**
- `id` - UUID
- `movement_type` - Enum (INBOUND, OUTBOUND, ADJUSTMENT_INCREASE, etc.)
- `product_id`, `warehouse_id`, `location_id`, `batch_id` - FKs
- `quantity_change` - Can be positive or negative
- `quantity_before` - Quantity before this movement
- `quantity_after` - Quantity after this movement
- `movement_date` - Business date (may differ from created_at)
- `reference_type` - PURCHASE_ORDER, SALES_ORDER, ADJUSTMENT, TRANSFER
- `reference_id` - FK to reference document
- `reference_number` - Human-readable (PO-2026-0001, SO-2026-0001)
- `notes` - Additional context
- `created_at` - Record creation timestamp (immutable)
- `created_by` - User who triggered movement

**Constraints:**
- All fields NOT NULL except notes
- No UPDATE allowed (application-level enforcement)
- No DELETE allowed (database permission)

**Indexes:**
- idx on (product_id, movement_date) for product history
- idx on (batch_id) for batch traceability
- idx on (warehouse_id, movement_date) for warehouse analytics
- idx on (reference_type, reference_id) for lookups
- idx on (movement_type, movement_date) for analytics

---

## 🔄 Integration Points

### With Inbound Module
- Receipt confirmation creates INBOUND movements
- One movement per receipt line

### With Outbound Module
- Order confirmation creates RESERVE movements
- Shipment confirmation creates OUTBOUND movements
- Order cancellation creates UNRESERVE movements

### With Inventory Module
- Adjustment approval creates ADJUSTMENT_INCREASE or ADJUSTMENT_DECREASE movements
- Transfer creates two movements: TRANSFER_OUT and TRANSFER_IN

### With Batch Module
- All batch-tracked product movements include batch_id
- Traceability queries join with batches table

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-MOV-01 | Movement records immutable |
| BR-MOV-02 | Movement creation atomic with inventory change |
| BR-MOV-03 | Must reference source document |
| BR-MOV-04 | Accessible with VIEW_MOVEMENTS permission |
| BR-MOV-05 | Retained indefinitely |
| BR-MOV-06 | Forward trace shows all customers |
| BR-MOV-07 | Backward trace shows supplier |
| BR-MOV-08 | Includes all intermediate movements |
| BR-MOV-09 | Analytics use movement_date |
| BR-MOV-10 | RESERVE/UNRESERVE excluded from quantity analytics |

---

## ✅ Acceptance Criteria

1. ✅ Every inventory change creates movement record
2. ✅ Movement records immutable (no updates/deletes)
3. ✅ Complete product movement history available
4. ✅ Batch forward traceability (supplier to customers)
5. ✅ Batch backward traceability (customer to supplier)
6. ✅ Movement analytics dashboard functional
7. ✅ Export to Excel/PDF works
8. ✅ Performance: Query product history < 200ms

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
