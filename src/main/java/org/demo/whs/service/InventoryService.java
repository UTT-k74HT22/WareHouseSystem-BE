package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Inventory service interface for managing inventory operations.
 */
public interface InventoryService {
    /***
     * Get inventories with pagination and filtering
     * @param filter InventoryFilterRequest
     * @param pageable Pageable
     * @return PageResponse<InventoryResponse>
     */
    PageResponse<InventoryResponse> getInventories(InventoryFilterRequest filter, Pageable pageable);

    /**
     * Get inventory summary for a specific product.
     *
     * @param productId The product ID
     * @return Inventory summary
     */
    InventorySummaryResponse getSummaryByProduct(String productId);

    /**
     * Get inventory grouped by location with filtering
     * @param filter InventoryFilterRequest
     * @return List<InventoryByLocationResponse>
     */
    List<InventoryByLocationResponse> getInventoryByLocation(InventoryFilterRequest filter);

    /**
     * Check if requested quantity is available for a product.
     *
     * @param request The check availability request
     * @return Availability result
     */
    CheckAvailabilityResponse checkAvailability(CheckAvailabilityRequest request);

    /**
     * Reserve inventory for an order line.
     *
     * @param request The reservation request
     * @return Reservation result
     */
    InventoryReserveResponse reserve(InventoryReserveRequest request);
}
