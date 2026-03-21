package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.service.StockTransfersService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing stock transfers.
 */
@RequestMapping("/api/v1/stock-transfers")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class StockTransfersController {

    private final StockTransfersService stockTransfersService;

    /**
     * Endpoint to create a new stock transfer.
     *
     * @param request the stock transfer request details
     * @return a response entity containing the created stock transfer response
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockTransfersResponse>> createTransfer(
            @RequestBody @Valid StockTransfersRequest request) {
        StockTransfersResponse response = stockTransfersService.createTransfer(request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve a stock transfer by its ID.
     *
     * @param id the ID of the stock transfer to retrieve
     * @return a response entity containing the retrieved stock transfer response
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockTransfersResponse>> getTransfer(@PathVariable String id) {
        StockTransfersResponse response = stockTransfersService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to submit a stock transfer for approval.
     *
     * @param id the ID of the stock transfer to submit
     * @return a response entity containing the updated stock transfer response after submission
     */
    @PutMapping("/{id}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockTransfersResponse>> submitTransfer(@PathVariable String id) {
        StockTransfersResponse response = stockTransfersService.submit(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve a paginated list of all stock transfers.
     *
     * @param page the page number for pagination
     * @param size the page size for pagination
     * @return a response entity containing a paginated response with the list of stock transfers
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<StockTransfersResponse>>> getTransfers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<StockTransfersResponse> response = stockTransfersService.getAll(page, size);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to complete a stock transfer.
     *
     * @param id the ID of the stock transfer to complete
     * @return a response entity containing the updated stock transfer response after completion
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockTransfersResponse>> completeTransfer(@PathVariable String id) {
        StockTransfersResponse response = stockTransfersService.complete(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to cancel a stock transfer.
     *
     * @param id the ID of the stock transfer to cancel
     * @return a response entity containing the updated stock transfer response after cancellation
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<StockTransfersResponse>> cancelTransfer(@PathVariable String id) {
        StockTransfersResponse response = stockTransfersService.cancel(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
