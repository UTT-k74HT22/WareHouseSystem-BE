# Batch Module Specification v1.0

## 1. Domain Overview

The Batch module manages **product lots/batches** for traceability, inventory control, and quality management.

Primary goals:

- Enable **traceability** across inbound, storage, and outbound flows.
- Support **FEFO/FIFO allocation strategies**.
- Manage **quality isolation (quarantine)**.
- Track **expiration lifecycle** of products.

Batch tracking is enabled only for products where:

```
product.requires_batch_tracking = true
```

### Integration with Other Modules

| Module | Relationship |
|------|------|
Inventory | Stock quantities per batch |
Inbound | Batch creation during receiving |
Outbound | Batch allocation for picking |
Stock Movement | Traceability of batch movements |
Quality Control | Quarantine / release |

---

# 2. Batch Entity Model

## Table: `batches`

| Field | Type | Description |
|-----|-----|-----|
id | CHAR(36) PK | Batch unique identifier |
batch_number | VARCHAR(50) | Batch/lot number |
product_id | CHAR(36) FK | Linked product |
manufacturing_date | DATE | Production date |
expiry_date | DATE | Expiration date |
status | ENUM | Batch status |
supplier_batch_number | VARCHAR(50) | Supplier reference |
notes | TEXT | Optional notes |
created_by | FK accounts | Creator |
updated_by | FK accounts | Last updater |
created_at | TIMESTAMP | Creation time |
updated_at | TIMESTAMP | Update time |

### Unique Constraint

```
(product_id, batch_number)
```

### Recommended Indexes

```
idx_batch_product
idx_batch_status
idx_batch_expiry
idx_batch_manufacturing
idx_batch_fifo(product_id, expiry_date, manufacturing_date)
```

---

# 3. Batch Status Lifecycle

Enum: `BatchStatus`

| Status | Meaning |
|------|------|
AVAILABLE | Batch is usable for allocation |
QUARANTINE | Batch isolated due to quality issue |
EXPIRED | Expiration date passed |
RECALLED | Product recall issued |

### Allocation Rules

| Status | Allocatable |
|------|------|
AVAILABLE | YES |
QUARANTINE | NO |
EXPIRED | NO |
RECALLED | NO |

Note:

Non-allocatable batches **remain visible in traceability and reporting**.

---

# 4. Batch State Machine

| From | To | Allowed | Trigger |
|----|----|----|----|
AVAILABLE | QUARANTINE | YES | Quarantine API |
QUARANTINE | AVAILABLE | YES | Release API |
AVAILABLE | EXPIRED | SYSTEM | Expiry job |
QUARANTINE | EXPIRED | SYSTEM | Expiry job |
ANY | RECALLED | YES | Recall process |
EXPIRED | AVAILABLE | NO | Invalid |
EXPIRED | QUARANTINE | NO | Invalid |

Invalid transitions return:

```
BATCH_003 - Invalid batch status transition
```

---

# 5. API Contracts

All responses use the standard envelope:

```
BaseResponse<T>
```

Example:

```json
{
  "success": true,
  "data": {}
}
```

---

# 5.1 Update Batch

```
PUT /api/v1/batches/{id}
```

## Request Body

| Field | Type | Required |
|-----|-----|-----|
batchNumber | string | yes |
manufacturingDate | date | yes |
expiryDate | date | yes |
supplierBatchNumber | string | no |
notes | string | no |

### Example Request

```json
{
  "batchNumber": "LOT-2024-001",
  "manufacturingDate": "2024-01-01",
  "expiryDate": "2025-01-01",
  "supplierBatchNumber": "SUP-123"
}
```

### Response

```
BaseResponse<BatchResponse>
```

---

# 5.2 Quarantine Batch

```
PUT /api/v1/batches/{id}/quarantine
```

Optional request body:

```json
{
  "reason": "Quality inspection failed"
}
```

### Response

```
BaseResponse<BatchResponse>
```

