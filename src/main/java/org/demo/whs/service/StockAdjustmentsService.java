package org.demo.whs.service;

import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;

/**
 * Service interface for stock adjustments workflows.
 */
public interface StockAdjustmentsService {

    /**
     * Creates a new stock adjustment request.
     *
     * @param request the stock adjustment request details
     * @return the created stock adjustment response
     */
    StockAdjustmentsResponse createAdjustment(StockAdjustmentsRequest request);

    /**
     * Retrieves a stock adjustment by its ID.
     *
     * @param id the ID of the stock adjustment
     * @return the stock adjustment response
     */
    StockAdjustmentsResponse getById(String id);

    /**
     * Retrieves a paginated list of all stock adjustments.
     *
     * @param page the page number for pagination
     * @param size the page size for pagination
     * @return a paginated response containing the list of stock adjustments
     */
    PageResponse<StockAdjustmentsResponse> getAll(Integer page, Integer size);

    /**
     * Searches for stock adjustments based on the provided criteria.
     *
     * @param request the search criteria for stock adjustments
     * @param page    the page number for pagination
     * @param size    the page size for pagination
     * @return a paginated response containing the search results
     */
    PageResponse<StockAdjustmentsResponse> search(SearchStockAdjustmentsRequest request, Integer page, Integer size);

    /**
     * Approves a stock adjustment request.
     *
     * @param id         the ID of the stock adjustment to approve
     * @param request    the approval details
     * @return the updated stock adjustment response after approval
     */
    StockAdjustmentsResponse approve(String id, ApproveStockAdjustmentRequest request);

    StockAdjustmentsResponse reject(String id, RejectStockAdjustmentRequest request);
}
