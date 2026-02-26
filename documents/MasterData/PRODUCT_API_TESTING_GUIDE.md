# Product Management API - Testing Guide

## 🧪 API Testing với Postman/cURL

### Base URL
```
http://localhost:8080/api/v1/products
```

## 📋 Test Scenarios

### 1. Create Product (Happy Path)

**Endpoint:** `POST /api/v1/products`

**Request:**
```json
{
  "sku": "LAPTOP-001",
  "name": "Dell XPS 15",
  "description": "High-performance laptop with Intel i7 processor",
  "category_id": "{{categoryId}}",
  "uom_id": "{{uomId}}",
  "weight": 2.5,
  "dimensions": "35.7x23.5x1.8",
  "min_stock_level": 5,
  "max_stock_level": 50,
  "reorder_point": 10,
  "cost_price": 1200.00,
  "selling_price": 1500.00,
  "barcode": "5901234123457",
  "image_url": "https://example.com/laptop.jpg",
  "requires_batch_tracking": false
}
```

**Expected Response (201 Created):**
```json
{
  "success": true,
  "error_code": null,
  "message": "Product created successfully",
  "data": {
    "id": "generated-uuid",
    "sku": "LAPTOP-001",
    "name": "Dell XPS 15",
    "description": "High-performance laptop with Intel i7 processor",
    "category_id": "{{categoryId}}",
    "category_name": "Electronics",
    "uom_id": "{{uomId}}",
    "uom_code": "UNIT",
    "uom_name": "Unit",
    "weight": 2.5,
    "dimensions": "35.7x23.5x1.8",
    "status": "ACTIVE",
    "min_stock_level": 5.00,
    "max_stock_level": 50.00,
    "reorder_point": 10.00,
    "cost_price": 1200.00,
    "selling_price": 1500.00,
    "barcode": "5901234123457",
    "image_url": "https://example.com/laptop.jpg",
    "requires_batch_tracking": false,
    "created_at": "2026-02-05T10:30:00",
    "updated_at": "2026-02-05T10:30:00",
    "created_by": "user-uuid",
    "updated_by": "user-uuid"
  },
  "field_errors": null,
  "timestamp": "2026-02-05T10:30:00"
}
```

---

### 2. Create Product - Validation Errors

#### Test Case 2.1: Duplicate SKU
**Request:**
```json
{
  "sku": "LAPTOP-001",  // SKU đã tồn tại
  "name": "Another Product",
  "category_id": "{{categoryId}}",
  "uom_id": "{{uomId}}"
}
```

**Expected Response (400 Bad Request):**
```json
{
  "success": false,
  "error_code": "PROD_002",
  "message": "Product SKU already exists",
  "data": null,
  "field_errors": null,
  "timestamp": "2026-02-05T10:31:00"
}
```

#### Test Case 2.2: Invalid Category
**Request:**
```json
{
  "sku": "PROD-002",
  "name": "Test Product",
  "category_id": "invalid-uuid",  // Category không tồn tại
  "uom_id": "{{uomId}}"
}
```

**Expected Response (404 Not Found):**
```json
{
  "success": false,
  "error_code": "PROD_004",
  "message": "Category not found or inactive",
  "data": null,
  "field_errors": null,
  "timestamp": "2026-02-05T10:32:00"
}
```

#### Test Case 2.3: Invalid Stock Levels
**Request:**
```json
{
  "sku": "PROD-003",
  "name": "Test Product",
  "category_id": "{{categoryId}}",
  "uom_id": "{{uomId}}",
  "min_stock_level": 100,
  "max_stock_level": 50  // Max < Min
}
```

**Expected Response (400 Bad Request):**
```json
{
  "success": false,
  "error_code": "PROD_007",
  "message": "Max stock level must be greater than or equal to min stock level",
  "data": null,
  "field_errors": null,
  "timestamp": "2026-02-05T10:33:00"
}
```

#### Test Case 2.4: Missing Required Fields
**Request:**
```json
{
  "name": "Test Product"
  // Thiếu SKU, category_id, uom_id
}
```

**Expected Response (400 Bad Request):**
```json
{
  "success": false,
  "error_code": "COM_001",
  "message": "Validation error - please check your input",
  "data": null,
  "field_errors": [
    {
      "field": "sku",
      "message": "SKU is required",
      "rejected_value": null
    },
    {
      "field": "category_id",
      "message": "Category ID is required",
      "rejected_value": null
    },
    {
      "field": "uom_id",
      "message": "Unit of Measure ID is required",
      "rejected_value": null
    }
  ],
  "timestamp": "2026-02-05T10:34:00"
}
```

---

### 3. Get Product by ID

**Endpoint:** `GET /api/v1/products/{id}`

**Request:**
```
GET /api/v1/products/{{productId}}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "error_code": null,
  "message": "Success",
  "data": {
    "id": "{{productId}}",
    "sku": "LAPTOP-001",
    "name": "Dell XPS 15",
    // ... full product details
  },
  "field_errors": null,
  "timestamp": "2026-02-05T10:35:00"
}
```

**Not Found (404):**
```json
{
  "success": false,
  "error_code": "PROD_001",
  "message": "Product not found",
  "data": null,
  "field_errors": null,
  "timestamp": "2026-02-05T10:36:00"
}
```

---

### 4. Get Product by SKU

**Endpoint:** `GET /api/v1/products/sku/{sku}`

**Request:**
```
GET /api/v1/products/sku/LAPTOP-001
```

