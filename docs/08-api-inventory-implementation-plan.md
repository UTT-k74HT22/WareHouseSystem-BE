# API Inventory Implementation Plan (Jira-driven)

Updated: 2026-03-06

## Scope baseline
- Source of truth reviewed: `docs/API_INVENTORY_COMPREHENSIVE.md`
- Codebase check completed against current controllers in `src/main/java/org/demo/whs/controller`
- Existing implemented endpoints were excluded from new task creation (for example: `GET/POST /api/v1/batches`, `GET /api/v1/batches/{id}`)
- Existing Jira tasks were reused, no duplicate tasks created for modules already planned (`Category`, `Inventory`, `Stock Adjustments`, `Stock Transfers`)
- Missing Jira coverage for stock transfer completion was corrected on `2026-03-06` by adding `WHS-87`

## Module task map
- Auth remaining: parent `WHS-28`, subtasks `WHS-29`, `WHS-30`
- Category: parent `WHS-18`, subtasks `WHS-5` .. `WHS-9`
- Inventory: parent `WHS-19`, subtasks `WHS-10` .. `WHS-17`
- Stock Adjustments: parent `WHS-20`, subtasks `WHS-21` .. `WHS-24`
- Stock Transfers: parent `WHS-25`, subtasks `WHS-26`, `WHS-27`, `WHS-87`
- Warehouse remaining: parent `WHS-31`, subtask `WHS-36`
- Location remaining: parent `WHS-32`, subtask `WHS-37`
- Product remaining: parent `WHS-33`, subtasks `WHS-38`, `WHS-39`
- Business Partner remaining: parent `WHS-34`, subtask `WHS-40`
- Batch remaining: parent `WHS-35`, subtasks `WHS-41`, `WHS-42`
- Purchase Orders: parent `WHS-43`, subtasks `WHS-53`, `WHS-54`
- Purchase Order Lines: parent `WHS-44`, subtask `WHS-55`
- Inbound Receipts: parent `WHS-45`, subtasks `WHS-56`, `WHS-57`
- Inbound Receipt Lines: parent `WHS-46`, subtask `WHS-58`
- Sales Orders: parent `WHS-47`, subtasks `WHS-59`, `WHS-60`
- Sales Order Lines: parent `WHS-48`, subtask `WHS-61`
- Outbound Shipments: parent `WHS-49`, subtasks `WHS-62`, `WHS-63`
- Outbound Shipment Lines: parent `WHS-50`, subtask `WHS-65`
- Stock Movements: parent `WHS-51`, subtasks `WHS-64`, `WHS-66`
- Reporting: parent `WHS-52`, subtasks `WHS-67`, `WHS-68`, `WHS-69`

## Execution order (mandatory)
1. Design
- Finalize endpoint contracts, DTOs, validation rules, error codes, authorization matrix.
- Lock workflow/state machine for: PO, Inbound Receipt, SO, Outbound Shipment, Stock Adjustment/Transfer.

2. Migration
- Add/adjust schema only when needed; follow `V{version}__{description}.sql`.
- Add indexes for high-frequency filter/join columns before enabling list/report endpoints at scale.

3. Implementation
- Implement by module in this order:
  - Category/Inventory/Stock Adjustment/Stock Transfer (already planned parents `WHS-18`, `WHS-19`, `WHS-20`, `WHS-25`)
  - Inbound chain: `WHS-43` -> `WHS-44` -> `WHS-45` -> `WHS-46`
  - Outbound chain: `WHS-47` -> `WHS-48` -> `WHS-49` -> `WHS-50`
  - Tracking and analytics: `WHS-35`, `WHS-51`, `WHS-52`
  - Remaining master/auth improvements: `WHS-28`, `WHS-31`, `WHS-32`, `WHS-33`, `WHS-34`

4. Testing
- Unit tests for all service rules and transition constraints.
- Integration tests for:
  - inventory impact (reserve/unreserve/increase/decrease/confirm flows),
  - concurrency-sensitive paths (confirm, stock mutations),
  - validation and authorization failures.

5. Review
- Architecture checks: thin controller, business logic in service, `@Transactional` in service layer.
- Security checks: auth/authz enforced, no sensitive data exposure, strict input validation.
- DB/performance checks: no N+1 in list/report paths, proper pagination, indexes validated.

## Delivery cadence
- Complete and move each module parent to Done only after all child subtasks are Done and checklist is satisfied.
- Prefer small PR slices per subtask: endpoint + service + tests + swagger.
