package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.request.WareHouse.UpdateWarehouseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.entity.enums.WareHouseType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.WareHouseMapper;
import org.demo.whs.repository.*;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.WareHouseService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
    private final LocationRepository locationRepository;
    private final WareHouseMapper wareHouseMapper;
    private final InventoryRepository inventoryRepository;
    private final IdentifierGenerator identifierGenerator;

    /**
     * Creates a new warehouse based on the provided request.
     *
     * @param request the request containing warehouse details
     * @return the response containing created warehouse information
     */
    @Override
    @Transactional
    public WareHouseResponse createWH(CreateWarehouseRequest request) {
        log.info("Create Warehouse request: {}", request);
        String code = identifierGenerator.generateSystemManaged(
                request.getCode(),
                "Warehouse code",
                "WH",
                20,
                wareHouseRepository::existsByCode
        );

        String managerId = normalizeOptionalId(request.getManagerId());
        if (managerId == null) {
            throw new BadRequestException(ErrorCode.AUTH_002);
        }

        if (!accountRepository.existsById(managerId)) {
            throw new NotFoundException(ErrorCode.AUTH_002);
        }

        log.info("Creating warehouse with code={}, name={}", code, request.getName());

        Warehouses warehouses = wareHouseMapper.toEntity(request);
        warehouses.setCode(code);
        warehouses.setManagerId(managerId);

        Account account = getCurrentUser();
        setAuditField(warehouses, account);

        wareHouseRepository.save(warehouses);

        AccountResponse manager = fetchSingleManager(managerId);
        return wareHouseMapper.toResponse(warehouses, manager);
    }

    private String normalizeOptionalId(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()
                || "undefined".equalsIgnoreCase(trimmed)
                || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private static void setAuditField(Warehouses warehouses, Account account) {
        warehouses.setCreatedBy(account.getId());
        warehouses.setUpdatedBy(account.getId());
        warehouses.setCreatedAt(LocalDateTime.now());
        warehouses.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Retrieves a paginated list of all warehouses (legacy path, no filters).
     */
    @Override
    public PageResponse<WareHouseResponse> getAll(Integer page, Integer size) {
        log.info("Retrieving all warehouses - page: {}, size: {}", page, size);
        if (page == null || page < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (size == null || size <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }
        if (size > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }
        Page<Warehouses> warehousePage = wareHouseRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        List<Warehouses> content = warehousePage.getContent();
        Map<String, AccountResponse> managerMap = fetchManagerMap(content);
        List<WareHouseResponse> responses = wareHouseMapper.toResponses(content, managerMap);
        return PageResponse.from(warehousePage, responses);
    }

    /**
     * Retrieves warehouses with pagination and optional filters.
     * No filter -> uses the legacy path to keep original behavior.
     */
    @Override
    public PageResponse<WareHouseResponse> getWarehouses(Integer page, Integer size, String keyword, WareHouseStatus status, WareHouseType type) {
        // Không có filter mở rộng -> dùng đường cũ để giữ nguyên hành vi.
        if ((keyword == null || keyword.isBlank()) && status == null && type == null) {
            return getAll(page, size);
        }
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = size == null ? 10 : Math.min(Math.max(size, 1), 100);
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        log.info("Searching warehouses - page: {}, size: {}, keyword: {}, status: {}, type: {}",
                safePage, safeSize, safeKeyword, status, type);
        Page<Warehouses> warehousePage = wareHouseRepository.searchWarehouses(
                safeKeyword, status, type,
                PageRequest.of(safePage, safeSize, Sort.by("createdAt").descending()));
        List<WareHouseResponse> responses = wareHouseMapper.toResponses(
                warehousePage.getContent(), fetchManagerMap(warehousePage.getContent()));
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
                .orElseThrow(() -> new BadRequestException(ErrorCode.WHS_001));

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

    @Override
    public Map<String, Long> getStats() {
        long active = wareHouseRepository.countByStatus(WareHouseStatus.ACTIVE);
        long inactive = wareHouseRepository.countByStatus(WareHouseStatus.INACTIVE);
        long maintenance = wareHouseRepository.countByStatus(WareHouseStatus.MAINTENANCE);
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", active + inactive + maintenance);
        stats.put("active", active);
        stats.put("inactive", inactive);
        stats.put("maintenance", maintenance);
        return stats;
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
                .orElseThrow(() -> new BadRequestException(ErrorCode.WHS_001));

        wareHouseMapper.updateEntity(warehouse, request);

        if (request.getManagerId() != null) {
            String managerId = normalizeOptionalId(request.getManagerId());
            if (managerId == null) {
                throw new BadRequestException(ErrorCode.AUTH_002);
            }

            if (!accountRepository.existsById(managerId)) {
                throw new NotFoundException(ErrorCode.AUTH_002);
            }

            warehouse.setManagerId(managerId);
        }

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
    public WareHouseResponse changeStatus(String id, UpdateWarehouseRequest request) {
        log.info("Changing status of warehouse with code={}, name={}", id, request.getStatus());
        if (request.getStatus() == null) {
            throw new BadRequestException(ErrorCode.COM_003);
        }
        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.WHS_001));

        if (warehouse.getStatus() == request.getStatus()) {
            throw new BadRequestException(ErrorCode.WHS_003);
        }
        warehouse.setStatus(request.getStatus());
        warehouse.setUpdatedBy(getCurrentUser().getId());
        warehouse.setUpdatedAt(LocalDateTime.now());

        wareHouseRepository.save(warehouse);

        AccountResponse manager = fetchSingleManager(warehouse.getManagerId());
        return wareHouseMapper.toResponse(warehouse, manager);
    }

    @Override
    @Transactional
    public void deleteWarehouse (String id) {
        log.info("Deleting warehouse id ={}", id);

        Warehouses warehouse = wareHouseRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ErrorCode.WH_001));

        if (warehouse.getStatus() == WareHouseStatus.INACTIVE) {
            log.info("Warehouse {} already - skip delete", id);
            throw new BadRequestException(ErrorCode.WH_007);
        }

        long activeLocationCount = locationRepository.countByWarehouseIdAndStatusNot(
                id,
                LocationStatus.INACTIVE
        );

        if (activeLocationCount > 0) {
            log.warn("Warehouse {} has {} active locations", id, activeLocationCount);
            throw  new BadRequestException(ErrorCode.WH_005);
        }

        boolean hasInventory =
                inventoryRepository.existsActiveInventoryByWarehouseId(id);

        if (hasInventory) {
            log.warn("Warehouse {} has inventory", id);
            throw new BadRequestException(ErrorCode.WH_006);
        }

        Account currentUser = getCurrentUser();
        warehouse.setStatus(WareHouseStatus.INACTIVE);
        warehouse.setUpdatedBy(currentUser.getId());
        warehouse.setUpdatedAt(LocalDateTime.now());

        wareHouseRepository.save(warehouse);

        log.info("Warehouse {} delete successfully", id);
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