**Note:** SKU search is case-insensitive
```
GET /api/v1/products/sku/laptop-001  ✅ Works
GET /api/v1/products/sku/LAPTOP-001  ✅ Works
```

---

### 5. Get All Products (Paginated)

**Endpoint:** `GET /api/v1/products?page=0&size=10`

**Request:**
```
GET /api/v1/products?page=0&size=10
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "error_code": null,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": "uuid-1",
        "sku": "LAPTOP-001",
        "name": "Dell XPS 15",
        // ... product details
      },
      {
        "id": "uuid-2",
        "sku": "PHONE-001",
        "name": "iPhone 15",
        // ... product details
      }
    ],
    "page": 0,
    "size": 10,
    "total_elements": 25,
    "total_pages": 3,
    "is_first": true,
    "is_last": false
  },
  "field_errors": null,
  "timestamp": "2026-02-05T10:37:00"
}
```

---

### 6. Search Products with Filters

**Endpoint:** `POST /api/v1/products/search?page=0&size=10`

#### Test Case 6.1: Search by Name
**Request:**
```json
{
  "name": "laptop"
}
```

#### Test Case 6.2: Multiple Filters
**Request:**
```json
{
  "category_id": "{{categoryId}}",
  "status": "ACTIVE",
  "requires_batch_tracking": false
}
```

#### Test Case 6.3: Search by SKU (Exact Match)
**Request:**
```json
{
  "sku": "LAPTOP-001"
}
```

**Expected Response:** Same paginated format as Get All Products

---

### 7. Update Product

**Endpoint:** `PUT /api/v1/products/{id}`

#### Test Case 7.1: Partial Update
**Request:**
```json
{
  "name": "Dell XPS 15 (Updated)",
  "selling_price": 1600.00,
  "max_stock_level": 60
}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "error_code": null,
  "message": "Product updated successfully",
  "data": {
    "id": "{{productId}}",
    "sku": "LAPTOP-001",  // SKU không thể thay đổi
    "name": "Dell XPS 15 (Updated)",
    "selling_price": 1600.00,
    "max_stock_level": 60.00,
    // ... other fields remain unchanged
  },
  "field_errors": null,
  "timestamp": "2026-02-05T10:38:00"
}
```

#### Test Case 7.2: Change Status
**Request:**
```json
{
  "status": "INACTIVE"
}
```

---

### 8. Delete Product (Soft Delete)

**Endpoint:** `DELETE /api/v1/products/{id}`

**Request:**
```
DELETE /api/v1/products/{{productId}}
```

**Expected Response (200 OK):**
```json
{
  "success": true,
  "error_code": null,
  "message": "Product deleted successfully",
  "data": null,
  "field_errors": null,
  "timestamp": "2026-02-05T10:39:00"
}
```

**Note:** Product status sẽ được set thành `DISCONTINUED`

---

### 9. Get Products by Category

**Endpoint:** `GET /api/v1/products/category/{categoryId}?page=0&size=10`

**Request:**
```
GET /api/v1/products/category/{{categoryId}}?page=0&size=10
```

---

### 10. Get Batch Tracking Products

**Endpoint:** `GET /api/v1/products/batch-tracking?page=0&size=10`

**Request:**
```
GET /api/v1/products/batch-tracking?page=0&size=10
```

**Expected:** Returns only products where `requires_batch_tracking = true`

---

## 🔐 Authentication

All endpoints require authentication. Include JWT token in headers:

```
Authorization: Bearer {{access_token}}
```

---

## 📊 Postman Collection Structure

```
Product Management API/
├── 1. Prerequisites/
│   ├── Login
│   ├── Create Category
│   └── Create UOM
├── 2. Create Product/
│   ├── Create - Success
│   ├── Create - Duplicate SKU
│   ├── Create - Invalid Category
│   └── Create - Invalid Stock Levels
├── 3. Get Product/
│   ├── Get by ID
│   ├── Get by SKU
│   └── Get All (Paginated)
├── 4. Search Products/
│   ├── Search by Name
│   ├── Search with Multiple Filters
│   └── Search by Category
├── 5. Update Product/
│   ├── Update - Partial
│   └── Update - Change Status
└── 6. Delete Product/
    └── Soft Delete
```

---

## 🧪 Automated Testing Script (Newman)

```bash
# Run Postman collection với Newman
newman run Product_Management_API.postman_collection.json \
  -e environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

---

## 📝 Test Data Setup

### Step 1: Create Category
```json
POST /api/v1/categories
{
  "code": "ELEC",
  "name": "Electronics",
  "status": "ACTIVE"
}
```
Save `categoryId` to environment variable.

### Step 2: Create UOM
```json
POST /api/v1/units-of-measure
{
  "code": "UNIT",
  "name": "Unit",
  "type": "COUNT"
}
```
Save `uomId` to environment variable.

### Step 3: Now create products using these IDs

---

## ✅ Testing Checklist

- [ ] Create product with valid data
- [ ] Create product with duplicate SKU
- [ ] Create product with invalid category
- [ ] Create product with invalid UOM
- [ ] Create product with invalid stock levels
- [ ] Get product by ID (existing)
- [ ] Get product by ID (non-existing)
- [ ] Get product by SKU (case-insensitive)
- [ ] Get all products with pagination
- [ ] Search products by name
- [ ] Search products with multiple filters
- [ ] Update product (partial update)
- [ ] Update product status
- [ ] Delete product (soft delete)
- [ ] Get products by category
- [ ] Get batch tracking products

---

**Note:** Replace `{{variables}}` với actual values từ environment hoặc previous responses.
