package org.demo.whs.service;

import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;

/**
 * Service interface for managing StockMovements operations.
 */
public interface StockMovementsService {

    StockMovementsResponse getById(String id);

    PageResponse<StockMovementsResponse> getAll(Integer page, Integer size);

    PageResponse<StockMovementsResponse> getByReference(
            ReferenceType referenceType,
            String referenceId,
            Integer page,
            Integer size
    );
}
