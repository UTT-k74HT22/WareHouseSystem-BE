package org.demo.whs.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.InventoryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing inventory-related operations.
 */
@RestController
@RequestMapping("/api/v1/inventories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Get list of inventories with filtering and pagination.
     *
     * @param productId   Filter by product ID
     * @param productSku  Filter by product SKU (partial match)
     * @param productName Filter by product name (partial match)
     * @param warehouseId Filter by warehouse ID
     * @param locationId  Filter by location ID
     * @param batchId     Filter by batch ID
     * @param batchNumber Filter by batch number (partial match)
     * @param page        Page number (0-based)
     * @param size        Page size
     * @param sortBy      Field to sort by
     * @param direction   Sort direction (ASC/DESC)
     * @return Paginated list of inventories
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<InventoryResponse>>> getInventories(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productSku,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String locationId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String batchNumber,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction){

        List<String> allowedSortFields = List.of(
                "updatedAt",
                "createdAt",
                "onHandQuantity"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        if (page < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }

        if (size <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }

        if (size > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }

        InventoryFilterRequest filter = InventoryFilterRequest.builder()
                .productId(productId)
                .productSku(productSku)
                .productName(productName)
                .warehouseId(warehouseId)
                .locationId(locationId)
                .batchId(batchId)
                .batchNumber(batchNumber)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<InventoryResponse> response =
                inventoryService.getInventories(filter, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get inventory summary for a specific product.
     *
     * @param productId The product ID (must be a valid UUID)
     * @return Inventory summary
     */
    @GetMapping("/summary/{productId}")
    public ResponseEntity<BaseResponse<InventorySummaryResponse>> getInventorySummary(
            @PathVariable @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String productId) {
        log.info("Received request to get inventory summary for product ID: {}", productId);
        InventorySummaryResponse response = inventoryService.getSummaryByProduct(productId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
