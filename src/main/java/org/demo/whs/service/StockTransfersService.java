package org.demo.whs.service;

import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;

/**
 * Service interface for managing stock transfers operations.
 */
public interface StockTransfersService {

    /**
     * Creates a new stock transfer based on the provided request details.
     *
     * @param request the stock transfer request containing the details for the transfer
     * @return the response containing the details of the created stock transfer
     */
    StockTransfersResponse createTransfer(StockTransfersRequest request);

    /**
     * Retrieves a stock transfer by its unique identifier.
     *
     * @param id the unique identifier of the stock transfer to retrieve
     * @return the response containing the details of the retrieved stock transfer
     */
    StockTransfersResponse getById(String id);

    /**
     * Retrieves a paginated list of all stock transfers.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing the list of stock transfers
     */
    PageResponse<StockTransfersResponse> getAll(Integer page, Integer size);

    /**
     * Marks a stock transfer as complete based on its unique identifier.
     *
     * @param id the unique identifier of the stock transfer to mark as complete
     * @return the response containing the details of the completed stock transfer
     */
    StockTransfersResponse complete(String id);

    /**
     * Cancels a stock transfer based on its unique identifier.
     *
     * @param id the unique identifier of the stock transfer to cancel
     * @return the response containing the details of the canceled stock transfer
     */
    StockTransfersResponse cancel(String id);
}
