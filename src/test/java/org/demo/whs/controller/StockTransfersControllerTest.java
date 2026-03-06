package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.StockTransfersReason;
import org.demo.whs.entity.enums.StockTransfersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.service.StockTransfersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockTransfersController.class)
@ActiveProfiles("test")
@Import({StockTransfersControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser
class StockTransfersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockTransfersService stockTransfersService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should complete stock transfer when request is authenticated")
    void should_CompleteStockTransfer_When_RequestIsAuthenticated() throws Exception {
        StockTransfersResponse response = buildResponse("trf-1", StockTransfersStatus.COMPLETED);

        when(stockTransfersService.complete("trf-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/stock-transfers/{id}/complete", "trf-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("trf-1"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.transfer_number").value("TRF-001"));

        verify(stockTransfersService).complete("trf-1");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when complete is called without authentication")
    void should_Return401_When_CompleteWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/stock-transfers/{id}/complete", "trf-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 404 when transfer is not found during completion")
    void should_Return404_When_TransferNotFoundDuringCompletion() throws Exception {
        when(stockTransfersService.complete("trf-404"))
                .thenThrow(new NotFoundException("Stock transfer not found", ErrorCode.STF_001));

        mockMvc.perform(put("/api/v1/stock-transfers/{id}/complete", "trf-404")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("STF_001"))
                .andExpect(jsonPath("$.message").value("Stock transfer not found"));

        verify(stockTransfersService).complete("trf-404");
    }

    @Test
    @DisplayName("Should return 400 when transfer cannot be completed due to invalid state")
    void should_Return400_When_TransferCannotBeCompletedDueToInvalidState() throws Exception {
        when(stockTransfersService.complete("trf-invalid"))
                .thenThrow(new BadRequestException("Only draft transfer can be completed", ErrorCode.STF_002));

        mockMvc.perform(put("/api/v1/stock-transfers/{id}/complete", "trf-invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("STF_002"))
                .andExpect(jsonPath("$.message").value("Only draft transfer can be completed"));

        verify(stockTransfersService).complete("trf-invalid");
    }

    @Test
    @DisplayName("Should create stock transfer when request is valid")
    void should_CreateStockTransfer_When_RequestIsValid() throws Exception {
        StockTransfersResponse response = buildResponse("trf-create", StockTransfersStatus.DRAFT);

        when(stockTransfersService.createTransfer(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "product_id", "prod-1",
                                "warehouse_id", "wh-1",
                                "from_location_id", "loc-1",
                                "to_location_id", "loc-2",
                                "batch_id", "batch-1",
                                "quantity", new BigDecimal("10.00"),
                                "reason", "REORG",
                                "notes", "Move stock"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("trf-create"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Should return 400 when create request is invalid")
    void should_Return400_When_CreateRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/stock-transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "product_id", "",
                                "warehouse_id", "wh-1",
                                "from_location_id", "loc-1",
                                "to_location_id", "loc-2",
                                "quantity", new BigDecimal("0.00"),
                                "reason", "REORG"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"))
                .andExpect(jsonPath("$.field_errors[*].field", hasItem("productId")))
                .andExpect(jsonPath("$.field_errors[*].field", hasItem("quantity")));

        verify(stockTransfersService, never()).createTransfer(any());
    }

    @Test
    @DisplayName("Should get stock transfer by id")
    void should_GetStockTransferById() throws Exception {
        StockTransfersResponse response = buildResponse("trf-detail", StockTransfersStatus.DRAFT);

        when(stockTransfersService.getById("trf-detail")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stock-transfers/{id}", "trf-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("trf-detail"))
                .andExpect(jsonPath("$.data.transfer_number").value("TRF-001"));
    }

    @Test
    @DisplayName("Should get paged stock transfers")
    void should_GetPagedStockTransfers() throws Exception {
        PageResponse<StockTransfersResponse> pageResponse = PageResponse.<StockTransfersResponse>builder()
                .content(List.of(buildResponse("trf-page", StockTransfersStatus.DRAFT)))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(stockTransfersService.getAll(0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/stock-transfers")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("trf-page"));
    }

    private StockTransfersResponse buildResponse(String id, StockTransfersStatus status) {
        return StockTransfersResponse.builder()
                .id(id)
                .transferNumber("TRF-001")
                .productId("prod-1")
                .warehouseId("wh-1")
                .fromLocationId("loc-1")
                .toLocationId("loc-2")
                .batchId("batch-1")
                .quantity(new BigDecimal("10.00"))
                .reason(StockTransfersReason.REORG)
                .notes("Move stock")
                .status(status)
                .completedAt(status == StockTransfersStatus.COMPLETED
                        ? LocalDateTime.parse("2026-03-06T10:00:00")
                        : null)
                .createdBy("acc-1")
                .createdAt(LocalDateTime.parse("2026-03-06T09:00:00"))
                .updatedBy("acc-1")
                .updatedAt(LocalDateTime.parse("2026-03-06T10:00:00"))
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((request, response, exception) ->
                                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                            .accessDeniedHandler((request, response, exception) ->
                                    response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }
}
