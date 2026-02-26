package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.BusinessPartnerMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.service.BusinessPartnerService;
import org.springframework.stereotype.Service;

import java.util.List;

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

    private final BusinessPartnersRepository repository;
    private final BusinessPartnerMapper mapper;

    /**
     * Retrieve all business partners.
     */
    @Override
    public List<BusinessPartnerResponse> getAll() {
        log.info("[SERVICE][GET_ALL] Fetching all business partners");

        List<BusinessPartners> entities = repository.findAll();

        log.info("[SERVICE][GET_ALL] Found {} business partners", entities.size());

        return mapper.toResponseList(entities);
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
