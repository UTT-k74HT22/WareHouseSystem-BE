package org.demo.whs.service;

import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing Sales Orders.
 */
public interface SalesOrdersService {

    SalesOrdersResponse create(SalesOrdersRequest request);

    PageResponse<SalesOrdersResponse> getAll(SalesOrdersFilterRequest filter, Pageable pageable);

    SalesOrdersResponse getById(String id);

    SalesOrdersResponse update(String id, UpdateSalesOrdersRequest request);

    SalesOrdersResponse confirm(String id);

    SalesOrdersResponse cancel(String id);
}
