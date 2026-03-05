package org.demo.whs.service.impl;

import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Group By Location Unit Tests")
class InventoryServiceImplByLocationTest {

    @Mock
    private InventoryRepository inventoryRepository;

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
    @DisplayName("should_ReturnEmptyList_When_NoInventoryFound")
    void should_ReturnEmptyList_When_NoInventoryFound() {
        InventoryFilterRequest filter = InventoryFilterRequest.builder().build();
        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        List<InventoryByLocationResponse> result = inventoryService.getInventoryByLocation(filter);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should_ReturnGroupedInventory_When_InventoryExists")
    void should_ReturnGroupedInventory_When_InventoryExists() {
        // Arrange
        String prodId = "prod-1";
        String whId = "wh-1";
        String locId = "loc-1";
        
        Inventory inv = Inventory.builder()
                .productId(prodId)
                .warehouseId(whId)
                .locationId(locId)
                .onHandQuantity(BigDecimal.valueOf(100))
                .reservedQuantity(BigDecimal.valueOf(10))
                .build();

        Products product = Products.builder().id(prodId).sku("SKU-1").name("Product 1").build();
        Warehouses warehouse = Warehouses.builder().id(whId).name("Warehouse 1").build();
        Locations location = Locations.builder().id(locId).code("LOC-1").name("Location 1").build();

        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inv));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(wareHouseRepository.findAllById(any())).thenReturn(List.of(warehouse));
        when(locationRepository.findAllById(any())).thenReturn(List.of(location));

        InventoryFilterRequest filter = InventoryFilterRequest.builder().warehouseId(whId).build();

        // Act
        List<InventoryByLocationResponse> result = inventoryService.getInventoryByLocation(filter);

        // Assert
        assertThat(result).hasSize(1);
        InventoryByLocationResponse locResponse = result.get(0);
        assertThat(locResponse.getLocationId()).isEqualTo(locId);
        assertThat(locResponse.getLocationCode()).isEqualTo("LOC-1");
        assertThat(locResponse.getItems()).hasSize(1);
        assertThat(locResponse.getItems().get(0).getProductSku()).isEqualTo("SKU-1");
        assertThat(locResponse.getItems().get(0).getOnHandQuantity()).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    @DisplayName("should_HandleUnassignedLocation_When_LocationIdIsNull")
    void should_HandleUnassignedLocation_When_LocationIdIsNull() {
        // Arrange
        String prodId = "prod-1";
        String whId = "wh-1";
        
        Inventory inv = Inventory.builder()
                .productId(prodId)
                .warehouseId(whId)
                .locationId(null)
                .onHandQuantity(BigDecimal.valueOf(50))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        Products product = Products.builder().id(prodId).sku("SKU-1").name("Product 1").build();
        Warehouses warehouse = Warehouses.builder().id(whId).name("Warehouse 1").build();

        when(inventoryRepository.findAll(any(Specification.class))).thenReturn(List.of(inv));
        when(productRepository.findAllById(any())).thenReturn(List.of(product));
        when(wareHouseRepository.findAllById(any())).thenReturn(List.of(warehouse));
        when(locationRepository.findAllById(any())).thenReturn(Collections.emptyList());

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
}
