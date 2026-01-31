package org.demo.whs.mapper;

import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.springframework.stereotype.Component;

@Component
public class WareHouseMapper {

    public Warehouses toEntity(CreateWarehouseRequest request) {
        if (request == null) {
            return null;
        }
        return Warehouses.builder()
                .code(request.getCode())
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .type(request.getWareHouseType())
                .status(request.getStatus())
                .managerId(request.getManagerId())
                .build();
    }

    public WareHouseResponse toResponse(Warehouses warehouses) {
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
                .status(warehouses.getStatus().name())
                .wareHouseType(warehouses.getType().name())
                .managerId(warehouses.getManagerId())
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

        if (request.getName() != null) {
            warehouse.setName(request.getName());
        }

        if (request.getAddress() != null) {
            warehouse.setAddress(request.getAddress());
        }

        if (request.getCity() != null) {
            warehouse.setCity(request.getCity());
        }

        if (request.getState() != null) {
            warehouse.setState(request.getState());
        }

        if (request.getCountry() != null) {
            warehouse.setCountry(request.getCountry());
        }

        if (request.getPostalCode() != null) {
            warehouse.setPostalCode(request.getPostalCode());
        }

        if (request.getPhone() != null) {
            warehouse.setPhone(request.getPhone());
        }

        if (request.getEmail() != null) {
            warehouse.setEmail(request.getEmail());
        }

        if (request.getWareHouseType() != null) {
            warehouse.setType(request.getWareHouseType());
        }

        if (request.getCapacity() != null) {
            warehouse.setCapacity(request.getCapacity());
        }

        if (request.getManagerId() != null) {
            warehouse.setManagerId(request.getManagerId());
        }
    }
}
