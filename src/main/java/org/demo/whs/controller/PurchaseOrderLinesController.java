package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.service.PurchaseOrderLinesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing purchase order lines.
 */
@RequestMapping("/api/v1/purchase-order-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Purchase Order Lines", description = "APIs for managing purchase order lines")
@PreAuthorize("isAuthenticated()")
public class PurchaseOrderLinesController {
    private final PurchaseOrderLinesService purchaseOrderLinesService;

    /**
     * Get purchase order lines by purchase order ID.
     *
     * @param purchaseOrderId the ID of the purchase order
     * @return a list of purchase order lines associated with the given purchase order ID
     */
    @Operation(summary = "Get purchase order lines by purchase order ID")
    @GetMapping("/purchase-order/{purchaseOrderId}")
    public ResponseEntity<BaseResponse<Iterable<PurchaseOrderLinesResponse>>> getByPurchaseOrderId(@PathVariable String purchaseOrderId) {
        log.info("Get purchase order lines by purchaseOrderId={}", purchaseOrderId);
        Iterable<PurchaseOrderLinesResponse> response = purchaseOrderLinesService.getByPurchaseOrderId(purchaseOrderId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Create a new purchase order line.
     *
     * @param request the request body containing the details of the purchase order line to be created
     * @return the created purchase order line
     */
    @Operation(summary = "Create purchase order line")
    @PostMapping
    public ResponseEntity<BaseResponse<PurchaseOrderLinesResponse>> create(@RequestBody @Valid PurchaseOrderLinesRequest request) {
        log.info("Create purchase order line");
        PurchaseOrderLinesResponse response = purchaseOrderLinesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Update an existing purchase order line.
     *
     * @param id      the ID of the purchase order line to be updated
     * @param request the request body containing the updated details of the purchase order line
     * @return the updated purchase order line
     */
    @Operation(summary = "Update purchase order line")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<PurchaseOrderLinesResponse>> update(@PathVariable String id,
                                                                           @RequestBody @Valid UpdatePurchaseOrderLinesRequest request) {
        log.info("Update purchase order line with id: {}", id);
        PurchaseOrderLinesResponse response = purchaseOrderLinesService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Delete a purchase order line by ID.
     *
     * @param id the ID of the purchase order line to be deleted
     * @return a response indicating the success of the deletion
     */
    @Operation(summary = "Delete purchase order line")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Delete purchase order line with id: {}", id);
        purchaseOrderLinesService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
