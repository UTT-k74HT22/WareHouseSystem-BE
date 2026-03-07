package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.PurchaseOrdersService;
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
 * Controller for managing purchase orders.
 */
@RequestMapping("/api/v1/purchase-orders")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Purchase Orders", description = "APIs for managing purchase orders")
@PreAuthorize("isAuthenticated()")
public class PurchaseOrdersController {

    private final PurchaseOrdersService purchaseOrdersService;

    /**
     * Create a new purchase order draft.
     *
     * @param request the request containing the details of the purchase order to create
     * @return the response containing the details of the created purchase order
     */
    @Operation(summary = "Create purchase order draft")
    @PostMapping
    public ResponseEntity<BaseResponse<PurchaseOrdersResponse>> create(@RequestBody @Valid PurchaseOrdersRequest request) {
        log.info("Create purchase order draft");
        PurchaseOrdersResponse response = purchaseOrdersService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Get a paginated list of purchase orders with optional filtering and sorting.
     *
     * @param purchaseOrderNumber      optional filter by purchase order number
     * @param supplierId               optional filter by supplier ID
     * @param warehouseId              optional filter by warehouse ID
     * @param status                   optional filter by purchase order status
     * @param orderDateFrom            optional filter for order date range start
     * @param orderDateTo              optional filter for order date range end
     * @param expectedDeliveryDateFrom optional filter for expected delivery date range start
     * @param expectedDeliveryDateTo   optional filter for expected delivery date range end
     * @param page                     the page number to retrieve (default is 0)
     * @param size                     the number of items per page (default is 10)
     * @param sortBy                   the field to sort by (default is "updatedAt")
     * @param direction                the sort direction, either ASC or DESC (default is DESC)
     * @return a paginated response containing the list of purchase orders matching the filters
     */
    @Operation(summary = "Get paginated purchase orders")
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<PurchaseOrdersResponse>>> getAll(
            @RequestParam(required = false) String purchaseOrderNumber,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDeliveryDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedDeliveryDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        List<String> allowedSortFields = List.of(
                "createdAt",
                "updatedAt",
                "purchaseOrderNumber",
                "orderDate",
                "expectedDeliveryDate",
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

        PurchaseOrdersFilterRequest filter = PurchaseOrdersFilterRequest.builder()
                .purchaseOrderNumber(purchaseOrderNumber)
                .supplierId(supplierId)
                .warehouseId(warehouseId)
                .status(status)
                .orderDateFrom(orderDateFrom)
                .orderDateTo(orderDateTo)
                .expectedDeliveryDateFrom(expectedDeliveryDateFrom)
                .expectedDeliveryDateTo(expectedDeliveryDateTo)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<PurchaseOrdersResponse> response = purchaseOrdersService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get a purchase order by its unique identifier.
     *
     * @param id the unique identifier of the purchase order to retrieve
     * @return the response containing the details of the retrieved purchase order
     */
    @Operation(summary = "Get purchase order by id")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<PurchaseOrdersResponse>> getById(@PathVariable String id) {
        log.info("Get purchase order by id: {}", id);
        PurchaseOrdersResponse response = purchaseOrdersService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Update a purchase order draft by its unique identifier.
     *
     * @param id      the unique identifier of the purchase order to update
     * @param request the request containing the updated details of the purchase order
     * @return the response containing the details of the updated purchase order
     */
    @Operation(summary = "Update purchase order draft")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<PurchaseOrdersResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdatePurchaseOrdersRequest request) {
        log.info("Update purchase order draft with id: {}, request: {}", id, request);
        PurchaseOrdersResponse response = purchaseOrdersService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Delete a purchase order draft by its unique identifier.
     *
     * @param id the unique identifier of the purchase order to delete
     * @return a response indicating the success of the deletion operation
     */
    @Operation(summary = "Delete purchase order draft")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting purchase order with id: {}", id);
        purchaseOrdersService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }

    /**
     * Confirm a purchase order, changing its status from DRAFT to CONFIRMED.
     *
     * @param id the unique identifier of the purchase order to confirm
     * @return the response containing the details of the confirmed purchase order
     */
    @Operation(summary = "Confirm purchase order")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BaseResponse<PurchaseOrdersResponse>> confirm(@PathVariable String id) {
        log.info("Confirming purchase order with id: {}", id);
        PurchaseOrdersResponse response = purchaseOrdersService.confirm(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
