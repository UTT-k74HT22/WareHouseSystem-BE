package org.demo.whs.service;

import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;

import java.util.List;

/**
 * Service interface for managing purchase order lines.
 */
public interface PurchaseOrderLinesService {

    /**
     * Retrieves all purchase order lines for a given purchase order, ordered by line number ascending.
     *
     * @param purchaseOrderId the ID of the parent purchase order
     * @return the list of purchase order line responses
     */
    List<PurchaseOrderLinesResponse> getByPurchaseOrderId(String purchaseOrderId);

    /**
     * Creates a new purchase order line.
     *
     * @param request the request containing the details of the purchase order line to create
     * @return the response containing the created purchase order line details
     */
    PurchaseOrderLinesResponse create(PurchaseOrderLinesRequest request);

    /**
     * Updates an existing purchase order line.
     *
     * @param id the ID of the purchase order line to update
     * @param request the request containing the updated details of the purchase order line
     * @return the response containing the updated purchase order line details
     */
    PurchaseOrderLinesResponse update(String id, UpdatePurchaseOrderLinesRequest request);

    void delete(String id);
}
