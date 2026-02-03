# BA Document - Module 4: Inventory Management
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Inventory Management |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 📑 Table of Contents

1. [Business Context](#business-context)
2. [Actors & Roles](#actors--roles)
3. [Module Overview](#module-overview)
4. [Feature 1: Real-Time Inventory Tracking](#feature-1-real-time-inventory-tracking)
5. [Feature 2: Stock Reservation](#feature-2-stock-reservation)
6. [Feature 3: Stock Adjustment](#feature-3-stock-adjustment)
7. [Feature 4: Stock Transfer Between Locations](#feature-4-stock-transfer-between-locations)
8. [Feature 5: Low Stock Alerts](#feature-5-low-stock-alerts)
9. [API Impact Summary](#api-impact-summary)
10. [Database Impact Summary](#database-impact-summary)
11. [Integration Points](#integration-points)

---

## 🎯 Business Context

### Current Pain Points

1. **Inventory Inaccuracy**: Physical stock doesn't match system records, leading to overselling or stock-outs
2. **No Real-Time Visibility**: Managers cannot see current stock levels across locations in real-time
3. **Double Allocation**: Same stock allocated to multiple orders, causing fulfillment issues
4. **Manual Stock Counts**: Time-consuming physical counts with high error rates
5. **Location Confusion**: Items stored but location not tracked, wasting time searching
6. **No Alert System**: Stock-outs discovered only when customer orders cannot be fulfilled

### Business Value

✅ **99%+ Inventory Accuracy** - Eliminate discrepancies between physical and system stock  
✅ **Real-Time Visibility** - Instant view of stock levels across all warehouses and locations  
✅ **Prevent Overselling** - Reserved stock ensures orders can be fulfilled  
✅ **Optimize Storage** - Track exactly where items are stored for faster picking  
✅ **Proactive Replenishment** - Auto-alerts when stock falls below minimum levels  
✅ **Reduce Stock-Outs** - Maintain optimal stock levels based on min/max thresholds  

---

## 👥 Actors & Roles

| Actor | Goal | Permissions |
|-------|------|-------------|
| **Warehouse Manager** | Monitor overall inventory health, approve adjustments | View all inventory, approve stock adjustments |
| **Inventory Controller** | Maintain accurate inventory records | Create/update inventory, perform stock counts |
| **Warehouse Staff** | Process receipts and shipments | View inventory, update locations |
| **Purchasing Manager** | Identify reorder needs | View inventory levels, receive low-stock alerts |
| **Sales Manager** | Check product availability | View available inventory (read-only) |

---

## 📦 Module Overview

Inventory Management is the core module that tracks the quantity and location of all products in real-time. It maintains separate tracking for on-hand, reserved, and available stock.

### Core Entities

1. **Inventory** - Stock records per product-warehouse-location-batch combination
2. **Stock Adjustments** - Manual corrections to inventory with approval workflow
3. **Stock Transfers** - Movement of stock between locations within same warehouse
4. **Stock Alerts** - Low stock notifications

### Key Concepts

- **On-Hand Quantity**: Physical stock in warehouse (total)
- **Reserved Quantity**: Stock allocated to confirmed orders (not yet shipped)
- **Available Quantity**: On-hand minus reserved (can be allocated to new orders)
- **Stock Adjustment**: Manual correction with reason and approval
- **Cycle Counting**: Regular counting of subset of products for accuracy

### Dependencies

- **Depends on**: Master Data (Warehouses, Locations, Products, Batches)
- **Required by**: Inbound, Outbound, Reporting modules

---

## 📋 Feature 1: Real-Time Inventory Tracking

### User Stories

**US-INV-01**: As a Warehouse Manager, I want to view real-time stock levels for any product across all locations, so that I can make informed decisions about stock allocation.

**US-INV-02**: As a Sales Manager, I want to check available quantity before promising delivery to customers, so that I avoid overselling.

**US-INV-03**: As an Inventory Controller, I want to see inventory broken down by batch and location, so that I can identify oldest stock (FIFO) and optimize picking.

**US-INV-04**: As a Warehouse Staff, I want to quickly check if a product is in stock at a specific location, so that I can direct pickers efficiently.

---

### Use Cases

#### UC-INV-01: View Inventory Summary

**Brief Description**: Display aggregated inventory across all warehouses and locations for a product.

**Primary Actor**: Warehouse Manager, Sales Manager, Inventory Controller

**Pre-conditions**:
- User is authenticated
- User has VIEW_INVENTORY permission

**Post-conditions**:
- Inventory summary displayed

**Main Flow**:
1. User opens Inventory Dashboard
2. User searches by product (SKU, name, or barcode scan)
3. System displays summary:
   - Total on-hand quantity across all locations
   - Total reserved quantity
   - Total available quantity
   - Breakdown by warehouse
   - Breakdown by location within each warehouse
   - Breakdown by batch (if batch-tracked)
   - Last movement date/time
4. User can drill down into specific warehouse/location
5. System shows detailed view with:
   - Specific location code
   - Quantity at that location
   - Batch information (if applicable)
   - Days since last movement

**Business Rules**:
- BR-INV-01: Available = On-Hand - Reserved
- BR-INV-02: Real-time data must be accurate to within 1 second
- BR-INV-03: Inventory records created automatically on first receipt

---

#### UC-INV-02: Query Available Stock

**Brief Description**: Check if sufficient stock is available for order fulfillment.

**Primary Actor**: Sales Manager, Outbound Service (System)

**Pre-conditions**:
- User or system needs to check availability
- Product and warehouse specified

**Post-conditions**:
- Availability confirmed or insufficient stock indicated

**Main Flow**:
1. System receives availability query:
   - product_id
   - warehouse_id
   - requested_quantity
   - location_id (optional)
   - batch_id (optional)
2. System queries inventory:
   ```
   SELECT SUM(on_hand_quantity - reserved_quantity) AS available
   FROM inventory
   WHERE product_id = ? 
     AND warehouse_id = ?
     AND (location_id = ? OR ? IS NULL)
     AND (batch_id = ? OR ? IS NULL)
   ```
3. System compares available vs requested
4. System returns result:
   - `{ "available": true, "quantity_available": 150 }`
   - OR `{ "available": false, "quantity_available": 50, "shortfall": 50 }`

**Business Rules**:
- BR-INV-04: Only AVAILABLE batches counted (exclude QUARANTINE, EXPIRED)
- BR-INV-05: Negative stock not allowed (enforced by constraints)

---

## 📋 Feature 2: Stock Reservation

### User Stories

**US-INV-05**: As a System, I want to reserve stock when a sales order is confirmed, so that the same stock is not allocated to multiple orders.

**US-INV-06**: As a Warehouse Manager, I want to view reserved quantities, so that I understand why available stock differs from on-hand stock.

**US-INV-07**: As a System, I want to automatically release reservations if an order is cancelled, so that stock becomes available again.

---

### Use Cases

#### UC-INV-03: Reserve Stock

**Brief Description**: Allocate stock to a confirmed sales order, preventing it from being allocated elsewhere.

**Primary Actor**: Outbound Service (System)

**Pre-conditions**:
- Sales order confirmed
- Sufficient available stock exists
- Stock is in AVAILABLE status (for batches)

**Post-conditions**:
- Reserved quantity increased
- Available quantity decreased
- Reservation logged in audit trail

**Main Flow**:
1. Outbound service calls `reserveStock(productId, warehouseId, locationId, batchId, quantity)`
2. System checks available stock:
   ```sql
   SELECT on_hand_quantity - reserved_quantity AS available
   FROM inventory
   WHERE product_id = ? AND warehouse_id = ? 
     AND location_id = ? AND batch_id = ?
   FOR UPDATE; -- Lock row for concurrency control
   ```
3. System validates: `available >= quantity`
4. If sufficient:
   ```sql
   UPDATE inventory
   SET reserved_quantity = reserved_quantity + ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
5. System records stock movement:
   - movement_type = 'RESERVE'
   - quantity_change = +quantity (on reserved)
6. System commits transaction
7. System returns success

**Alternative Flows**:
- A1: Insufficient stock
  - System returns error: `InsufficientStockException`
  - Transaction rolled back

**Exception Flows**:
- E1: Concurrent reservation (another order just reserved same stock)
  - Optimistic lock version mismatch
  - Transaction retried or returns conflict error

**Business Rules**:
- BR-INV-06: Reservations are atomic (all-or-nothing per order)
- BR-INV-07: Reserved quantity cannot exceed on-hand quantity
- BR-INV-08: Reservation uses pessimistic locking (`SELECT FOR UPDATE`)

---

#### UC-INV-04: Release Reservation (Unreserve)

**Brief Description**: Free up reserved stock when order cancelled or shipment confirmed.

**Primary Actor**: Outbound Service (System)

**Pre-conditions**:
- Stock was previously reserved
- Order is being cancelled OR shipment completed

**Post-conditions**:
- Reserved quantity decreased
- Available quantity increased (if cancelled) OR on-hand decreased (if shipped)

**Main Flow (Order Cancelled)**:
1. Outbound service calls `unreserveStock(...)`
2. System decreases reserved_quantity:
   ```sql
   UPDATE inventory
   SET reserved_quantity = reserved_quantity - ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
3. System records movement (type = 'UNRESERVE')
4. Stock becomes available again

**Main Flow (Shipment Confirmed)**:
1. Outbound service calls `decreaseStock(...)`
2. System updates both on-hand and reserved:
   ```sql
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity - ?,
       reserved_quantity = reserved_quantity - ?,
       updated_at = NOW()
   WHERE id = ?;
   ```
3. System records movement (type = 'OUTBOUND')

**Business Rules**:
- BR-INV-09: Cannot unreserve more than currently reserved
- BR-INV-10: Shipment confirmation decreases both on-hand AND reserved

---

## 📋 Feature 3: Stock Adjustment

### User Stories

**US-INV-08**: As an Inventory Controller, I want to adjust stock quantities after physical count, so that system matches reality.

**US-INV-09**: As a Warehouse Manager, I want to approve stock adjustments above a threshold, so that large discrepancies are reviewed.

**US-INV-10**: As an Auditor, I want to see all stock adjustments with reasons, so that I can identify patterns of shrinkage or damage.

---

### Use Cases

#### UC-INV-05: Create Stock Adjustment

**Brief Description**: Create a manual stock adjustment to correct inventory discrepancies.

**Primary Actor**: Inventory Controller

**Pre-conditions**:
- User has CREATE_ADJUSTMENT permission
- Physical count completed
- Discrepancy identified

**Post-conditions**:
- Stock adjustment created in PENDING status
- If below approval threshold, auto-approved and inventory updated
- If above threshold, awaits manager approval

**Main Flow**:
1. User opens Stock Adjustment form
2. User enters:
   - product (search/scan)
   - warehouse
   - location
   - batch (if batch-tracked)
   - current_quantity (from system)
   - actual_quantity (from physical count)
   - adjustment_quantity (calculated: actual - current)
   - reason (DAMAGE, THEFT, COUNT_ERROR, EXPIRED, OTHER)
   - notes (detailed explanation)
3. System validates inputs
4. System checks approval threshold (configurable, e.g., ±100 units or $1000 value)
5. If **below threshold**:
   - Create adjustment with status APPROVED
   - Apply to inventory immediately
   - Log in audit trail
6. If **above threshold**:
   - Create adjustment with status PENDING_APPROVAL
   - Send notification to Warehouse Manager
   - Await approval
7. System returns confirmation

**Business Rules**:
- BR-INV-11: Adjustment reason is mandatory
- BR-INV-12: Adjustments above threshold require manager approval
- BR-INV-13: Default approval threshold: 100 units OR value > $1000
- BR-INV-14: Negative adjustments (shrinkage) highlighted in reports

---

#### UC-INV-06: Approve Stock Adjustment

**Brief Description**: Warehouse Manager reviews and approves/rejects pending stock adjustments.

**Primary Actor**: Warehouse Manager

**Pre-conditions**:
- Adjustment in PENDING_APPROVAL status
- User has APPROVE_ADJUSTMENT permission

**Post-conditions**:
- If approved: Inventory updated, adjustment marked APPROVED
- If rejected: Adjustment marked REJECTED, inventory unchanged

**Main Flow (Approval)**:
1. Manager opens Pending Adjustments list
2. Manager selects adjustment to review
3. System displays:
   - Product details
   - Current vs actual quantity
   - Adjustment amount
   - Reason and notes from creator
   - Dollar value impact
4. Manager clicks "Approve"
5. System applies adjustment to inventory:
   ```sql
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity + adjustment_qty
   WHERE id = ?;
   ```
6. System marks adjustment APPROVED
7. System records stock movement
8. System sends confirmation to requester

**Main Flow (Rejection)**:
1-4. Same as approval
5. Manager clicks "Reject" and enters rejection reason
6. System marks adjustment REJECTED
7. Inventory remains unchanged
8. System notifies requester with rejection reason

**Business Rules**:
- BR-INV-15: Only PENDING adjustments can be approved/rejected
- BR-INV-16: Manager cannot approve their own adjustments
- BR-INV-17: Rejection reason is mandatory

---

## 📋 Feature 4: Stock Transfer Between Locations

### User Stories

**US-INV-11**: As a Warehouse Staff, I want to transfer stock from one location to another within the same warehouse, so that I can optimize storage space.

**US-INV-12**: As a Warehouse Manager, I want to track all stock transfers, so that I can ensure proper location accuracy.

---

### Use Cases

#### UC-INV-07: Create Stock Transfer

**Brief Description**: Move stock from one location to another within the same warehouse.

**Primary Actor**: Warehouse Staff

**Pre-conditions**:
- User has TRANSFER_STOCK permission
- Source location has sufficient stock
- Destination location exists and is ACTIVE

**Post-conditions**:
- Stock quantity decreased at source location
- Stock quantity increased at destination location
- Transfer recorded in audit trail

**Main Flow**:
1. User opens Stock Transfer form
2. User enters:
   - product (search/scan)
   - warehouse
   - from_location
   - to_location
   - batch (if batch-tracked)
   - quantity
   - reason (REORG, PICKING_PREP, OVERFLOW, OTHER)
3. System validates:
   - Source has sufficient stock
   - Source ≠ Destination
   - Both locations in same warehouse
4. System creates transfer in transaction:
   ```sql
   -- Decrease source
   UPDATE inventory
   SET on_hand_quantity = on_hand_quantity - ?
   WHERE product_id = ? AND warehouse_id = ? 
     AND location_id = ? AND batch_id = ?;
   
   -- Increase or create destination
   INSERT INTO inventory (...) VALUES (...)
   ON DUPLICATE KEY UPDATE 
     on_hand_quantity = on_hand_quantity + ?;
   ```
5. System records two stock movements:
   - Movement 1: TRANSFER_OUT from source location
   - Movement 2: TRANSFER_IN to destination location
6. System returns success

**Business Rules**:
- BR-INV-18: Transfer must be within same warehouse (use different process for inter-warehouse)
- BR-INV-19: Cannot transfer more than available at source
- BR-INV-20: Transfer is atomic (both locations updated in same transaction)

---

## 📋 Feature 5: Low Stock Alerts

### User Stories

**US-INV-13**: As a Warehouse Manager, I want to be alerted when stock falls below minimum level, so that I can initiate reordering.

**US-INV-14**: As a Purchasing Manager, I want to see a list of products below reorder point, so that I can create purchase orders.

---

### Use Cases

#### UC-INV-08: Automatic Low Stock Detection

**Brief Description**: System automatically detects products below minimum stock level and sends alerts.

**Primary Actor**: System (Scheduled Job)

**Pre-conditions**:
- Products have min_stock_level or reorder_point configured
- Scheduled job configured to run periodically

**Post-conditions**:
- Low stock items identified
- Alerts sent to relevant stakeholders
- Dashboard updated

**Main Flow**:
1. System runs scheduled job (e.g., every 6 hours)
2. System queries:
   ```sql
   SELECT 
     p.id, p.sku, p.name,
     p.min_stock_level, p.reorder_point,
     SUM(i.on_hand_quantity) AS current_stock
   FROM products p
   INNER JOIN inventory i ON p.id = i.product_id
   WHERE p.status = 'ACTIVE'
   GROUP BY p.id, p.sku, p.name, p.min_stock_level, p.reorder_point
   HAVING current_stock < p.reorder_point;
   ```
3. For each low-stock product:
   - Calculate recommended order quantity:
     `order_qty = max_stock_level - current_stock`
   - Determine urgency:
     - CRITICAL: stock < min_stock_level
     - WARNING: stock < reorder_point
4. System sends consolidated email to Purchasing Manager
5. System updates dashboard counters
6. System logs detection

**Business Rules**:
- BR-INV-21: Reorder point typically set at 2 weeks of average demand
- BR-INV-22: Min stock level is safety stock (e.g., 1 week demand)
- BR-INV-23: Low stock alerts sent maximum once per day per product

---

## 🔗 API Impact Summary

### New Endpoints

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| GET | /api/inventory | List inventory with filters | VIEWER |
| GET | /api/inventory/summary/{productId} | Get inventory summary for product | VIEWER |
| GET | /api/inventory/by-location | Get inventory grouped by location | VIEWER |
| POST | /api/inventory/check-availability | Check if stock available | VIEWER |
| POST | /api/inventory/reserve | Reserve stock (internal) | SYSTEM |
| POST | /api/inventory/unreserve | Release reservation | SYSTEM |
| POST | /api/inventory/increase | Increase stock (from inbound) | SYSTEM |
| POST | /api/inventory/decrease | Decrease stock (from outbound) | SYSTEM |
| POST | /api/stock-adjustments | Create stock adjustment | INVENTORY_CONTROLLER |
| GET | /api/stock-adjustments | List adjustments | INVENTORY_CONTROLLER |
| PUT | /api/stock-adjustments/{id}/approve | Approve adjustment | WAREHOUSE_MANAGER |
| PUT | /api/stock-adjustments/{id}/reject | Reject adjustment | WAREHOUSE_MANAGER |
| POST | /api/stock-transfers | Create stock transfer | WAREHOUSE_STAFF |
| GET | /api/stock-transfers | List transfers | WAREHOUSE_STAFF |
| GET | /api/inventory/low-stock | Get low stock items | PURCHASING_MANAGER |

---

## 💾 Database Impact Summary

See detailed schema in [DB_MODULE_04_INVENTORY.md](./DB_MODULE_04_INVENTORY.md)

### New Tables

#### Table: inventory

Primary table tracking stock at product-warehouse-location-batch level.

**Key Fields**:
- `id` - UUID
- `product_id` - FK to products
- `warehouse_id` - FK to warehouses
- `location_id` - FK to locations (nullable)
- `batch_id` - FK to batches (nullable)
- `on_hand_quantity` - Physical stock
- `reserved_quantity` - Allocated to orders
- `version` - Optimistic locking
- `last_movement_at` - Last transaction timestamp

**Unique Key**: (product_id, warehouse_id, location_id, batch_id)

#### Table: stock_adjustments

Records manual inventory adjustments.

**Key Fields**:
- `adjustment_number` - Human-readable identifier
- `status` - PENDING_APPROVAL, APPROVED, REJECTED
- `reason` - DAMAGE, THEFT, COUNT_ERROR, etc.
- `adjustment_quantity` - Can be positive or negative
- `approved_by`, `approved_at` - Approval tracking

#### Table: stock_transfers

Records movement between locations.

**Key Fields**:
- `transfer_number`
- `from_location_id`
- `to_location_id`
- `quantity`
- `reason`

---

## 🔄 Integration Points

### With Inbound Module
- Inbound receipt confirmation calls `increaseStock()`
- Creates or updates inventory records
- On-hand quantity increased

### With Outbound Module
- Sales order confirmation calls `reserveStock()`
- Shipment creation can check availability
- Shipment confirmation calls `decreaseStock()`
- Both on-hand and reserved decreased

### With Batch Module
- Inventory tracked per batch for batch-tracked products
- Batch status (QUARANTINE, EXPIRED) excludes stock from available

### With Stock Movement Module
- Every inventory change creates stock movement record
- Provides complete audit trail

### With Notification Module
- Low stock alerts sent to Purchasing Manager
- Stock-out warnings sent to Warehouse Manager
- Adjustment approval requests notify manager

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-INV-01 | Available = On-Hand - Reserved |
| BR-INV-02 | Real-time data accuracy within 1 second |
| BR-INV-03 | Inventory records auto-created on first receipt |
| BR-INV-04 | Only AVAILABLE batches counted in available stock |
| BR-INV-05 | Negative stock not allowed |
| BR-INV-06 | Reservations are atomic (all-or-nothing) |
| BR-INV-07 | Reserved cannot exceed on-hand |
| BR-INV-08 | Reservation uses pessimistic locking |
| BR-INV-09 | Cannot unreserve more than reserved |
| BR-INV-10 | Shipment decreases both on-hand and reserved |
| BR-INV-11 | Adjustment reason is mandatory |
| BR-INV-12 | Adjustments above threshold require approval |
| BR-INV-13 | Default threshold: 100 units OR $1000 value |
| BR-INV-14 | Negative adjustments highlighted in reports |
| BR-INV-15 | Only PENDING adjustments can be approved/rejected |
| BR-INV-16 | Manager cannot approve own adjustments |
| BR-INV-17 | Rejection reason is mandatory |
| BR-INV-18 | Transfer within same warehouse only |
| BR-INV-19 | Cannot transfer more than available |
| BR-INV-20 | Transfer is atomic |
| BR-INV-21 | Reorder point = 2 weeks average demand |
| BR-INV-22 | Min stock = 1 week demand (safety stock) |
| BR-INV-23 | Low stock alerts max once per day per product |

---

## ✅ Acceptance Criteria

1. ✅ Real-time inventory visible across all warehouses and locations
2. ✅ Available stock accurately calculated (on-hand - reserved)
3. ✅ Stock reservations prevent double allocation
4. ✅ Adjustments above threshold require approval
5. ✅ Stock transfers update both locations atomically
6. ✅ Low stock alerts sent when below reorder point
7. ✅ Concurrency handled (no lost updates)
8. ✅ All inventory changes logged in audit trail

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
