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

    PurchaseOrdersResponse create(PurchaseOrdersRequest request);

    PageResponse<PurchaseOrdersResponse> getAll(PurchaseOrdersFilterRequest filter, Pageable pageable);

    PurchaseOrdersResponse getById(String id);

    PurchaseOrdersResponse update(String id, UpdatePurchaseOrdersRequest request);

    void delete(String id);

    PurchaseOrdersResponse confirm(String id);
}
