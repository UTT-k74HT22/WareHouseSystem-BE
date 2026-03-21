package org.demo.whs.service;

import org.demo.whs.entity.dto.request.SalesOrderLines.CreateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.UpdateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;

import java.util.List;

/**
 * Service interface for managing SalesOrderLines.
 */
public interface SalesOrderLinesService {

    SalesOrderLinesResponse create(CreateSalesOrderLinesRequest request);

    SalesOrderLinesResponse update(String id, UpdateSalesOrderLinesRequest request);

    List<SalesOrderLinesResponse> getBySalesOrder(String salesOrderId);
}
