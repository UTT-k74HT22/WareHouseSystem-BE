package org.demo.whs.mapper;


import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.UpdateBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class responsible for converting between
 * BusinessPartner entities and DTOs.
 */
@Component
public class BusinessPartnerMapper {

    /**
     * Convert creation request to entity.
     *
     * @param request {@link BusinessPartnerRequest}
     * @return {@link BusinessPartners} entity
     */
    public BusinessPartners toEntity(BusinessPartnerRequest request) {
        if (request == null) return null;

        BusinessPartners bp = new BusinessPartners();
        bp.setName(request.getName());
        bp.setType(parseType(request.getType()));
        bp.setContactPerson(request.getContactPerson());
        bp.setEmail(request.getEmail());
        bp.setPhone(request.getPhone());
        bp.setAddress(request.getAddress());
        bp.setCity(request.getCity());
        bp.setCountry(request.getCountry());
        bp.setTaxId(request.getTaxId());
        bp.setPaymentTerms(request.getPaymentTerms());
        bp.setCreditLimit(request.getCreditLimit());
        bp.setStatus(parseStatus(request.getStatus()));
        bp.setNotes(request.getNotes());

        return bp;
    }

    /**
     * Convert entity list to response list.
     *
     * @param entities list of {@link BusinessPartners}
     * @return list of {@link BusinessPartnerResponse}
     */
    public List<BusinessPartnerResponse> toResponseList(List<BusinessPartners> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to response DTO.
     *
     * @param entity {@link BusinessPartners}
     * @return {@link BusinessPartnerResponse}
     */
    public BusinessPartnerResponse toResponse(BusinessPartners entity) {
        if (entity == null) return null;

        return BusinessPartnerResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .type(entity.getType().name())
                .status(entity.getStatus().name())
                .contactPerson(entity.getContactPerson())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .address(entity.getAddress())
                .city(entity.getCity())
                .country(entity.getCountry())
                .taxId(entity.getTaxId())
                .paymentTerms(entity.getPaymentTerms())
                .creditLimit(entity.getCreditLimit())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .purchaseOrderCount(0)
                .salesOrderCount(0)
                .build();
    }

    /**
     * Update entity fields using update request.
     * <p>
     * Only non-null fields will be updated.
     *
     * @param entity  existing entity
     * @param request update request
     */
    public void updateEntity(BusinessPartners entity,
                             UpdateBusinessPartnerRequest request) {

        if (entity == null || request == null) return;

        if (request.getName() != null)
            entity.setName(request.getName());

        if (request.getType() != null)
            entity.setType(parseType(request.getType()));

        if (request.getStatus() != null)
            entity.setStatus(parseStatus(request.getStatus()));

        if (request.getContactPerson() != null)
            entity.setContactPerson(request.getContactPerson());

        if (request.getEmail() != null)
            entity.setEmail(request.getEmail());

        if (request.getPhone() != null)
            entity.setPhone(request.getPhone());

        if (request.getAddress() != null)
            entity.setAddress(request.getAddress());

        if (request.getCity() != null)
            entity.setCity(request.getCity());

        if (request.getCountry() != null)
            entity.setCountry(request.getCountry());

        if (request.getTaxId() != null)
            entity.setTaxId(request.getTaxId());

        if (request.getPaymentTerms() != null)
            entity.setPaymentTerms(request.getPaymentTerms());

        if (request.getCreditLimit() != null)
            entity.setCreditLimit(request.getCreditLimit());

        if (request.getNotes() != null)
            entity.setNotes(request.getNotes());
    }

    /* ================= PRIVATE PARSERS ================= */
    private BusinessPartnerType parseType(String type) {
        return BusinessPartnerType.valueOf(type.toUpperCase());
    }

    private BusinessPartnerStatus parseStatus(String status) {
        return BusinessPartnerStatus.valueOf(status.toUpperCase());
    }
}
