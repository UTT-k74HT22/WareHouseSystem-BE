package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.service.StockAdjustmentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockAdjustmentsController.class)
@ActiveProfiles("test")
@Import({StockAdjustmentsControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser(authorities = {
        "PERM_STOCK_ADJUSTMENT_CREATE",
        "PERM_STOCK_ADJUSTMENT_READ",
        "PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE"
})
class StockAdjustmentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockAdjustmentsService stockAdjustmentsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create stock adjustment when request is valid")
    void should_CreateStockAdjustment_When_RequestIsValid() throws Exception {
        StockAdjustmentsResponse response = buildResponse("adj-1", StockAdjustmentsStatus.PENDING_APPROVAL);

        when(stockAdjustmentsService.createAdjustment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inventory_id", "inv-1",
                                "quantity_after", new BigDecimal("100.00"),
                                "reason", "COUNT_ERROR",
                                "notes", "Cycle count adjustment"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("adj-1"))
                .andExpect(jsonPath("$.data.inventory_id").value("inv-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"));

        verify(stockAdjustmentsService).createAdjustment(argThat(request ->
                "inv-1".equals(request.getInventoryId())
                        && request.getQuantityAfter().compareTo(new BigDecimal("100.00")) == 0
                        && "Cycle count adjustment".equals(request.getNotes())
        ));
    }

    @Test
    @DisplayName("Should return 400 when quantity_after has more than 2 decimal places")
    void should_Return400_When_QuantityAfterHasTooManyDecimals() throws Exception {
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inventory_id", "inv-1",
                                "quantity_after", new BigDecimal("100.001"),
                                "reason", "COUNT_ERROR",
                                "notes", "precision test"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"))
                .andExpect(jsonPath("$.field_errors[*].field", hasItem("quantityAfter")));

        verify(stockAdjustmentsService, never()).createAdjustment(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/stock-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inventory_id", "inv-1",
                                "quantity_after", new BigDecimal("100.00"),
                                "reason", "COUNT_ERROR"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get stock adjustment by id")
    void should_GetStockAdjustmentById_When_RequestIsValid() throws Exception {
        StockAdjustmentsResponse response = buildResponse("adj-1", StockAdjustmentsStatus.PENDING_APPROVAL);

        when(stockAdjustmentsService.getById("adj-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/stock-adjustments/{id}", "adj-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("adj-1"))
                .andExpect(jsonPath("$.data.adjustment_number").value("ADJ-001"));

        verify(stockAdjustmentsService).getById("adj-1");
    }

    @Test
    @DisplayName("Should get all stock adjustments when no filter is provided")
    void should_GetAllStockAdjustments_When_NoFilterProvided() throws Exception {
        PageResponse<StockAdjustmentsResponse> pageResponse = buildPageResponse(
                buildResponse("adj-1", StockAdjustmentsStatus.PENDING_APPROVAL), 0, 10);

        when(stockAdjustmentsService.getAll(0, 10)).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/stock-adjustments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("adj-1"));

        verify(stockAdjustmentsService).getAll(0, 10);
        verify(stockAdjustmentsService, never()).search(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should search stock adjustments when filters are provided")
    void should_SearchStockAdjustments_When_FiltersAreProvided() throws Exception {
        PageResponse<StockAdjustmentsResponse> pageResponse = buildPageResponse(
                buildResponse("adj-2", StockAdjustmentsStatus.APPROVED), 2, 15);
        ArgumentCaptor<SearchStockAdjustmentsRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchStockAdjustmentsRequest.class);

        when(stockAdjustmentsService.search(any(SearchStockAdjustmentsRequest.class), eq(2), eq(15)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/stock-adjustments")
                        .param("status", "APPROVED")
                        .param("productId", "prod-1")
                        .param("warehouseId", "wh-1")
                        .param("inventoryId", "inv-1")
                        .param("adjustmentNumber", "ADJ-002")
                        .param("createdFrom", "2026-03-01T10:00:00")
                        .param("createdTo", "2026-03-02T11:30:00")
                        .param("page", "2")
                        .param("size", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(15))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));

        verify(stockAdjustmentsService).search(requestCaptor.capture(), eq(2), eq(15));
        verify(stockAdjustmentsService, never()).getAll(anyInt(), anyInt());

        SearchStockAdjustmentsRequest capturedRequest = requestCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(StockAdjustmentsStatus.APPROVED, capturedRequest.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("prod-1", capturedRequest.getProductId());
        org.junit.jupiter.api.Assertions.assertEquals("wh-1", capturedRequest.getWarehouseId());
        org.junit.jupiter.api.Assertions.assertEquals("inv-1", capturedRequest.getInventoryId());
        org.junit.jupiter.api.Assertions.assertEquals("ADJ-002", capturedRequest.getAdjustmentNumber());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.parse("2026-03-01T10:00:00"),
                capturedRequest.getCreatedFrom());
        org.junit.jupiter.api.Assertions.assertEquals(LocalDateTime.parse("2026-03-02T11:30:00"),
                capturedRequest.getCreatedTo());
    }

    @Test
    @DisplayName("Should return 400 when status filter is invalid")
    void should_Return400_When_StatusFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/stock-adjustments")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(stockAdjustmentsService, never()).getAll(anyInt(), anyInt());
        verify(stockAdjustmentsService, never()).search(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should allow approve endpoint without request body")
    void should_AllowApproveWithoutBody() throws Exception {
        StockAdjustmentsResponse response = buildResponse("adj-1", StockAdjustmentsStatus.APPROVED);

        when(stockAdjustmentsService.approve(eq("adj-1"), isNull())).thenReturn(response);

        mockMvc.perform(put("/api/v1/stock-adjustments/{id}/approve", "adj-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("adj-1"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        verify(stockAdjustmentsService).approve(eq("adj-1"), isNull());
    }

    @Test
    @DisplayName("Should return 400 when approval note exceeds max length")
    void should_Return400_When_ApprovalNoteExceedsMaxLength() throws Exception {
        mockMvc.perform(put("/api/v1/stock-adjustments/{id}/approve", "adj-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "approval_note", "a".repeat(501)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"))
                .andExpect(jsonPath("$.field_errors[*].field", hasItem("approvalNote")));

        verify(stockAdjustmentsService, never()).approve(any(), any());
    }

    @Test
    @DisplayName("Should reject stock adjustment when request is valid")
    void should_RejectStockAdjustment_When_RequestIsValid() throws Exception {
        StockAdjustmentsResponse response = buildResponse("adj-1", StockAdjustmentsStatus.REJECTED);

        when(stockAdjustmentsService.reject(eq("adj-1"), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/stock-adjustments/{id}/reject", "adj-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "rejection_reason", "Mismatch with cycle count evidence"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("adj-1"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        verify(stockAdjustmentsService).reject(eq("adj-1"), argThat(request ->
                "Mismatch with cycle count evidence".equals(request.getRejectionReason())
        ));
    }

    @Test
    @DisplayName("Should return 400 when rejection reason is blank")
    void should_Return400_When_RejectionReasonIsBlank() throws Exception {
        mockMvc.perform(put("/api/v1/stock-adjustments/{id}/reject", "adj-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "rejection_reason", "   "
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"))
                .andExpect(jsonPath("$.field_errors[*].field", hasItem("rejectionReason")));

        verify(stockAdjustmentsService, never()).reject(any(), any());
    }

    private StockAdjustmentsResponse buildResponse(String id, StockAdjustmentsStatus status) {
        return StockAdjustmentsResponse.builder()
                .id(id)
                .adjustmentNumber("ADJ-001")
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantityBefore(new BigDecimal("95.00"))
                .quantityAfter(new BigDecimal("100.00"))
                .adjustmentQuantity(new BigDecimal("5.00"))
                .status(status)
                .requiresApproval(Boolean.TRUE)
                .createdAt(LocalDateTime.parse("2026-03-01T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-01T11:00:00"))
                .build();
    }

    private PageResponse<StockAdjustmentsResponse> buildPageResponse(
            StockAdjustmentsResponse response, int page, int size) {
        return PageResponse.<StockAdjustmentsResponse>builder()
                .content(List.of(response))
                .page(page)
                .size(size)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(page == 0)
                .isLast(true)
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
