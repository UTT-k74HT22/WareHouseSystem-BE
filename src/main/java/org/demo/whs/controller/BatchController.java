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
import org.demo.whs.entity.dto.request.Batch.SearchBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.service.BatchService;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;

@RequestMapping("/api/v1/batches")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Batch Management", description = "Endpoints for managing product batches")
public class BatchController {

    private final BatchService batchService;

    /**
     * Create a new batch for a product that requires batch tracking.
     *
     * @param request the request containing the details of the batch to be created
     * @return the response containing the details of the created batch
     */
    @PostMapping
    @Operation(summary = "Create batch", description = "Create a new batch for a product that requires batch tracking")
    @PreAuthorize("hasAuthority('PERM_BATCH_CREATE')")
    public ResponseEntity<BaseResponse<BatchResponse>> createBatch(
            @RequestBody @Valid CreateBatchRequest request) {
        log.info("Create batch request: {}", request);
        BatchResponse response = batchService.createBatch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Batch created successfully"));
    }

    /**
     * Get batch details by its identifier.
     *
     * @param id the identifier of the batch to be retrieved
     * @return the response containing the details of the batch with the specified identifier
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get batch by id", description = "Fetch a batch by its identifier")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<BatchResponse>> getBatchesById(
            @PathVariable String id) {
        log.info("Get batches with id: {}", id);

        BatchResponse response = batchService.getBatchById(id);
        BaseResponse<BatchResponse> baseResponse = BaseResponse.success(response, "Batch retrieved successfully");
        return ResponseEntity.ok(baseResponse);
    }

    /**
     * List batches with business filters and pagination.
     *
     * @param keyword                 optional search keyword for batch number or product name
     * @param productId               optional filter by product identifier
     * @param warehouseId             optional filter by warehouse identifier
     * @param status                  optional filter by batch status
     * @param manufacturingDateFrom   optional filter for manufacturing date range start
     * @param manufacturingDateTo     optional filter for manufacturing date range end
     * @param expiryDateFrom         optional filter for expiry date range start
     * @param expiryDateTo           optional filter for expiry date range end
     * @param page                    page number for pagination (default: 0)
     * @param size                    page size for pagination (default: 10, max: 100)
     * @return paginated response containing the list of batches matching the filters
     */
    @GetMapping
    @Operation(summary = "List batches", description = "List batches with business filters and pagination")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<PageResponse<BatchResponse>>> getAllBatches(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "product_id", required = false) String productId,
            @RequestParam(name = "warehouse_id", required = false) String warehouseId,
            @RequestParam(name = "status", required = false) BatchStatus status,
            @RequestParam(name = "manufacturing_date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate manufacturingDateFrom,
            @RequestParam(name = "manufacturing_date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate manufacturingDateTo,
            @RequestParam(name = "expiry_date_from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateFrom,
            @RequestParam(name = "expiry_date_to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDateTo,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name = "size", defaultValue = "10") @Min(1) @Max(100) Integer size) {
        SearchBatchRequest request = buildSearchRequest(
                keyword,
                productId,
                warehouseId,
                status,
                manufacturingDateFrom,
                manufacturingDateTo,
                expiryDateFrom,
                expiryDateTo
        );
        log.info("Fetching batches with filters - request={}, page={}, size={}", request, page, size);

        PageResponse<BatchResponse> response = batchService.getAllBatches(request, page, size);
        BaseResponse<PageResponse<BatchResponse>> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Get complete traceability of a batch from receipt to shipment.
     *
     * @param id the identifier of the batch to retrieve traceability for
     * @return the response containing the complete traceability information of the batch
     */
    @GetMapping("/{id}/traceability")
    @Operation(summary = "Get batch traceability", description = "Display complete batch traceability from receipt to shipment")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<BatchTraceabilityResponse>> getBatchTraceability(@PathVariable String id) {
        BatchTraceabilityResponse response = batchService.getBatchTraceability(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch traceability retrieved successfully"));
    }

    /**
     * List batches approaching expiry that still have stock.
     *
     * @param thresholdDays the number of days until expiry to consider a batch as expiring (default: 30)
     * @param warehouseId   optional filter by warehouse identifier
     * @return the response containing the list of expiring batches
     */
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring batches", description = "List batches approaching expiry that still have stock")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<List<BatchExpiringResponse>>> getExpiringBatches(
            @RequestParam(name = "threshold_days", defaultValue = "30") @Min(0) Integer thresholdDays,
            @RequestParam(name = "warehouse_id", required = false) String warehouseId) {
        List<BatchExpiringResponse> response = batchService.getExpiringBatches(thresholdDays, warehouseId);
        return ResponseEntity.ok(BaseResponse.success(response, "Expiring batches retrieved successfully"));
    }

    /**
     * Recommend oldest eligible batches for outbound picking based on FIFO principle.
     *
     * @param productId   the identifier of the product to get batch recommendations for
     * @param warehouseId the identifier of the warehouse to filter batches by
     * @param limit       the maximum number of batch recommendations to return (default: 5, max: 50)
     * @return the response containing the list of FIFO batch recommendations
     */
    @GetMapping("/fifo-recommendations")
    @Operation(summary = "Get FIFO recommendations", description = "Recommend oldest eligible batches for outbound picking")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<List<BatchFifoRecommendationResponse>>> getFifoRecommendations(
            @RequestParam(name = "product_id") @NotBlank String productId,
            @RequestParam(name = "warehouse_id") @NotBlank String warehouseId,
            @RequestParam(name = "limit", defaultValue = "5") @Min(1) @Max(50) Integer limit) {
        List<BatchFifoRecommendationResponse> response = batchService.getFifoRecommendations(productId, warehouseId, limit);
        return ResponseEntity.ok(BaseResponse.success(response, "FIFO recommendations retrieved successfully"));
    }

    /**
     * List all batches for a product with inventory summary.
     *
     * @param productId   the identifier of the product to list batches for
     * @param warehouseId optional filter by warehouse identifier
     * @return the response containing the list of batches for the product with inventory summary
     */
    @GetMapping("/by-product/{productId}")
    @Operation(summary = "Get batches by product", description = "List all batches for a product with inventory summary")
    @PreAuthorize("hasAuthority('PERM_BATCH_READ')")
    public ResponseEntity<BaseResponse<List<BatchByProductResponse>>> getBatchesByProduct(
            @PathVariable String productId,
            @RequestParam(name = "warehouse_id", required = false) String warehouseId) {
        List<BatchByProductResponse> response = batchService.getBatchesByProduct(productId, warehouseId);
        return ResponseEntity.ok(BaseResponse.success(response, "Batches retrieved successfully"));
    }

    /**
     * Change the status of a batch with generic status transitions.
     *
     * @param id      the identifier of the batch to change status for
     * @param request the request containing the new status and optional reason
     * @return the response containing the details of the batch with updated status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change batch status", description = "Generic status changes are blocked. Use dedicated workflow endpoints instead")
    @PreAuthorize("hasAuthority('PERM_BATCH_UPDATE')")
    public ResponseEntity<BaseResponse<BatchResponse>> changeBatchStatus(
            @PathVariable String id,
            @RequestBody @Valid ChangeBatchStatusRequest request) {

        BatchResponse response = batchService.changeBatchStatus(id, request);

        return ResponseEntity.ok(BaseResponse.success(response, "Batch status changed successfully"));
    }

    /**
     * Update mutable batch master data such as manufacturing date, expiry date, and quantity.
     *
     * @param id      the identifier of the batch to be updated
     * @param request the request containing the updated batch details
     * @return the response containing the details of the updated batch
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update batch", description = "Update mutable batch master data")
    @PreAuthorize("hasAuthority('PERM_BATCH_UPDATE')")
    public ResponseEntity<BaseResponse<BatchResponse>> updateBatch(
            @PathVariable String id,
            @Valid @RequestBody UpdateBatchRequest request) {

        BatchResponse response = batchService.updateBatch(id, request);

        return ResponseEntity.ok(
                BaseResponse.success(response, "Batch updated successfully")
        );
    }

    /**
     * Move an AVAILABLE batch to QUARANTINE with required reason.
     *
     * @param id      the identifier of the batch to be quarantined
     * @param request the request containing the reason for quarantine
     * @return the response containing the details of the quarantined batch
     */
    @PutMapping("/{id}/quarantine")
    @Operation(summary = "Quarantine batch", description = "Move an AVAILABLE batch to QUARANTINE with required reason")
    @PreAuthorize("hasAuthority('PERM_BATCH_UPDATE')")
    public ResponseEntity<BaseResponse<BatchResponse>> quarantineBatch(
            @PathVariable String id,
            @Valid @RequestBody QuarantineBatchRequest request) {
        BatchResponse response = batchService.quarantineBatch(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch quarantined successfully"));
    }

    /**
     * Release a QUARANTINE batch back to AVAILABLE with required release notes.
     *
     * @param id      the identifier of the batch to be released
     * @param request the request containing the release notes
     * @return the response containing the details of the released batch
     */
    @PutMapping("/{id}/release")
    @Operation(summary = "Release batch", description = "Release a QUARANTINE batch back to AVAILABLE with required release notes")
    @PreAuthorize("hasAuthority('PERM_BATCH_UPDATE')")
    public ResponseEntity<BaseResponse<BatchResponse>> releaseBatch(
            @PathVariable String id,
            @Valid @RequestBody ReleaseBatchRequest request) {
        BatchResponse response = batchService.releaseBatch(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Batch released successfully"));
    }

    private SearchBatchRequest buildSearchRequest(
            String keyword,
            String productId,
            String warehouseId,
            BatchStatus status,
            LocalDate manufacturingDateFrom,
            LocalDate manufacturingDateTo,
            LocalDate expiryDateFrom,
            LocalDate expiryDateTo) {
        SearchBatchRequest request = new SearchBatchRequest();
        request.setKeyword(keyword);
        request.setProductId(productId);
        request.setWarehouseId(warehouseId);
        request.setStatus(status);
        request.setManufacturingDateFrom(manufacturingDateFrom);
        request.setManufacturingDateTo(manufacturingDateTo);
        request.setExpiryDateFrom(expiryDateFrom);
        request.setExpiryDateTo(expiryDateTo);
        return request;
    }
}
