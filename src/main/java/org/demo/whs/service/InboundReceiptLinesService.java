package org.demo.whs.service;

import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLineUpdateRequest;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import java.util.List;

public interface InboundReceiptLinesService {

    /**
     * Create a new line for an inbound receipt in DRAFT status.
     *
     * @param request the request body containing line details
     * @return the created line details
     */
    InboundReceiptLinesResponse create(InboundReceiptLinesRequest request);

    /**
     * Update an existing line of an inbound receipt in DRAFT status.
     *
     * @param id      the ID of the line to update
     * @param request the request body containing updated line details
     * @return the updated line details
     */
    InboundReceiptLinesResponse update(String id, InboundReceiptLineUpdateRequest request);

    /**
     * Delete a line of an inbound receipt in DRAFT status.
     *
     * @param id the ID of the line to delete
     */
    void delete(String id);

    /**
     * Get all lines for a specific inbound receipt, ordered by line number.
     *
     * @param inboundReceiptId the ID of the inbound receipt
     * @return list of lines for the specified inbound receipt
     */
    List<InboundReceiptLinesResponse> findByInboundReceiptId(String inboundReceiptId);
}
