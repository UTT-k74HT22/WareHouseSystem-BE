package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.mapper.InventoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Unit Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

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

        when(productRepository.existsById(productId)).thenReturn(true);
        
        InventoryAvailabilityProjection mockProjection = mock(InventoryAvailabilityProjection.class);
        when(mockProjection.getTotalOnHandQuantity()).thenReturn(new BigDecimal("100.00"));
        when(mockProjection.getTotalReservedQuantity()).thenReturn(new BigDecimal("20.00"));
        
        when(inventoryRepository.getAvailability(eq(productId), eq(null), eq(null)))
                .thenReturn(mockProjection);

        // Act
        CheckAvailabilityResponse response = inventoryService.checkAvailability(request);

        // Assert
        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("80.00");
        assertThat(response.getMessage()).isEqualTo("Đủ hàng");
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

        when(productRepository.existsById(productId)).thenReturn(true);
        
        InventoryAvailabilityProjection mockProjection = mock(InventoryAvailabilityProjection.class);
        when(mockProjection.getTotalOnHandQuantity()).thenReturn(new BigDecimal("100.00"));
        when(mockProjection.getTotalReservedQuantity()).thenReturn(new BigDecimal("20.00"));
        
        when(inventoryRepository.getAvailability(eq(productId), eq(null), eq(null)))
                .thenReturn(mockProjection);

        // Act
        CheckAvailabilityResponse response = inventoryService.checkAvailability(request);

        // Assert
        assertThat(response.isAvailable()).isFalse();
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("80.00");
        assertThat(response.getMessage()).isEqualTo("Ko đủ hàng");
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

        when(productRepository.existsById(productId)).thenReturn(false);

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

        when(productRepository.existsById(productId)).thenReturn(true);
        when(wareHouseRepository.existsById(warehouseId)).thenReturn(false);

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

        when(productRepository.existsById(productId)).thenReturn(true);
        when(locationRepository.existsById(locationId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.checkAvailability(request))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOC_001");
    }
}
