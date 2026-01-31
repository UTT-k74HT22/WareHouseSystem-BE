package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.WareHouseMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.WareHouseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

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
        String username = SecurityUtils.getCurrentUsername();
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException(ErrorCode.AUTH_002));
        warehouses.setCreatedBy(account.getId());
        warehouses.setUpdatedBy(account.getId());
        warehouses.setCreatedAt(LocalDateTime.now());
        warehouses.setUpdatedAt(LocalDateTime.now());
        wareHouseRepository.save(warehouses);
        return wareHouseMapper.toResponse(warehouses);
    }
}
