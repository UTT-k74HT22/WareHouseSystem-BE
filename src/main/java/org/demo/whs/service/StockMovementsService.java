package org.demo.whs.service;

import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;

import java.math.BigDecimal;

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

    /**
     * Records a new stock movement.
     *
     * @param movement the stock movement entity to record
     * @return the recorded stock movement response
     */
    StockMovementsResponse recordMovement(StockMovements movement);

    /**
     * Records an increase in inventory based on the provided request and balance details.
     *
     * @param request        the inventory increase request
     * @param quantityBefore the quantity before the increase
     * @param quantityAfter  the quantity after the increase
     * @return the recorded stock movement response
     */
    StockMovementsResponse recordIncrease(
            InventoryIncreaseRequest request,
            BigDecimal quantityBefore,
            BigDecimal quantityAfter
    );

    /**
     * Checks if a stock movement exists for the provided reference type and ID.
     *
     * @param referenceType the type of reference
     * @param referenceId   the ID of the reference
     * @return true if a stock movement exists for the given reference, false otherwise
     */
    boolean existsByReference(ReferenceType referenceType, String referenceId);

    /**
     * Checks if a stock movement exists for the provided reference type and reference number.
     *
     * @param referenceType    the type of reference
     * @param referenceNumber the number of the reference
     * @return true if a stock movement exists for the given reference, false otherwise
     */
    boolean existsByReferenceNumber(ReferenceType referenceType, String referenceNumber);
}
