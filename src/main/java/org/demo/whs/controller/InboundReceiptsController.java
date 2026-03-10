package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing inbound receipts.
 */
@RequestMapping("/api/v1/inbound-receipts")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Inbound Receipts", description = "APIs for managing inbound receipts")
@PreAuthorize("isAuthenticated()")
public class InboundReceiptsController {
    private final InboundReceiptsService inboundReceiptsService;

    /**
     * Create a new inbound receipt draft.
     *
     * @param request the request containing the details of the inbound receipt to be created
     * @return the response containing the details of the created inbound receipt
     */
    @Operation(summary = "Create inbound receipt draft")
    @PostMapping
    public ResponseEntity<BaseResponse<InboundReceiptsResponse>> create(@RequestBody @Valid InboundReceiptsRequest request) {
        log.info("Create inbound receipt draft");
        InboundReceiptsResponse response = inboundReceiptsService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Get a paginated list of inbound receipts based on the provided filter criteria.
     *
     * @param receiptNumber   the receipt number to filter by (optional)
     * @param purchaseOrderId the purchase order ID to filter by (optional)
     * @param warehouseId     the warehouse ID to filter by (optional)
     * @param status          the status to filter by (optional)
     * @param receiptDateFrom the start date for receipt date filtering (optional)
     * @param receiptDateTo   the end date for receipt date filtering (optional)
     * @param page            the page number for pagination (default is 0)
     * @param size            the page size for pagination (default is 10)
     * @param sortBy          the field to sort by (default is "updatedAt")
     * @param direction       the sort direction, either ASC or DESC (default is DESC)
     * @return a paginated response containing the list of inbound receipts matching the filter criteria
     */
    @Operation(summary = "Get paginated inbound receipts")
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<InboundReceiptsResponse>>> getAll(
            @RequestParam(required = false) String receiptNumber,
            @RequestParam(required = false) String purchaseOrderId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiptDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        List<String> allowedSortFields = List.of(
                "createdAt",
                "updatedAt",
                "receiptNumber",
                "receiptDate",
                "status"
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

        InboundReceiptsFilterRequest filter = InboundReceiptsFilterRequest.builder()
                .receiptNumber(receiptNumber)
                .purchaseOrderId(purchaseOrderId)
                .warehouseId(warehouseId)
                .status(status)
                .receiptDateFrom(receiptDateFrom)
                .receiptDateTo(receiptDateTo)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<InboundReceiptsResponse> response = inboundReceiptsService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get a list of inbound receipts by purchase order ID.
     *
     * @param purchaseOrderId the purchase order ID to filter by
     * @return a response containing the list of inbound receipts associated with the specified purchase order ID
     */
    @Operation(summary = "Get inbound receipts by purchase order id")
    @GetMapping("/by-po/{purchaseOrderId}")
    public ResponseEntity<BaseResponse<List<InboundReceiptsResponse>>> getByPurchaseOrderId(@PathVariable String purchaseOrderId) {
        log.info("Get inbound receipts by purchase order id={}", purchaseOrderId);
        List<InboundReceiptsResponse> response = inboundReceiptsService.getByPurchaseOrderId(purchaseOrderId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get the details of an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be retrieved
     * @return the response containing the details of the inbound receipt with the specified ID
     */
    @Operation(summary = "Get inbound receipt by id")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<InboundReceiptsResponse>> getById(@PathVariable String id) {
        log.info("Get inbound receipt by id={}", id);
        InboundReceiptsResponse response = inboundReceiptsService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Update the details of an existing inbound receipt.
     *
     * @param id      the ID of the inbound receipt to be updated
     * @param request the request containing the updated details of the inbound receipt
     * @return the response containing the details of the updated inbound receipt
     */
    @Operation(summary = "Update inbound receipt draft")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<InboundReceiptsResponse>> update(@PathVariable String id, @RequestBody @Valid UpdateInboundReceiptsRequest request) {
        log.info("Update inbound receipt draft, id={}", id);
        InboundReceiptsResponse response = inboundReceiptsService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Delete an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be deleted
     * @return a response indicating the successful deletion of the inbound receipt
     */
    @Operation(summary = "Delete inbound receipt draft")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Delete inbound receipt draft, id={}", id);
        inboundReceiptsService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    /**
     * Confirm an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be confirmed
     * @return the response containing the details of the confirmed inbound receipt
     */
    @Operation(summary = "Confirm inbound receipt")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BaseResponse<InboundReceiptsResponse>> confirm(@PathVariable String id) {
        log.info("Confirm inbound receipt, id={}", id);
        InboundReceiptsResponse response = inboundReceiptsService.confirm(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
