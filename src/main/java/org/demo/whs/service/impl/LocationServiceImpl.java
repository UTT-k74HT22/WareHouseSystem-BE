package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Location.ChangeLocationStatusRequest;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.request.Location.SearchLocationRequest;
import org.demo.whs.entity.dto.request.Location.UpdateLocationRequest;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.LocationMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.LocationService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of the LocationService interface.
 * Handles all location management operations with comprehensive validation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final WareHouseRepository wareHouseRepository;
    private final AccountRepository accountRepository;
    private final InventoryRepository inventoryRepository;
    private final LocationMapper locationMapper;
    private final IdentifierGenerator identifierGenerator;

    /**
     * Creates a new location with comprehensive validation.
     *
     * @param request the create location request
     * @return the created location response
     */
    @Override
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        Warehouses warehouse = validateWarehouse(request.getWarehouseId());
        validateWarehouseStatus(warehouse);
        String locationCode = identifierGenerator.generateSystemManaged(
                request.getCode(),
                "Location code",
                "LOC",
                50,
                candidate -> locationRepository.existsByWarehouseIdAndCode(request.getWarehouseId(), candidate)
        );
        log.info("Creating location with code={} in warehouse={}",
                locationCode, request.getWarehouseId());
        validateDuplicateLocationCode(request.getWarehouseId(), locationCode);

        Locations location = locationMapper.toEntity(request);
        location.setCode(locationCode);

        location.setUsedCapacity(BigDecimal.ZERO);

        Account currentUser = getCurrentUser();
        setAuditFieldsForCreate(location, currentUser);
        Locations savedLocation = locationRepository.save(location);
        log.info("Location created successfully: id={}, code={}, warehouse={}", savedLocation.getId(), savedLocation.getCode(), savedLocation.getWarehouseId());
        return locationMapper.toResponseWithWarehouse(savedLocation, warehouse);
    }

    private Warehouses validateWarehouse(String warehouseId) {
        return wareHouseRepository.findById(warehouseId)
                .orElseThrow(() -> {
                    log.warn("Warehouse not found: {}", warehouseId);
                    return new BadRequestException(ErrorCode.WHS_001);
                });
    }

    private void validateWarehouseStatus(Warehouses warehouse) {
        if (warehouse.getStatus() != WareHouseStatus.ACTIVE) {
            log.warn("Warehouse is not active: warehouse={}, status={}",
                    warehouse.getId(), warehouse.getStatus());
            throw new BadRequestException(ErrorCode.LOC_004);
        }
    }

    private void validateDuplicateLocationCode(String warehouseId, String code) {
        if (locationRepository.existsByWarehouseIdAndCode(warehouseId, code)) {
            log.warn("Location code already exists: warehouse={}, code={}",
                    warehouseId, code);
            throw new BadRequestException(ErrorCode.LOC_003);
        }
    }

    /**
     * Retrieves all locations with pagination.
     *
     * @param page the page number
     * @param size the page size
     * @return paginated location response
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> getAllLocations(Integer page, Integer size) {
        log.info("Fetching all locations - page: {}, size: {}", page, size);

        // Validate pagination parameters
        validatePaginationParams(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Locations> locationPage = locationRepository.findAll(pageable);

        Set<String> wareHouseIds = locationPage.getContent().stream()
                .map(Locations::getWarehouseId)
                .collect(Collectors.toSet());

        List<Warehouses> warehouses = wareHouseRepository.findByIdIn(wareHouseIds);
        Map<String, Warehouses> warehouseMap = warehouses.stream()
                .collect(Collectors.toMap(Warehouses::getId, wh -> wh));

        List<LocationResponse> responses = locationPage.getContent().stream()
                .map(location -> {
                    Warehouses warehouse = warehouseMap.get(location.getWarehouseId());
                    return locationMapper.toResponseWithWarehouse(location, warehouse);
                })
                .collect(Collectors.toList());

        return PageResponse.from(locationPage, responses);
    }

    /**
     * Retrieves a location by ID.
     *
     * @param id the location ID
     * @return the location response
     */
    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(String id) {
        log.info("Fetching location by id: {}", id);

        Locations location = findLocationById(id);
        Warehouses warehouse = wareHouseRepository.findById(location.getWarehouseId())
                .orElse(null);

        return locationMapper.toResponseWithWarehouse(location, warehouse);
    }

    /**
     * Retrieves all locations for a specific warehouse.
     *
     * @param warehouseId the warehouse ID
     * @param page        the page number
     * @param size        the page size
     * @return paginated location response
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> getLocationsByWarehouse(
            String warehouseId, Integer page, Integer size) {
        log.info("Fetching locations for warehouse: {}, page: {}, size: {}",
                warehouseId, page, size);

        // Validate warehouse exists
        if (!wareHouseRepository.existsById(warehouseId)) {
            log.warn("Warehouse not found: {}", warehouseId);
            throw new BadRequestException(ErrorCode.WHS_001);
        }

        // Validate pagination parameters
        validatePaginationParams(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("code").ascending());
        Page<Locations> locationPage = locationRepository.findByWarehouseId(warehouseId, pageable);

        List<LocationResponse> responses = locationPage.getContent().stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<LocationResponse>builder()
                .page(page)
                .size(size)
                .totalPages(locationPage.getTotalPages())
                .totalElements(locationPage.getTotalElements())
                .content(responses)
                .isFirst(locationPage.isFirst())
                .isLast(locationPage.isLast())
                .build();
    }

    /**
     * Searches locations with multiple filters.
     *
     * @param request the search criteria
     * @param page    the page number
     * @param size    the page size
     * @return paginated location response
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LocationResponse> searchLocations(
            SearchLocationRequest request, Integer page, Integer size) {
        log.info("Searching locations with filters: warehouse={}, code={}, zone={}, type={}, status={}",
                request.getWarehouseId(), request.getCode(), request.getZone(),
                request.getType(), request.getStatus());

        // Validate pagination parameters
        validatePaginationParams(page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("code").ascending());
        Page<Locations> locationPage = locationRepository.searchLocations(
                request.getWarehouseId(),
                request.getCode(),
                request.getName(),
                request.getZone(),
                request.getType(),
                request.getStatus(),
                pageable
        );

        List<LocationResponse> responses = locationPage.getContent().stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<LocationResponse>builder()
                .page(page)
                .size(size)
                .totalPages(locationPage.getTotalPages())
                .totalElements(locationPage.getTotalElements())
                .content(responses)
                .isFirst(locationPage.isFirst())
                .isLast(locationPage.isLast())
                .build();
    }

    /**
     * Updates an existing location.
     *
     * @param id      the location ID
     * @param request the update request
     * @return the updated location response
     */
    @Override
    @Transactional
    public LocationResponse updateLocation(String id, UpdateLocationRequest request) {
        log.info("Updating location: id={}", id);

        // Find existing location
        Locations location = findLocationById(id);

        // Update fields from request
        locationMapper.updateEntity(location, request);

        // Update audit fields
        Account currentUser = getCurrentUser();
        setAuditFieldsForUpdate(location, currentUser);

        // Save updated location
        Locations updatedLocation = locationRepository.save(location);

        log.info("Location updated successfully: id={}, code={}",
                updatedLocation.getId(), updatedLocation.getCode());

        Warehouses warehouse = wareHouseRepository.findById(location.getWarehouseId())
                .orElse(null);

        return locationMapper.toResponseWithWarehouse(updatedLocation, warehouse);
    }

    /**
     * Changes the status of a location with validation.
     *
     * @param id      the location ID
     * @param request the status change request
     * @return the updated location response
     */
    @Override
    @Transactional
    public LocationResponse changeLocationStatus(String id, ChangeLocationStatusRequest request) {
        log.info("Changing location status: id={}, newStatus={}, reason={}",
                id, request.getStatus(), request.getReason());

        // Find existing location
        Locations location = findLocationById(id);

        LocationStatus oldStatus = location.getStatus();
        LocationStatus newStatus = request.getStatus();

        // Validate status transition
        validateStatusTransition(oldStatus, newStatus);

        // Check if location has active inventory when changing to INACTIVE
        if (newStatus == LocationStatus.INACTIVE) {
            validateNoActiveInventory(id);
        }

        // Update status
        location.setStatus(newStatus);

        // Update audit fields
        Account currentUser = getCurrentUser();
        setAuditFieldsForUpdate(location, currentUser);

        // Save updated location
        Locations updatedLocation = locationRepository.save(location);

        log.info("Location status changed successfully: id={}, oldStatus={}, newStatus={}",
                id, oldStatus, newStatus);

        Warehouses warehouse = wareHouseRepository.findById(location.getWarehouseId())
                .orElse(null);

        return locationMapper.toResponseWithWarehouse(updatedLocation, warehouse);
    }

    /**
     * Deletes a location (soft delete by setting status to INACTIVE).
     *
     * @param id the location ID
     */
    @Override
    @Transactional
    public void deleteLocation(String id) {
        log.info("Deleting (soft delete) location: id={}", id);

        // Find existing location
        Locations location = findLocationById(id);

        // Check if location has inventory before deletion
        validateNoActiveInventory(id);

        location.setStatus(LocationStatus.INACTIVE);

        // Update audit fields
        Account currentUser = getCurrentUser();
        setAuditFieldsForUpdate(location, currentUser);

        locationRepository.save(location);

        log.info("Location soft deleted successfully: id={}, code={}",
                location.getId(), location.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public Locations resolveLocationByType(String warehouseId, LocationType type) {
        log.info("Resolving location for warehouse: {} and type: {}", warehouseId, type);
        List<Locations> locations = locationRepository.findByWarehouseIdAndTypeAndStatus(warehouseId, type, LocationStatus.ACTIVE);
        if (locations.isEmpty()) {
            log.error("No active location found for warehouse: {} and type: {}", warehouseId, type);
            throw new BadRequestException(ErrorCode.LOC_001);
        }
        return locations.get(0);
    }

    @Override
    @Transactional
    public int increaseUsedCapacity(String locationId, BigDecimal quantity) {
        Locations location = locationRepository.findByIdForUpdate(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found", ErrorCode.LOC_001));

        if (isTransitLocation(location.getType())) {
            BigDecimal currentUsed = location.getUsedCapacity() == null ? BigDecimal.ZERO : location.getUsedCapacity();
            BigDecimal newUsed = currentUsed.add(quantity);
            int updated = locationRepository.forceUpdateUsedCapacity(locationId, newUsed);

            if (updated == 0) {
                throw new ConflictException("Location capacity exceeded or invalid", ErrorCode.LOC_002);
            }

            return updated;
        }

        int updated = locationRepository.increaseUsedCapacity(locationId, quantity);

        if (updated == 0) {
            throw new ConflictException("Location capacity exceeded or invalid",ErrorCode.LOC_002);
        }

        return updated;
    }

    @Override
    @Transactional
    public int decreaseUsedCapacity(String locationId, BigDecimal quantity) {
        int updated = locationRepository.decreaseUsedCapacity(locationId, quantity);

        if (updated == 0) {
            throw new ConflictException("Location used capacity is insufficient or invalid", ErrorCode.LOC_002);
        }

        return updated;
    }

    private boolean isTransitLocation(LocationType type) {
        return type == LocationType.PICKING
                || type == LocationType.PACKING
                || type == LocationType.STAGING;
    }

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Finds a location by ID or throws exception.
     *
     * @param id the location ID
     * @return the location entity
     */
    private Locations findLocationById(String id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Location not found: id={}", id);
                    return new BadRequestException(ErrorCode.LOC_001);
                });
    }

    /**
     * Validates pagination parameters.
     *
     * @param page the page number
     * @param size the page size
     */
    private void validatePaginationParams(Integer page, Integer size) {
        if (page < 0) {
            log.warn("Invalid page number: {}", page);
            throw new BadRequestException(ErrorCode.COM_003);
        }
        if (size <= 0 || size > 100) {
            log.warn("Invalid page size: {}", size);
            throw new BadRequestException(ErrorCode.COM_003);
        }
    }

    /**
     * Validates that a location has no active inventory.
     *
     * @param locationId the location ID
     */
    private void validateNoActiveInventory(String locationId) {
        if (inventoryRepository.existsActiveInventoryByLocationId(locationId)) {
            log.warn("Location has active inventory: locationId={}", locationId);
            throw new BadRequestException(ErrorCode.LOC_006);
        }
    }

    /**
     * Validates location status transition.
     *
     * @param oldStatus the current status
     * @param newStatus the new status
     */
    private void validateStatusTransition(LocationStatus oldStatus, LocationStatus newStatus) {
        // Same status - no need to change
        if (oldStatus == newStatus) {
            log.warn("Status is already {}", newStatus);
            throw new BadRequestException(ErrorCode.LOC_005);
        }
        log.debug("Status transition validated: {} -> {}", oldStatus, newStatus);
    }

    /**
     * Sets audit fields for create operation.
     *
     * @param location the location entity
     * @param user     the current user
     */
    private void setAuditFieldsForCreate(Locations location, Account user) {
        String userId = user.getId();
        LocalDateTime now = LocalDateTime.now();

        location.setCreatedBy(userId);
        location.setUpdatedBy(userId);
        location.setCreatedAt(now);
        location.setUpdatedAt(now);
    }

    /**
     * Sets audit fields for update operation.
     *
     * @param location the location entity
     * @param user     the current user
     */
    private void setAuditFieldsForUpdate(Locations location, Account user) {
        location.setUpdatedBy(user.getId());
        location.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Gets the current authenticated user.
     *
     * @return the current user account
     */
    private Account getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("No authenticated user found with username: {}", username);
                    return new BadRequestException(ErrorCode.AUTH_002);
                });
    }
}
