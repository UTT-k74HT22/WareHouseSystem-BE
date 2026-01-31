package org.demo.whs.service;

import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
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
}
