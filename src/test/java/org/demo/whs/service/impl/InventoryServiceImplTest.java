package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
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

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    // --- INCREASE TESTS ---

    @Test
    @DisplayName("increase_shouldSucceed_WhenInventoryExists")
    void increase_shouldSucceed_WhenInventoryExists() {
        // Arrange
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
                .id("inv-1").productId(productId).warehouseId(warehouseId)
                .onHandQuantity(new BigDecimal("50.00")).reservedQuantity(BigDecimal.ZERO).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByDimensionForUpdate(productId, warehouseId, null, null))
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
    void increase_shouldCreateNew_WhenInventoryMissing() {
        // Arrange
        String productId = "prod-1"; String warehouseId = "wh-1";
        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId(productId).warehouseId(warehouseId).quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.INBOUND_RECEIPT).referenceNumber("REC-001").build();

        Inventory newInv = Inventory.builder().productId(productId).warehouseId(warehouseId)
                .onHandQuantity(BigDecimal.ZERO).reservedQuantity(BigDecimal.ZERO).build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        when(wareHouseRepository.findById(warehouseId)).thenReturn(Optional.of(new Warehouses()));
        when(inventoryRepository.findByDimensionForUpdate(productId, warehouseId, null, null))
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
    void increase_shouldThrowConflict_WhenBatchProductMismatch() {
        // Arrange
        String productId = "prod-1"; String otherProductId = "prod-2";
        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId(productId).warehouseId("wh-1").batchId("batch-1").quantity(BigDecimal.TEN)
                .referenceType(ReferenceType.INBOUND_RECEIPT).referenceNumber("REC-001").build();

        Products product = new Products(); product.setId(productId);
        Batch batch = new Batch(); batch.setId("batch-1"); batch.setProductId(otherProductId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(wareHouseRepository.findById(anyString())).thenReturn(Optional.of(new Warehouses()));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.increase(request))
                .isInstanceOf(ConflictException.class);
    }

    // --- RESERVE TESTS ---

    @Test
    @DisplayName("reserve_shouldSucceed_AndRecordMovement")
    void reserve_shouldSucceed() {
        String productId = "prod-1"; BigDecimal qty = BigDecimal.TEN;
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId).warehouseId("wh-1").orderLineId("OL-1").quantity(qty).build();

        Inventory inventory = Inventory.builder().id("inv-1").productId(productId).warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("100")).reservedQuantity(new BigDecimal("10")).build();

        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.empty());
        when(inventoryRepository.findBestSuitableForUpdate(any(), any(), any(), any(), any())).thenReturn(Optional.of(inventory));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        inventoryService.reserve(request);

        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("20");
        verify(stockMovementsService).recordMovement(any());
    }

    // --- UNRESERVE TESTS ---

    @Test
    @DisplayName("unreserve_shouldSucceed_AndDeleteLedger_WhenZero")
    void unreserve_shouldSucceed() {
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("p1").warehouseId("w1").orderLineId("OL-1").quantity(BigDecimal.TEN).build();

        InventoryReservation res = InventoryReservation.builder().id("r1").inventoryId("inv-1")
                .productId("p1").warehouseId("w1").quantity(BigDecimal.TEN).status(InventoryReservationStatus.RESERVED).build();

        Inventory inv = Inventory.builder().id("inv-1").reservedQuantity(BigDecimal.TEN).onHandQuantity(new BigDecimal("100")).build();

        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.of(res));
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inv));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        inventoryService.unreserve(request);

        assertThat(inv.getReservedQuantity()).isEqualByComparingTo("0");
        verify(inventoryReservationRepository).delete(res);
    }
}
