package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.SearchBusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.BusinessPartnerMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.service.BusinessPartnerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Pageable;
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public BusinessPartnerResponse getById(String id) {
        log.info("[SERVICE][GET_BY_ID] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][GET_BY_ID] Not found, id={}", id);
                    return new NotFoundException(ErrorCode.BP_001);
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
    @Transactional
    public BusinessPartnerResponse create(BusinessPartnerRequest request) {
        log.info("[SERVICE][CREATE] Start, code={}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            log.warn("[SERVICE][CREATE] Code already exists, code={}", request.getCode());
            throw new ConflictException(ErrorCode.BP_002);
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
    @Transactional
    public BusinessPartnerResponse update(String id, UpdateBusinessPartnerRequest request) {
        log.info("[SERVICE][UPDATE] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][UPDATE] Not found, id={}", id);
                    return new NotFoundException(ErrorCode.BP_001);
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
    @Transactional
    public void delete(String id) {
        log.info("[SERVICE][DELETE] Start, id={}", id);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][DELETE] Not found, id={}", id);
                    return new NotFoundException(ErrorCode.BP_001);
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
    @Transactional
    public BusinessPartnerResponse changeStatus(String id, String status) {
        log.info("[SERVICE][CHANGE_STATUS] Start, id={}, status={}", id, status);

        BusinessPartners entity = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("[SERVICE][CHANGE_STATUS] Not found, id={}", id);
                    return new NotFoundException(ErrorCode.BP_001);
                });

        try {
            entity.setStatus(
                    BusinessPartnerStatus.valueOf(status.toUpperCase())
            );
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("[SERVICE][CHANGE_STATUS] Invalid status={}, id={}", status, id);
            throw new BadRequestException(ErrorCode.BP_003);
        }

        repository.save(entity);

        log.info("[SERVICE][CHANGE_STATUS] Success, id={}, newStatus={}",
                entity.getId(), entity.getStatus());

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BusinessPartnerResponse> searchBusinessPartners(
            SearchBusinessPartnerRequest request,
            Integer page,
            Integer size
    ) {
        log.info("Searching business partners = page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<BusinessPartnerType> types = normalizeType(request.getType());

        Page<BusinessPartners> partnerPage = repository.search(
                request.getCode(),
                request.getName(),
                request.getStatus(),
                types,
                pageable
        );

        return buildPageResponse(partnerPage);
    }

    private List<BusinessPartnerType> normalizeType(BusinessPartnerType type) {

        if (type == null) return null;

        return switch (type) {

            case SUPPLIER -> List.of(
                    BusinessPartnerType.SUPPLIER,
                    BusinessPartnerType.BOTH
            );

            case CUSTOMER -> List.of(
                    BusinessPartnerType.CUSTOMER,
                    BusinessPartnerType.BOTH
            );

            case BOTH -> List.of(BusinessPartnerType.BOTH);
        };
    }

    private PageResponse<BusinessPartnerResponse> buildPageResponse(
            Page<BusinessPartners> partnerPage
    ) {

        List<BusinessPartners> partners = partnerPage.getContent();

        List<BusinessPartnerResponse> responses =
                partners.stream()
                        .map(mapper::toResponse)
                        .toList();

        return PageResponse.from(partnerPage, responses);
    }

}
