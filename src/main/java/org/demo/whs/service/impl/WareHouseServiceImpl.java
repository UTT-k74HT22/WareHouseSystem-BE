package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.ChangeStatusRequest;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.WareHouseMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.WareHouseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of the WareHouseService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WareHouseServiceImpl implements WareHouseService {

    private final WareHouseRepository wareHouseRepository;
    private final AccountRepository accountRepository;
    private final WareHouseMapper wareHouseMapper;

    /**
     * Creates a new warehouse based on the provided request.
     *
     * @param request the request containing warehouse details
     * @return the response containing created warehouse information
     */
    @Override
    @Transactional
    public WareHouseResponse createWH(CreateWarehouseRequest request) {
        log.info("Creating warehouse with code={}, name={}",
                request.getCode(), request.getName());
        if (wareHouseRepository.existsByCode(request.getCode())) {
            log.warn("Warehouse code already exists: {}", request.getCode());
            throw new BadRequestException(ErrorCode.WH_004);
        }
        Warehouses warehouses = wareHouseMapper.toEntity(request);
        Account account = getCurrentUser();
        setAuditField(warehouses, account);
        wareHouseRepository.save(warehouses);
        return wareHouseMapper.toResponse(warehouses);
    }

    private static void setAuditField(Warehouses warehouses, Account account) {
        warehouses.setCreatedBy(account.getId());
        warehouses.setUpdatedBy(account.getId());
        warehouses.setCreatedAt(LocalDateTime.now());
        warehouses.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Retrieves a paginated list of all warehouses.
     *
     * @param page the page number to retrieve
     * @param size the number of items per page
     * @return a paginated response containing warehouse information
     */
    @Override
    public PageResponse<WareHouseResponse> getAll(Integer page, Integer size) {
        log.info("Retrieving all warehouses - page: {}, size: {}", page, size);
        Page<Warehouses> warehousePage = wareHouseRepository.findAll(PageRequest.of(page, size));
        Page<WareHouseResponse> responsePage = warehousePage.map(wareHouseMapper::toResponse);
        return PageResponse.from(responsePage);
    }

    /**
     * Retrieves a warehouse by its unique identifier.
     *
     * @param id the unique identifier of the warehouse
     * @return the response containing warehouse information
     */
    @Override
    public WareHouseResponse getWareHouseById(String id) {
        log.info("Retrieving warehouse with id={}", id);
        Warehouses warehouses = wareHouseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Warehouse not found with id={}", id);
                    return new BadRequestException(ErrorCode.WH_001);
                });
        return wareHouseMapper.toResponse(warehouses);
    }

    /**
     * Retrieves a list of all warehouses.
     *
     * @return list of warehouse responses
     */
    @Override
    public List<WareHouseResponse> getWareHouses() {
        log.info("Get all warehouses");
        List<Warehouses> warehouses = wareHouseRepository.findAll();
        return wareHouseMapper.toResponses(warehouses);
    }

    /**
     * Updates an existing warehouse.
     *
     * @param id      the unique identifier of the warehouse to update
     * @param request the request containing updated warehouse details
     * @return the response containing updated warehouse information
     */
    @Override
    @Transactional
    public WareHouseResponse updateWareHouse(String id, UpdateWarehouseRequest request) {
        log.info("Updating warehouse with id={}", id);

        // Find the warehouse
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Warehouse not found with id={}", id);
                    return new BadRequestException(ErrorCode.WH_001);
                });

        // Update the warehouse fields
        wareHouseMapper.updateEntity(warehouse, request);

        // Update metadata
        Account account = getCurrentUser();
        warehouse.setUpdatedBy(account.getId());
        warehouse.setUpdatedAt(LocalDateTime.now());

        // Save and return
        wareHouseRepository.save(warehouse);
        log.info("Warehouse updated successfully with id={}", id);

        return wareHouseMapper.toResponse(warehouse);
    }

    /**
     * Changes the status of a warehouse.
     *
     * @param id      the unique identifier of the warehouse
     * @param request the request containing the new status
     * @return the response containing updated warehouse information
     */
    @Override
    @Transactional
    public WareHouseResponse changeStatus(String id, ChangeStatusRequest request) {
        log.info("Changing status for warehouse with id={} to status={}", id, request.getStatus());

        // Find the warehouse
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Warehouse not found with id={}", id);
                    return new BadRequestException(ErrorCode.WH_001);
                });
        if (warehouse.getStatus() == request.getStatus()) {
            log.info("Warehouse status is already {}, no update needed", request.getStatus());
            return wareHouseMapper.toResponse(warehouse);
        }
        // Update status
        warehouse.setStatus(request.getStatus());
        // Update metadata
        Account account = getCurrentUser();
        warehouse.setUpdatedBy(account.getId());
        warehouse.setUpdatedAt(LocalDateTime.now());
        // Save and return
        wareHouseRepository.save(warehouse);
        log.info("Warehouse status changed successfully for id={}", id);
        return wareHouseMapper.toResponse(warehouse);
    }

    private Account getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_002));
    }
}
