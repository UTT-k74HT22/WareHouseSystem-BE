# BA Document - Module 6: Outbound Operations
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Outbound Operations (Sales Orders & Shipments) |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 🎯 Business Context

### Current Pain Points

1. **No Order Tracking**: Sales orders tracked in spreadsheets, prone to errors
2. **Double Allocation**: Same stock promised to multiple customers
3. **Picking Errors**: Wrong products or quantities picked
4. **No FIFO Enforcement**: Oldest stock not picked first, leading to expiry
5. **Delayed Updates**: Inventory not updated until end of day
6. **Poor Customer Communication**: Cannot provide accurate ETAs

### Business Value

✅ **Prevent Overselling** - Stock reserved when order confirmed  
✅ **Accurate Picking** - System-generated pick lists reduce errors  
✅ **FIFO Compliance** - Recommendations ensure oldest stock shipped first  
✅ **Real-Time Updates** - Inventory decreased immediately on shipment  
✅ **Customer Satisfaction** - Accurate order status and tracking  
✅ **Complete Traceability** - Track which batch shipped to which customer  

---

## 📦 Module Overview

### Core Entities
1. **Sales Orders** - Customer orders
2. **Sales Order Lines** - Items ordered
3. **Outbound Shipments** - Physical shipments to customers
4. **Outbound Shipment Lines** - Items shipped

### Process Flow

```
Create Sales Order (DRAFT)
    ↓
Confirm Sales Order (CONFIRMED)
    → Reserve Stock
    ↓
Create Shipment (DRAFT)
    → Generate Pick List with FIFO recommendations
    ↓
Pick Items (PICKING)
    → Scan/verify items
    ↓
Pack & Ship (PACKED)
    ↓
Confirm Shipment (SHIPPED)
    → Decrease inventory (on-hand & reserved)
    → Record movement
    → Update sales order status
    → Notify customer
```

---

## 📋 Features

### Feature 1: Sales Order Management

**US-OUT-01**: As a Sales Manager, I want to create sales orders with multiple line items, so that I can process customer orders.

**US-OUT-02**: As a Sales Manager, I want to confirm a sales order to reserve stock, so that inventory is allocated to the customer.

**UC-OUT-01: Create Sales Order**
- Select customer (business partner with type CUSTOMER)
- Add line items (product, quantity, price)
- Calculate totals
- Save as DRAFT (editable)

**UC-OUT-02: Confirm Sales Order**
- Validate stock availability for all lines
- Reserve stock (call inventory.reserveStock())
- Update status to CONFIRMED
- Send confirmation to warehouse
- Generate order confirmation email to customer

**Business Rules:**
- BR-OUT-01: SO number auto-generated (SO-YYYY-NNNN)
- BR-OUT-02: Cannot confirm if insufficient stock
- BR-OUT-03: Reservation atomic (all lines or none)
- BR-OUT-04: CONFIRMED SOs cannot be edited (must cancel)

---

### Feature 2: Shipment Processing

**US-OUT-03**: As Warehouse Staff, I want to create a shipment against a sales order, so that I can prepare items for shipping.

**US-OUT-04**: As a Picker, I want to see FIFO recommendations, so that I ship oldest batches first.

**US-OUT-05**: As Warehouse Staff, I want to confirm shipment to update inventory, so that stock levels are accurate.

**UC-OUT-03: Create Shipment & Generate Pick List**
- Select CONFIRMED sales order
- System suggests batches in FIFO order (oldest manufacturing date first)
- Staff can override but system warns
- Assign to picker
- Print/display pick list with locations

**UC-OUT-04: Pick Items**
- Picker scans items from pick list
- System verifies correct product, quantity, batch
- Mark as PICKING status

**UC-OUT-05: Confirm Shipment**
- Validate all items picked
- Transaction starts:
  - Decrease inventory (on-hand - shipped qty)
  - Decrease reserved (reserved - shipped qty)
  - Record stock movement (OUTBOUND)
  - Update SO line shipped_quantity
  - Update SO status (PARTIALLY_SHIPPED or COMPLETED)
- Transaction commits
- Update shipment status to SHIPPED
- Notify customer with tracking info

**Business Rules:**
- BR-OUT-05: Shipment number auto-generated (SHIP-YYYY-NNNN)
- BR-OUT-06: Cannot ship more than ordered quantity
- BR-OUT-07: FIFO recommended but not enforced
- BR-OUT-08: Shipment confirmation is atomic
- BR-OUT-09: Both on-hand and reserved decreased on shipment

---

### Feature 3: Partial Shipments

**US-OUT-06**: As Warehouse Staff, I want to ship partial quantities when full order not available, so that customers receive what's ready.

