package org.demo.whs.mapper;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.repository.projection.InventorySummaryProjection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    public InventorySummaryResponse toSummaryResponse(InventorySummaryProjection projection) {
        if (projection == null) {
            return null;
        }

        BigDecimal onHand = projection.getTotalOnHandQuantity() != null
                ? projection.getTotalOnHandQuantity()
                : BigDecimal.ZERO;

        BigDecimal reserved = projection.getTotalReservedQuantity() != null
                ? projection.getTotalReservedQuantity()
                : BigDecimal.ZERO;

        return InventorySummaryResponse.builder()
                .productId(projection.getProductId())
                .productSku(projection.getProductSku())
                .productName(projection.getProductName())
                .totalOnHandQuantity(onHand)
                .totalReservedQuantity(reserved)
                .totalAvailableQuantity(onHand.subtract(reserved))
                .warehouseCount(projection.getWarehouseCount())
                .locationCount(projection.getLocationCount())
                .build();
    }
    public InventoryByLocationResponse.LocationInventoryItem toLocationItem(
            Inventory inv,
            Map<String, Products> productMap,
            Map<String, Batch> batchMap) {

        Products prod = productMap.get(inv.getProductId());
        Batch batch = inv.getBatchId() != null ? batchMap.get(inv.getBatchId()) : null;

        return InventoryByLocationResponse.LocationInventoryItem.builder()
                .productId(inv.getProductId())
                .productSku(prod != null ? prod.getSku() : null)
                .productName(prod != null ? prod.getName() : null)
                .batchId(inv.getBatchId())
                .batchNumber(batch != null ? batch.getBatchNumber() : null)
                .onHandQuantity(inv.getOnHandQuantity())
                .reservedQuantity(inv.getReservedQuantity())
                .availableQuantity(inv.getAvailableQuantity())
                .build();
    }
    public InventoryByLocationResponse toLocationResponse(
            String locationId,
            List<Inventory> inventories,
            Map<String, Products> productMap,
            Map<String, Warehouses> warehouseMap,
            Map<String, Locations> locationMap,
            Map<String, Batch> batchMap) {

        Locations loc = locationMap.get(locationId);

        String warehouseId = inventories.get(0).getWarehouseId();
        Warehouses wh = warehouseMap.get(warehouseId);

        List<InventoryByLocationResponse.LocationInventoryItem> items =
                inventories.stream()
                        .map(inv -> toLocationItem(inv, productMap, batchMap))
                        .toList();

        return InventoryByLocationResponse.builder()
                .locationId("UNASSIGNED".equals(locationId) ? null : locationId)
                .locationCode(loc != null ? loc.getCode() : "N/A")
                .locationName(loc != null ? loc.getName() : "Unassigned")
                .warehouseId(warehouseId)
                .warehouseName(wh != null ? wh.getName() : "Unknown")
                .items(items)
                .build();
    }
    public CheckAvailabilityResponse toCheckAvailabilityResponse(
            CheckAvailabilityRequest request,
            BigDecimal available,
            boolean isAvailable) {

        if (request == null) {
            return null;
        }

        String message = isAvailable ? "Stock available" : "Stock not available";

        return CheckAvailabilityResponse.builder()
                .productId(request.getProductId())
                .warehouseId(request.getWarehouseId())
                .locationId(request.getLocationId())
                .requestedQuantity(request.getQuantity())
                .availableQuantity(available)
                .isAvailable(isAvailable)
                .message(message)
                .build();
    }
}