---

# 5.3 Release Batch

```
PUT /api/v1/batches/{id}/release
```

Moves batch from `QUARANTINE → AVAILABLE`.

---

# 5.4 Get Batch Traceability

```
GET /api/v1/batches/{id}/traceability
```

Returns full movement history.

### TraceabilityResponse

| Field | Description |
|-----|-----|
batch | Batch info |
inboundRecords | Receiving records |
stockMovements | Internal movements |
outboundRecords | Shipment records |
adjustments | Inventory adjustments |

Example:

```json
{
  "batchId": "uuid",
  "movements": [
    {
      "type": "INBOUND",
      "documentId": "GRN-001"
    },
    {
      "type": "OUTBOUND",
      "documentId": "SO-1001"
    }
  ]
}
```

---

# 5.5 Get Expiring Batches

```
GET /api/v1/batches/expiring
```

Query parameters:

| Param | Type | Description |
|-----|-----|-----|
days | int | expiration window |

Example:

```
/batches/expiring?days=30
```

---

# 5.6 FIFO / FEFO Recommendation

```
GET /api/v1/batches/fifo-recommendations?productId={productId}
```

Allocation strategy:

```
ORDER BY expiry_date ASC,
         manufacturing_date ASC
```

Explanation:

1. **FEFO first** (earliest expiry)
2. **FIFO fallback** (oldest manufacturing)

Excluded batches:

```
status != AVAILABLE
```

---

# 5.7 Get Batches by Product

```
GET /api/v1/batches/by-product/{productId}
```

Returns all batches for the product.

---

# 6. Response Schema

## BatchResponse

| Field |
|-----|
id |
batchNumber |
productId |
manufacturingDate |
expiryDate |
status |
availableQuantity |
createdAt |
updatedAt |

---

# 7. Validation Rules

| Rule | Error Code |
|----|----|
expiryDate > manufacturingDate | BATCH_005 |
batchNumber unique per product | BATCH_002 |
product must exist | PROD_001 |
batchId must exist | BATCH_001 |
invalid state transition | BATCH_003 |

---

# 8. Error Code Mapping

| Code | Meaning |
|----|----|
BATCH_001 | Batch not found |
BATCH_002 | Duplicate batch number |
BATCH_003 | Invalid batch transition |
BATCH_004 | Batch expired |
BATCH_005 | Invalid manufacturing date |
PROD_006 | Batch tracking cannot be disabled |

---

# 9. Edge Cases

| Scenario | Expected Behavior |
|------|------|
Quarantine already quarantined batch | error BATCH_003 |
Release expired batch | error BATCH_004 |
Update expiry after expiration | error BATCH_004 |
Traceability for missing batch | error BATCH_001 |
Expiry before manufacturing | error BATCH_005 |

---

# 10. Service Layer Responsibilities

Service:

```
BatchService
```

Core methods:

```
updateBatch()
quarantineBatch()
releaseBatch()
getTraceability()
getExpiringBatches()
getFifoRecommendations()
getBatchesByProduct()
```

Business rules must be enforced **in the service layer**.

Controllers only:

- validate request
- call service
- return BaseResponse

---

# 11. Scheduled Jobs

Expiration handling:

```
BatchExpirationJob
```

Runs daily:

```
UPDATE batches
SET status = EXPIRED
WHERE expiry_date < CURRENT_DATE
```

---

# 12. Swagger Documentation Plan

Each endpoint must include:

```
@Operation(summary = "")
@ApiResponse(responseCode = "200")
@ApiResponse(responseCode = "400")
@ApiResponse(responseCode = "404")
```

Schema mapping:

```
BatchResponse
TraceabilityResponse
BaseResponse<T>
```

---

# 13. Notes

- Business logic resides in **service layer only**
- Controllers must remain **thin**
- Batch status transitions must follow the defined state machine
- Any logic not verified in code should be marked:

```
REQUIRES CONFIRMATION
```
