package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.ChangeStatusRequest;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.WareHouseMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.WareHouseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of the WareHouseService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WareHouseServiceImpl implements WareHouseService {

    private final WareHouseRepository wareHouseRepository;
    private final AccountRepository accountRepository;
    private final UserProfileRepository userProfileRepository;
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

        AccountResponse manager = fetchSingleManager(warehouses.getManagerId());
        return wareHouseMapper.toResponse(warehouses, manager);
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
        List<Warehouses> content = warehousePage.getContent();
        Map<String, AccountResponse> managerMap = fetchManagerMap(content);
        List<WareHouseResponse> responses = wareHouseMapper.toResponses(content, managerMap);
        return PageResponse.from(warehousePage, responses);
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
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.WH_001));

        AccountResponse manager = fetchSingleManager(warehouse.getManagerId());
        return wareHouseMapper.toResponse(warehouse, manager);
    }

    /**
     * Retrieves a list of all warehouses.
     *
     * @return list of warehouse responses
     */
    @Override
    public List<WareHouseResponse> getWareHouses() {
        log.info("Retrieving all warehouses");
        List<Warehouses> warehouses = wareHouseRepository.findAll();
        Map<String, AccountResponse> managerMap = fetchManagerMap(warehouses);
        return wareHouseMapper.toResponses(warehouses, managerMap);
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
        log.info("Updating warehouse with id={}, name={}", id, request.getName());
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.WH_001));

        wareHouseMapper.updateEntity(warehouse, request);

        Account currentUser = getCurrentUser();
        warehouse.setUpdatedBy(currentUser.getId());
        warehouse.setUpdatedAt(LocalDateTime.now());
        wareHouseRepository.save(warehouse);
        AccountResponse manager = fetchSingleManager(warehouse.getManagerId());
        return wareHouseMapper.toResponse(warehouse, manager);
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
        log.info("Changing status of warehouse with code={}, name={}", id, request.getStatus());
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.WH_001));

        warehouse.setStatus(request.getStatus());
        warehouse.setUpdatedBy(getCurrentUser().getId());
        warehouse.setUpdatedAt(LocalDateTime.now());

        wareHouseRepository.save(warehouse);

        AccountResponse manager = fetchSingleManager(warehouse.getManagerId());
        return wareHouseMapper.toResponse(warehouse, manager);
    }

    private Account getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_002));
    }

    private AccountResponse fetchSingleManager(String managerId) {
        if (managerId == null || managerId.isBlank()) return null;
        List<AccountResponse> managers = userProfileRepository.getAccountsByIds(List.of(managerId));
        return managers.isEmpty() ? null : managers.get(0);
    }

    private Map<String, AccountResponse> fetchManagerMap(List<Warehouses> warehouses) {
        List<String> managerIds = warehouses.stream()
                .map(Warehouses::getManagerId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (managerIds.isEmpty()) return Map.of();

        return userProfileRepository.getAccountsByIds(managerIds).stream()
                .collect(Collectors.toMap(AccountResponse::getAccountId, m -> m));
    }
}
