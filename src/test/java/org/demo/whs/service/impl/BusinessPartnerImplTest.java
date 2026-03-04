package org.demo.whs.service.impl;

import org.demo.whs.entity.BusinessPartners;
import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.BusinessPartnerMapper;
import org.demo.whs.repository.BusinessPartnersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BusinessPartnerImpl Unit Tests")
class BusinessPartnerImplTest {

    @Mock
    private BusinessPartnersRepository repository;

    @Mock
    private BusinessPartnerMapper mapper;

    @InjectMocks
    private BusinessPartnerImpl businessPartnerService;

    @Test
    void should_ReturnPageResponse_When_PaginationValid() {
        BusinessPartners entity = BusinessPartners.builder()
                .code("BP-001")
                .name("Partner A")
                .build();
        entity.setId("bp-1");

        Page<BusinessPartners> page = new PageImpl<>(
                List.of(entity),
                PageRequest.of(0, 10),
                1
        );

        BusinessPartnerResponse response = BusinessPartnerResponse.builder()
                .id("bp-1")
                .code("BP-001")
                .name("Partner A")
                .build();

        when(repository.findAll(any(PageRequest.class))).thenReturn(page);
        when(mapper.toResponseList(page.getContent())).thenReturn(List.of(response));

        PageResponse<BusinessPartnerResponse> result =
                businessPartnerService.getAll(0, 10, "createdAt", "DESC");

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(10);
        verify(repository).findAll(any(PageRequest.class));
    }

    @Test
    void should_ThrowBadRequest_When_PageLessThanZero() {
        assertThatThrownBy(() -> businessPartnerService.getAll(-1, 10, "createdAt", "DESC"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_006.getCode());
    }

    @Test
    void should_ThrowBadRequest_When_SizeLessThanOrEqualToZero() {
        assertThatThrownBy(() -> businessPartnerService.getAll(0, 0, "createdAt", "DESC"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_007.getCode());
    }

    @Test
    void should_ThrowBadRequest_When_SizeGreaterThanHundred() {
        assertThatThrownBy(() -> businessPartnerService.getAll(0, 101, "createdAt", "DESC"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_008.getCode());
    }

    @Test
    void should_ThrowBadRequest_When_SortByNotAllowed() {
        assertThatThrownBy(() -> businessPartnerService.getAll(0, 10, "unknownField", "DESC"))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());
    }
}
