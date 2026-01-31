# BA Document - Module 2: Master Data Management
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Master Data Management |
| **Version** | 1.0 |
| **Date** | January 29, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 📑 Table of Contents

1. [Business Context](#business-context)
2. [Actors & Roles](#actors--roles)
3. [Module Overview](#module-overview)
4. [Feature 1: Warehouse Management](#feature-1-warehouse-management)
5. [Feature 2: Location Management](#feature-2-location-management)
6. [Feature 3: Product Management](#feature-3-product-management)
7. [Feature 4: Unit of Measure (UOM) Management](#feature-4-unit-of-measure-uom-management)
8. [Feature 5: Business Partner Management](#feature-5-business-partner-management)
9. [Feature 6: Category Management](#feature-6-category-management)
10. [API Impact Summary](#api-impact-summary)
11. [Database Impact Summary](#database-impact-summary)
12. [Background Job Requirements](#background-job-requirements)

---

## 🎯 Business Context

### Current Pain Points

1. **Manual Data Entry**: Staff waste time entering warehouse, location, and product information repeatedly.
2. **Data Inconsistency**: Different departments use different codes/names for the same items.
3. **No Validation**: Products can be created with incomplete information, causing issues in inventory tracking.
4. **Duplicate Records**: Without proper checks, duplicate warehouses or products get created.
5. **No Audit Trail**: Cannot track who created or modified master data records.
6. **No Bulk Operations**: Cannot import large sets of products or locations efficiently.

### Business Value

✅ **Centralized master data** - Single source of truth for all warehouse operations  
✅ **Data quality** - Validation rules ensure complete and accurate data  
✅ **Efficiency** - Reduce manual data entry by 60%  
✅ **Traceability** - Full audit trail for compliance  
✅ **Scalability** - Support for multiple warehouses and thousands of products  

---

## 👥 Actors & Roles

| Actor | Goal | Permissions |
|-------|------|-------------|
| **System Admin** | Manage all master data | Full CRUD access to all master data |
| **Warehouse Manager** | Manage warehouse configuration, locations, and local product information | Full access to assigned warehouse(s) |
| **Inventory Controller** | View and update product information, manage stock levels | Read products, update stock levels |
| **Accountant** | View master data for reporting | Read-only access to master data |
| **Data Entry Operator** | Input new products and business partners | Create/Update products and partners |

---

## 📦 Module Overview

Master Data Management is the foundation module that manages all reference data used throughout the WMS system.

### Core Entities

1. **Warehouses** - Physical warehouse locations where inventory is stored
2. **Locations** - Specific storage positions within warehouses
3. **Products** - Items that can be stored and tracked in inventory
4. **Product Categories** - Classification for products
5. **Units of Measure (UOM)** - Standard measurement units (pieces, boxes, kg, etc.)
6. **Business Partners** - Suppliers and customers

### Dependencies

- **Depends on**: Authentication & Authorization module (accounts, roles)
- **Required by**: Inventory Management, Order Management, Stock Movement modules

---

## 📋 Feature 1: Warehouse Management

### User Stories

**US-MD-01**: As a System Admin, I want to create new warehouse records, so that I can set up multiple storage facilities in the system.

**US-MD-02**: As a Warehouse Manager, I want to view warehouse details including capacity and status, so that I can plan inventory allocation.

**US-MD-03**: As a System Admin, I want to activate/deactivate warehouses, so that I can control which facilities are operational without deleting historical data.

**US-MD-04**: As a Warehouse Manager, I want to assign a warehouse manager to each facility, so that there is clear accountability.

---

### Use Case: UC-MD-01 - Create Warehouse

**Primary Actor**: System Admin  
**Goal**: Create a new warehouse record in the system  

**Pre-conditions**:
- User is authenticated with ADMIN role
- User has CREATE_WAREHOUSE permission

**Main Flow**:
1. User navigates to Warehouse Management screen
2. User clicks "Create New Warehouse" button
3. System displays warehouse creation form
4. User enters warehouse details:
   - Code (unique, alphanumeric, max 20 chars)
   - Name (required, max 100 chars)
   - Address details (address, city, state, country, postal code)
   - Contact info (phone, email)
   - Type (MAIN, SATELLITE, TRANSIT, RETURN)
   - Capacity (in cubic meters, optional)
   - Manager (select from active accounts)
5. User clicks "Save"
6. System validates all fields
7. System checks code uniqueness
8. System creates warehouse record with status = ACTIVE
9. System logs audit info (created_by, created_at)
10. System displays success message
11. System redirects to warehouse detail page

**Alternative Flow 1**: Validation Fails
- 6a. If required fields are missing → display error message, highlight fields
- 6b. If code format is invalid → display format error
- 6c. If email format is invalid → display email error
- System keeps form data, user corrects and resubmits

**Alternative Flow 2**: Duplicate Code
- 7a. If warehouse code already exists → display error "Warehouse code already exists"
- User changes code and resubmits

**Post-conditions**:
- New warehouse is created with ACTIVE status
- Warehouse is available for location assignment
- Audit trail is recorded

---

### Use Case: UC-MD-02 - Update Warehouse

**Primary Actor**: System Admin or Warehouse Manager  
**Goal**: Modify warehouse information  

**Pre-conditions**:
- User has UPDATE_WAREHOUSE permission
- Warehouse exists in the system

**Main Flow**:
1. User searches and selects warehouse to edit
2. System displays warehouse detail with "Edit" button
3. User clicks "Edit"
4. System displays warehouse update form with current values pre-filled
5. User modifies allowed fields (cannot change code)
6. User clicks "Save"
7. System validates changes
8. System updates warehouse record
9. System updates `updated_by` and `updated_at` fields
10. System displays success message
11. System refreshes warehouse detail view

**Alternative Flow 1**: No Changes Made
- 5a. User clicks "Cancel" → System returns to detail view without saving

**Alternative Flow 2**: Concurrent Update
- 8a. If record was modified by another user → display conflict error
- User must refresh and reapply changes

**Post-conditions**:
- Warehouse information is updated
- Audit trail records the change
- Related locations remain linked

---

### Use Case: UC-MD-03 - Change Warehouse Status

**Primary Actor**: System Admin  
**Goal**: Activate, deactivate, or set warehouse to maintenance mode  

**Pre-conditions**:
- User has UPDATE_WAREHOUSE permission
- Warehouse exists

**Main Flow**:
1. User views warehouse detail
2. User clicks "Change Status" button
3. System displays status options:
   - ACTIVE: Warehouse is operational
   - INACTIVE: Warehouse is closed (no transactions allowed)
   - MAINTENANCE: Warehouse is under maintenance (read-only)
4. User selects new status
5. User provides reason (optional, max 500 chars)
6. User clicks "Confirm"
7. System validates status transition
8. System updates warehouse status
9. System logs status change with reason in audit log
10. System displays success message

**Alternative Flow 1**: Warehouse Has Active Transactions
- 7a. If changing to INACTIVE and warehouse has pending inbound/outbound orders → display warning
- User must either confirm force close or cancel

**Business Rules**:
- ACTIVE → INACTIVE: Allowed only if no pending transactions
- INACTIVE → ACTIVE: Always allowed
- Any status → MAINTENANCE: Always allowed
- MAINTENANCE → ACTIVE: Allowed after maintenance completion

**Post-conditions**:
- Warehouse status is updated
- Status change is logged in audit trail
- System behavior adapts to new status

---

### Acceptance Criteria: AC-MD-WAREHOUSE

**AC-MD-WH-01**: Create Warehouse
```gherkin
Given I am authenticated as System Admin
When I submit a warehouse creation form with valid data
Then a new warehouse record is created with status ACTIVE
And the warehouse code must be unique
And all audit fields (created_by, created_at) are populated
And I receive a success message
```

**AC-MD-WH-02**: Validation Rules
```gherkin
Given I am creating or updating a warehouse
When I submit the form
Then code must be alphanumeric, 1-20 characters
And name must not be empty, max 100 characters
And email must be valid format if provided
And type must be one of: MAIN, SATELLITE, TRANSIT, RETURN
And capacity must be positive number if provided
```

**AC-MD-WH-03**: Search and Filter
```gherkin
Given I am on the warehouse list page
When I search by code or name
Then the system returns matching warehouses
And I can filter by status (ACTIVE, INACTIVE, MAINTENANCE)
And I can filter by type
And results are paginated (20 per page by default)
```

**AC-MD-WH-04**: Soft Delete Prevention
```gherkin
Given a warehouse has locations or inventory
When I try to delete the warehouse
Then the system prevents hard delete
And suggests changing status to INACTIVE instead
```

**AC-MD-WH-05**: Manager Assignment
```gherkin
Given I am assigning a manager to a warehouse
When I select a user account
Then only accounts with WAREHOUSE_MANAGER or ADMIN role are available
And one user can manage multiple warehouses
And a warehouse can have only one manager
```

---

## 📋 Feature 2: Location Management

### User Stories

**US-MD-05**: As a Warehouse Manager, I want to define storage locations within my warehouse, so that inventory can be stored in organized positions.

**US-MD-06**: As an Inventory Controller, I want to view location capacity and current utilization, so that I can place items in available locations.

**US-MD-07**: As a Warehouse Manager, I want to assign zones to locations (e.g., Zone A, Zone B), so that I can group related storage areas.

**US-MD-08**: As a Warehouse Manager, I want to mark locations as FULL when they reach capacity, so that no more items are assigned there.

---

### Use Case: UC-MD-04 - Create Location

**Primary Actor**: Warehouse Manager  
**Goal**: Create a new storage location within a warehouse  

**Pre-conditions**:
- User has CREATE_LOCATION permission
- Target warehouse exists and is ACTIVE

**Main Flow**:
1. User navigates to Warehouse Detail page
2. User clicks "Manage Locations" tab
3. User clicks "Add New Location" button
4. System displays location creation form
5. User enters location details:
   - Code (unique within warehouse, max 50 chars)
   - Name (required, max 100 chars)
   - Zone (optional, max 50 chars, e.g., "Zone-A", "Freezer-1")
   - Type (STORAGE, PICKING, PACKING, STAGING, RETURN)
   - Capacity (in cubic meters, optional)
   - Notes (optional)
6. User clicks "Save"
7. System validates all fields
8. System checks code uniqueness within the warehouse
9. System creates location record with status = ACTIVE
10. System logs audit info
11. System displays success message
12. System adds location to warehouse's location list

**Alternative Flow 1**: Duplicate Code
- 8a. If location code already exists in this warehouse → display error
- User changes code and resubmits

**Alternative Flow 2**: Warehouse Inactive
- Pre-condition fails: User cannot add locations to INACTIVE warehouses

**Post-conditions**:
- New location is created and linked to warehouse
- Location is available for inventory placement
- Audit trail is recorded

---

### Use Case: UC-MD-05 - Update Location Status

**Primary Actor**: Warehouse Manager  
**Goal**: Change location status based on operational needs  

**Pre-conditions**:
- User has UPDATE_LOCATION permission
- Location exists

**Main Flow**:
1. User views location detail
2. User clicks "Change Status" button
3. System displays status options:
   - ACTIVE: Available for storage
   - INACTIVE: Not in use
   - FULL: At capacity (system can also auto-set this)
   - MAINTENANCE: Under repair
4. User selects new status
5. User clicks "Confirm"
6. System validates transition
7. System updates location status
8. System displays success message

**Business Rules**:
- If location has inventory and status changes to INACTIVE → display warning
- System can auto-set status to FULL when inventory reaches capacity
- FULL locations should not accept new inventory

**Post-conditions**:
- Location status is updated
- Status affects inventory placement logic

---

### Acceptance Criteria: AC-MD-LOCATION

**AC-MD-LOC-01**: Create Location
```gherkin
Given I am a Warehouse Manager for warehouse W1
When I create a location with code "A01-R01-B01" in warehouse W1
Then the location is created successfully
And location code must be unique within warehouse W1
And location is linked to warehouse W1
```

**AC-MD-LOC-02**: Location Code Uniqueness
```gherkin
Given warehouse W1 has a location with code "A01"
When I try to create another location with code "A01" in warehouse W1
Then the system rejects it with error "Location code already exists"
But I can create location "A01" in warehouse W2 (different warehouse)
```

**AC-MD-LOC-03**: Search Locations
```gherkin
Given I am viewing warehouse locations
When I search by code or zone
Then the system returns matching locations
And I can filter by status
And I can filter by type
And results show current capacity utilization
```

**AC-MD-LOC-04**: Bulk Create Locations
```gherkin
Given I am a Warehouse Manager
When I upload a CSV file with location data
Then the system validates all rows
And creates all valid locations
And returns error report for invalid rows
And logs all creations in audit trail
```

---

## 📋 Feature 3: Product Management

### User Stories

**US-MD-09**: As a Data Entry Operator, I want to create new product records with complete information, so that products can be tracked in inventory.

**US-MD-10**: As an Inventory Controller, I want to set minimum and maximum stock levels for products, so that the system can alert me when stock is low.

**US-MD-11**: As a System Admin, I want to import products in bulk from Excel, so that I can quickly onboard hundreds of products.

**US-MD-12**: As a Warehouse Manager, I want to enable batch tracking for products with expiry dates, so that I can manage FIFO/FEFO properly.

**US-MD-13**: As a Data Entry Operator, I want to search products by SKU, name, or category, so that I can quickly find and update product information.

---

### Use Case: UC-MD-06 - Create Product

**Primary Actor**: Data Entry Operator  
**Goal**: Create a new product record  

**Pre-conditions**:
- User has CREATE_PRODUCT permission
- At least one UOM exists in the system

**Main Flow**:
1. User navigates to Product Management page
2. User clicks "Create New Product" button
3. System displays product creation form
4. User enters product details:
   - SKU (unique, alphanumeric, max 50 chars)
   - Name (required, max 200 chars)
   - Description (optional, text)
   - Category (optional, max 50 chars, e.g., "Electronics", "Food")
   - UOM (required, select from dropdown)
   - Weight (optional, in KG)
   - Dimensions (optional, format: LxWxH in CM)
   - Min Stock Level (optional, default 0)
   - Max Stock Level (optional)
   - Reorder Point (optional)
   - Cost Price (optional)
   - Selling Price (optional)
   - Barcode (optional, max 100 chars)
   - Image URL (optional)
   - Requires Batch Tracking (checkbox, default false)
5. User clicks "Save"
6. System validates all fields
7. System checks SKU uniqueness
8. System creates product record with status = ACTIVE
9. System logs audit info
10. System displays success message
11. System redirects to product detail page

**Alternative Flow 1**: Duplicate SKU
- 7a. If SKU already exists → display error "Product SKU already exists"
- User changes SKU and resubmits

**Alternative Flow 2**: Invalid Data Format
- 6a. If weight is negative → display error
- 6b. If dimensions format is wrong → display format hint "LxWxH in CM, e.g., 10x20x30"
- 6c. If prices are negative → display error

**Post-conditions**:
- New product is created with ACTIVE status
- Product is available for use in orders and inventory
- If batch tracking enabled, all future inventory must have batch info

---

### Use Case: UC-MD-07 - Update Product

**Primary Actor**: Data Entry Operator or Inventory Controller  
**Goal**: Modify product information  

**Pre-conditions**:
- User has UPDATE_PRODUCT permission
- Product exists

**Main Flow**:
1. User searches and selects product to edit
2. System displays product detail with "Edit" button
3. User clicks "Edit"
4. System displays product update form with current values
5. User modifies fields (SKU is read-only after creation)
6. User clicks "Save"
7. System validates changes
8. System checks if critical fields changed (e.g., enabling batch tracking)
9. If critical changes detected → system displays warning
10. User confirms changes
11. System updates product record
12. System updates `updated_by` and `updated_at`
13. System displays success message

**Alternative Flow 1**: Enable Batch Tracking on Existing Product
- 8a. If user enables batch tracking for product with existing inventory → display warning
- Warning: "This product has existing inventory. Future transactions will require batch information."
- User must acknowledge before proceeding

**Business Rules**:
- SKU cannot be changed after creation
- Disabling batch tracking is not allowed if product has batch-tracked inventory
- Status can be changed to INACTIVE or DISCONTINUED but not deleted if inventory exists

**Post-conditions**:
- Product information is updated
- Changes are logged in audit trail

---

### Use Case: UC-MD-08 - Bulk Import Products

**Primary Actor**: System Admin  
**Goal**: Import multiple products from Excel file  

**Pre-conditions**:
- User has CREATE_PRODUCT permission
- User has valid Excel file with product data

**Main Flow**:
1. User navigates to Product Management page
2. User clicks "Import Products" button
3. System displays file upload dialog
4. System provides Excel template download link
5. User uploads Excel file (.xlsx or .csv)
6. System validates file format
7. System reads all rows and validates each:
   - SKU uniqueness
   - Required fields presence
   - Data format correctness
   - UOM code existence
8. System displays validation summary:
   - Total rows: X
   - Valid rows: Y
   - Invalid rows: Z (with error details)
9. User reviews errors
10. User clicks "Import Valid Rows"
11. System creates products for all valid rows
12. System generates import report
13. System sends report via email or download link
14. System displays success message with summary

**Alternative Flow 1**: All Rows Invalid
- 8a. If all rows fail validation → display error message
- User must fix Excel file and re-upload

**Alternative Flow 2**: Partial Success
- 11a. Some rows fail due to concurrent SKU creation → include in failure report
- User can re-import failed rows after correction

**Post-conditions**:
- Valid products are created
- Import log is recorded
- Audit trail captures bulk import event

---

### Use Case: UC-MD-09 - Search and Filter Products

**Primary Actor**: Any authenticated user  
**Goal**: Find products using various criteria  

**Pre-conditions**:
- User is authenticated
- User has VIEW_PRODUCT permission

**Main Flow**:
1. User navigates to Product List page
2. System displays search and filter panel
3. User can apply filters:
   - Search text (searches SKU, name, description)
   - Category (dropdown)
   - Status (ACTIVE, INACTIVE, DISCONTINUED)
   - Batch tracking enabled (yes/no)
   - Stock level range (min-max)
4. User enters search criteria
5. System queries products matching criteria
6. System returns paginated results (20 per page)
7. User can sort by: SKU, name, category, created date

**Post-conditions**:
- User sees relevant products
- Search criteria are preserved for pagination

---

### Acceptance Criteria: AC-MD-PRODUCT

**AC-MD-PROD-01**: Create Product
```gherkin
Given I am a Data Entry Operator
When I create a product with valid SKU, name, and UOM
Then the product is created with status ACTIVE
And SKU must be unique across all products
And all audit fields are populated
```

**AC-MD-PROD-02**: SKU Validation
```gherkin
Given I am creating a product
When I enter SKU "PROD-001"
Then the system accepts alphanumeric characters and hyphens
And SKU length must be 1-50 characters
And SKU must be unique
And SKU is case-insensitive for uniqueness check
```

**AC-MD-PROD-03**: Batch Tracking
```gherkin
Given I create a product with batch tracking enabled
When inventory is received for this product
Then batch number, manufacture date, and expiry date must be provided
And the system creates/links batch records automatically
```

**AC-MD-PROD-04**: Stock Level Thresholds
```gherkin
Given I set min_stock_level = 10 and reorder_point = 20 for a product
When current stock falls below 20
Then the system generates a low stock alert
And when stock falls below 10
Then the system generates a critical stock alert
```

**AC-MD-PROD-05**: Bulk Import
```gherkin
Given I upload an Excel file with 100 product rows
When 95 rows are valid and 5 have errors
Then the system imports 95 products successfully
And returns a report listing 5 failed rows with error reasons
And all imports are logged in audit trail
```

**AC-MD-PROD-06**: Product Search
```gherkin
Given there are products with SKU "LAPTOP-001" and name "Dell Laptop"
When I search for "LAPTOP"
Then the system returns all products matching SKU or name
And search is case-insensitive
And results are paginated
```

**AC-MD-PROD-07**: Product Status Change
```gherkin
Given a product with status ACTIVE
When I change status to INACTIVE
Then the product cannot be used in new orders
But existing inventory and orders remain unaffected
And when I change to DISCONTINUED
Then the product is flagged for phase-out
```

---

## 📋 Feature 4: Unit of Measure (UOM) Management

### User Stories

**US-MD-14**: As a System Admin, I want to define standard units of measure (pieces, boxes, kg, liters), so that products can be measured consistently.

**US-MD-15**: As a Data Entry Operator, I want to view all available UOMs when creating products, so that I select the correct unit.

---

### Use Case: UC-MD-10 - Create Unit of Measure

**Primary Actor**: System Admin  
**Goal**: Define a new unit of measure  

**Pre-conditions**:
- User has CREATE_UOM permission

**Main Flow**:
1. User navigates to UOM Management page
2. User clicks "Create New UOM" button
3. System displays UOM creation form
4. User enters:
   - Code (unique, max 10 chars, e.g., "PCS", "KG", "BOX")
   - Name (required, max 50 chars, e.g., "Pieces", "Kilogram")
   - Description (optional, max 255 chars)
5. User clicks "Save"
6. System validates code uniqueness
7. System creates UOM record
8. System displays success message

**Alternative Flow 1**: Duplicate Code
- 6a. If UOM code exists → display error
- User changes code and resubmits

**Post-conditions**:
- New UOM is available for product creation

---

### Acceptance Criteria: AC-MD-UOM

**AC-MD-UOM-01**: Create UOM
```gherkin
Given I am a System Admin
When I create a UOM with code "PCS" and name "Pieces"
Then the UOM is created successfully
And code must be unique
And code is stored in uppercase
```

**AC-MD-UOM-02**: UOM Deletion Prevention
```gherkin
Given a UOM is used by existing products
When I try to delete the UOM
Then the system prevents deletion
And displays error "UOM is in use by X products"
```

**AC-MD-UOM-03**: Pre-populated UOMs
```gherkin
Given the system is freshly installed
When I view the UOM list
Then default UOMs are present: PCS, BOX, KG, LITER, METER
```

---

## 📋 Feature 5: Business Partner Management

### User Stories

**US-MD-16**: As a Data Entry Operator, I want to create supplier records, so that we can track who we purchase from.

**US-MD-17**: As a Data Entry Operator, I want to create customer records, so that we can track who we sell to.

**US-MD-18**: As an Accountant, I want to set credit limits for customers, so that sales orders respect financial constraints.

**US-MD-19**: As a Warehouse Manager, I want to view supplier contact information, so that I can coordinate incoming shipments.

---

### Use Case: UC-MD-11 - Create Business Partner

**Primary Actor**: Data Entry Operator  
**Goal**: Create a new supplier or customer record  

**Pre-conditions**:
- User has CREATE_PARTNER permission

**Main Flow**:
1. User navigates to Business Partner Management page
2. User clicks "Create New Partner" button
3. System displays partner creation form
4. User enters partner details:
   - Code (unique, alphanumeric, max 20 chars)
   - Name (required, max 200 chars)
   - Type (SUPPLIER, CUSTOMER, BOTH)
   - Contact Person (optional, max 100 chars)
   - Email (optional, valid format)
   - Phone (optional, max 20 chars)
   - Address details (address, city, country)
   - Tax ID (optional, max 50 chars)
   - Payment Terms (optional, max 100 chars, e.g., "Net 30")
   - Credit Limit (optional, for customers)
   - Notes (optional)
5. User clicks "Save"
6. System validates all fields
7. System checks code uniqueness
8. System creates partner record with status = ACTIVE
9. System logs audit info
10. System displays success message

**Alternative Flow 1**: Duplicate Code
- 7a. If partner code exists → display error
- User changes code and resubmits

**Post-conditions**:
- New business partner is created
- Partner is available for use in purchase/sales orders

---

### Use Case: UC-MD-12 - Update Business Partner

**Primary Actor**: Data Entry Operator or Accountant  
**Goal**: Modify business partner information  

**Pre-conditions**:
- User has UPDATE_PARTNER permission
- Partner exists

**Main Flow**:
1. User searches and selects partner to edit
2. System displays partner detail with "Edit" button
3. User clicks "Edit"
4. System displays partner update form
5. User modifies fields (code is read-only)
6. User clicks "Save"
7. System validates changes
8. System updates partner record
9. System updates audit fields
10. System displays success message

**Business Rules**:
- Accountant role can update credit_limit and payment_terms
- Other roles cannot modify financial fields
- Code cannot be changed after creation

**Post-conditions**:
- Partner information is updated
- Changes are logged in audit trail

---

### Use Case: UC-MD-13 - Change Partner Status

**Primary Actor**: System Admin or Accountant  
**Goal**: Activate, deactivate, or blacklist a business partner  

**Pre-conditions**:
- User has UPDATE_PARTNER permission
- Partner exists

**Main Flow**:
1. User views partner detail
2. User clicks "Change Status" button
3. System displays status options:
   - ACTIVE: Normal operations
   - INACTIVE: Temporarily suspended
   - BLACKLISTED: Permanently blocked
4. User selects new status
5. User provides reason (required for BLACKLISTED)
6. User clicks "Confirm"
7. System validates transition
8. System updates partner status
9. System logs status change with reason
10. System displays success message

**Business Rules**:
- BLACKLISTED partners cannot be used in new orders
- Changing to INACTIVE displays warning if partner has pending orders
- Only System Admin can blacklist partners

**Post-conditions**:
- Partner status is updated
- Status affects order creation logic

---

### Acceptance Criteria: AC-MD-PARTNER

**AC-MD-PARTNER-01**: Create Business Partner
```gherkin
Given I am a Data Entry Operator
When I create a supplier with valid code and name
Then the supplier is created with status ACTIVE
And code must be unique across all partners
And type must be SUPPLIER, CUSTOMER, or BOTH
```

**AC-MD-PARTNER-02**: Partner Type Filtering
```gherkin
Given I have partners of different types
When I create a purchase order
Then the system shows only SUPPLIER or BOTH types in partner dropdown
And when I create a sales order
Then the system shows only CUSTOMER or BOTH types
```

**AC-MD-PARTNER-03**: Credit Limit Validation
```gherkin
Given a customer has credit_limit = 10000
When a sales order would exceed this limit
Then the system displays a warning
And requires manager approval to proceed
```

**AC-MD-PARTNER-04**: Search Partners
```gherkin
Given there are multiple business partners
When I search by code or name
Then the system returns matching partners
And I can filter by type (SUPPLIER/CUSTOMER/BOTH)
And I can filter by status
```

**AC-MD-PARTNER-05**: Blacklist Prevention
```gherkin
Given a partner is BLACKLISTED
When I try to create a purchase or sales order with this partner
Then the system prevents order creation
And displays error "Partner is blacklisted"
```

---

## 📋 Feature 6: Category Management

See `FEATURE_6_CATEGORY_MANAGEMENT.md`.

---

## 📋 API Impact Summary

### New Endpoints Required

#### Warehouse APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/warehouses` | List all warehouses with filtering | All authenticated |
| GET | `/api/warehouses/{id}` | Get warehouse details | All authenticated |
| POST | `/api/warehouses` | Create new warehouse | ADMIN |
| PUT | `/api/warehouses/{id}` | Update warehouse | ADMIN, WAREHOUSE_MANAGER |
| PATCH | `/api/warehouses/{id}/status` | Change warehouse status | ADMIN |
| GET | `/api/warehouses/{id}/locations` | Get locations in warehouse | All authenticated |

#### Location APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/locations` | List all locations with filtering | All authenticated |
| GET | `/api/locations/{id}` | Get location details | All authenticated |
| POST | `/api/locations` | Create new location | ADMIN, WAREHOUSE_MANAGER |
| PUT | `/api/locations/{id}` | Update location | ADMIN, WAREHOUSE_MANAGER |
| PATCH | `/api/locations/{id}/status` | Change location status | WAREHOUSE_MANAGER |
| POST | `/api/locations/bulk` | Bulk create locations from CSV | ADMIN, WAREHOUSE_MANAGER |

#### Product APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/products` | List products with search and filters | All authenticated |
| GET | `/api/products/{id}` | Get product details | All authenticated |
| GET | `/api/products/sku/{sku}` | Get product by SKU | All authenticated |
| POST | `/api/products` | Create new product | ADMIN, DATA_ENTRY |
| PUT | `/api/products/{id}` | Update product | ADMIN, DATA_ENTRY, INVENTORY_CONTROLLER |
| PATCH | `/api/products/{id}/status` | Change product status | ADMIN |
| POST | `/api/products/import` | Bulk import products from Excel | ADMIN |
| GET | `/api/products/import/template` | Download import template | ADMIN, DATA_ENTRY |
| GET | `/api/products/export` | Export products to Excel | All authenticated |

#### UOM APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/uoms` | List all units of measure | All authenticated |
| GET | `/api/uoms/{id}` | Get UOM details | All authenticated |
| POST | `/api/uoms` | Create new UOM | ADMIN |
| PUT | `/api/uoms/{id}` | Update UOM | ADMIN |
| DELETE | `/api/uoms/{id}` | Delete UOM (if not in use) | ADMIN |

#### Business Partner APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/partners` | List partners with filtering | All authenticated |
| GET | `/api/partners/{id}` | Get partner details | All authenticated |
| POST | `/api/partners` | Create new partner | ADMIN, DATA_ENTRY |
| PUT | `/api/partners/{id}` | Update partner | ADMIN, DATA_ENTRY, ACCOUNTANT |
| PATCH | `/api/partners/{id}/status` | Change partner status | ADMIN, ACCOUNTANT |
| GET | `/api/partners/suppliers` | List only suppliers | All authenticated |
| GET | `/api/partners/customers` | List only customers | All authenticated |

#### Category APIs
| Method | Endpoint | Description | Roles |
|--------|----------|-------------|-------|
| GET | `/api/categories` | List categories with filtering | All authenticated |
| GET | `/api/categories/{id}` | Get category details | All authenticated |
| POST | `/api/categories` | Create new category | ADMIN |
| PUT | `/api/categories/{id}` | Update category | ADMIN |
| PATCH | `/api/categories/{id}/status` | Change category status | ADMIN |

---

## 🗄️ Database Impact Summary

### Tables Created/Modified

All tables are already defined in `04_DATABASE_SCHEMA.md`. This module implements CRUD operations for:

1. **warehouses** - Complete CRUD
2. **locations** - Complete CRUD + bulk creation
3. **products** - Complete CRUD + bulk import/export
4. **product_categories** - Complete CRUD
5. **units_of_measure** - Complete CRUD
6. **business_partners** - Complete CRUD

### Indexes Required

Already defined in schema, but ensure these are present for performance:

**warehouses**:
- `idx_code` (for code-based lookups)
- `idx_status` (for filtering active warehouses)
- `idx_type` (for filtering by warehouse type)

**locations**:
- `idx_warehouse_id` (for listing locations by warehouse)
- `idx_zone` (for zone-based queries)
- `idx_status` (for filtering available locations)
- `uk_warehouse_code` (unique constraint for code within warehouse)

**products**:
- `idx_sku` (for SKU-based lookups - most common)
- `idx_status` (for filtering active products)
- `idx_category_id` (for category filtering)
- `idx_name` (for name-based sorting)
- `idx_search` (full-text search on name and description)

**product_categories**:
- `idx_code` (for code-based lookups)
- `idx_status` (for filtering active categories)
- `idx_name` (for name-based sorting)

**business_partners**:
- `idx_code` (for code-based lookups)
- `idx_type` (for filtering suppliers/customers)
- `idx_status` (for filtering active partners)
- `idx_name` (for name-based sorting)

---

## ⚙️ Background Job Requirements

### Job 1: Product Import Processing

**Purpose**: Process large Excel files asynchronously to avoid timeout

**Trigger**: User uploads product import file via API

**Flow**:
1. API receives file upload
2. API validates file format and stores file temporarily
3. API queues import job to RabbitMQ with queue name: `product.import`
4. API returns job ID to user immediately
5. Worker consumes job from queue
6. Worker processes all rows, validates, and imports
7. Worker generates import report
8. Worker sends email notification with report attachment
9. Worker updates job status to COMPLETED or FAILED

**Retry Logic**: 3 retries with exponential backoff

---

### Job 2: Product Export Processing

**Purpose**: Generate Excel export of products (potentially thousands of rows)

**Trigger**: User clicks "Export Products" with filters

**Flow**:
1. API receives export request with filter criteria
2. API queues export job to RabbitMQ with queue name: `product.export`
3. API returns job ID to user
4. Worker consumes job
5. Worker queries products matching filters
6. Worker generates Excel file
7. Worker uploads file to temporary storage (S3 or local)
8. Worker sends notification with download link
9. File expires after 24 hours

---

### Job 3: Low Stock Alerts

**Purpose**: Notify inventory controllers when products fall below reorder point

**Trigger**: Scheduled job (runs every 6 hours) or triggered after stock movement

**Flow**:
1. System queries products where current_stock < reorder_point
2. System groups products by warehouse
3. System sends email alert to relevant warehouse managers
4. System logs alert in notification table

**Frequency**: Every 6 hours or after inventory decrease

---

## 📝 Validation Rules Summary

### Warehouse Validation
- `code`: Required, alphanumeric, 1-20 chars, unique
- `name`: Required, max 100 chars
- `email`: Valid email format if provided
- `type`: Must be MAIN, SATELLITE, TRANSIT, or RETURN
- `capacity`: Positive number if provided

### Location Validation
- `code`: Required, max 50 chars, unique within warehouse
- `name`: Required, max 100 chars
- `warehouse_id`: Must reference existing active warehouse
- `type`: Must be STORAGE, PICKING, PACKING, STAGING, or RETURN
- `capacity`: Positive number if provided

### Product Validation
- `sku`: Required, alphanumeric with hyphens/underscores, 1-50 chars, unique (case-insensitive)
- `name`: Required, max 200 chars
- `uom_id`: Must reference existing UOM
- `category_id`: Optional, must reference existing ACTIVE category
- `weight`: Positive number if provided
- `dimensions`: Format "LxWxH" in CM if provided
- `min_stock_level`: Non-negative if provided
- `max_stock_level`: Must be >= min_stock_level if both provided
- `reorder_point`: Should be >= min_stock_level if provided
- `cost_price`, `selling_price`: Non-negative if provided

### Category Validation
- `code`: Required, max 20 chars, unique, stored uppercase
- `name`: Required, max 100 chars
- `status`: Must be ACTIVE or INACTIVE

### UOM Validation
- `code`: Required, max 10 chars, unique, stored in uppercase
- `name`: Required, max 50 chars

### Business Partner Validation
- `code`: Required, alphanumeric, 1-20 chars, unique
- `name`: Required, max 200 chars
- `type`: Must be SUPPLIER, CUSTOMER, or BOTH
- `email`: Valid email format if provided
- `credit_limit`: Non-negative if provided

---

## 🔄 Data Flow Examples

### Example 1: Create Warehouse → Create Locations

```
User Action: Create Warehouse
   ↓
POST /api/warehouses
   ↓
Validate & Create warehouse record
   ↓
Return warehouse_id
   ↓
User Action: Create Location
   ↓
POST /api/locations (with warehouse_id)
   ↓
Validate warehouse exists and is ACTIVE
   ↓
Create location linked to warehouse
```

### Example 2: Bulk Import Products

```
User Action: Upload Excel file
   ↓
POST /api/products/import
   ↓
Validate file format
   ↓
Queue job to RabbitMQ: product.import
   ↓
Return job_id immediately
   ↓
[Async] Worker picks up job
   ↓
Read and validate all rows
   ↓
Create products for valid rows
   ↓
Generate report (success + errors)
   ↓
Send email with report
```

### Example 3: Product Search with Filters

```
User Action: Search "laptop" + category_id=10
   ↓
GET /api/products?search=laptop&category_id=10&status=ACTIVE
   ↓
Query: WHERE (name LIKE '%laptop%' OR sku LIKE '%laptop%')
       AND category_id = 10
       AND status = 'ACTIVE'
   ↓
Return paginated results
```

---

## ✅ Implementation Checklist

### Phase 1: Core Entities (Week 1)
- [ ] Create Entity classes (Warehouse, Location, Product, UOM, BusinessPartner)
- [ ] Create Repository interfaces
- [ ] Write Flyway migration scripts
- [ ] Set up database with sample data

### Phase 2: Basic CRUD (Week 2)
- [ ] Implement Service layer for all entities
- [ ] Implement Controller layer with basic CRUD
- [ ] Add validation using `jakarta.validation`
- [ ] Add global exception handling
- [ ] Write unit tests for services

### Phase 3: Advanced Features (Week 3)
- [ ] Implement search and filter logic
- [ ] Add pagination support
- [ ] Implement status change workflows
- [ ] Add business rule validations

### Phase 4: Bulk Operations (Week 4)
- [ ] Implement product import (Excel)
- [ ] Implement product export (Excel)
- [ ] Create RabbitMQ queues
- [ ] Implement async workers
- [ ] Add email notifications

### Phase 5: Testing & Documentation (Week 5)
- [ ] Write integration tests
- [ ] Test all API endpoints with Postman
- [ ] Generate OpenAPI documentation
- [ ] Update API documentation
- [ ] Performance testing with 10k+ products

---

## 📚 Related Documents

- [01_PROJECT_OVERVIEW.md](../CORE/01_PROJECT_OVERVIEW.md) - Overall system context
- [02_SYSTEM_ARCHITECTURE.md](../CORE/02_SYSTEM_ARCHITECTURE.md) - Technical architecture
- [04_DATABASE_SCHEMA.md](../CORE/04_DATABASE_SCHEMA.md) - Complete database schema
- [05_IMPLEMENTATION_GUIDE.md](../CORE/05_IMPLEMENTATION_GUIDE.md) - Development guidelines

---

## 📞 Questions & Clarifications

If you need clarification on any requirement, please contact:
- **Business Owner**: [TBD]
- **Product Manager**: [TBD]
- **Technical Lead**: [TBD]

---

**Document Status**: ✅ Ready for Implementation  
**Approval Date**: [Pending]  
**Approved By**: [Pending]
