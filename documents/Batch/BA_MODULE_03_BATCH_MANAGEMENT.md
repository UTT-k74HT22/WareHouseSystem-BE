# BA Document - Module 3: Batch Management
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Batch Management |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 📑 Table of Contents

1. [Business Context](#business-context)
2. [Actors & Roles](#actors--roles)
3. [Module Overview](#module-overview)
4. [Feature 1: Batch Creation & Management](#feature-1-batch-creation--management)
5. [Feature 2: Batch Tracking & Traceability](#feature-2-batch-tracking--traceability)
6. [Feature 3: Expiry Management](#feature-3-expiry-management)
7. [API Impact Summary](#api-impact-summary)
8. [Database Impact Summary](#database-impact-summary)
9. [Integration Points](#integration-points)

---

## 🎯 Business Context

### Current Pain Points

1. **No Batch Tracking**: Cannot track which batch of product was sold to which customer, causing issues in recalls.
2. **Expired Stock**: No automatic alerts when products approach expiry date, leading to wastage.
3. **FIFO Not Enforced**: Oldest batches not picked first, causing older stock to expire while newer stock is used.
4. **Manual Record Keeping**: Paper-based batch records are error-prone and hard to search.
5. **Compliance Issues**: Cannot provide batch traceability reports required by regulators.

### Business Value

✅ **Product Recalls**: Quickly identify affected customers when a batch needs to be recalled  
✅ **Waste Reduction**: Reduce expired stock by 50% through proactive expiry alerts  
✅ **Compliance**: Meet regulatory requirements for batch tracking (FDA, ISO standards)  
✅ **Traceability**: Complete supply chain visibility from supplier batch to customer  
✅ **Quality Control**: Track quality metrics per batch for continuous improvement  

---

## 👥 Actors & Roles

| Actor | Goal | Permissions |
|-------|------|-------------|
| **Warehouse Manager** | Monitor batch status, manage expiring stock | View all batches, receive expiry alerts, initiate stock adjustments |
| **Warehouse Staff** | Record batch information during receiving | Create/update batches during inbound operations |
| **Quality Controller** | Track batch quality and status | View batch details, update quality status, quarantine batches |
| **Inventory Controller** | Manage stock levels by batch | View batch inventory, adjust batch quantities |
| **Compliance Officer** | Generate batch traceability reports | Read-only access to all batch data and movements |

---

## 📦 Module Overview

Batch Management tracks groups of products with the same manufacturing date, expiry date, and quality characteristics. It enables full traceability from receiving to shipment.

### Core Entities

1. **Batches** - Group of products manufactured together with same expiry
2. **Batch Inventory** - Stock levels per batch per location
3. **Batch Movements** - History of batch transactions

### Key Concepts

- **Batch Number**: Unique identifier provided by manufacturer or generated internally
- **Manufacturing Date**: When the batch was produced
- **Expiry Date**: When the batch should no longer be used (for perishables)
- **Batch Status**: AVAILABLE, QUARANTINE, EXPIRED, RECALLED
- **FIFO Recommendation**: System suggests oldest batches first (not enforced in V1)

### Dependencies

- **Depends on**: Master Data (Products with `requires_batch_tracking = true`)
- **Required by**: Inventory Management, Inbound Operations, Outbound Operations, Stock Movements

---

## 📋 Feature 1: Batch Creation & Management

### User Stories

**US-BATCH-01**: As a Warehouse Staff, I want to create batch records during goods receipt, so that I can track product batches throughout their lifecycle.

**US-BATCH-02**: As a Warehouse Manager, I want to view all batches with their status and quantities, so that I can make informed decisions about stock usage.

**US-BATCH-03**: As a Quality Controller, I want to quarantine a batch when quality issues are detected, so that defective stock is not shipped to customers.

**US-BATCH-04**: As a Warehouse Manager, I want to search batches by product, date range, or status, so that I can quickly find relevant batches.

---

### Use Cases

#### UC-BATCH-01: Create Batch

**Brief Description**: Create a new batch record for a product that requires batch tracking.

**Primary Actor**: Warehouse Staff

**Pre-conditions**:
- User is authenticated with WAREHOUSE_STAFF or higher role
- Product must have `requires_batch_tracking = true`
- User has CREATE_BATCH permission

**Post-conditions**:
- Batch is created with AVAILABLE status
- Audit fields (created_by, created_at) are recorded
- Batch is ready to receive inventory

**Main Flow**:
1. User creates/updates inbound receipt for a batch-tracked product
2. System prompts for batch information
3. User enters:
   - batch_number (unique per product)
   - manufacturing_date (required, cannot be future)
   - expiry_date (optional, must be after manufacturing_date)
   - supplier_batch_number (optional, for traceability)
   - notes (optional)
4. User submits
5. System validates all fields
6. System checks batch_number uniqueness for this product
7. System creates batch with status AVAILABLE
8. System returns success message with batch ID

**Alternative Flows**:
- A1: Duplicate batch number for product
  - System returns error "Batch number already exists for this product"
- A2: Expiry date before manufacturing date
  - System returns validation error
- A3: Manufacturing date in future
  - System returns validation error

**Business Rules**:
- BR-BATCH-01: Batch number must be unique per product
- BR-BATCH-02: Manufacturing date cannot be in the future
- BR-BATCH-03: Expiry date must be after manufacturing date (if provided)
- BR-BATCH-04: Only products with `requires_batch_tracking = true` can have batches
- BR-BATCH-05: Batch status defaults to AVAILABLE on creation

---

#### UC-BATCH-02: Quarantine Batch

**Brief Description**: Put a batch on hold due to quality concerns, preventing it from being shipped.

**Primary Actor**: Quality Controller or Warehouse Manager

**Pre-conditions**:
- User has QUARANTINE_BATCH permission
- Batch exists and is in AVAILABLE status
- Batch has on-hand quantity > 0

**Post-conditions**:
- Batch status changed to QUARANTINE
- Batch quantity excluded from available-to-ship calculations
- Audit log records who quarantined and why
- Notification sent to relevant stakeholders

**Main Flow**:
1. User views batch details
2. User clicks "Quarantine Batch"
3. System shows confirmation form
4. User enters:
   - reason (required, max 500 chars)
   - expected_resolution_date (optional)
   - notify_manager (checkbox, default true)
5. User submits
6. System validates current status is AVAILABLE
7. System updates status to QUARANTINE
8. System records audit log
9. System sends notification if requested
10. System returns success message

**Alternative Flows**:
- A1: Batch already quarantined
  - System returns error "Batch is already in QUARANTINE status"
- A2: Batch has zero quantity
  - System returns warning but allows quarantine

**Business Rules**:
- BR-BATCH-06: Only AVAILABLE batches can be quarantined
- BR-BATCH-07: Quarantined batches cannot be picked for outbound orders
- BR-BATCH-08: Quarantine reason must be recorded
- BR-BATCH-09: Warehouse Manager must be notified of quarantine actions

---

#### UC-BATCH-03: Release from Quarantine

**Brief Description**: Return a batch to available status after quality issues are resolved.

**Primary Actor**: Quality Controller or Warehouse Manager

**Pre-conditions**:
- User has RELEASE_BATCH permission
- Batch is in QUARANTINE status

**Post-conditions**:
- Batch status changed to AVAILABLE
- Batch quantity included in available-to-ship calculations
- Audit log records release action

**Main Flow**:
1. User views quarantined batch
2. User clicks "Release from Quarantine"
3. System shows confirmation form
4. User enters release notes (required)
5. User submits
6. System validates current status is QUARANTINE
7. System updates status to AVAILABLE
8. System records audit log with notes
9. System returns success message

**Alternative Flows**:
- A1: Batch not quarantined
  - System returns error "Only quarantined batches can be released"

**Business Rules**:
- BR-BATCH-10: Only QUARANTINE batches can be released
- BR-BATCH-11: Release notes must be provided for audit trail

---

## 📋 Feature 2: Batch Tracking & Traceability

### User Stories

**US-BATCH-05**: As a Compliance Officer, I want to view complete traceability for a batch (from receipt to shipment), so that I can respond to regulatory inquiries.

**US-BATCH-06**: As a Warehouse Manager, I want to see which customers received products from a specific batch, so that I can execute targeted recalls if needed.

**US-BATCH-07**: As a Quality Controller, I want to trace a batch back to the original supplier, so that I can investigate quality issues at the source.

---

### Use Cases

#### UC-BATCH-04: View Batch Traceability

**Brief Description**: Display complete history of a batch including all receipts, movements, and shipments.

**Primary Actor**: Compliance Officer, Warehouse Manager, Quality Controller

**Pre-conditions**:
- User has VIEW_BATCH_TRACEABILITY permission
- Batch exists in the system

**Post-conditions**:
- Complete batch history is displayed

**Main Flow**:
1. User searches for batch by batch number or product
2. User selects batch from results
3. System displays batch details:
   - Basic info (batch number, dates, status)
   - Current inventory by location
   - Inbound history:
     - Receipt date
     - Quantity received
     - Supplier
     - Purchase order reference
   - Outbound history:
     - Shipment date
     - Quantity shipped
     - Customer
     - Sales order reference
   - Movement history:
     - All stock movements with timestamps
     - Source and destination locations
   - Status change history:
     - When quarantined/released
     - Who made the change
     - Reason
4. User can export report to PDF/Excel

**Business Rules**:
- BR-BATCH-12: All batch movements must be recorded in audit trail
- BR-BATCH-13: Traceability report must show forward trace (who received it) and backward trace (where it came from)

---

## 📋 Feature 3: Expiry Management

### User Stories

**US-BATCH-08**: As a Warehouse Manager, I want to receive alerts when batches are approaching expiry, so that I can prioritize their shipment or disposal.

**US-BATCH-09**: As an Inventory Controller, I want the system to automatically mark expired batches, so that they cannot be accidentally shipped.

**US-BATCH-10**: As a Warehouse Staff, I want to see FIFO recommendations when picking, so that oldest batches are shipped first.

---

### Use Cases

#### UC-BATCH-05: Automatic Expiry Detection

**Brief Description**: System automatically detects expired batches and marks them as EXPIRED.

**Primary Actor**: System (Scheduled Job)

**Pre-conditions**:
- Batches with expiry_date exist
- Daily scheduled job is configured

**Post-conditions**:
- Batches past expiry date are marked EXPIRED
- Notifications sent to Warehouse Manager
- Expired batches excluded from picking

**Main Flow**:
1. System runs daily at configured time (e.g., 02:00 AM)
2. System queries all batches where:
   - status = 'AVAILABLE'
   - expiry_date < CURRENT_DATE
3. For each expired batch:
   - Update status to EXPIRED
   - Log status change in audit trail
   - Add to daily expiry report
4. System sends consolidated notification to Warehouse Managers
5. System logs job completion

**Business Rules**:
- BR-BATCH-14: Batches are marked EXPIRED on the expiry date (not day after)
- BR-BATCH-15: EXPIRED batches cannot be picked for orders
- BR-BATCH-16: Expiry check runs daily

---

#### UC-BATCH-06: Expiry Alert

**Brief Description**: System alerts when batches are approaching expiry (within configurable threshold).

**Primary Actor**: System (Scheduled Job)

**Pre-conditions**:
- Batches with expiry_date exist
- Alert threshold configured (e.g., 30 days)

**Post-conditions**:
- Near-expiry alerts sent to Warehouse Manager
- Dashboard shows batches requiring urgent action

**Main Flow**:
1. System runs daily after expiry detection
2. System queries batches where:
   - status = 'AVAILABLE'
   - expiry_date BETWEEN CURRENT_DATE AND CURRENT_DATE + threshold_days
3. System categorizes by urgency:
   - CRITICAL: < 7 days to expiry
   - WARNING: 7-14 days to expiry
   - INFO: 15-30 days to expiry
4. System sends email notification with summary
5. System updates dashboard counters

**Business Rules**:
- BR-BATCH-17: Default expiry alert threshold is 30 days
- BR-BATCH-18: CRITICAL alerts sent daily, WARNING alerts sent twice weekly, INFO alerts sent weekly
- BR-BATCH-19: Alert email includes product, batch number, quantity, location, days to expiry

---

#### UC-BATCH-07: FIFO Picking Recommendation

**Brief Description**: When creating outbound shipment, system recommends batches in FIFO order.

**Primary Actor**: Warehouse Staff

**Pre-conditions**:
- Creating outbound shipment for batch-tracked product
- Multiple batches available

**Post-conditions**:
- System displays batches sorted by manufacturing date (oldest first)
- Staff can see recommended vs. actual picks

**Main Flow**:
1. Staff adds batch-tracked product to shipment
2. System queries available batches for the product in the warehouse
3. System sorts batches by:
   - Primary: manufacturing_date ASC (oldest first)
   - Secondary: expiry_date ASC (earliest expiry first)
4. System displays list with:
   - Batch number
   - Manufacturing date
   - Expiry date
   - Days to expiry
   - Available quantity
   - Location
   - Recommendation badge (RECOMMENDED for first few batches)
5. Staff selects batch and quantity
6. System validates quantity available
7. System proceeds with shipment

**Alternative Flows**:
- A1: Staff selects non-recommended batch
  - System shows warning "Not following FIFO order. Recommended batch: {batch_number}"
  - Staff can confirm and proceed

**Business Rules**:
- BR-BATCH-20: FIFO recommendations based on manufacturing date (oldest first)
- BR-BATCH-21: System does NOT enforce FIFO (user can override)
- BR-BATCH-22: Quarantined and expired batches excluded from recommendations

---

## 🔗 API Impact Summary

### New Endpoints

| Method | Endpoint | Description | Role Required |
|--------|----------|-------------|---------------|
| POST | /api/batches | Create batch | WAREHOUSE_STAFF |
| GET | /api/batches | List batches with filters | VIEWER |
| GET | /api/batches/{id} | Get batch details | VIEWER |
| PUT | /api/batches/{id} | Update batch info | WAREHOUSE_STAFF |
| PUT | /api/batches/{id}/quarantine | Quarantine batch | QUALITY_CONTROLLER |
| PUT | /api/batches/{id}/release | Release from quarantine | QUALITY_CONTROLLER |
| GET | /api/batches/{id}/traceability | Get batch traceability | COMPLIANCE_OFFICER |
| GET | /api/batches/expiring | Get expiring batches | WAREHOUSE_MANAGER |
| GET | /api/batches/fifo-recommendations | Get FIFO picking recommendations | WAREHOUSE_STAFF |
| GET | /api/batches/by-product/{productId} | Get all batches for product | VIEWER |

### Query Parameters

**GET /api/batches**:
- `productId` - Filter by product
- `warehouseId` - Filter by warehouse
- `status` - Filter by status (AVAILABLE, QUARANTINE, EXPIRED, RECALLED)
- `manufacturingDateFrom` - Filter by manufacturing date range
- `manufacturingDateTo`
- `expiryDateFrom` - Filter by expiry date range
- `expiryDateTo`
- `page`, `size`, `sort` - Pagination

---

## 💾 Database Impact Summary

### New Tables

See detailed schema in [DB_MODULE_03_BATCH.md](./DB_MODULE_03_BATCH.md)

#### Table: batches

Primary table storing batch master data.

**Key Fields**:
- `id` - UUID primary key
- `batch_number` - Unique identifier (unique per product)
- `product_id` - Foreign key to products
- `manufacturing_date` - Production date
- `expiry_date` - Expiration date (nullable)
- `status` - AVAILABLE, QUARANTINE, EXPIRED, RECALLED
- `supplier_batch_number` - Original batch number from supplier
- `notes` - Additional information
- `created_at`, `updated_at`, `created_by`, `updated_by` - Audit fields

**Indexes**:
- Unique index on (product_id, batch_number)
- Index on status
- Index on expiry_date for expiry queries
- Index on manufacturing_date for FIFO

---

## 🔄 Integration Points

### With Inbound Module
- When confirming inbound receipt for batch-tracked product, batch must be created or selected
- Receipt lines must reference batch_id
- Inventory increases are tied to specific batches

### With Inventory Module
- Inventory records include batch_id for batch-tracked products
- Stock queries must consider batch availability (exclude QUARANTINE, EXPIRED)
- Available-to-ship calculations exclude unavailable batches

### With Outbound Module
- Picking lists show batch recommendations
- Shipment lines must reference batch_id for batch-tracked products
- System warns if non-FIFO batch selected

### With Stock Movement Module
- All batch movements recorded in stock_movements table
- Traceability reports pull from stock movements

### With Notification Module
- Daily expiry alerts sent to Warehouse Managers
- Immediate notification on batch quarantine

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-BATCH-01 | Batch number must be unique per product |
| BR-BATCH-02 | Manufacturing date cannot be in the future |
| BR-BATCH-03 | Expiry date must be after manufacturing date (if provided) |
| BR-BATCH-04 | Only products with `requires_batch_tracking = true` can have batches |
| BR-BATCH-05 | Batch status defaults to AVAILABLE on creation |
| BR-BATCH-06 | Only AVAILABLE batches can be quarantined |
| BR-BATCH-07 | Quarantined batches cannot be picked for outbound orders |
| BR-BATCH-08 | Quarantine reason must be recorded |
| BR-BATCH-09 | Warehouse Manager must be notified of quarantine actions |
| BR-BATCH-10 | Only QUARANTINE batches can be released |
| BR-BATCH-11 | Release notes must be provided for audit trail |
| BR-BATCH-12 | All batch movements must be recorded in audit trail |
| BR-BATCH-13 | Traceability report must show forward and backward trace |
| BR-BATCH-14 | Batches are marked EXPIRED on the expiry date |
| BR-BATCH-15 | EXPIRED batches cannot be picked for orders |
| BR-BATCH-16 | Expiry check runs daily |
| BR-BATCH-17 | Default expiry alert threshold is 30 days |
| BR-BATCH-18 | Alert frequency based on urgency level |
| BR-BATCH-19 | Alert email includes all relevant batch details |
| BR-BATCH-20 | FIFO recommendations based on manufacturing date |
| BR-BATCH-21 | System does NOT enforce FIFO (user can override) |
| BR-BATCH-22 | Quarantined and expired batches excluded from recommendations |

---

## ✅ Acceptance Criteria

### Overall Module Success Criteria

1. ✅ All batch-tracked products can have batches created
2. ✅ Batch traceability reports show complete history from receipt to shipment
3. ✅ Expired batches automatically marked and excluded from picking
4. ✅ Expiry alerts sent 30/15/7 days before expiry
5. ✅ FIFO recommendations displayed during picking
6. ✅ Quarantine/release workflow prevents defective stock shipment
7. ✅ All batch status changes logged in audit trail

---

## 🧪 Test Scenarios

### Functional Testing

1. **Batch Creation**:
   - Create batch with all fields
   - Duplicate batch number rejected
   - Invalid dates rejected
   - Non-batch products cannot have batches

2. **Batch Status Management**:
   - Quarantine available batch
   - Cannot quarantine already quarantined batch
   - Release quarantined batch with notes
   - Cannot release non-quarantined batch

3. **Expiry Management**:
   - Batch auto-expired on expiry date
   - Alerts sent at 30/15/7 days threshold
   - Expired batches excluded from picking

4. **FIFO Recommendations**:
   - Oldest batches recommended first
   - Warning shown when non-FIFO selected
   - Can override FIFO recommendation

5. **Traceability**:
   - View complete batch history
   - Trace from supplier to customer
   - Export traceability report

---

## 📅 Implementation Timeline

| Phase | Duration | Tasks |
|-------|----------|-------|
| **Phase 1: Foundation** | Week 1 | Database schema, entity models, repositories |
| **Phase 2: Core APIs** | Week 2 | CRUD operations, batch creation/update |
| **Phase 3: Status Management** | Week 3 | Quarantine/release workflow |
| **Phase 4: Expiry Management** | Week 4 | Scheduled jobs, alerts, auto-expiry |
| **Phase 5: Traceability** | Week 5 | Traceability reports, FIFO recommendations |
| **Phase 6: Testing** | Week 6 | Unit tests, integration tests, UAT |

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
