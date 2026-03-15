# WHS-86 Batch Testing Plan + Coverage - 2026-03-15

## Covered Layers

### Controller tests
Existing and retained:
- `BatchControllerTest`
- `BatchQueryControllerTest`

Covers:
- lifecycle endpoints
- query endpoints
- validation failures
- authenticated access behavior
- response envelope contract

### Service tests
Existing and retained:
- `BatchServiceImplTest`
- `BatchQueryServiceImplTest`

Covers:
- create/update/quarantine/release rules
- blocked generic status patch
- traceability assembly
- expiring filtering
- FIFO eligibility and ranking
- by-product warehouse filtering

### Integration tests added in WHS-86
- `BatchRepositoryQueryIntegrationTest`
- `InventoryRepositoryBatchQueryIntegrationTest`
- `BatchTraceabilityRepositoryIntegrationTest`

Covers:
- repository filter behavior on real persistence layer
- ordering for expiring, by-product, inventory, inbound, outbound, and stock movement queries
- batch/warehouse/product filter combinations used by the new Batch APIs

## Test Command
`./mvnw test -DskipITs "-Dtest=org.demo.whs.service.impl.BatchServiceImplTest,org.demo.whs.controller.BatchControllerTest,org.demo.whs.service.impl.BatchQueryServiceImplTest,org.demo.whs.controller.BatchQueryControllerTest,org.demo.whs.repository.BatchRepositoryQueryIntegrationTest,org.demo.whs.repository.InventoryRepositoryBatchQueryIntegrationTest,org.demo.whs.repository.BatchTraceabilityRepositoryIntegrationTest"`

## Remaining Risk
- No full `@SpringBootTest` end-to-end Batch query scenario was added in this task.
- DB index effectiveness is validated by migration design in `WHS-84`; this task validates repository behavior, not MySQL execution plans.
