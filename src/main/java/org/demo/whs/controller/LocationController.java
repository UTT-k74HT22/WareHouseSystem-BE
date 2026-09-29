package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.request.Location.UpdateLocationRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
import org.demo.whs.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing warehouse locations.
 * Provides endpoints for CRUD operations and location search.
 */
@RequestMapping("/api/v1/locations")
@RestController
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    /**
     * Endpoint to create a new location.
     *
     * @param request the request containing location details
     * @return the response containing created location information
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_LOCATION_CREATE')")
    public ResponseEntity<BaseResponse<LocationResponse>> createLocation(
            @RequestBody @Valid CreateLocationRequest request) {
        log.info("Received request to create location: warehouse={}, code={}",
                request.getWarehouseId(), request.getCode());

        LocationResponse response = locationService.createLocation(request);
        BaseResponse<LocationResponse> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to retrieve locations with pagination and optional filters.
     * No filter params = all locations; with filters = filtered search.
     *
     * @param page the page number to retrieve (default: 0)
     * @param size the number of items per page (default: 10)
     * @param warehouseId the warehouse ID (optional)
     * @param keyword optional keyword matched against code, name and zone
     * @param type the location type (optional)
     * @param status the location status (optional)
     * @return a paginated response containing location information
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_LOCATION_READ')")
    public ResponseEntity<BaseResponse<PageResponse<LocationResponse>>> getAllLocations(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocationType type,
            @RequestParam(required = false) LocationStatus status) {
        log.info("Fetching locations - page: {}, size: {}, warehouse={}, keyword={}, type={}, status={}",
                page, size, warehouseId, keyword, type, status);

        PageResponse<LocationResponse> response =
                locationService.getLocations(page, size, warehouseId, keyword, type, status);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Endpoint to retrieve location statistics by status.
     *
     * @return location statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_LOCATION_READ')")
    public ResponseEntity<BaseResponse<Map<String, Long>>> getStats() {
        log.info("Fetching location statistics");
        return ResponseEntity.ok(BaseResponse.success(locationService.getStats()));
    }

    /**
     * Endpoint to retrieve a specific location by ID.
     *
     * @param id the unique identifier of the location
     * @return the response containing location information
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_LOCATION_READ')")
    public ResponseEntity<BaseResponse<LocationResponse>> getLocationById(
            @PathVariable String id) {
        log.info("Fetching location by id: {}", id);

        LocationResponse response = locationService.getLocationById(id);
        BaseResponse<LocationResponse> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to retrieve locations for a specific warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param page        the page number to retrieve (default: 0)
     * @param size        the number of items per page (default: 10)
     * @return a paginated response containing location information
     */
    @GetMapping("/warehouse/{warehouseId}")
    @PreAuthorize("hasAuthority('PERM_LOCATION_READ')")
    public ResponseEntity<BaseResponse<PageResponse<LocationResponse>>> getLocationsByWarehouse(
            @PathVariable String warehouseId,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Fetching locations for warehouse: {}, page: {}, size: {}",
                warehouseId, page, size);

        PageResponse<LocationResponse> response =
                locationService.getLocationsByWarehouse(warehouseId, page, size);
        BaseResponse<PageResponse<LocationResponse>> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to update an existing location.
     *
     * @param id      the unique identifier of the location to update
     * @param request the request containing updated location details
     * @return the response containing updated location information
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_LOCATION_UPDATE')")
    public ResponseEntity<BaseResponse<LocationResponse>> updateLocation(
            @PathVariable String id,
            @RequestBody @Valid UpdateLocationRequest request) {
        log.info("Received request to update location: id={}", id);

        LocationResponse response = locationService.updateLocation(id, request);
        BaseResponse<LocationResponse> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to change the status of a location.
     *
     * @param id      the unique identifier of the location
     * @param request the request containing the new status
     * @return the response containing updated location information
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_LOCATION_UPDATE')")
    public ResponseEntity<BaseResponse<LocationResponse>> changeLocationStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateLocationRequest request) {
        log.info("Received request to change location status: id={}, newStatus={}",
                id, request.getStatus());

        LocationResponse response = locationService.changeLocationStatus(id, request);
        BaseResponse<LocationResponse> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to delete a location (soft delete).
     *
     * @param id the unique identifier of the location
     * @return success response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_LOCATION_DELETE')")
    public ResponseEntity<BaseResponse<Void>> deleteLocation(@PathVariable String id) {
        log.info("Received request to delete location: id={}", id);

        locationService.deleteLocation(id);
        BaseResponse<Void> baseResponse = BaseResponse.success(null);

        return ResponseEntity.ok(baseResponse);
    }
}
