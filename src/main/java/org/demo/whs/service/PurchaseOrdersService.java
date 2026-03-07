package org.demo.whs.service;

import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing purchase orders.
 */
public interface PurchaseOrdersService {

    /**
     * Creates a new purchase order draft based on the provided request details.
     *
     * @param request the purchase order request containing the details for the purchase order
     * @return the response containing the details of the created purchase order
     */
    PurchaseOrdersResponse create(PurchaseOrdersRequest request);

    /**
     * Retrieves a paginated list of purchase orders based on the provided filter criteria.
     *
     * @param filter   the filter criteria for retrieving purchase orders
     * @param pageable the pagination information for retrieving purchase orders
     * @return a paginated response containing the list of purchase orders that match the filter criteria
     */
    PageResponse<PurchaseOrdersResponse> getAll(PurchaseOrdersFilterRequest filter, Pageable pageable);

    /**
     * Retrieves a purchase order by its unique identifier.
     *
     * @param id the unique identifier of the purchase order to retrieve
     * @return the response containing the details of the retrieved purchase order
     */
    PurchaseOrdersResponse getById(String id);

    /**
     * Updates an existing purchase order based on the provided unique identifier and update details.
     *
     * @param id      the unique identifier of the purchase order to update
     * @param request the update request containing the details to update for the purchase order
     * @return the response containing the details of the updated purchase order
     */
    PurchaseOrdersResponse update(String id, UpdatePurchaseOrdersRequest request);

    /**
     * Deletes a purchase order draft based on the provided unique identifier.
     *
     * @param id the unique identifier of the purchase order to delete
     */
    void delete(String id);

    /**
     * Confirms a purchase order based on the provided unique identifier.
     *
     * @param id the unique identifier of the purchase order to confirm
     * @return the response containing the details of the confirmed purchase order
     */
    PurchaseOrdersResponse confirm(String id);
}
