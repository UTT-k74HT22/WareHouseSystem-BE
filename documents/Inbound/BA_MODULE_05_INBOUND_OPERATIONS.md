# BA Document - Module 5: Inbound Operations
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Inbound Operations (Purchase Orders & Goods Receipt) |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 📑 Table of Contents

1. [Business Context](#business-context)
2. [Actors & Roles](#actors--roles)
3. [Module Overview](#module-overview)
4. [Feature 1: Purchase Order Management](#feature-1-purchase-order-management)
5. [Feature 2: Goods Receipt](#feature-2-goods-receipt)
6. [Feature 3: Quality Inspection](#feature-3-quality-inspection)
7. [Feature 4: Partial Receipt & Over-Receipt](#feature-4-partial-receipt--over-receipt)
8. [API Impact Summary](#api-impact-summary)
9. [Database Impact Summary](#database-impact-summary)
10. [Integration Points](#integration-points)

---

## 🎯 Business Context

### Current Pain Points

1. **No PO Tracking**: Purchase orders managed in spreadsheets, easy to lose track
2. **Receipt Errors**: Manual data entry causes quantity and product mismatches
3. **No Quality Gate**: Defective goods accepted and put into stock
4. **Inventory Delays**: Stock levels not updated until end of day
5. **Poor Traceability**: Cannot link received goods back to original PO
6. **Batch Confusion**: Batch numbers not captured during receipt

### Business Value

✅ **Real-Time Stock Updates** - Inventory updated immediately upon receipt confirmation  
✅ **Complete Traceability** - Link from PO → Receipt → Batch → Inventory  
✅ **Quality Control** - Inspect goods before accepting into stock  
✅ **Reduce Errors** - System validation prevents incorrect quantities  
✅ **Supplier Performance** - Track on-time delivery and quality issues  
✅ **Compliance** - Full audit trail for regulatory requirements  

---

## 👥 Actors & Roles

| Actor | Goal | Permissions |
|-------|------|-------------|
| **Purchasing Manager** | Create and manage purchase orders | Create/edit POs, view all POs |
| **Warehouse Manager** | Oversee receiving process | View all receipts, approve discrepancies |
| **Receiving Staff** | Process incoming goods | Create receipts, confirm receipts |
| **Quality Inspector** | Inspect received goods | View receipts, mark quality status |
| **Inventory Controller** | Monitor stock increases | View receipts and inventory impact |

---

## 📦 Module Overview

Inbound Operations manages the complete flow from purchase order creation to goods receipt and stock increase.

### Core Entities

1. **Purchase Orders** - Orders placed with suppliers
2. **Purchase Order Lines** - Individual items in PO
3. **Inbound Receipts** - Physical receipt of goods
4. **Inbound Receipt Lines** - Items received

### Process Flow

```
┌──────────────────┐
│  Create PO       │ (DRAFT)
│  Add PO Lines    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Confirm PO      │ (CONFIRMED)
│  Send to Supplier│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Goods Arrive    │
│  Create Receipt  │ (DRAFT)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Quality Check    │ (Optional)
│ Record Batch Info│
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ Confirm Receipt  │ (CONFIRMED)
│ → Update Inventory│
│ → Record Movement│
│ → Notify Users   │
└──────────────────┘
```

### Dependencies

- **Depends on**: Master Data (Warehouses, Locations, Products, Business Partners)
- **Requires**: Batch Management (for batch-tracked products), Inventory Management
- **Triggers**: Stock Movements, Notifications

---

## 📋 Feature 1: Purchase Order Management

### User Stories

**US-INB-01**: As a Purchasing Manager, I want to create purchase orders with multiple line items, so that I can order products from suppliers.

**US-INB-02**: As a Purchasing Manager, I want to confirm a PO before sending to supplier, so that it becomes an official order.

**US-INB-03**: As a Receiving Staff, I want to view confirmed POs, so that I know what deliveries to expect.

**US-INB-04**: As a Purchasing Manager, I want to track PO status (draft, confirmed, partially received, completed), so that I can follow up on pending orders.

---

### Use Cases

#### UC-INB-01: Create Purchase Order

**Brief Description**: Create a new purchase order to order products from a supplier.

**Primary Actor**: Purchasing Manager

**Pre-conditions**:
- User has CREATE_PURCHASE_ORDER permission
- Supplier exists in system
- Products exist in system

**Post-conditions**:
- PO created with DRAFT status
- PO lines linked to PO
- PO can be edited until confirmed

**Main Flow**:
1. User opens Create PO form
2. User selects:
   - Supplier (from business partners with type SUPPLIER)
   - Warehouse (destination)
   - Expected delivery date
   - Payment terms (optional)
3. User adds line items:
   - Product (search/scan)
   - Quantity ordered
   - Unit price
   - Total = quantity × unit price
4. System calculates PO totals:
   - Subtotal = sum of line totals
   - Tax (if applicable)
   - Grand total
5. User saves as DRAFT
6. System generates PO number (e.g., PO-2026-0001)
7. System returns success with PO ID

**Alternative Flows**:
- A1: Import from Excel
  - User uploads Excel file with line items
  - System validates and creates PO with lines

**Business Rules**:
- BR-INB-01: PO number auto-generated and unique
- BR-INB-02: PO must have at least one line item
- BR-INB-03: Expected delivery date cannot be in the past
- BR-INB-04: DRAFT POs can be edited and deleted

---

#### UC-INB-02: Confirm Purchase Order

**Brief Description**: Finalize and confirm PO, making it official and immutable.

**Primary Actor**: Purchasing Manager

**Pre-conditions**:
- PO exists in DRAFT status
- PO has at least one line item
- All required fields filled

**Post-conditions**:
- PO status changed to CONFIRMED
- PO becomes immutable (cannot edit/delete)
- PO ready for receipt creation
- Notification sent to warehouse

**Main Flow**:
1. User views DRAFT PO
2. User reviews all details
3. User clicks "Confirm PO"
4. System validates:
   - At least one line item
   - All quantities > 0
   - All prices valid
5. System updates status to CONFIRMED
6. System records confirmed_at timestamp
7. System sends notification to Warehouse Manager
8. System returns success

**Business Rules**:
- BR-INB-05: Only DRAFT POs can be confirmed
- BR-INB-06: CONFIRMED POs cannot be edited (must cancel and recreate)
- BR-INB-07: Warehouse notified when PO confirmed

---

## 📋 Feature 2: Goods Receipt

### User Stories

**US-INB-05**: As a Receiving Staff, I want to create a goods receipt against a PO, so that I can record what was delivered.

**US-INB-06**: As a Receiving Staff, I want to specify which warehouse location the goods are placed in, so that we can find them later.

**US-INB-07**: As a Receiving Staff, I want to record batch information during receipt, so that batch-tracked products have proper traceability.

**US-INB-08**: As a Receiving Staff, I want to confirm receipt to update inventory, so that stock is immediately available.

---

### Use Cases

#### UC-INB-03: Create Goods Receipt

**Brief Description**: Create a receipt record when goods are delivered by supplier.

**Primary Actor**: Receiving Staff

**Pre-conditions**:
- User has CREATE_RECEIPT permission
- Referenced PO is in CONFIRMED status
- Goods physically delivered

**Post-conditions**:
- Receipt created with DRAFT status
- Receipt lines created
- Receipt can be edited before confirmation

**Main Flow**:
1. Supplier delivers goods
2. Receiving staff opens Create Receipt
3. Staff selects PO (dropdown of confirmed POs)
4. System loads PO lines with:
   - Product
   - Ordered quantity
   - Received so far
   - Remaining to receive
5. For each line, staff enters:
   - Quantity received (this shipment)
   - Location (where to store)
   - Batch info (if batch-tracked):
     - Batch number
     - Manufacturing date
     - Expiry date (if applicable)
   - Quality status (PASS, QUARANTINE)
   - Notes (optional, e.g., damage observations)
6. System validates:
   - Quantity received ≤ ordered quantity (unless over-receipt allowed)
   - Location exists and is ACTIVE
   - Batch info complete for batch-tracked products
7. System saves as DRAFT
8. System generates receipt number (e.g., GR-2026-0001)

**Alternative Flows**:
- A1: Over-receipt (quantity > ordered)
  - If over-receipt policy allows, system shows warning but accepts
  - If not allowed, system rejects with error
- A2: Partial receipt
  - Staff receives only subset of ordered items
  - Remaining quantity tracked for future receipts

**Business Rules**:
- BR-INB-08: Receipt must reference a CONFIRMED PO
- BR-INB-09: Receipt number auto-generated and unique
- BR-INB-10: Batch info mandatory for products with requires_batch_tracking = true
- BR-INB-11: DRAFT receipts can be edited and deleted
- BR-INB-12: Over-receipt policy configurable (default: not allowed)

---

#### UC-INB-04: Confirm Goods Receipt

**Brief Description**: Finalize receipt and update inventory.

**Primary Actor**: Receiving Staff

**Pre-conditions**:
- Receipt exists in DRAFT status
- All required fields completed
- Quality inspection passed (if required)

**Post-conditions**:
- Receipt status changed to CONFIRMED
- Inventory increased for each line
- Batches created (if needed)
- Stock movements recorded
- PO marked PARTIALLY_RECEIVED or COMPLETED
- Notifications sent

**Main Flow**:
1. Staff reviews DRAFT receipt
2. Staff clicks "Confirm Receipt"
3. System validates all lines
4. **Transaction starts**
5. For each receipt line:
   - a. Create batch record (if batch-tracked and new batch)
   - b. Find or create inventory record:
     ```
     inventory(product, warehouse, location, batch)
     ```
   - c. Increase on_hand_quantity by received quantity
   - d. Record stock movement:
     - type = INBOUND
     - reference = receipt number
     - quantity_change = +received quantity
6. System updates PO status:
   - If all lines fully received: status = COMPLETED
   - If partial: status = PARTIALLY_RECEIVED
7. System sets receipt.status = CONFIRMED
8. System sets receipt.confirmed_at = NOW()
9. **Transaction commits**
10. System sends notifications:
    - Inventory Controller: stock increased
    - Purchasing Manager: PO progress update
11. System returns success

**Exception Flows**:
- E1: Database error during transaction
  - Transaction rolls back
  - No inventory changes
  - Receipt remains DRAFT
  - Error message to user

**Business Rules**:
- BR-INB-13: Receipt confirmation is atomic (all lines or none)
- BR-INB-14: Inventory update and stock movement creation in same transaction
- BR-INB-15: PO status updated based on received quantities
- BR-INB-16: CONFIRMED receipts cannot be edited or deleted

---

## 📋 Feature 3: Quality Inspection

### User Stories

**US-INB-09**: As a Quality Inspector, I want to mark receipt lines as QUARANTINE if quality issues found, so that defective stock is not used.

**US-INB-10**: As a Warehouse Manager, I want to be notified when goods are quarantined, so that I can follow up with supplier.

---

### Use Cases

#### UC-INB-05: Quarantine Received Goods

**Brief Description**: Mark receipt line for quarantine due to quality issues.

**Primary Actor**: Quality Inspector, Receiving Staff

**Pre-conditions**:
- Receipt in DRAFT status
- Quality issue identified

**Post-conditions**:
- Receipt line marked with quality_status = QUARANTINE
- When receipt confirmed, batch created with status QUARANTINE
- Stock goes to quarantine (excluded from available)

**Main Flow**:
1. During receipt creation/editing
2. Inspector reviews goods
3. Inspector identifies issue (damage, wrong product, defect)
4. Inspector sets quality_status = QUARANTINE
5. Inspector enters notes explaining issue
6. When receipt confirmed:
   - Batch created with status QUARANTINE
   - Inventory increased but excluded from available (batch status)
   - Notification sent to Warehouse Manager and Purchasing

**Business Rules**:
- BR-INB-17: QUARANTINE goods increase inventory but are not available for sale
- BR-INB-18: Quality issues must be documented in notes
- BR-INB-19: Warehouse Manager notified of all quarantine items

---

## 📋 Feature 4: Partial Receipt & Over-Receipt

### Use Cases

#### UC-INB-06: Partial Receipt

**Brief Description**: Receive only part of ordered quantity in a shipment.

**Main Flow**:
1. Staff creates receipt for PO
2. Staff enters quantity received < quantity ordered
3. System saves as partial receipt
4. When confirmed:
   - PO status = PARTIALLY_RECEIVED
   - PO lines track: ordered, received so far, remaining
5. Future receipts can reference same PO until fully received

**Business Rules**:
- BR-INB-20: Multiple receipts can reference same PO
- BR-INB-21: Total received cannot exceed ordered (unless over-receipt allowed)
- BR-INB-22: PO marked COMPLETED when all lines fully received

---

#### UC-INB-07: Over-Receipt

**Brief Description**: Receive more than ordered (if policy allows).

**Pre-conditions**:
- Over-receipt policy enabled in system configuration

**Main Flow**:
1. Staff receives quantity > ordered quantity
2. System shows warning: "Receiving X more than ordered. Continue?"
3. If staff confirms:
   - Receipt accepted
   - Over-receipt logged in notes
   - Notification sent to Purchasing Manager
4. Inventory increased by actual received quantity

**Business Rules**:
- BR-INB-23: Over-receipt configurable (default: not allowed)
- BR-INB-24: Over-receipt tolerance can be set (e.g., +10%)
- BR-INB-25: All over-receipts logged and notified

---

## 🔗 API Impact Summary

### New Endpoints

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| POST | /api/purchase-orders | Create PO | PURCHASING_MANAGER |
| GET | /api/purchase-orders | List POs | VIEWER |
| GET | /api/purchase-orders/{id} | Get PO details | VIEWER |
| PUT | /api/purchase-orders/{id} | Update PO (DRAFT only) | PURCHASING_MANAGER |
| DELETE | /api/purchase-orders/{id} | Delete PO (DRAFT only) | PURCHASING_MANAGER |
| PUT | /api/purchase-orders/{id}/confirm | Confirm PO | PURCHASING_MANAGER |
| POST | /api/inbound-receipts | Create receipt | RECEIVING_STAFF |
| GET | /api/inbound-receipts | List receipts | VIEWER |
| GET | /api/inbound-receipts/{id} | Get receipt details | VIEWER |
| PUT | /api/inbound-receipts/{id} | Update receipt (DRAFT only) | RECEIVING_STAFF |
| DELETE | /api/inbound-receipts/{id} | Delete receipt (DRAFT only) | RECEIVING_STAFF |
| PUT | /api/inbound-receipts/{id}/confirm | Confirm receipt → update inventory | RECEIVING_STAFF |
| GET | /api/inbound-receipts/by-po/{poId} | Get all receipts for a PO | VIEWER |

---

## 💾 Database Impact Summary

See detailed schema in [DB_MODULE_05_INBOUND.md](./DB_MODULE_05_INBOUND.md)

### New Tables

#### Table: purchase_orders

**Key Fields**:
- `po_number` - Unique identifier (PO-YYYY-NNNN)
- `supplier_id` - FK to business_partners
- `warehouse_id` - Destination warehouse
- `status` - DRAFT, CONFIRMED, PARTIALLY_RECEIVED, COMPLETED, CANCELLED
- `order_date`, `expected_delivery_date`
- `subtotal`, `tax_amount`, `total_amount`
- `confirmed_at`, `confirmed_by`

#### Table: purchase_order_lines

**Key Fields**:
- `purchase_order_id` - FK to purchase_orders
- `product_id` - FK to products
- `quantity_ordered` - Original quantity
- `quantity_received` - Running total of received
- `unit_price`
- `line_total` - quantity × unit_price

#### Table: inbound_receipts

**Key Fields**:
- `receipt_number` - Unique (GR-YYYY-NNNN)
- `purchase_order_id` - FK to purchase_orders
- `warehouse_id`
- `status` - DRAFT, CONFIRMED, CANCELLED
- `receipt_date`
- `confirmed_at`, `confirmed_by`

#### Table: inbound_receipt_lines

**Key Fields**:
- `inbound_receipt_id` - FK to inbound_receipts
- `purchase_order_line_id` - FK to purchase_order_lines
- `product_id`
- `batch_id` - FK to batches (created during confirmation)
- `location_id` - Where stock placed
- `quantity_received`
- `quality_status` - PASS, QUARANTINE
- `notes`

---

## 🔄 Integration Points

### With Batch Management
- If product requires batch tracking, batch created during receipt confirmation
- Batch status set to QUARANTINE if quality_status = QUARANTINE

### With Inventory Management
- Receipt confirmation calls `increaseStock()` for each line
- Inventory records created/updated with received quantities

### With Stock Movement Module
- Each receipt line creates stock movement with type = INBOUND
- Full traceability from PO → Receipt → Movement

### With Notification Module
- PO confirmed: notify warehouse
- Receipt confirmed: notify inventory controller, purchasing
- Quarantine items: notify warehouse manager, purchasing

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-INB-01 | PO number auto-generated and unique |
| BR-INB-02 | PO must have at least one line item |
| BR-INB-03 | Expected delivery date cannot be past |
| BR-INB-04 | DRAFT POs can be edited/deleted |
| BR-INB-05 | Only DRAFT POs can be confirmed |
| BR-INB-06 | CONFIRMED POs cannot be edited |
| BR-INB-07 | Warehouse notified when PO confirmed |
| BR-INB-08 | Receipt must reference CONFIRMED PO |
| BR-INB-09 | Receipt number auto-generated and unique |
| BR-INB-10 | Batch info mandatory for batch-tracked products |
| BR-INB-11 | DRAFT receipts can be edited/deleted |
| BR-INB-12 | Over-receipt policy configurable |
| BR-INB-13 | Receipt confirmation is atomic |
| BR-INB-14 | Inventory update in same transaction |
| BR-INB-15 | PO status updated based on received quantities |
| BR-INB-16 | CONFIRMED receipts immutable |
| BR-INB-17 | QUARANTINE goods not available for sale |
| BR-INB-18 | Quality issues documented |
| BR-INB-19 | Manager notified of quarantines |
| BR-INB-20 | Multiple receipts per PO allowed |
| BR-INB-21 | Total received ≤ ordered (unless over-receipt) |
| BR-INB-22 | PO completed when fully received |
| BR-INB-23 | Over-receipt configurable |
| BR-INB-24 | Over-receipt tolerance configurable |
| BR-INB-25 | All over-receipts logged |

---

## ✅ Acceptance Criteria

1. ✅ Can create and confirm purchase orders
2. ✅ Can create goods receipts against confirmed POs
3. ✅ Batch information captured for batch-tracked products
4. ✅ Inventory updated atomically on receipt confirmation
5. ✅ Partial receipts supported (multiple receipts per PO)
6. ✅ Quality inspection with quarantine capability
7. ✅ Over-receipt handled per policy
8. ✅ Complete audit trail (PO → Receipt → Inventory → Movement)
9. ✅ Real-time notifications to stakeholders

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
