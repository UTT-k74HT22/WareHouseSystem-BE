package org.demo.whs.service.impl;

import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.request.BusinessPartner.BusinessPartnerRequest;
import org.demo.whs.entity.dto.request.BusinessPartner.SearchBusinessPartnerRequest;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.BusinessPartnerStatus;
import org.demo.whs.entity.enums.BusinessPartnerType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.BusinessPartnerMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessPartnerImpl Unit Tests")
class BusinessPartnerImplTest {

    @Mock
    private BusinessPartnersRepository repository;

    @Mock
    private BusinessPartnerMapper mapper;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private BusinessPartnerImpl service;

    @Test
    @DisplayName("should_ThrowNotFoundException_When_GetByIdWithUnknownId")
    void should_ThrowNotFoundException_When_GetByIdWithUnknownId() {
        when(repository.findById("bp-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("bp-unknown"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BP_001");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_CreateRequestProvidesCode")
    void should_ThrowBadRequest_When_CreateRequestProvidesCode() {
        BusinessPartnerRequest request = BusinessPartnerRequest.builder()
                .code("BP-001")
                .name("Partner A")
                .type("SUPPLIER")
                .status("ACTIVE")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

        verify(repository, never()).save(any(BusinessPartners.class));
    }

    @Test
    @DisplayName("should_GenerateBusinessPartnerCode_When_CreateWithoutCode")
    void should_GenerateBusinessPartnerCode_When_CreateWithoutCode() {
        BusinessPartnerRequest request = BusinessPartnerRequest.builder()
                .name("Partner A")
                .type("SUPPLIER")
                .status("ACTIVE")
                .build();

        BusinessPartners entity = new BusinessPartners();
        entity.setName("Partner A");
        entity.setStatus(BusinessPartnerStatus.ACTIVE);

        when(mapper.toEntity(request)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenAnswer(invocation ->
                BusinessPartnerResponse.builder()
                        .code(entity.getCode())
                        .name("Partner A")
                        .status("ACTIVE")
                        .type("SUPPLIER")
                        .build()
        );

        BusinessPartnerResponse response = service.create(request);

        assertThat(response.getCode()).startsWith("SUP-");
        assertThat(response.getCode()).hasSizeLessThanOrEqualTo(20);
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("should_ThrowBadRequestException_When_ChangeStatusWithInvalidStatus")
    void should_ThrowBadRequestException_When_ChangeStatusWithInvalidStatus() {
        BusinessPartners entity = new BusinessPartners();
        entity.setId("bp-001");
        entity.setCode("BP-001");
        entity.setStatus(BusinessPartnerStatus.ACTIVE);

        when(repository.findById("bp-001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.changeStatus("bp-001", "NOT_A_STATUS"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BP_003");

        verify(repository, never()).save(any(BusinessPartners.class));
    }

    @Test
    @DisplayName("should_ApplyTransactionalBoundaries_When_ServiceMethodsInvoked")
    void should_ApplyTransactionalBoundaries_When_ServiceMethodsInvoked() throws NoSuchMethodException {
        Method getAllMethod = BusinessPartnerImpl.class.getMethod("getAll");
        Method createMethod = BusinessPartnerImpl.class.getMethod("create", BusinessPartnerRequest.class);

        Transactional readOnlyTx = getAllMethod.getAnnotation(Transactional.class);
        Transactional writeTx = createMethod.getAnnotation(Transactional.class);

        assertThat(readOnlyTx).isNotNull();
        assertThat(readOnlyTx.readOnly()).isTrue();
        assertThat(writeTx).isNotNull();
        assertThat(writeTx.readOnly()).isFalse();
    }

    @Test
    void should_ReturnPageResponse_When_SearchWithoutFilter() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();

        BusinessPartners entity = new BusinessPartners();
        entity.setCode("BP-001");

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);

        when(repository.search(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(page);

        when(mapper.toResponse(entity))
                .thenReturn(new BusinessPartnerResponse());

        PageResponse<BusinessPartnerResponse> result =
                service.searchBusinessPartners(request, 0, 10);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("should_SearchByCode")
    void should_SearchByCode() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();
        request.setCode("BP");

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(new BusinessPartners()));

        when(repository.search(eq("BP"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        when(mapper.toResponse(any())).thenReturn(new BusinessPartnerResponse());

        PageResponse<BusinessPartnerResponse> result =
                service.searchBusinessPartners(request, 0, 10);

        assertThat(result.getContent()).hasSize(1);

        verify(repository).search(eq("BP"), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("should_NormalizeSupplierType_When_Search")
    void should_NormalizeSupplierType_When_Search() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();
        request.setType(BusinessPartnerType.SUPPLIER);

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(new BusinessPartners()));

        when(repository.search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.SUPPLIER, BusinessPartnerType.BOTH)),
                any(Pageable.class)
        )).thenReturn(page);

        when(mapper.toResponse(any())).thenReturn(new BusinessPartnerResponse());

        service.searchBusinessPartners(request, 0, 10);

        verify(repository).search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.SUPPLIER, BusinessPartnerType.BOTH)),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("should_NormalizeCustomerType_When_Search")
    void should_NormalizeCustomerType_When_Search() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();
        request.setType(BusinessPartnerType.CUSTOMER);

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(new BusinessPartners()));

        when(repository.search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.CUSTOMER, BusinessPartnerType.BOTH)),
                any(Pageable.class)
        )).thenReturn(page);

        when(mapper.toResponse(any())).thenReturn(new BusinessPartnerResponse());

        service.searchBusinessPartners(request, 0, 10);

        verify(repository).search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.CUSTOMER, BusinessPartnerType.BOTH)),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("should_SearchOnlyBothType_When_TypeIsBoth")
    void should_SearchOnlyBothType_When_TypeIsBoth() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();
        request.setType(BusinessPartnerType.BOTH);

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(new BusinessPartners()));

        when(repository.search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.BOTH)),
                any(Pageable.class)
        )).thenReturn(page);

        when(mapper.toResponse(any())).thenReturn(new BusinessPartnerResponse());

        service.searchBusinessPartners(request, 0, 10);

        verify(repository).search(
                any(),
                any(),
                any(),
                eq(List.of(BusinessPartnerType.BOTH)),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("should_ReturnEmptyPage_When_NoResultFound")
    void should_ReturnEmptyPage_When_NoResultFound() {
        SearchBusinessPartnerRequest request = new SearchBusinessPartnerRequest();

        Page<BusinessPartners> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(repository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<BusinessPartnerResponse> result =
                service.searchBusinessPartners(request, 0, 10);

        assertThat(result.getContent()).isEmpty();
    }
}
