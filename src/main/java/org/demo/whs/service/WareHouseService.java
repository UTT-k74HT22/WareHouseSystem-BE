package org.demo.whs.service;

import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;

/**
 * Service interface for warehouse operations.
 */
public interface WareHouseService {

    /**
     * Creates a new warehouse based on the provided request.
     *
     * @param request the request containing warehouse details
     * @return the response containing created warehouse information
     */
    WareHouseResponse createWH(CreateWarehouseRequest request);

    /**
     * Retrieves a paginated list of all warehouses.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing warehouse information
     */
    PageResponse<WareHouseResponse> getAll(Integer page, Integer size);

    /**
     * Retrieves a warehouse by its unique identifier.
     *
     * @param id the unique identifier of the warehouse
     * @return the response containing warehouse information
     */
    WareHouseResponse getWareHouseById(String id);
}
