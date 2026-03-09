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

    InboundReceiptsResponse create(InboundReceiptsRequest request);

    PageResponse<InboundReceiptsResponse> getAll(InboundReceiptsFilterRequest filter, Pageable pageable);

    InboundReceiptsResponse getById(String id);

    InboundReceiptsResponse update(String id, UpdateInboundReceiptsRequest request);

    void delete(String id);

    List<InboundReceiptsResponse> getByPurchaseOrderId(String purchaseOrderId);

    InboundReceiptsResponse confirm(String id);
}
