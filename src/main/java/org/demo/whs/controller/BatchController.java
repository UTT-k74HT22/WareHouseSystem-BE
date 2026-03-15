package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.BatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/v1/batches")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Batch Management", description = "Endpoints for managing product batches")
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    @Operation(summary = "Create batch", description = "Create a new batch for a product that requires batch tracking")
    public ResponseEntity<BaseResponse<BatchResponse>> createBatch(
            @RequestBody @Valid CreateBatchRequest request) {
        log.info("Create batch request: {}", request);
        BatchResponse response = batchService.createBatch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Batch created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch by id", description = "Fetch a batch by its identifier")
    public ResponseEntity<BaseResponse<BatchResponse>> getBatchesById(
            @PathVariable String id) {
        log.info("Get batches with id: {}", id);

        BatchResponse response = batchService.getBatchById(id);
        BaseResponse<BatchResponse> baseResponse = BaseResponse.success(response, "Batch retrieved successfully");
        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping
    @Operation(summary = "List batches", description = "List batches with pagination")
    public ResponseEntity<BaseResponse<PageResponse<BatchResponse>>> getAllBatches(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Fetching all batches - page: {}, size: {}", page, size);

        PageResponse<BatchResponse> response = batchService.getAllBatches(page, size);
        BaseResponse<PageResponse<BatchResponse>> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping("/{id}/traceability")
    @Operation(summary = "Get batch traceability", description = "Display complete batch traceability from receipt to shipment")
    public ResponseEntity<BaseResponse<BatchTraceabilityResponse>> getBatchTraceability(@PathVariable String id) {
        BatchTraceabilityResponse response = batchService.getBatchTraceability(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch traceability retrieved successfully"));
    }

    @GetMapping("/expiring")
    @Operation(summary = "Get expiring batches", description = "List batches approaching expiry that still have stock")
    public ResponseEntity<BaseResponse<List<BatchExpiringResponse>>> getExpiringBatches(
            @RequestParam(name = "threshold_days", defaultValue = "30") @Min(0) Integer thresholdDays,
            @RequestParam(name = "warehouse_id", required = false) String warehouseId) {
        List<BatchExpiringResponse> response = batchService.getExpiringBatches(thresholdDays, warehouseId);
        return ResponseEntity.ok(BaseResponse.success(response, "Expiring batches retrieved successfully"));
    }

    @GetMapping("/fifo-recommendations")
    @Operation(summary = "Get FIFO recommendations", description = "Recommend oldest eligible batches for outbound picking")
    public ResponseEntity<BaseResponse<List<BatchFifoRecommendationResponse>>> getFifoRecommendations(
            @RequestParam(name = "product_id") @NotBlank String productId,
            @RequestParam(name = "warehouse_id") @NotBlank String warehouseId,
            @RequestParam(name = "limit", defaultValue = "5") @Min(1) @Max(50) Integer limit) {
        List<BatchFifoRecommendationResponse> response = batchService.getFifoRecommendations(productId, warehouseId, limit);
        return ResponseEntity.ok(BaseResponse.success(response, "FIFO recommendations retrieved successfully"));
    }

    @GetMapping("/by-product/{productId}")
    @Operation(summary = "Get batches by product", description = "List all batches for a product with inventory summary")
    public ResponseEntity<BaseResponse<List<BatchByProductResponse>>> getBatchesByProduct(
            @PathVariable String productId,
            @RequestParam(name = "warehouse_id", required = false) String warehouseId) {
        List<BatchByProductResponse> response = batchService.getBatchesByProduct(productId, warehouseId);
        return ResponseEntity.ok(BaseResponse.success(response, "Batches retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change batch status", description = "Generic status changes are blocked. Use dedicated workflow endpoints instead")
    public ResponseEntity<BaseResponse<BatchResponse>> changeBatchStatus(
            @PathVariable String id,
            @RequestBody @Valid ChangeBatchStatusRequest request) {

        BatchResponse response = batchService.changeBatchStatus(id, request);

        return ResponseEntity.ok(BaseResponse.success(response, "Batch status changed successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update batch", description = "Update mutable batch master data")
    public ResponseEntity<BaseResponse<BatchResponse>> updateBatch(
            @PathVariable String id,
            @Valid @RequestBody UpdateBatchRequest request) {

        BatchResponse response = batchService.updateBatch(id, request);

        return ResponseEntity.ok(
                BaseResponse.success(response, "Batch updated successfully")
        );
    }

    @PutMapping("/{id}/quarantine")
    @Operation(summary = "Quarantine batch", description = "Move an AVAILABLE batch to QUARANTINE with required reason")
    public ResponseEntity<BaseResponse<BatchResponse>> quarantineBatch(
            @PathVariable String id,
            @Valid @RequestBody QuarantineBatchRequest request) {
        BatchResponse response = batchService.quarantineBatch(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch quarantined successfully"));
    }

    @PutMapping("/{id}/release")
    @Operation(summary = "Release batch", description = "Release a QUARANTINE batch back to AVAILABLE with required release notes")
    public ResponseEntity<BaseResponse<BatchResponse>> releaseBatch(
            @PathVariable String id,
            @Valid @RequestBody ReleaseBatchRequest request) {
        BatchResponse response = batchService.releaseBatch(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch released successfully"));
    }
}
