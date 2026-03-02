package org.demo.whs.mapper;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        return InventoryResponse.builder()
                .id(inventory.getId())

                .productId(inventory.getProductId())
                .productSku(inventory.getProduct() != null ? inventory.getProduct().getSku() : null)
                .productName(inventory.getProduct() != null ? inventory.getProduct().getName() : null)

                .warehouseId(inventory.getWarehouseId())
                .warehouseName(inventory.getWarehouse() != null ? inventory.getWarehouse().getName() : null)

                .locationId(inventory.getLocationId())
                .locationCode(inventory.getLocation() != null ? inventory.getLocation().getCode() : null)

                .batchId(inventory.getBatchId())
                .batchNumber(inventory.getBatch() != null ? inventory.getBatch().getBatchNumber() : null)

                .onHandQuantity(inventory.getOnHandQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .availableQuantity(inventory.getAvailableQuantity())

                .version(inventory.getVersion())
                .lastMovementAt(inventory.getLastMovementAt())
                .createdAt(inventory.getCreatedAt())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }

    public List<InventoryResponse> toResponses(List<Inventory> inventories) {
        if (inventories == null || inventories.isEmpty()) {
            return List.of();
        }

        return inventories.stream()
                .map(this::toResponse)
                .toList();
    }

    public PageResponse<InventoryResponse> toPageResponse(Page<Inventory> page) {
        if (page == null || page.isEmpty()) {
            return PageResponse.<InventoryResponse>builder()
                    .content(List.of())
                    .page(0)
                    .size(0)
                    .totalElements(0L)
                    .totalPages(0)
                    .isFirst(true)
                    .isLast(true)
                    .build();
        }

        return PageResponse.<InventoryResponse>builder()
                .content(page.map(this::toResponse).getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}