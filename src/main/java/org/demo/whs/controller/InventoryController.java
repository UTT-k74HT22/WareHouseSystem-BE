package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.InventoryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for managing inventory operations.
 */
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints for managing warehouse inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Get inventories with pagination and filtering.
     *
     * @param filter   Inventory filter criteria
     * @param pageable Pagination information
     * @return List of inventories
     */
    @GetMapping
    @Operation(summary = "Get inventories", description = "Fetch inventories with filtering and pagination")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_READ')")
    public ResponseEntity<BaseResponse<PageResponse<InventoryResponse>>> getInventories(
            InventoryFilterRequest filter, Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.getInventories(filter, pageable)));
    }

    /**
     * Get inventory summary for a product.
     *
     * @param productId The product ID
     * @return Inventory summary
     */
    @GetMapping("/summary/{productId}")
    @Operation(summary = "Get product summary", description = "Get aggregate inventory data for a product")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_READ')")
    public ResponseEntity<BaseResponse<InventorySummaryResponse>> getSummaryByProduct(
            @PathVariable String productId) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.getSummaryByProduct(productId)));
    }

    /**
     * Get inventory grouped by location.
     *
     * @param filter Inventory filter criteria
     * @return List of inventory by location
     */
    @GetMapping("/by-location")
    @Operation(summary = "Get inventory by location", description = "Get inventory data grouped by warehouse location")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_READ')")
    public ResponseEntity<BaseResponse<List<InventoryByLocationResponse>>> getInventoryByLocation(
            InventoryFilterRequest filter) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.getInventoryByLocation(filter)));
    }

    /**
     * Check inventory availability.
     *
     * @param request The check availability request
     * @return Availability result
     */
    @PostMapping("/check-availability")
    @Operation(summary = "Check availability", description = "Check if requested product quantity is available")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_READ')")
    public ResponseEntity<BaseResponse<CheckAvailabilityResponse>> checkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.checkAvailability(request)));
    }

    /**
     * Reserve inventory for an order line.
     *
     * @param request The reservation request
     * @return Reservation result
     */
    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory", description = "Reserve stock for an order line using allocation strategy")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_RESERVATION_UPDATE')")
    public ResponseEntity<BaseResponse<InventoryReserveResponse>> reserve(
            @Valid @RequestBody InventoryReserveRequest request) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.reserve(request)));
    }

    /**
     * Release previously reserved inventory.
     *
     * @param request The unreservation request
     * @return Unreservation result
     */
    @PostMapping("/unreserve")
    @Operation(summary = "Unreserve inventory", description = "Release previously reserved stock for an order line")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_RESERVATION_UPDATE')")
    public ResponseEntity<BaseResponse<InventoryUnreserveResponse>> unreserve(
            @Valid @RequestBody InventoryUnreserveRequest request) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.unreserve(request)));
    }

    /**
     * Increase inventory on-hand quantity.
     *
     * @param request The increase request
     * @return Updated inventory data
     */
    @PostMapping("/increase")
    @Operation(summary = "Increase inventory", description = "Increase on-hand stock from inbound or adjustment")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_MUTATION_UPDATE')")
    public ResponseEntity<BaseResponse<InventoryResponse>> increase(
            @Valid @RequestBody InventoryIncreaseRequest request) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.increase(request)));
    }

    /**
     * Decrease inventory on-hand quantity.
     *
     * @param request The decrease request
     * @return Updated inventory data
     */
    @PostMapping("/decrease")
    @Operation(summary = "Decrease inventory", description = "Decrease on-hand stock from outbound or adjustment")
    @PreAuthorize("hasAuthority('PERM_INVENTORY_MUTATION_UPDATE')")
    public ResponseEntity<BaseResponse<InventoryResponse>> decrease(
            @Valid @RequestBody org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request) {
        return ResponseEntity.ok(BaseResponse.success(inventoryService.decrease(request)));
    }
}
