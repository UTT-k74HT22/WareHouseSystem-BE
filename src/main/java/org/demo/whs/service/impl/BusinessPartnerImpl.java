package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.BusinessPartnerMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.service.BusinessPartnerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Implementation of the BusinessPartnerService interface.
 */
/**
 * Service implementation for managing Business Partners.
 * <p>
 * Handles CRUD operations, status management, and business rules
 * related to {@link BusinessPartners}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BusinessPartnerImpl implements BusinessPartnerService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "code", "name", "type", "status", "createdAt", "updatedAt"
    );

    private final BusinessPartnersRepository repository;
    private final BusinessPartnerMapper mapper;

    /**
     * Retrieve all business partners.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessPartnerResponse> getAll(Integer page, Integer size, String sortBy, String sortDir) {
        int targetPage = page == null ? 0 : page;
        int targetSize = size == null ? 10 : size;

        if (targetPage < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (targetSize <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }
        if (targetSize > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }

        String sortField = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(targetPage, targetSize, Sort.by(direction, sortField));

        log.info("[SERVICE][GET_ALL] Fetching business partners page={}, size={}, sortField={}, direction={}",
                targetPage, targetSize, sortField, direction);

        Page<BusinessPartners> entityPage = repository.findAll(pageRequest);

        return PageResponse.from(entityPage, mapper.toResponseList(entityPage.getContent()));
    }

    /**
     * Retrieve a business partner by its identifier.
     */
    @Override
    public BusinessPartnerResponse getById(String id) {
        log.info("[SERVICE][GET_BY_ID] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][GET_BY_ID] Not found, id={}", id);
                    return new RuntimeException(ErrorCode.BP_001.getCode());
                });

//        if (entity.getStatus() == BusinessPartnerStatus.INACTIVE) {
//            log.warn("[SERVICE][GET_BY_ID] Inactive business partner, id={}", id);
//            throw new RuntimeException(ErrorCode.BP_004.getCode());
//        }

        log.info("[SERVICE][GET_BY_ID] Success, id={}, code={}",
                entity.getId(), entity.getCode());

        return mapper.toResponse(entity);
    }

    /**
     * Create a new business partner.
     */
    @Override
    public BusinessPartnerResponse create(BusinessPartnerRequest request) {
        log.info("[SERVICE][CREATE] Start, code={}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            log.warn("[SERVICE][CREATE] Code already exists, code={}", request.getCode());
            throw new RuntimeException(ErrorCode.BP_002.getCode());
        }

        BusinessPartners entity = mapper.toEntity(request);
        repository.save(entity);

        log.info("[SERVICE][CREATE] Success, id={}, code={}",
                entity.getId(), entity.getCode());

        return mapper.toResponse(entity);
    }

    /**
     * Update an existing business partner.
     */
    @Override
    public BusinessPartnerResponse update(String id, UpdateBusinessPartnerRequest request) {
        log.info("[SERVICE][UPDATE] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][UPDATE] Not found, id={}", id);
                    return new RuntimeException(ErrorCode.BP_001.getCode());
                });

        mapper.updateEntity(entity, request);
        repository.save(entity);

        log.info("[SERVICE][UPDATE] Success, id={}, code={}",
                entity.getId(), entity.getCode());

        return mapper.toResponse(entity);
    }

    /**
     * Soft delete a business partner.
     */
    @Override
    public void delete(String id) {
        log.info("[SERVICE][DELETE] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][DELETE] Not found, id={}", id);
                    return new RuntimeException(ErrorCode.BP_001.getCode());
                });

        entity.setStatus(BusinessPartnerStatus.INACTIVE);
        repository.save(entity);

        log.info("[SERVICE][DELETE] Success (soft delete), id={}, code={}",
                entity.getId(), entity.getCode());
    }

    /**
     * Change the status of a business partner.
     */
    @Override
    public BusinessPartnerResponse changeStatus(String id, String status) {
        log.info("[SERVICE][CHANGE_STATUS] Start, id={}, status={}", id, status);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][CHANGE_STATUS] Not found, id={}", id);
                    return new RuntimeException(ErrorCode.BP_001.getCode());
                });

        try {
            entity.setStatus(
                    BusinessPartnerStatus.valueOf(status.toUpperCase())
            );
        } catch (IllegalArgumentException ex) {
            log.warn("[SERVICE][CHANGE_STATUS] Invalid status={}, id={}", status, id);
            throw new RuntimeException(ErrorCode.BP_003.getCode());
        }

        repository.save(entity);

        log.info("[SERVICE][CHANGE_STATUS] Success, id={}, newStatus={}",
                entity.getId(), entity.getStatus());

        return mapper.toResponse(entity);
    }

}
