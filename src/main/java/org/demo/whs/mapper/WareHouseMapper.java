package org.demo.whs.mapper;

import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
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
}
