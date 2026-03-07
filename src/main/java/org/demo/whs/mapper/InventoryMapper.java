package org.demo.whs.mapper;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class InventoryMapper {

    /**
     * Entity → Response
     */
    public InventoryResponse toResponse(
            Inventory inventory,
            Products product,
            Warehouses warehouse,
            Locations location,
            Batch batch
    ) {

        if (inventory == null) {
            return null;
        }

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

    /**
     * Entity list → Response list
     */
    public List<InventoryResponse> toResponses(
            List<Inventory> inventories,
            Map<String, Products> productMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap,
            Map<String, Batch> batchMap
    ) {

        if (inventories == null || inventories.isEmpty()) {
            return List.of();
        }

        return inventories.stream()
                .map(inv -> {

                    Products product = productMap.get(inv.getProductId());
                    Warehouses warehouse = warehouseMap.get(inv.getWarehouseId());
                    Locations location = locationMap.get(inv.getLocationId());
                    Batch batch = batchMap.get(inv.getBatchId());

                    return toResponse(inv, product, warehouse, location, batch);

                })
                .toList();
    }

    /**
     * Inventory → Location item
     */
    public LocationInventoryItemResponse toLocationItem(
            Inventory inventory,
            Products product,
            Batch batch
    ) {

        return LocationInventoryItemResponse.builder()
                .productId(inventory.getProductId())
                .productSku(product != null ? product.getSku() : null)
                .productName(product != null ? product.getName() : null)

                .batchId(inventory.getBatchId())
                .batchNumber(batch != null ? batch.getBatchNumber() : null)

                .onHandQuantity(inventory.getOnHandQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())

                .build();
    }

    /**
     * Build location response
     */
    public InventoryByLocationResponse toLocationResponse(
            Locations location,
            Warehouses warehouse,
            List<LocationInventoryItemResponse> items
    ) {

        return InventoryByLocationResponse.builder()
                .locationId(location != null ? location.getId() : null)
                .locationCode(location != null ? location.getCode() : null)
                .locationName(location != null ? location.getName() : "Unassigned")

                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : "Unknown")

                .items(items)

                .build();
    }

    /**
     * Check availability response
     */
    public CheckAvailabilityResponse toCheckAvailabilityResponse(
            CheckAvailabilityRequest request,
            BigDecimal available,
            boolean isAvailable) {

        String message = isAvailable
                ? "Stock available"
                : "Insufficient stock";

        return CheckAvailabilityResponse.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .locationId(request.getLocationId())

                .requestedQuantity(request.getQuantity())

                .availableQuantity(available)
                .message(message)
                .build();
    }
}