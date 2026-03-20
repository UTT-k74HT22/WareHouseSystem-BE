package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.SalesOrderLines.CreateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.UpdateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.service.SalesOrderLinesService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesOrderLinesController.class)
@ActiveProfiles("test")
@Import({SalesOrderLinesControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser
class SalesOrderLinesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SalesOrderLinesService salesOrderLinesService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create line when request is valid")
    void should_CreateLine_When_RequestIsValid() throws Exception {
        SalesOrderLinesResponse response = buildLineResponse("line-1", "so-1");

        when(salesOrderLinesService.create(any(CreateSalesOrderLinesRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sales-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1",
                                "product_id", "prod-1",
                                "quantity_ordered", 5,
                                "unit_price", 100.00,
                                "notes", "Line note"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"))
                .andExpect(jsonPath("$.data.line_number").value(1))
                .andExpect(jsonPath("$.data.line_total").value(500.00));

        verify(salesOrderLinesService).create(any(CreateSalesOrderLinesRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void should_Return400_When_CreateRequestHasMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/sales-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when quantity is zero or negative")
    void should_Return400_When_QuantityIsZeroOrNegative() throws Exception {
        mockMvc.perform(post("/api/v1/sales-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1",
                                "product_id", "prod-1",
                                "quantity_ordered", 0,
                                "unit_price", 100.00
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sales-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1",
                                "product_id", "prod-1",
                                "quantity_ordered", 5,
                                "unit_price", 100.00
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update line when request is valid")
    void should_UpdateLine_When_RequestIsValid() throws Exception {
        SalesOrderLinesResponse response = buildLineResponse("line-1", "so-1");

        when(salesOrderLinesService.update(eq("line-1"), any(UpdateSalesOrderLinesRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/sales-order-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity_ordered", 10,
                                "unit_price", 120.00,
                                "notes", "Updated note"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"));

        verify(salesOrderLinesService).update(eq("line-1"), any(UpdateSalesOrderLinesRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update request has negative unit price")
    void should_Return400_When_UpdateRequestHasNegativeUnitPrice() throws Exception {
        mockMvc.perform(put("/api/v1/sales-order-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unit_price", -10.00
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should get lines by sales order id")
    void should_GetLinesBySalesOrderId_When_RequestIsValid() throws Exception {
        List<SalesOrderLinesResponse> responses = List.of(
                buildLineResponse("line-1", "so-1"),
                buildLineResponse("line-2", "so-1")
        );

        when(salesOrderLinesService.getBySalesOrder("so-1")).thenReturn(responses);

        mockMvc.perform(get("/api/v1/sales-order-lines/by-so/{soId}", "so-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("line-1"))
                .andExpect(jsonPath("$.data[1].id").value("line-2"));

        verify(salesOrderLinesService).getBySalesOrder("so-1");
    }

    private SalesOrderLinesResponse buildLineResponse(String id, String soId) {
        return SalesOrderLinesResponse.builder()
                .id(id)
                .salesOrderId(soId)
                .productId("prod-1")
                .lineNumber(1)
                .quantityOrdered(new BigDecimal("5.00"))
                .quantityShipped(BigDecimal.ZERO)
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("500.00"))
                .notes("Line note")
                .createdAt(LocalDateTime.parse("2026-03-01T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-01T11:00:00"))
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
