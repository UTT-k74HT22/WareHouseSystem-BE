package org.demo.whs.service;

import org.demo.whs.entity.Locations;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.request.Location.UpdateLocationRequest;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service interface for location-related operations.
 */
public interface LocationService {

    /**
     * Creates a new location in a warehouse.
     *
     * @param request the request containing location details
     * @return the response containing created location information
     */
    LocationResponse createLocation(CreateLocationRequest request);

    /**
     * Retrieves a paginated list of all locations (legacy path, no filters).
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing location information
     */
    PageResponse<LocationResponse> getAllLocations(Integer page, Integer size);

    /**
     * Retrieves locations with pagination and optional filters.
     * All filter params are optional: all null returns all locations.
     *
     * @param page the page number to retrieve
     * @param size the page size
     * @param warehouseId optional warehouse ID
     * @param keyword optional keyword matched against code, name and zone
     * @param type optional location type
     * @param status optional location status
     * @return a paginated response containing location information
     */
    PageResponse<LocationResponse> getLocations(Integer page, Integer size, String warehouseId,
                                                String keyword, LocationType type, LocationStatus status);

    /**
     * Retrieves a location by its unique identifier.
     *
     * @param id the unique identifier of the location
     * @return the response containing location information
     */
    LocationResponse getLocationById(String id);

    /**
     * Retrieves all locations for a specific warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param page        the page number to retrieve
     * @param size        the number of items per page
     * @return a paginated response containing location information
     */
    PageResponse<LocationResponse> getLocationsByWarehouse(String warehouseId, Integer page, Integer size);

    /**
     * Updates an existing location.
     *
     * @param id      the unique identifier of the location to update
     * @param request the request containing updated location details
     * @return the response containing updated location information
     */
    LocationResponse updateLocation(String id, UpdateLocationRequest request);

    /**
     * Changes the status of a location.
     *
     * @param id      the unique identifier of the location
     * @param request the request containing the new status
     * @return the response containing updated location information
     */
    LocationResponse changeLocationStatus(String id, UpdateLocationRequest request);

    /**
     * Deletes a location (soft delete).
     *
     * @param id the unique identifier of the location
     */
    void deleteLocation(String id);

    /**
     * Counts locations by status for dashboard statistics.
     *
     * @return the location statistics
     */
    Map<String, Long> getStats();

    /**
     * Resolves an active location by type in a warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param type        the location type
     * @return the resolved location
     */
    Locations resolveLocationByType(String warehouseId, LocationType type);

    /**
     * Increases the used capacity of a location when physical stock arrives.
     *
     * @param locationId the location ID
     * @param quantity  the quantity to add to used capacity
     */
    int increaseUsedCapacity(String locationId, BigDecimal quantity);

    /**
     * Decreases the used capacity of a location when physical stock leaves.
     *
     * @param locationId the location ID
     * @param quantity  the quantity to subtract from used capacity
     */
    int decreaseUsedCapacity(String locationId, BigDecimal quantity);
}
