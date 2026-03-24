package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
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

    /**
     * Release previously reserved inventory.
     *
     * @param request The unreservation request
     * @return Unreservation result
     */
    InventoryUnreserveResponse unreserve(InventoryUnreserveRequest request);

    /**
     * Increase inventory on-hand quantity.
     * Source: Inbound receipt or stock adjustment.
     *
     * @param request The increase request
     * @return Updated inventory data
     */
    InventoryResponse increase(org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest request);

    /**
     * Decrease inventory on-hand quantity.
     * Source: Outbound shipment or stock adjustment.
     *
     * @param request The decrease request
     * @return Updated inventory data
     */
    InventoryResponse decrease(org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request);

    /**
     * Moves inventory from one location to another within the same warehouse.
     *
     * @param fromLocationId Source location ID
     * @param toLocationId   Destination location ID
     * @param productId      Product ID
     * @param batchId        Batch ID
     * @param quantity       Quantity to move
     * @param referenceType  Reference type
     * @param referenceId    Reference ID
     */
    void moveInventory(String fromLocationId, String toLocationId, String productId, String batchId, BigDecimal quantity, ReferenceType referenceType, String referenceId, String orderLineId, boolean consumeReserved);
}
