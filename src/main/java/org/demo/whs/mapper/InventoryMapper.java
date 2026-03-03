package org.demo.whs.mapper;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory, 
                                      Map<String, Products> productMap,
                                      Map<String, Warehouses> warehouseMap,
                                      Map<String, Locations> locationMap,
                                      Map<String, Batch> batchMap) {
        if (inventory == null) {
            return null;
        }

        Products product = productMap != null ? productMap.get(inventory.getProductId()) : null;
        Warehouses warehouse = warehouseMap != null ? warehouseMap.get(inventory.getWarehouseId()) : null;
        Locations location = locationMap != null ? locationMap.get(inventory.getLocationId()) : null;
        Batch batch = batchMap != null ? batchMap.get(inventory.getBatchId()) : null;

        return InventoryResponse.builder()
                .id(inventory.getId())

                .productId(inventory.getProductId())
                .productSku(product != null ? product.getSku() : null)
                .productName(product != null ? product.getName() : null)

                .warehouseId(inventory.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)

                .locationId(inventory.getLocationId())
                .locationCode(location != null ? location.getCode() : null)

                .batchId(inventory.getBatchId())
                .batchNumber(batch != null ? batch.getBatchNumber() : null)

                .onHandQuantity(inventory.getOnHandQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())

                .lastMovementAt(inventory.getLastMovementAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public List<InventoryResponse> toResponses(List<Inventory> inventories,
                                             Map<String, Products> productMap,
                                             Map<String, Warehouses> warehouseMap,
                                             Map<String, Locations> locationMap,
                                             Map<String, Batch> batchMap) {
        if (inventories == null || inventories.isEmpty()) {
            return List.of();
        }

        return inventories.stream()
                .map(i -> toResponse(i, productMap, warehouseMap, locationMap, batchMap))
                .toList();
    }
}
