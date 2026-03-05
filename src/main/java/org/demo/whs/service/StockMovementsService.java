package org.demo.whs.service;

import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;

/**
 * Service interface for managing StockMovements operations.
 */
public interface StockMovementsService {

    /**
     * Retrieves a stock movement by its ID.
     *
     * @param id the ID of the stock movement to retrieve
     * @return the stock movement response corresponding to the provided ID
     */
    StockMovementsResponse getById(String id);

    /**
     * Retrieves a paginated list of all stock movements.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing the list of stock movements
     */
    PageResponse<StockMovementsResponse> getAll(Integer page, Integer size);

    /**
     * Retrieves a paginated list of stock movements based on a reference type and reference ID.
     *
     * @param referenceType the type of reference to filter stock movements
     * @param referenceId   the ID of the reference to filter stock movements
     * @param page          the page number to retrieve
     * @param size          the number of items per page
     * @return a paginated response containing the list of stock movements matching the reference criteria
     */
    PageResponse<StockMovementsResponse> getByReference(
            ReferenceType referenceType,
            String referenceId,
            Integer page,
            Integer size
    );
}
