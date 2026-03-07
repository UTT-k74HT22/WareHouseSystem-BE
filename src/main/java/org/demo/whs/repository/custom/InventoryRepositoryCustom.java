package org.demo.whs.repository.custom;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryLocationProjection;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;

import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryCustom {
    /**
     * Calculates the summary of inventory for a given product.
     *
     * @param productId The ID of the product.
     * @return An optional containing the inventory summary response if the product exists, otherwise empty.
     */
    Optional<InventorySummaryResponse> getSummaryByProductId(String productId);

    CheckAvailabilityResponse getAvailability(
            String productId,
            String warehouseId,
            String locationId
    );

    List<InventoryLocationProjection> getInventoryByLocation(InventoryFilterRequest filter);
}