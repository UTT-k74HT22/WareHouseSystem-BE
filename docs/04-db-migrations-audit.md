# DB Migrations Audit (Flyway / Entities / Repositories)

Audit scope:
- Flyway migration scripts under `src/main/resources/db/migration`
- JPA entities under `src/main/java/org/demo/whs/entity`
- Repositories under `src/main/java/org/demo/whs/repository`
- Soft-delete and `created_at` / `updated_at` conventions

## 1) Flyway migration naming/versioning patterns

### Observed pattern
- File naming mostly follows: `VYYYYMMDD_NN__Description.sql`
  - Examples:
    - `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql`
    - `src/main/resources/db/migration/V20260301_03__Create_inbound_operations.sql`
    - `src/main/resources/db/migration/V20260401_02__Create_stock_movements.sql`
- Numbering by date + sequence is consistent at filename level.

### Inconsistencies found

| Area | File reference | Finding |
|---|---|---|
| Header version vs filename | `src/main/resources/db/migration/V20260301_01__Create_batch_management.sql` | Header says `Version: V20260201_01` (does not match filename `V20260301_01`). |
| Header version vs filename | `src/main/resources/db/migration/V20260301_02__Create_inventory_management.sql` | Header says `Version: V20260201_02` (does not match filename `V20260301_02`). |
| Header version vs filename | `src/main/resources/db/migration/V20260401_02__Create_stock_movements.sql` | Header says `Version: V20260201_05` (does not match filename `V20260401_02`). |
| Module comment mismatch | `src/main/resources/db/migration/V20260301_03__Create_inbound_operations.sql` | Top comment says "Create Outbound Operations Module", but SQL creates inbound tables (`purchase_orders`, `inbound_receipts`, ...). |
| Module comment mismatch | `src/main/resources/db/migration/V20260401_01__Create_outbound_operations.sql` | Top comment says "Create Inbound Operations Module", but SQL creates outbound tables (`sales_orders`, `outbound_shipments`, ...). |

## 2) `created_at` / `updated_at` convention coverage

### Convention in code
- `BaseEntity` defines:
  - `created_at`, `updated_at`, `created_by`, `updated_by`
  - lifecycle hooks `@PrePersist` / `@PreUpdate`
- Reference: `src/main/java/org/demo/whs/entity/BaseEntity.java`

### Convention in migrations
Most business tables include both `created_at` and `updated_at`.

### Exceptions (tables without `created_at` / `updated_at`)

| Table | Migration reference | Notes |
|---|---|---|
| `account_roles` | `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql` | Junction table with composite PK only (`account_id`, `role_id`). |
| `role_permissions` | `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql` | Junction table with composite PK only (`role_id`, `permission_id`). |

### Additional alignment notes (entity side)

| Entity file | Finding |
|---|---|
| `src/main/java/org/demo/whs/entity/PurchaseOrders.java` | `@Table(name = "purchase_orders ")` has trailing space. |
| `src/main/java/org/demo/whs/entity/InboundReceiptLines.java` | `@Table(name = "inbound_receipt_lines ")` has trailing space. |
| `src/main/java/org/demo/whs/entity/Permission.java` | Extends `BaseEntity` but is not annotated with `@Entity` / `@Table`. |
| `src/main/java/org/demo/whs/entity/RoleHasPermission.java` | Extends `BaseEntity` but is not annotated with `@Entity` / `@Table`. |

## 3) Soft-delete pattern and repository filtering behavior

### Soft-delete columns
- No `is_deleted` / `deleted_at` (or equivalent timestamp/flag) found in migration table definitions under:
  - `src/main/resources/db/migration/*.sql`

### Entity-level soft-delete behavior
- No Hibernate soft-delete annotations found (`@SQLDelete`, `@Where`, `@SQLRestriction`) in entity package:
  - `src/main/java/org/demo/whs/entity/*`
- `BaseEntity` also has no soft-delete fields:
  - `src/main/java/org/demo/whs/entity/BaseEntity.java`

### Repository filtering behavior
- Repositories do not implement global soft-delete predicates.
- No repository queries filtering by `is_deleted` / `deleted_at`.
- Examples:
  - `src/main/java/org/demo/whs/repository/AccountRepository.java`
  - `src/main/java/org/demo/whs/repository/ProductRepository.java`
  - `src/main/java/org/demo/whs/repository/LocationRepository.java`
  - `src/main/java/org/demo/whs/repository/EmailLogRepository.java`
  - `src/main/java/org/demo/whs/repository/custom/impl/UserProfileRepositoryImpl.java`

### Current practical pattern
- Logical deactivation appears to use status enums (not true soft-delete), e.g. account status includes `DELETED`:
  - migration: `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql` (`accounts.status`)
  - entity: `src/main/java/org/demo/whs/entity/Account.java` (`status` field)

## 4) Entity/Repository coverage observations relevant to DB audit

| DB table | Entity/Repository reference | Observation |
|---|---|---|
| `permissions` | `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql`, `src/main/java/org/demo/whs/entity/Permission.java` | Table exists in migration; entity class exists but is not mapped as `@Entity`; no repository found. |
| `role_permissions` | `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql`, `src/main/java/org/demo/whs/entity/RoleHasPermission.java` | Table exists in migration; class exists but is not mapped as `@Entity`; no repository found. |
| `account_roles` | `src/main/resources/db/migration/V20260107_01__Create_table_rbac.sql`, `src/main/java/org/demo/whs/entity/AccountHasRole.java` | Mapped as `@Entity` with `@EmbeddedId`; no dedicated repository found. |

