package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.WareHouse.ChangeStatusRequest;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.service.WareHouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RequestMapping("/api/v1/warehouse")
@RestController
@RequiredArgsConstructor
@Slf4j
public class WareHouseController {

    public final WareHouseService wareHouseService;

    @GetMapping("/test")
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

    /**
     * Endpoint to retrieve all warehouses with pagination.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing warehouse information
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<WareHouseResponse>>> getAll(@RequestParam(name = "page", defaultValue = "0") Integer page, @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Fetching all warehouses - page: {}, size: {}", page, size);
        PageResponse<WareHouseResponse> response = wareHouseService.getAll(page, size);
        BaseResponse<PageResponse<WareHouseResponse>> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to retrieve all warehouses without pagination.
     *
     * @return list of warehouse responses
     */
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<List<WareHouseResponse>>> getAll() {
        log.info("Fetching all warehouses without pagination");
        List<WareHouseResponse> response = wareHouseService.getWareHouses();
        BaseResponse<List<WareHouseResponse>> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to retrieve a warehouse by its unique identifier.
     *
     * @param id the unique identifier of the warehouse
     * @return the response containing warehouse information
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<WareHouseResponse>> getById(@PathVariable("id") String id) {
        log.info("Fetching warehouse with id: {}", id);
        WareHouseResponse response = wareHouseService.getWareHouseById(id);
        BaseResponse<WareHouseResponse> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to update an existing warehouse.
     *
     * @param id      the unique identifier of the warehouse to update
     * @param request the request containing updated warehouse details
     * @return the response containing updated warehouse information
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<WareHouseResponse>> update(
            @PathVariable("id") String id,
            @RequestBody @Valid UpdateWarehouseRequest request) {
        log.info("Received request to update warehouse with id: {}", id);
        WareHouseResponse response = wareHouseService.updateWareHouse(id, request);
        BaseResponse<WareHouseResponse> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }

    /**
     * Endpoint to change the status of a warehouse.
     *
     * @param id      the unique identifier of the warehouse
     * @param request the request containing the new status
     * @return the response containing updated warehouse information
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<BaseResponse<WareHouseResponse>> changeStatus(
            @PathVariable("id") String id,
            @RequestBody @Valid ChangeStatusRequest request) {
        log.info("Received request to change status for warehouse with id: {} to status: {}",
                id, request.getStatus());
        WareHouseResponse response = wareHouseService.changeStatus(id, request);
        BaseResponse<WareHouseResponse> baseResponse = BaseResponse.success(response);
        return ResponseEntity.ok(baseResponse);
    }
}