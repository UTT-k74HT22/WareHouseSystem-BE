package org.demo.whs.service.impl;

import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.InventoryReservation;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.InventoryReservationRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.service.StockMovementsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageNumberIsNegative")
    void should_ThrowBadRequest_When_PageNumberIsNegative() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(-1);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_006");
    }

    @Test
    @DisplayName("reserve_shouldSucceed_And_RecordStockMovement")
    void reserve_shouldSucceed_And_RecordStockMovement() {
        // Arrange
        String productId = "prod-1";
        String warehouseId = "wh-1";
        String orderLineId = "OL-1";
        BigDecimal requestedQty = new BigDecimal("10.00");
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .orderLineId(orderLineId)
                .quantity(requestedQty)
                .build();

        Inventory inventory = Inventory.builder()
                .id("inv-1")
                .productId(productId)
                .warehouseId(warehouseId)
                .onHandQuantity(new BigDecimal("100.00"))
                .reservedQuantity(new BigDecimal("20.00"))
                .build();

        when(inventoryReservationRepository.findByOrderLineId(orderLineId)).thenReturn(Optional.empty());
        when(inventoryRepository.findBestSuitableForUpdate(productId, warehouseId, null, null, requestedQty))
                .thenReturn(Optional.of(inventory));
        
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        // Act
        inventoryService.reserve(request);

        // Assert
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("30.00");
        verify(inventoryRepository).save(inventory);
        verify(stockMovementsService).recordMovement(any(StockMovements.class));
        verify(inventoryReservationRepository).saveAndFlush(any(InventoryReservation.class));
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
    @DisplayName("unreserve_shouldSucceed_And_DeleteReservation_WhenQuantityReachesZero")
    void unreserve_shouldSucceed_And_DeleteReservation_WhenQuantityReachesZero() {
        // Arrange
        String orderLineId = "OL-1";
        BigDecimal unreserveQty = new BigDecimal("10.00");
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .orderLineId(orderLineId)
                .quantity(unreserveQty)
                .build();

        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("10.00"))
                .status(InventoryReservationStatus.RESERVED)
                .build();

        Inventory inventory = Inventory.builder()
                .id("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("100.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .build();

        when(inventoryReservationRepository.findByOrderLineId(orderLineId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        // Act
        inventoryService.unreserve(request);

        // Assert
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("0.00");
        assertThat(reservation.getQuantity()).isEqualByComparingTo("0.00");
        verify(inventoryReservationRepository).delete(reservation); // Verify deletion
        verify(stockMovementsService).recordMovement(any(StockMovements.class));
    }

    @Test
    @DisplayName("unreserve_shouldUpdateQuantity_WhenRemainingIsGreaterThanZero")
    void unreserve_shouldUpdateQuantity_WhenRemainingIsGreaterThanZero() {
        // Arrange
        String orderLineId = "OL-1";
        BigDecimal unreserveQty = new BigDecimal("4.00");
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .orderLineId(orderLineId)
                .quantity(unreserveQty)
                .build();

        InventoryReservation reservation = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("10.00"))
                .status(InventoryReservationStatus.RESERVED)
                .build();

        Inventory inventory = Inventory.builder()
                .id("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("100.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .build();

        when(inventoryReservationRepository.findByOrderLineId(orderLineId)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByIdForUpdate("inv-1")).thenReturn(Optional.of(inventory));
        when(stockMovementsMapper.toEntity(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new StockMovements());

        // Act
        inventoryService.unreserve(request);

        // Assert
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("6.00");
        assertThat(reservation.getQuantity()).isEqualByComparingTo("6.00");
        verify(inventoryReservationRepository).save(reservation); // Verify save instead of delete
        verify(inventoryReservationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("unreserve_shouldThrowConflict_WhenUnreserveExceedsReserved")
    void unreserve_shouldThrowConflict_WhenUnreserveExceedsReserved() {
        // Arrange
        BigDecimal unreserveQty = new BigDecimal("15.00");
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .orderLineId("OL-1")
                .quantity(unreserveQty)
                .build();

        InventoryReservation reservation = InventoryReservation.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("10.00"))
                .build();

        when(inventoryReservationRepository.findByOrderLineId("OL-1")).thenReturn(Optional.of(reservation));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.unreserve(request))
                .isInstanceOf(ConflictException.class);
    }
}
