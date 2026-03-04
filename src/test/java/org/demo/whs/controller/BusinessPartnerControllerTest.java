package org.demo.whs.controller;

import org.demo.whs.entity.dto.response.BusinessPartner.BusinessPartnerResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.BusinessPartnerService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessPartnerController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class BusinessPartnerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessPartnerService businessPartnerService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Get paginated business partners should return page metadata")
    void should_ReturnPaginatedBusinessPartners_When_RequestValid() throws Exception {
        BusinessPartnerResponse item = BusinessPartnerResponse.builder()
                .id("bp-1")
                .code("BP-001")
                .name("Partner A")
                .build();

        PageResponse<BusinessPartnerResponse> pageResponse = PageResponse.<BusinessPartnerResponse>builder()
                .content(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(businessPartnerService.getAll(0, 10, "createdAt", "DESC")).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/business-partners")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value("bp-1"));

        verify(businessPartnerService).getAll(0, 10, "createdAt", "DESC");
    }

    @Test
    @DisplayName("Get paginated business partners should return 400 when size is invalid")
    void should_ReturnBadRequest_When_SizeInvalid() throws Exception {
        when(businessPartnerService.getAll(0, 0, "createdAt", "DESC"))
                .thenThrow(new BadRequestException(ErrorCode.COM_007));

        mockMvc.perform(get("/api/v1/business-partners")
                        .param("page", "0")
                        .param("size", "0")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "DESC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_007"));
    }
}
