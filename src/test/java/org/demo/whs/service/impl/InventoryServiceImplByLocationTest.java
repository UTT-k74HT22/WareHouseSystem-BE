package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Group By Location Unit Tests")
class InventoryServiceImplByLocationTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("should_ReturnEmptyList_When_NoInventoryFound")
    void should_ReturnEmptyList_When_NoInventoryFound() {
        InventoryFilterRequest filter = InventoryFilterRequest.builder().build();
        when(inventoryRepository.getInventoryByLocation(any(InventoryFilterRequest.class))).thenReturn(Collections.emptyList());

        List<InventoryByLocationResponse> result = inventoryService.getInventoryByLocation(filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_ReturnGroupedInventory_When_InventoryExists")
    void should_ReturnGroupedInventory_When_InventoryExists() {
        // Arrange
        String locId = "loc-1";
        
        InventoryLocationProjection p1 = InventoryLocationProjection.builder()
                .locationId(locId)
                .locationCode("LOC-1")
                .locationName("Location 1")
                .warehouseId("wh-1")
                .warehouseName("Warehouse 1")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Product 1")
                .onHandQuantity(BigDecimal.valueOf(100))
                .reservedQuantity(BigDecimal.valueOf(10))
                .build();

        InventoryLocationProjection p2 = InventoryLocationProjection.builder()
                .locationId(locId)
                .locationCode("LOC-1")
                .locationName("Location 1")
                .warehouseId("wh-1")
                .warehouseName("Warehouse 1")
                .productId("prod-2")
                .productSku("SKU-2")
                .productName("Product 2")
                .onHandQuantity(BigDecimal.valueOf(50))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        when(inventoryRepository.getInventoryByLocation(any(InventoryFilterRequest.class))).thenReturn(List.of(p1, p2));

        InventoryFilterRequest filter = InventoryFilterRequest.builder().productId("prod-1").build();

        // Act
        List<InventoryByLocationResponse> result = inventoryService.getInventoryByLocation(filter);

        // Assert
        assertThat(result).hasSize(1);
        InventoryByLocationResponse locResponse = result.get(0);
        assertThat(locResponse.getLocationId()).isEqualTo(locId);
        assertThat(locResponse.getItems()).hasSize(2);
        
        assertThat(locResponse.getItems().get(0).getProductId()).isEqualTo("prod-1");
        assertThat(locResponse.getItems().get(0).getAvailableQuantity()).isEqualTo(BigDecimal.valueOf(90));
        
        assertThat(locResponse.getItems().get(1).getProductId()).isEqualTo("prod-2");
        assertThat(locResponse.getItems().get(1).getAvailableQuantity()).isEqualTo(BigDecimal.valueOf(50));
    }

    @Test
    @DisplayName("should_HandleUnassignedLocation_When_LocationIdIsNull")
    void should_HandleUnassignedLocation_When_LocationIdIsNull() {
        // Arrange
        InventoryLocationProjection p = InventoryLocationProjection.builder()
                .locationId(null)
                .locationCode(null)
                .locationName(null)
                .warehouseId("wh-1")
                .warehouseName("Warehouse 1")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Product 1")
                .onHandQuantity(BigDecimal.valueOf(50))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        when(inventoryRepository.getInventoryByLocation(any(InventoryFilterRequest.class))).thenReturn(List.of(p));

        InventoryFilterRequest filter = InventoryFilterRequest.builder().build();

        // Act
        List<InventoryByLocationResponse> result = inventoryService.getInventoryByLocation(filter);

        // Assert
        assertThat(result).hasSize(1);
        InventoryByLocationResponse locResponse = result.get(0);
        assertThat(locResponse.getLocationId()).isNull();
        assertThat(locResponse.getLocationName()).isEqualTo("Unassigned");
        assertThat(locResponse.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("should_NotMergeUnassignedInventoryAcrossWarehouses")
    void should_NotMergeUnassignedInventoryAcrossWarehouses() {
        InventoryLocationProjection first = InventoryLocationProjection.builder()
                .locationId(null)
                .warehouseId("wh-1")
                .warehouseName("Warehouse 1")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Product 1")
                .onHandQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        InventoryLocationProjection second = InventoryLocationProjection.builder()
                .locationId(null)
                .warehouseId("wh-2")
                .warehouseName("Warehouse 2")
                .productId("prod-2")
                .productSku("SKU-2")
                .productName("Product 2")
                .onHandQuantity(BigDecimal.ONE)
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        when(inventoryRepository.getInventoryByLocation(any(InventoryFilterRequest.class)))
                .thenReturn(List.of(first, second));

        List<InventoryByLocationResponse> result =
                inventoryService.getInventoryByLocation(InventoryFilterRequest.builder().build());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(InventoryByLocationResponse::getWarehouseId)
                .containsExactly("wh-1", "wh-2");
    }
}
