package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.service.StockMovementsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing stock movements.
 */
@RequestMapping("/api/v1/stock-movements")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StockMovementsController {

    private final StockMovementsService stockMovementsService;

    /**
     * Endpoint to retrieve a stock movement by its ID.
     *
     * @param id the ID of the stock movement to retrieve
     * @return a response entity containing the retrieved stock movement response
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockMovementsResponse>> getMovement(@PathVariable String id) {
        StockMovementsResponse response = stockMovementsService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve all stock movements with pagination.
     *
     * @param page the page number to retrieve (default is 0)
     * @param size the number of items per page (default is 20)
     * @return a response entity containing a paginated list of stock movement responses
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<StockMovementsResponse>>> getMovements(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResponse<StockMovementsResponse> response = stockMovementsService.getAll(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve stock movements by reference type and ID with pagination.
     *
     * @param referenceType the type of reference (e.g., STOCK_ADJUSTMENT, STOCK_TRANSFER)
     * @param referenceId   the ID of the reference
     * @param page          the page number to retrieve (default is 0)
     * @param size          the number of items per page (default is 20)
     * @return a response entity containing a paginated list of stock movement responses
     */
    @GetMapping("/reference/{referenceType}/{referenceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<StockMovementsResponse>>> getMovementsByReference(
            @PathVariable ReferenceType referenceType,
            @PathVariable String referenceId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        PageResponse<StockMovementsResponse> response = stockMovementsService.getByReference(referenceType, referenceId, page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
