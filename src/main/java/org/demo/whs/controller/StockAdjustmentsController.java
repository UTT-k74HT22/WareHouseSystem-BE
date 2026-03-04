package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.demo.whs.service.StockAdjustmentsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Controller for managing stock adjustments.
 */
@RequestMapping("/api/v1/stock-adjustments")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StockAdjustmentsController {

    private final StockAdjustmentsService stockAdjustmentsService;

    /**
     * Endpoint to create a new stock adjustment request.
     *
     * @param request the stock adjustment request details
     * @return a response entity containing the created stock adjustment response
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockAdjustmentsResponse>> createStockAdjustment(
            @RequestBody @Valid StockAdjustmentsRequest request) {
        log.info("Attempting to create stock adjustment with details: {}", request);
        StockAdjustmentsResponse response = stockAdjustmentsService.createAdjustment(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve a stock adjustment by its ID.
     *
     * @param id the ID of the stock adjustment to retrieve
     * @return a response entity containing the retrieved stock adjustment response
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockAdjustmentsResponse>> getStockAdjustment(
            @PathVariable String id) {
        log.info("Attempting to retrieve stock adjustment with ID: {}", id);
        StockAdjustmentsResponse response = stockAdjustmentsService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve a paginated list of all stock adjustments.
     *
     * @param page the page number for pagination (default is 0)
     * @param size the page size for pagination (default is 10)
     * @return a response entity containing a paginated response of stock adjustments
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<StockAdjustmentsResponse>>> getStockAdjustments(
            @RequestParam(required = false) StockAdjustmentsStatus status,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String inventoryId,
            @RequestParam(required = false) String adjustmentNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Attempting to retrieve stock adjustments with page: {} and size: {}", page, size);

        SearchStockAdjustmentsRequest searchRequest = new SearchStockAdjustmentsRequest();
        searchRequest.setStatus(status);
        searchRequest.setProductId(productId);
        searchRequest.setWarehouseId(warehouseId);
        searchRequest.setInventoryId(inventoryId);
        searchRequest.setAdjustmentNumber(adjustmentNumber);
        searchRequest.setCreatedFrom(createdFrom);
        searchRequest.setCreatedTo(createdTo);

        boolean hasFilters = status != null
                || productId != null
                || warehouseId != null
                || inventoryId != null
                || adjustmentNumber != null
                || createdFrom != null
                || createdTo != null;

        PageResponse<StockAdjustmentsResponse> response = hasFilters
                ? stockAdjustmentsService.search(searchRequest, page, size)
                : stockAdjustmentsService.getAll(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to approve a stock adjustment request.
     *
     * @param id the ID of the stock adjustment to approve
     * @param request the approval details for the stock adjustment
     * @return a response entity containing the updated stock adjustment response after approval
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockAdjustmentsResponse>> approveStockAdjustment(
            @PathVariable String id,
            @RequestBody @Valid ApproveStockAdjustmentRequest request) {
        log.info("Attempting to approve stock adjustment with ID: {} and approval note: {}", id, request.getApprovalNote());
        StockAdjustmentsResponse response = stockAdjustmentsService.approve(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to reject a stock adjustment request.
     *
     * @param id the ID of the stock adjustment to reject
     * @param request the rejection details for the stock adjustment
     * @return a response entity containing the updated stock adjustment response after rejection
     */
    @PutMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockAdjustmentsResponse>> rejectStockAdjustment(
            @PathVariable String id,
            @RequestBody @Valid RejectStockAdjustmentRequest request) {
        log.info("Attempting to reject stock adjustment with ID: {} and rejection note: {}", id, request.getRejectionReason());
        StockAdjustmentsResponse response = stockAdjustmentsService.reject(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
