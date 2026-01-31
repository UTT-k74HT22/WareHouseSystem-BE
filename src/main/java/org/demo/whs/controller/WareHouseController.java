package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.service.WareHouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/warehouse")
@RestController
@RequiredArgsConstructor
@Slf4j
public class WareHouseController {

    public final WareHouseService wareHouseService;

    @GetMapping
    public ResponseEntity<String> getWareHouse() {
        log.info("Fetching warehouses");
        // Placeholder response
        return ResponseEntity.ok("List of warehouses");
    }

    /**
     * Endpoint to create a new warehouse.
     *
     * @param request the request containing warehouse details
     * @return the response containing created warehouse information
     */
    @PostMapping
    public ResponseEntity<BaseResponse<WareHouseResponse>> create(@RequestBody @Valid CreateWarehouseRequest request) {
        log.info("Received request to create warehouse: {}", request);
        WareHouseResponse response = wareHouseService.createWH(request);
        BaseResponse<WareHouseResponse> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }
}
