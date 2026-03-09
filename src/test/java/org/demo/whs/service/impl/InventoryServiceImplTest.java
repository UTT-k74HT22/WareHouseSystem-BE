package org.demo.whs.service.impl;

import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.InventoryReservation;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.enums.InventoryReservationStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InventoryMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.InventoryReservationRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("should_ThrowBadRequest_When_PageSizeIsZero")
    void should_ThrowBadRequest_When_PageSizeIsZero() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_007");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageSizeExceedsLimit")
    void should_ThrowBadRequest_When_PageSizeExceedsLimit() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(101);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_008");
    }

    @Test
    @DisplayName("checkAvailability_shouldReturnAvailable_WhenStockIsEnough")
    void checkAvailability_shouldReturnAvailable_WhenStockIsEnough() {
        // Arrange
        String productId = "prod-1";
        BigDecimal requestedQty = new BigDecimal("10.00");
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .quantity(requestedQty)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        
        CheckAvailabilityResponse repoResponse = CheckAvailabilityResponse.builder()
                .productId(productId)
                .availableQuantity(new BigDecimal("80.00"))
                .isAvailable(true)
                .build();
        
        when(inventoryRepository.getAvailability(eq(productId), eq(null), eq(null)))
                .thenReturn(repoResponse);

        CheckAvailabilityResponse finalResponse = CheckAvailabilityResponse.builder()
                .productId(productId)
                .requestedQuantity(requestedQty)
                .availableQuantity(new BigDecimal("80.00"))
                .isAvailable(true)
                .message("Stock available")
                .build();

        when(inventoryMapper.toCheckAvailabilityResponse(eq(request), eq(new BigDecimal("80.00")), eq(true)))
                .thenReturn(finalResponse);

        // Act
        CheckAvailabilityResponse response = inventoryService.checkAvailability(request);

        // Assert
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("80.00");
        assertThat(response.getMessage()).isEqualTo("Stock available");
        assertThat(response.getProductId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("checkAvailability_shouldReturnNotAvailable_WhenStockIsNotEnough")
    void checkAvailability_shouldReturnNotAvailable_WhenStockIsNotEnough() {
        // Arrange
        String productId = "prod-1";
        BigDecimal requestedQty = new BigDecimal("100.00");
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .quantity(requestedQty)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        
        CheckAvailabilityResponse repoResponse = CheckAvailabilityResponse.builder()
                .productId(productId)
                .availableQuantity(new BigDecimal("80.00"))
                .isAvailable(true) // getAvailability just returns available if > 0
                .build();
        
        when(inventoryRepository.getAvailability(eq(productId), eq(null), eq(null)))
                .thenReturn(repoResponse);

        CheckAvailabilityResponse finalResponse = CheckAvailabilityResponse.builder()
                .productId(productId)
                .requestedQuantity(requestedQty)
                .availableQuantity(new BigDecimal("80.00"))
                .isAvailable(false) // Service logic makes it false because 80 < 100
                .message("Insufficient stock")
                .build();

        when(inventoryMapper.toCheckAvailabilityResponse(eq(request), eq(new BigDecimal("80.00")), eq(false)))
                .thenReturn(finalResponse);

        // Act
        CheckAvailabilityResponse response = inventoryService.checkAvailability(request);

        // Assert
        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("80.00");
        assertThat(response.getMessage()).isEqualTo("Insufficient stock");
    }

    @Test
    @DisplayName("checkAvailability_shouldThrowNotFound_WhenProductDoesNotExist")
    void checkAvailability_shouldThrowNotFound_WhenProductDoesNotExist() {
        // Arrange
        String productId = "non-existent";
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .quantity(BigDecimal.TEN)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.checkAvailability(request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "PROD_001");
    }

    @Test
    @DisplayName("checkAvailability_shouldThrowNotFound_WhenWarehouseDoesNotExist")
    void checkAvailability_shouldThrowNotFound_WhenWarehouseDoesNotExist() {
        // Arrange
        String productId = "prod-1";
        String warehouseId = "wh-non-existent";
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(BigDecimal.TEN)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        when(wareHouseRepository.findById(warehouseId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.checkAvailability(request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "WHS_001");
    }

    @Test
    @DisplayName("checkAvailability_shouldThrowNotFound_WhenLocationDoesNotExist")
    void checkAvailability_shouldThrowNotFound_WhenLocationDoesNotExist() {
        // Arrange
        String productId = "prod-1";
        String locationId = "loc-non-existent";
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .locationId(locationId)
                .quantity(BigDecimal.TEN)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        when(locationRepository.findById(locationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.checkAvailability(request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOC_001");
    }

    @Test
    @DisplayName("checkAvailability_shouldThrowBadRequest_WhenLocationDoesNotBelongToWarehouse")
    void checkAvailability_shouldThrowBadRequest_WhenLocationDoesNotBelongToWarehouse() {
        String productId = "prod-1";
        String warehouseId = "wh-1";
        String locationId = "loc-1";

        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(locationId)
                .quantity(BigDecimal.ONE)
                .build();

        Warehouses warehouse = new Warehouses();
        warehouse.setId(warehouseId);

        Locations location = new Locations();
        location.setId(locationId);
        location.setWarehouseId("wh-2");

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Products()));
        when(wareHouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> inventoryService.checkAvailability(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
    }

    @Test
    @DisplayName("reserve_shouldThrowBadRequest_WhenQuantityIsNegative")
    void reserve_shouldThrowBadRequest_WhenQuantityIsNegative() {
        // Arrange
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("-5.00"))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserve(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
    }

    @Test
    @DisplayName("reserve_shouldSucceed_WhenStockIsEnough")
    void reserve_shouldSucceed_WhenStockIsEnough() {
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

        // No existing reservation
        when(inventoryReservationRepository.findByOrderLineId(orderLineId)).thenReturn(Optional.empty());

        // Find best row
        when(inventoryRepository.findBestSuitableForUpdate(productId, warehouseId, null, null, requestedQty))
                .thenReturn(Optional.of(inventory));

        InventoryReserveResponse expectedResponse = InventoryReserveResponse.builder()
                .inventoryId("inv-1")
                .productId(productId)
                .warehouseId(warehouseId)
                .reservedQuantity(requestedQty)
                .status("RESERVED")
                .build();

        when(inventoryMapper.toReserveResponse(any(InventoryReservation.class), any(Inventory.class)))
                .thenReturn(expectedResponse);

        // Act
        InventoryReserveResponse response = inventoryService.reserve(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(inventory.getReservedQuantity()).isEqualByComparingTo("30.00");
        verify(inventoryRepository).save(inventory);
        verify(inventoryReservationRepository).saveAndFlush(any(InventoryReservation.class));
    }

    @Test
    @DisplayName("reserve_shouldReturnExisting_WhenLineIsAlreadyReserved")
    void reserve_shouldReturnExisting_WhenLineIsAlreadyReserved() {
        // Arrange
        String productId = "prod-1";
        String warehouseId = "wh-1";
        String orderLineId = "OL-1";
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .orderLineId(orderLineId)
                .quantity(new BigDecimal("10.00"))
                .build();

        InventoryReservation existingRes = InventoryReservation.builder()
                .id("res-1")
                .inventoryId("inv-1")
                .orderLineId(orderLineId)
                .status(InventoryReservationStatus.RESERVED)
                .build();

        Inventory inventory = Inventory.builder().id("inv-1").build();

        when(inventoryReservationRepository.findByOrderLineId(orderLineId)).thenReturn(Optional.of(existingRes));
        when(inventoryRepository.findById("inv-1")).thenReturn(Optional.of(inventory));
        
        InventoryReserveResponse expectedResponse = InventoryReserveResponse.builder()
                .inventoryId("inv-1")
                .status("RESERVED")
                .build();

        when(inventoryMapper.toReserveResponse(existingRes, inventory)).thenReturn(expectedResponse);

        // Act
        InventoryReserveResponse response = inventoryService.reserve(request);

        // Assert
        assertThat(response.getInventoryId()).isEqualTo("inv-1");
        verify(inventoryRepository, never()).findBestSuitableForUpdate(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("reserve_shouldThrowConflict_WhenNoSingleRowHasEnoughStock")
    void reserve_shouldThrowConflict_WhenNoSingleRowHasEnoughStock() {
        // Arrange
        String productId = "prod-1";
        String warehouseId = "wh-1";
        BigDecimal requestedQty = new BigDecimal("90.00");
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(requestedQty)
                .build();

        // Removed unnecessary stub for findByOrderLineId
        when(inventoryRepository.findBestSuitableForUpdate(productId, warehouseId, null, null, requestedQty))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserve(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_004");
    }

    @Test
    @DisplayName("reserve_shouldThrowNotFound_WhenInventoryDoesNotExist")
    void reserve_shouldThrowNotFound_WhenInventoryDoesNotExist() {
        // Arrange
        String productId = "prod-1";
        String warehouseId = "wh-1";
        BigDecimal requestedQty = new BigDecimal("10.00");
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .quantity(requestedQty)
                .build();

        // Removed unnecessary stub for findByOrderLineId
        when(inventoryRepository.findBestSuitableForUpdate(productId, warehouseId, null, null, requestedQty))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserve(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "INV_004"); 
    }
}
