package org.demo.whs.service;

import org.demo.whs.entity.Locations;
import org.demo.whs.entity.dto.request.Location.ChangeLocationStatusRequest;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.request.Location.SearchLocationRequest;
import org.demo.whs.entity.dto.request.Location.UpdateLocationRequest;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.LocationType;

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
     * Retrieves a paginated list of all locations.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing location information
     */
    PageResponse<LocationResponse> getAllLocations(Integer page, Integer size);

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
     * Searches locations with multiple filters.
     *
     * @param request the search criteria
     * @param page    the page number to retrieve
     * @param size    the number of items per page
     * @return a paginated response containing matching locations
     */
    PageResponse<LocationResponse> searchLocations(SearchLocationRequest request, Integer page, Integer size);

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
    LocationResponse changeLocationStatus(String id, ChangeLocationStatusRequest request);

    /**
     * Deletes a location (soft delete).
     *
     * @param id the unique identifier of the location
     */
    void deleteLocation(String id);

    /**
     * Resolves an active location by type in a warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param type        the location type
     * @return the resolved location
     */
    Locations resolveLocationByType(String warehouseId, LocationType type);
}
