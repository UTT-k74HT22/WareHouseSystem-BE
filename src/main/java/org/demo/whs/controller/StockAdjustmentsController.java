package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.service.StockAdjustmentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<BaseResponse<PageResponse<StockAdjustmentsResponse>>> getStockAdjustments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Attempting to retrieve stock adjustments with page: {} and size: {}", page, size);
        PageResponse<StockAdjustmentsResponse> response = stockAdjustmentsService.getAll(page, size);
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
    public ResponseEntity<BaseResponse<StockAdjustmentsResponse>> rejectStockAdjustment(
            @PathVariable String id,
            @RequestBody @Valid RejectStockAdjustmentRequest request) {
        log.info("Attempting to reject stock adjustment with ID: {} and rejection note: {}", id, request.getRejectionReason());
        StockAdjustmentsResponse response = stockAdjustmentsService.reject(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
