package org.demo.whs.mapper;

import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.request.Location.UpdateLocationRequest;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Location entity and DTOs.
 */
@Component
public class LocationMapper {

    /**
     * Converts CreateLocationRequest to Location entity.
     *
     * @param request the create location request
     * @return the Location entity
     */
    public Locations toEntity(CreateLocationRequest request) {
        if (request == null) {
            return null;
        }

        return Locations.builder()
                .warehouseId(request.getWarehouseId())
                .name(request.getName())
                .zone(request.getZone())
                .type(request.getType())
                .capacity(request.getCapacity())
                .status(request.getStatus() != null ? request.getStatus() : LocationStatus.ACTIVE)
                .notes(request.getNotes())
                .build();
    }

    /**
     * Updates existing Location entity from UpdateLocationRequest.
     * Only updates non-null fields from the request.
     *
     * @param location the existing location entity
     * @param request  the update location request
     */
    public void updateEntity(Locations location, UpdateLocationRequest request) {
        if (location == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            location.setName(request.getName());
        }
        if (request.getZone() != null) {
            location.setZone(request.getZone());
        }
        if (request.getType() != null) {
            location.setType(request.getType());
        }
        if (request.getCapacity() != null) {
            location.setCapacity(request.getCapacity());
        }
        if (request.getNotes() != null) {
            location.setNotes(request.getNotes());
        }
    }

    /**
     * Converts Location entity to LocationResponse DTO.
     *
     * @param location the location entity
     * @return the LocationResponse DTO
     */
    public LocationResponse toResponse(Locations location) {
        if (location == null) {
            return null;
        }

        return LocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouseId())
                .code(location.getCode())
                .name(location.getName())
                .zone(location.getZone())
                .type(location.getType())
                .capacity(location.getCapacity())
                .status(location.getStatus())
                .notes(location.getNotes())
                .createdBy(location.getCreatedBy())
                .createdAt(location.getCreatedAt())
                .updatedBy(location.getUpdatedBy())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    /**
     * Converts Location entity to LocationResponse DTO with warehouse details.
     *
     * @param location  the location entity
     * @param warehouse the warehouse entity
     * @return the LocationResponse DTO with warehouse info
     */
    public LocationResponse toResponseWithWarehouse(Locations location, Warehouses warehouse) {
        if (location == null) {
            return null;
        }

        LocationResponse.LocationResponseBuilder builder = LocationResponse.builder()
                .id(location.getId())
                .warehouseId(location.getWarehouseId())
                .code(location.getCode())
                .name(location.getName())
                .zone(location.getZone())
                .type(location.getType())
                .capacity(location.getCapacity())
                .status(location.getStatus())
                .notes(location.getNotes())
                .createdBy(location.getCreatedBy())
                .createdAt(location.getCreatedAt())
                .updatedBy(location.getUpdatedBy())
                .updatedAt(location.getUpdatedAt());

        if (warehouse != null) {
            builder.warehouseCode(warehouse.getCode())
                   .warehouseName(warehouse.getName());
        }

        return builder.build();
    }
}
