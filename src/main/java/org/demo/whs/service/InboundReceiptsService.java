package org.demo.whs.service;

import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Service interface for managing inbound receipts.
 */
public interface InboundReceiptsService {

    /**
     * Create a new inbound receipt draft.
     *
     * @param request the request containing the details of the inbound receipt to be created
     * @return the response containing the details of the created inbound receipt
     */
    InboundReceiptsResponse create(InboundReceiptsRequest request);

    /**
     * Get a paginated list of inbound receipts based on the provided filter criteria.
     *
     * @param filter   the filter criteria for retrieving inbound receipts
     * @param pageable the pagination information
     * @return a paginated response containing the list of inbound receipts matching the filter criteria
     */
    PageResponse<InboundReceiptsResponse> getAll(InboundReceiptsFilterRequest filter, Pageable pageable);

    /**
     * Get the details of an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be retrieved
     * @return the response containing the details of the inbound receipt with the specified ID
     */
    InboundReceiptsResponse getById(String id);

    /**
     * Update the details of an existing inbound receipt.
     *
     * @param id      the ID of the inbound receipt to be updated
     * @param request the request containing the updated details of the inbound receipt
     * @return the response containing the details of the updated inbound receipt
     */
    InboundReceiptsResponse update(String id, UpdateInboundReceiptsRequest request);

    /**
     * Delete an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be deleted
     */
    void delete(String id);

    /**
     * Get a list of inbound receipts by purchase order ID.
     *
     * @param purchaseOrderId the purchase order ID to filter by
     * @return a list of inbound receipts associated with the specified purchase order ID
     */
    List<InboundReceiptsResponse> getByPurchaseOrderId(String purchaseOrderId);

    /**
     * Confirm an inbound receipt by its ID.
     *
     * @param id the ID of the inbound receipt to be confirmed
     * @return the response containing the details of the confirmed inbound receipt
     */
    InboundReceiptsResponse confirm(String id);
}