**UC-OUT-06: Partial Shipment**
- Create shipment with quantity < ordered quantity
- Remaining quantity stays reserved
- SO status = PARTIALLY_SHIPPED
- Future shipments can fulfill remaining

**Business Rules:**
- BR-OUT-10: Multiple shipments per SO allowed
- BR-OUT-11: Total shipped ≤ ordered quantity
- BR-OUT-12: SO marked COMPLETED when all lines fully shipped

---

### Feature 4: Order Cancellation

**US-OUT-07**: As Sales Manager, I want to cancel an order, so that reserved stock is released.

**UC-OUT-07: Cancel Sales Order**
- Only CONFIRMED SOs (not yet shipped) can be cancelled
- Unreserve all reserved stock
- Update status to CANCELLED
- Send cancellation notification

**Business Rules:**
- BR-OUT-13: Cannot cancel after shipment created
- BR-OUT-14: Cancellation releases all reservations
- BR-OUT-15: Cancelled orders retained for audit

---

## 🔗 API Impact Summary

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | /api/sales-orders | Create SO | SALES_MANAGER |
| GET | /api/sales-orders | List SOs | VIEWER |
| GET | /api/sales-orders/{id} | Get SO details | VIEWER |
| PUT | /api/sales-orders/{id} | Update SO (DRAFT only) | SALES_MANAGER |
| PUT | /api/sales-orders/{id}/confirm | Confirm SO → reserve stock | SALES_MANAGER |
| PUT | /api/sales-orders/{id}/cancel | Cancel SO → release stock | SALES_MANAGER |
| POST | /api/outbound-shipments | Create shipment | WAREHOUSE_STAFF |
| GET | /api/outbound-shipments | List shipments | VIEWER |
| GET | /api/outbound-shipments/{id} | Get shipment details | VIEWER |
| PUT | /api/outbound-shipments/{id}/pick | Mark as picking | WAREHOUSE_STAFF |
| PUT | /api/outbound-shipments/{id}/confirm | Confirm shipment → decrease stock | WAREHOUSE_STAFF |
| GET | /api/outbound-shipments/{id}/pick-list | Generate pick list PDF | WAREHOUSE_STAFF |

---

## 💾 Database Impact

See [DB_MODULE_06_OUTBOUND.md](./DB_MODULE_06_OUTBOUND.md)

### New Tables

#### sales_orders
- `so_number` (SO-YYYY-NNNN)
- `customer_id` FK to business_partners
- `warehouse_id`
- `status` (DRAFT, CONFIRMED, PARTIALLY_SHIPPED, COMPLETED, CANCELLED)
- `order_date`, `requested_delivery_date`
- Financial fields

#### sales_order_lines
- `sales_order_id` FK
- `product_id`
- `quantity_ordered`, `quantity_shipped`, `quantity_remaining`
- `unit_price`, `line_total`

#### outbound_shipments
- `shipment_number` (SHIP-YYYY-NNNN)
- `sales_order_id` FK
- `warehouse_id`
- `status` (DRAFT, PICKING, PACKED, SHIPPED, CANCELLED)
- `shipped_at`, `tracking_number`

#### outbound_shipment_lines
- `outbound_shipment_id` FK
- `sales_order_line_id` FK
- `product_id`, `batch_id`, `location_id`
- `quantity_shipped`
- `picked_by`, `picked_at`

---

## 🔄 Integration Points

### With Inventory Module
- SO confirmation: `reserveStock()` for each line
- Shipment confirmation: `decreaseStock()` (both on-hand and reserved)
- Cancellation: `unreserveStock()`

### With Batch Module
- FIFO recommendations query batches ordered by manufacturing_date
- Only AVAILABLE batches included in recommendations

### With Stock Movement Module
- Shipment confirmation creates OUTBOUND movements
- Cancellation creates UNRESERVE movements

### With Notification Module
- SO confirmed: notify warehouse, customer
- Shipment created: notify picker
- Shipment shipped: notify customer with tracking

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-OUT-01 | SO number auto-generated |
| BR-OUT-02 | Cannot confirm without sufficient stock |
| BR-OUT-03 | Reservation atomic |
| BR-OUT-04 | CONFIRMED SOs immutable |
| BR-OUT-05 | Shipment number auto-generated |
| BR-OUT-06 | Cannot ship more than ordered |
| BR-OUT-07 | FIFO recommended not enforced |
| BR-OUT-08 | Shipment confirmation atomic |
| BR-OUT-09 | Decrease both on-hand and reserved |
| BR-OUT-10 | Multiple shipments per SO |
| BR-OUT-11 | Total shipped ≤ ordered |
| BR-OUT-12 | SO completed when fully shipped |
| BR-OUT-13 | Cannot cancel after shipment |
| BR-OUT-14 | Cancellation releases reservations |
| BR-OUT-15 | Cancelled orders retained |

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
