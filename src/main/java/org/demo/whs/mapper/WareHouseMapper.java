package org.demo.whs.mapper;

import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class WareHouseMapper {

    /**
     * Converts a CreateWarehouseRequest DTO to a Warehouses entity.
     *
     * @param request the CreateWarehouseRequest containing warehouse details
     * @return a Warehouses entity populated with data from the request
     */
    public Warehouses toEntity(CreateWarehouseRequest request) {
        if (request == null) {
            return null;
        }
        return Warehouses.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .type(request.getWareHouseType())
                .status(request.getStatus())
                .capacity(request.getCapacity())
                .managerId(request.getManagerId())
                .build();
    }

    public WareHouseResponse toResponse(Warehouses warehouses, AccountResponse accountResponse) {
        if (warehouses == null) {
            return null;
        }

        return WareHouseResponse.builder()
                .id(warehouses.getId())
                .code(warehouses.getCode())
                .name(warehouses.getName())
                .address(warehouses.getAddress())
                .phone(warehouses.getPhone())
                .email(warehouses.getEmail())
                .status(warehouses.getStatus())
                .wareHouseType(warehouses.getType())
                .managerId(warehouses.getManagerId())
                .manager(accountResponse)
                .build();
    }

    /**
     * Updates an existing warehouse entity with data from UpdateWarehouseRequest.
     * Only updates fields that are not null in the request.
     *
     * @param warehouse the existing warehouse entity to update
     * @param request   the update request containing new values
     */
    public void updateEntity(Warehouses warehouse, UpdateWarehouseRequest request) {
        if (warehouse == null || request == null) {
            return;
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            warehouse.setName(request.getName().trim());
        }

        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            warehouse.setAddress(request.getAddress().trim());
        }

        if (request.getCity() != null && !request.getCity().isBlank()) {
            warehouse.setCity(request.getCity().trim());
        }

        if (request.getState() != null && !request.getState().isBlank()) {
            warehouse.setState(request.getState().trim());
        }

        if (request.getCountry() != null && !request.getCountry().isBlank()) {
            warehouse.setCountry(request.getCountry().trim());
        }

        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
            warehouse.setPostalCode(request.getPostalCode().trim());
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            warehouse.setPhone(request.getPhone().trim());
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            warehouse.setEmail(request.getEmail().trim());
        }

        if (request.getWareHouseType() != null) {
            warehouse.setType(request.getWareHouseType());
        }

        if (request.getCapacity() != null) {
            warehouse.setCapacity(request.getCapacity());
        }
    }

    /**
     * Converts a list of Warehouses entities to a list of WareHouseResponse DTOs.
     *
     * @param warehouses  the list of Warehouses entities
     * @param managerMap  a map of AccountResponse DTOs keyed by manager ID
     * @return the list of WareHouseResponse DTOs
     */
    public List<WareHouseResponse> toResponses(List<Warehouses> warehouses, Map<String, AccountResponse> managerMap) {
        if (warehouses == null) return Collections.emptyList();

        return warehouses.stream()
                .map(w -> toResponse(w, w.getManagerId() != null ? managerMap.get(w.getManagerId()) : null))
                .toList();
    }
}
