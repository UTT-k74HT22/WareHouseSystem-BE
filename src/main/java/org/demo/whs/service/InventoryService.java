package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

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
}
