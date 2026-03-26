package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.service.LocationService;
import org.demo.whs.service.StockMovementsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Unit Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WareHouseRepository wareHouseRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private StockMovementsService stockMovementsService;

    @Mock
    private StockMovementsMapper stockMovementsMapper;

    @Mock
    private org.redisson.api.RedissonClient redissonClient;

    @Mock
    private org.redisson.api.RLock lock;

    @Mock
    private SalesOrderLinesRepository salesOrderLinesRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private void setupLock() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(java.util.concurrent.TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
    }

    // --- INCREASE TESTS ---

    @Test
    @DisplayName("increase_shouldSucceed_WhenInventoryExists")
    void increase_shouldSucceed_WhenInventoryExists() throws InterruptedException {
        // Arrange
        setupLock();
        String productId = "prod-1";
        String warehouseId = "wh-1";
        BigDecimal increaseQty = new BigDecimal("10.00");

        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(increaseQty)
                .referenceType(ReferenceType.INBOUND_RECEIPT)
                .referenceId("REF-UUID")
                .referenceNumber("REC-001")
                .notes("Test increase")
                .build();

        Products product = new Products(); product.setId(productId);
        Warehouses warehouse = new Warehouses(); warehouse.setId(warehouseId);
        Inventory inventory = Inventory.builder()
                .id("inv-1")
                .productId(productId)
                .warehouseId(warehouseId)
                .onHandQuantity(new BigDecimal("50.00"))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        when(productRepository.findById(anyString())).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById(anyString())).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        inventoryService.increase(request);

        // Assert
        assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("60.00");
        verify(stockMovementsService).recordIncrease(eq(request), any(), any());
    }

    @Test
    @DisplayName("getInventoryByLocation_shouldExcludeQuarantineFromAvailableQuantity")
    void getInventoryByLocation_shouldExcludeQuarantineFromAvailableQuantity() {
        InventoryFilterRequest filter = InventoryFilterRequest.builder().build();
        InventoryLocationProjection projection = InventoryLocationProjection.builder()
                .locationId("loc-1")
                .locationCode("LOC-01")
                .locationName("Receiving")
                .warehouseId("wh-1")
                .warehouseName("Main WH")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Item 1")
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .onHandQuantity(new BigDecimal("20.00"))
                .quarantineQuantity(new BigDecimal("6.00"))
                .reservedQuantity(new BigDecimal("4.00"))
                .build();

        when(inventoryRepository.getInventoryByLocation(filter)).thenReturn(List.of(projection));

        List<InventoryByLocationResponse> response = inventoryService.getInventoryByLocation(filter);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getItems()).hasSize(1);
        assertThat(response.get(0).getItems().get(0).getAvailableQuantity()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("increase_shouldCreateNew_WhenInventoryMissing")
    void increase_shouldCreateNew_WhenInventoryMissing() throws InterruptedException {
        // Arrange
        setupLock();
        String productId = "prod-1"; String warehouseId = "wh-1";
        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId(productId).warehouseId(warehouseId).quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.INBOUND_RECEIPT).referenceNumber("REC-001").build();

        Inventory newInv = Inventory.builder().productId(productId).warehouseId(warehouseId)
                .onHandQuantity(BigDecimal.ZERO).reservedQuantity(BigDecimal.ZERO).build();

        when(productRepository.findById(anyString())).thenReturn(Optional.of(new Products()));
        when(wareHouseRepository.findById(anyString())).thenReturn(Optional.of(new Warehouses()));
        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(inventoryMapper.toEntity(any(InventoryIncreaseRequest.class))).thenReturn(newInv);
        when(inventoryRepository.saveAndFlush(any(Inventory.class))).thenReturn(newInv);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(newInv);

        // Act
        inventoryService.increase(request);

        // Assert
        verify(inventoryRepository).saveAndFlush(any(Inventory.class));
        verify(stockMovementsService).recordIncrease(eq(request), any(), any());
    }

    @Test
    @DisplayName("increase_shouldThrowConflict_WhenBatchProductMismatch")
    void increase_shouldThrowConflict_WhenBatchProductMismatch() throws InterruptedException {
        // Arrange
        setupLock();
        String productId = "prod-1"; String otherProductId = "prod-2";
        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId(productId).warehouseId("wh-1").batchId("batch-1").quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.INBOUND_RECEIPT).referenceNumber("REC-001").build();

        Products product = new Products(); product.setId(productId);
        Batch batch = new Batch(); batch.setId("batch-1"); batch.setProductId(otherProductId);

        when(productRepository.findById(anyString())).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById(anyString())).thenReturn(Optional.of(new Warehouses()));
        when(batchRepository.findById(anyString())).thenReturn(Optional.of(batch));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.increase(request))
                .isInstanceOf(ConflictException.class);
    }

    // --- RESERVE TESTS ---

    @Test
    @DisplayName("reserve_shouldSucceed_AndRecordMovement")
    void reserve_shouldSucceed() throws InterruptedException {
        setupLock();
        String productId = "prod-1"; BigDecimal qty = BigDecimal.TEN;
        Inventory inventory = Inventory.builder().id("inv-1").productId(productId).warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("100")).reservedQuantity(new BigDecimal("10")).build();

        SalesOrderLines orderLine = new SalesOrderLines();
        orderLine.setProductId(productId);

        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .orderLineId("OL-1")
                .warehouseId("wh-1")
                .productId(productId)
                .quantity(qty)
                .build();

        when(salesOrderLinesRepository.findById("OL-1")).thenReturn(Optional.of(orderLine));
        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.empty());
        when(inventoryRepository.findBestSuitableForUpdate(eq(productId), any(), any(), any(), any())).thenReturn(Optional.of(inventory));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        inventoryService.reserve(request);

        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("20");
        verify(stockMovementsService).recordMovement(any());
        verifyNoInteractions(locationService);
    }

    // --- UNRESERVE TESTS ---

    @Test
    @DisplayName("unreserve_shouldSucceed_AndDeleteLedger_WhenZero")
    void unreserve_shouldSucceed() {

        InventoryReservation res = InventoryReservation.builder().id("r1").inventoryId("inv-1")
                .productId("p1").warehouseId("w1").quantity(BigDecimal.TEN).status(InventoryReservationStatus.RESERVED).build();

        Inventory inv = Inventory.builder().id("inv-1").reservedQuantity(BigDecimal.TEN).onHandQuantity(new BigDecimal("100")).build();

        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("p1")
                .warehouseId("w1")
                .orderLineId("OL-1")
                .quantity(BigDecimal.TEN)
                .build();

        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.of(res));
        when(inventoryReservationRepository.sumQuantityByInventoryIdAndStatus("inv-1", InventoryReservationStatus.RESERVED))
                .thenReturn(BigDecimal.TEN);
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inv));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        inventoryService.unreserve(request);

        assertThat(inv.getReservedQuantity()).isEqualByComparingTo("0");
        verify(inventoryReservationRepository).delete(res);
        verifyNoInteractions(locationService);
    }

    @Test
    @DisplayName("unreserve_shouldHealReservedAggregate_When_InventoryReservedIsStale")
    void unreserve_shouldHealReservedAggregate_When_InventoryReservedIsStale() {

        InventoryReservation res = InventoryReservation.builder().id("r1").inventoryId("inv-1")
                .productId("p1").warehouseId("w1").quantity(BigDecimal.TEN).status(InventoryReservationStatus.RESERVED).build();

        Inventory inv = Inventory.builder().id("inv-1").reservedQuantity(new BigDecimal("20")).onHandQuantity(new BigDecimal("10")).build();

        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("p1")
                .warehouseId("w1")
                .orderLineId("OL-1")
                .quantity(BigDecimal.TEN)
                .build();

        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.of(res));
        when(inventoryReservationRepository.sumQuantityByInventoryIdAndStatus("inv-1", InventoryReservationStatus.RESERVED))
                .thenReturn(BigDecimal.TEN);
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inv));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        inventoryService.unreserve(request);

        assertThat(inv.getReservedQuantity()).isEqualByComparingTo("0");
        verify(inventoryReservationRepository).delete(res);
        verifyNoInteractions(locationService);
    }

    // --- DECREASE TESTS ---

    @Test
    @DisplayName("decrease_shouldSucceed_WhenNotConsumingReserved")
    void decrease_shouldSucceed_WhenNotConsumingReserved() throws InterruptedException {
        // Arrange
        setupLock();
        String productId = "prod-1"; String warehouseId = "wh-1";
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                .productId(productId).warehouseId(warehouseId).quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.OUTBOUND_SHIPMENT).referenceNumber("SHIP-001")
                .consumeReserved(false).build();

        Inventory inventory = Inventory.builder().id("inv-1").productId(productId).warehouseId(warehouseId)
                .onHandQuantity(new BigDecimal("50")).reservedQuantity(new BigDecimal("10")).build();

        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        inventoryService.decrease(request);

        // Assert
        assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("40");
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("10");
        verify(stockMovementsService).recordDecrease(eq(request), any(), any());
    }

    @Test
    @DisplayName("decrease_shouldSucceed_WhenConsumingReserved")
    void decrease_shouldSucceed_WhenConsumingReserved() throws InterruptedException {
        // Arrange
        setupLock();
        String productId = "prod-1"; String warehouseId = "wh-1";
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                .productId(productId).warehouseId(warehouseId).quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.OUTBOUND_SHIPMENT).referenceNumber("SHIP-001")
                .consumeReserved(true).build();

        Inventory inventory = Inventory.builder().id("inv-1").productId(productId).warehouseId(warehouseId)
                .onHandQuantity(new BigDecimal("50")).reservedQuantity(new BigDecimal("20")).build();

        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        // Act
        inventoryService.decrease(request);

        // Assert
        assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("40");
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("10");
        verify(stockMovementsService).recordDecrease(eq(request), any(), any());
    }

    @Test
    @DisplayName("decrease_shouldHealReservedAggregate_WhenConsumingReservedForOrderLine")
    void decrease_shouldHealReservedAggregate_WhenConsumingReservedForOrderLine() throws InterruptedException {
        setupLock();
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                .productId("p1").warehouseId("w1").locationId("loc-staging").quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.OUTBOUND_SHIPMENT).referenceNumber("SHIP-001")
                .consumeReserved(true).orderLineId("OL-1").build();

        Inventory inventory = Inventory.builder().id("inv-1").productId("p1").warehouseId("w1")
                .locationId("loc-staging").onHandQuantity(BigDecimal.TEN).reservedQuantity(BigDecimal.ZERO).build();
        InventoryReservation reservation = InventoryReservation.builder().id("res-1").inventoryId("inv-1")
                .productId("p1").warehouseId("w1").locationId("loc-staging").quantity(BigDecimal.TEN)
                .orderLineId("OL-1").status(InventoryReservationStatus.RESERVED).build();

        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any()))
                .thenReturn(Optional.of(inventory));
        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.of(reservation));
        when(inventoryReservationRepository.sumQuantityByInventoryIdAndStatus("inv-1", InventoryReservationStatus.RESERVED))
                .thenReturn(BigDecimal.ZERO);
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(inventory);

        inventoryService.decrease(request);

        assertThat(inventory.getOnHandQuantity()).isEqualByComparingTo("0");
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("0");
        verify(inventoryReservationRepository).delete(reservation);
        verify(stockMovementsService).recordDecrease(eq(request), any(), any());
    }

    @Test
    @DisplayName("decrease_shouldThrowConflict_WhenInsufficientAvailable")
    void decrease_shouldThrowConflict_WhenInsufficientAvailable() throws InterruptedException {
        // Arrange
        setupLock();
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                .productId("p1").warehouseId("w1").quantity(new BigDecimal("100"))
                .referenceType(ReferenceType.OUTBOUND_SHIPMENT).referenceNumber("S1")
                .consumeReserved(false).build();

        Inventory inventory = Inventory.builder().onHandQuantity(new BigDecimal("50")).reservedQuantity(new BigDecimal("10")).build();

        when(inventoryRepository.findByDimensionForUpdate(any(), any(), any(), any())).thenReturn(Optional.of(inventory));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.decrease(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("moveInventory_shouldMoveOnHandAndReservation_WhenOrderLineProvided")
    void moveInventory_shouldMoveOnHandAndReservation_WhenOrderLineProvided() {
        Locations fromLocation = Locations.builder()
                .id("loc-from")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.STORAGE)
                .build();
        Locations toLocation = Locations.builder()
                .id("loc-to")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.PICKING)
                .build();
        Inventory sourceInventory = Inventory.builder()
                .id("inv-from")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-from")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("50.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        Inventory destinationInventory = Inventory.builder()
                .id("inv-to")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-to")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("20.00"))
                .reservedQuantity(new BigDecimal("3.00"))
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("inv-from")
                .locationId("loc-from")
                .orderLineId("so-line-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .batchId("batch-1")
                .quantity(new BigDecimal("5.00"))
                .status(InventoryReservationStatus.RESERVED)
                .build();

        when(locationRepository.findByIdForUpdate("loc-from")).thenReturn(Optional.of(fromLocation));
        when(locationRepository.findByIdForUpdate("loc-to")).thenReturn(Optional.of(toLocation));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-from", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-to", "batch-1"))
                .thenReturn(Optional.of(destinationInventory));
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.moveInventory(
                "loc-from",
                "loc-to",
                "prod-1",
                "batch-1",
                new BigDecimal("5.00"),
                ReferenceType.OUTBOUND_SHIPMENT,
                "ship-1",
                "so-line-1",
                false
        );

        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("45.00");
        assertThat(sourceInventory.getReservedQuantity()).isEqualByComparingTo("5.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("25.00");
        assertThat(destinationInventory.getReservedQuantity()).isEqualByComparingTo("8.00");
        assertThat(reservation.getInventoryId()).isEqualTo("inv-to");
        assertThat(reservation.getLocationId()).isEqualTo("loc-to");
        verify(inventoryReservationRepository).save(reservation);
        verify(stockMovementsService, times(2)).recordMovement(any());
        verifyNoInteractions(locationService);
    }

    @Test
    void moveInventory_shouldHealReservationPointer_When_ReservationInventoryIdIsStale() {
        Locations fromLocation = Locations.builder()
                .id("loc-from")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.STORAGE)
                .build();
        Locations toLocation = Locations.builder()
                .id("loc-to")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.PICKING)
                .build();
        Inventory sourceInventory = Inventory.builder()
                .id("inv-from")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-from")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("30.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        Inventory destinationInventory = Inventory.builder()
                .id("inv-to")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-to")
                .batchId("batch-1")
                .onHandQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("stale-inv-id")
                .locationId("stale-loc-id")
                .orderLineId("so-line-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .batchId("batch-1")
                .quantity(new BigDecimal("10.00"))
                .status(InventoryReservationStatus.RESERVED)
                .build();

        when(locationRepository.findByIdForUpdate("loc-from")).thenReturn(Optional.of(fromLocation));
        when(locationRepository.findByIdForUpdate("loc-to")).thenReturn(Optional.of(toLocation));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-from", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-to", "batch-1"))
                .thenReturn(Optional.of(destinationInventory));
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.moveInventory(
                "loc-from",
                "loc-to",
                "prod-1",
                "batch-1",
                new BigDecimal("10.00"),
                ReferenceType.OUTBOUND_SHIPMENT,
                "ship-1",
                "so-line-1",
                false
        );

        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("20.00");
        assertThat(sourceInventory.getReservedQuantity()).isEqualByComparingTo("0.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("10.00");
        assertThat(destinationInventory.getReservedQuantity()).isEqualByComparingTo("10.00");
        assertThat(reservation.getInventoryId()).isEqualTo("inv-to");
        assertThat(reservation.getLocationId()).isEqualTo("loc-to");
        verify(inventoryReservationRepository).save(reservation);
    }

    @Test
    void moveInventory_shouldHealReservedQuantity_When_OrderReservationIsSufficient() {
        Locations fromLocation = Locations.builder()
                .id("loc-from")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.PACKING)
                .build();
        Locations toLocation = Locations.builder()
                .id("loc-to")
                .warehouseId("wh-1")
                .status(LocationStatus.ACTIVE)
                .type(LocationType.STAGING)
                .build();
        Inventory sourceInventory = Inventory.builder()
                .id("inv-from")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-from")
                .batchId("batch-1")
                .onHandQuantity(new BigDecimal("10.00"))
                .reservedQuantity(new BigDecimal("0.00"))
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        Inventory destinationInventory = Inventory.builder()
                .id("inv-to")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-to")
                .batchId("batch-1")
                .onHandQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .quarantineQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("inv-from")
                .locationId("loc-from")
                .orderLineId("so-line-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .batchId("batch-1")
                .quantity(new BigDecimal("10.00"))
                .status(InventoryReservationStatus.RESERVED)
                .build();

        when(locationRepository.findByIdForUpdate("loc-from")).thenReturn(Optional.of(fromLocation));
        when(locationRepository.findByIdForUpdate("loc-to")).thenReturn(Optional.of(toLocation));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-from", "batch-1"))
                .thenReturn(Optional.of(sourceInventory));
        when(inventoryRepository.findByDimensionForUpdate("prod-1", "wh-1", "loc-to", "batch-1"))
                .thenReturn(Optional.of(destinationInventory));
        when(inventoryReservationRepository.findByOrderLineId("so-line-1")).thenReturn(Optional.of(reservation));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inventoryService.moveInventory(
                "loc-from",
                "loc-to",
                "prod-1",
                "batch-1",
                new BigDecimal("10.00"),
                ReferenceType.OUTBOUND_SHIPMENT,
                "ship-1",
                "so-line-1",
                false
        );

        assertThat(sourceInventory.getOnHandQuantity()).isEqualByComparingTo("0.00");
        assertThat(sourceInventory.getReservedQuantity()).isEqualByComparingTo("0.00");
        assertThat(destinationInventory.getOnHandQuantity()).isEqualByComparingTo("10.00");
        assertThat(destinationInventory.getReservedQuantity()).isEqualByComparingTo("10.00");
        assertThat(reservation.getInventoryId()).isEqualTo("inv-to");
        assertThat(reservation.getLocationId()).isEqualTo("loc-to");
    }
}
