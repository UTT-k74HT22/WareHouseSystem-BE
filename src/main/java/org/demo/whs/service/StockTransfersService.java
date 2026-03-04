package org.demo.whs.service;

import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;

/**
 * Service interface for managing stock transfers operations.
 */
public interface StockTransfersService {

    StockTransfersResponse createTransfer(StockTransfersRequest request);

    StockTransfersResponse getById(String id);

    PageResponse<StockTransfersResponse> getAll(Integer page, Integer size);

    StockTransfersResponse complete(String id);

    StockTransfersResponse cancel(String id);
}
