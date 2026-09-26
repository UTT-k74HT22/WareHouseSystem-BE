package org.demo.whs.service;

import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.entity.enums.WareHouseType;
import java.util.List;
import java.util.Map;

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
     * Retrieves a paginated list of all warehouses (legacy path, no filters).
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing warehouse information
     */
    PageResponse<WareHouseResponse> getAll(Integer page, Integer size);

    /**
     * Retrieves warehouses with pagination and optional filters.
     * No filter -> delegates to {@link #getAll(Integer, Integer)} to keep legacy behavior.
     * Filter values are raw query strings: blank = ignored, invalid enum -> COM_003.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @param keyword optional keyword matched against code, name and address
     * @param status optional warehouse status
     * @param type optional warehouse type
     * @return a paginated response containing warehouse information
     */
    PageResponse<WareHouseResponse> getWarehouses(Integer page, Integer size, String keyword, WareHouseStatus status, WareHouseType type);

    /**
     * Retrieves a warehouse by its unique identifier.
     *
     * @param id the unique identifier of the warehouse
     * @return the response containing warehouse information
     */
    WareHouseResponse getWareHouseById(String id);

    /**
     * Retrieves a list of all warehouses.
     *
     * @return list of warehouse responses
     */
    List<WareHouseResponse> getWareHouses();

    /**
     * Updates an existing warehouse.
     *
     * @param id      the unique identifier of the warehouse to update
     * @param request the request containing updated warehouse details
     * @return the response containing updated warehouse information
     */
    WareHouseResponse updateWareHouse(String id, UpdateWarehouseRequest request);

    /**
     * Changes the status of a warehouse.
     *
     * @param id      the unique identifier of the warehouse
     * @param request the request containing the new status
     * @return the response containing updated warehouse information
     */
    WareHouseResponse changeStatus(String id, UpdateWarehouseRequest request);

    /**
     * Counts warehouses by status for dashboard statistics.
     */
    Map<String, Long> getStats();

    /**
     *
     * @param id
     */
    void deleteWarehouse (String id);
}
