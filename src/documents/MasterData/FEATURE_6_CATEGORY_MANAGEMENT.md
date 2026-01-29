# Feature 6: Category Management
## Business Requirements Specification

---

## Document Information

| Property | Value |
|----------|-------|
| Module | Master Data Management |
| Feature | Feature 6: Category Management |
| Version | 1.0 |
| Date | January 29, 2026 |
| Status | Draft |
| Author | Business Analyst |

---

## Step 1 - Clarify Context

### Actors
- System Admin
- Data Entry Operator
- Inventory Controller (read-only)

### Goals
- Maintain standardized product categories.
- Support product search and reporting by category.
- Prevent invalid category references in products.

### Triggers
- New product line introduction.
- Re-organization of catalog structure.
- Reporting needs by category.

### Pain Points
- Category values embedded in products cause inconsistency.
- Duplicate or misspelled categories.
- Hard to maintain filters when category naming changes.

---

## Step 2 - User Stories

- As a System Admin, I want to create and manage categories, so that products can be classified consistently.
- As a Data Entry Operator, I want to select an existing category when creating a product, so that product data stays standardized.
- As an Inventory Controller, I want to filter products by category, so that reporting is easier.

---

## Step 3 - Use Case Specifications

### UC-MD-14 - Create Category

**Brief Description**: Create a new category with unique code.

**Primary Actor**: System Admin

**Pre-conditions**:
- User has CREATE_CATEGORY permission.

**Post-conditions**:
- Category created with status ACTIVE.
- Category available for product assignment.

**Main Flow**:
1. User opens Category Management.
2. User clicks "Create New Category".
3. System displays creation form.
4. User enters:
   - code (unique, 1-20, stored uppercase)
   - name (required, max 100)
   - description (optional, max 255)
5. User clicks Save.
6. System validates fields.
7. System checks code uniqueness.
8. System creates category.
9. System returns success.

**Alternative Flows**:
- A1: Duplicate code
  - System returns error "Category code already exists".

**Rules & Constraints**:
- Code is stored uppercase.
- Code is immutable after creation.

---

### UC-MD-15 - Update Category

**Brief Description**: Update category name or description; code cannot change.

**Primary Actor**: System Admin

**Pre-conditions**:
- User has UPDATE_CATEGORY permission.
- Category exists.

**Post-conditions**:
- Category updated with audit fields.

**Main Flow**:
1. User opens category detail.
2. User clicks Edit.
3. System displays update form (code read-only).
4. User edits name/description.
5. User clicks Save.
6. System validates fields.
7. System updates category.
8. System returns success.

**Rules & Constraints**:
- Code is immutable.
- name is required, max 100.

---

### UC-MD-16 - Change Category Status

**Brief Description**: Deactivate category if no active products use it.

**Primary Actor**: System Admin

**Pre-conditions**:
- User has UPDATE_CATEGORY permission.
- Category exists.

**Post-conditions**:
- Status updated to ACTIVE or INACTIVE.

**Main Flow**:
1. User opens category detail.
2. User clicks "Change Status".
3. System displays status options: ACTIVE, INACTIVE.
4. User confirms status change.
5. System checks for active products linked to category.
6. System updates status if allowed.
7. System returns success.

**Alternative Flows**:
- A1: Category in use by active products
  - System rejects deactivation with error "Category is in use by active products".

**Rules & Constraints**:
- INACTIVE categories cannot be assigned to new products.

---

### UC-MD-17 - List and Search Categories

**Brief Description**: List and search categories for product assignment.

**Primary Actor**: Any authenticated user

**Pre-conditions**:
- User has VIEW_CATEGORY permission.

**Post-conditions**:
- User sees paginated list of categories.

**Main Flow**:
1. User opens category list.
2. User searches by code or name.
3. System returns matching results with pagination.

---

## Step 4 - Acceptance Criteria

**AC-MD-CAT-01 - Create Category**
```gherkin
Given I am a System Admin
When I create a category with code "ELEC" and name "Electronics"
Then the category is created successfully
And code is unique
And code is stored in uppercase
```

**AC-MD-CAT-02 - Prevent Inactivation if In Use**
```gherkin
Given a category is linked to active products
When I try to set the category status to INACTIVE
Then the system rejects the request with error "Category is in use by active products"
```

**AC-MD-CAT-03 - List Categories**
```gherkin
Given I am creating a product
When I open the category dropdown
Then all ACTIVE categories are listed
```

**AC-MD-CAT-04 - Update Category**
```gherkin
Given I am a System Admin
When I update category name from "Electronics" to "Electronic Devices"
Then the category is updated successfully
And the code remains unchanged
```

---

## Step 5 - Backend Impact Analysis

### API Impacts
- `GET /api/categories`
- `GET /api/categories/{id}`
- `POST /api/categories`
- `PUT /api/categories/{id}`
- `PATCH /api/categories/{id}/status`

### Request Validation Rules
- code: required, 1-20, unique, stored uppercase
- name: required, max 100
- status: ACTIVE or INACTIVE

### Response Data
- id, code, name, description, status
- created_by, created_at, updated_by, updated_at

### Error Codes
- CATEGORY_CODE_DUPLICATE
- CATEGORY_NOT_FOUND
- CATEGORY_IN_USE
- CATEGORY_INACTIVE

### Database Impacts
- Table: `product_categories`
- Columns: code, name, description, status, created_at, updated_at
- Indexes: idx_code, idx_status, idx_name

### Async / Background Jobs
- None required for this feature.

---

## BA Review Checklist

- [x] Actors identified
- [x] Preconditions defined
- [x] Business rules documented
- [x] Validation rules explicit
- [x] Success + alternative + exception flows present
- [x] API and data impacts defined
- [x] Acceptance criteria cover normal and edge cases
