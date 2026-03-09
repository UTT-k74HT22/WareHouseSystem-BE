package org.demo.whs.service;

import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;

/**
 * Service interface for managing purchase order lines.
 */
public interface PurchaseOrderLinesService {

    PurchaseOrderLinesResponse create(PurchaseOrderLinesRequest request);

    PurchaseOrderLinesResponse update(String id, UpdatePurchaseOrderLinesRequest request);

    void delete(String id);
}
