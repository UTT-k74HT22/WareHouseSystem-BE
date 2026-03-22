package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.RateLimitService;
import org.demo.whs.service.SalesOrdersService;
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
import java.time.LocalDate;
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

@WebMvcTest(SalesOrdersController.class)
@ActiveProfiles("test")
@Import({SalesOrdersControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser
class SalesOrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SalesOrdersService salesOrdersService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create sales order when request is valid")
    void should_CreateSalesOrder_When_RequestIsValid() throws Exception {
        SalesOrdersResponse response = buildResponse("so-1", "DRAFT");

        when(salesOrdersService.create(any(SalesOrdersRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customer_id", "bp-1",
                                "warehouse_id", "wh-1",
                                "order_date", "2026-03-20",
                                "requested_delivery_date", "2026-03-25",
                                "currency", "VND",
                                "notes", "Test SO",
                                "lines", List.of(Map.of(
                                        "product_id", "prod-1",
                                        "quantity_ordered", 5,
                                        "unit_price", 100.00
                                ))
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("so-1"))
                .andExpect(jsonPath("$.data.so_number").value("SO-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(salesOrdersService).create(any(SalesOrdersRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void should_Return400_When_CreateRequestHasMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customer_id", "bp-1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sales-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customer_id", "bp-1",
                                "warehouse_id", "wh-1",
                                "order_date", "2026-03-20",
                                "requested_delivery_date", "2026-03-25",
                                "currency", "VND",
                                "lines", List.of(Map.of(
                                        "product_id", "prod-1",
                                        "quantity_ordered", 5,
                                        "unit_price", 100.00
                                ))
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get sales order by id")
    void should_GetSalesOrderById_When_RequestIsValid() throws Exception {
        SalesOrdersResponse response = buildResponse("so-1", "DRAFT");

        when(salesOrdersService.getById("so-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/sales-orders/{id}", "so-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("so-1"))
                .andExpect(jsonPath("$.data.so_number").value("SO-001"));

        verify(salesOrdersService).getById("so-1");
    }

    @Test
    @DisplayName("Should get all sales orders when no filter is provided")
    void should_GetAllSalesOrders_When_NoFilterProvided() throws Exception {
        PageResponse<SalesOrdersResponse> pageResponse = buildPageResponse(buildResponse("so-1", "DRAFT"), 0, 10);

        when(salesOrdersService.getAll(any(SalesOrdersFilterRequest.class), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/sales-orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("so-1"));

        verify(salesOrdersService).getAll(any(SalesOrdersFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should filter sales orders when filters are provided")
    void should_FilterSalesOrders_When_FiltersAreProvided() throws Exception {
        PageResponse<SalesOrdersResponse> pageResponse = buildPageResponse(buildResponse("so-2", "CONFIRMED"), 0, 5);

        when(salesOrdersService.getAll(any(SalesOrdersFilterRequest.class), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/sales-orders")
                        .param("so_number", "SO-002")
                        .param("customer_id", "bp-1")
                        .param("warehouse_id", "wh-1")
                        .param("status", "CONFIRMED")
                        .param("order_date_from", "2026-03-01")
                        .param("order_date_to", "2026-03-31")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"));

        verify(salesOrdersService).getAll(any(SalesOrdersFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should return 400 when sort field is invalid")
    void should_Return400_When_SortFieldIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/sales-orders")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should update sales order when request is valid")
    void should_UpdateSalesOrder_When_RequestIsValid() throws Exception {
        SalesOrdersResponse response = buildResponse("so-1", "DRAFT");

        when(salesOrdersService.update(eq("so-1"), any(UpdateSalesOrdersRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/sales-orders/{id}", "so-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "Updated notes",
                                "currency", "USD"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("so-1"));

        verify(salesOrdersService).update(eq("so-1"), any(UpdateSalesOrdersRequest.class));
    }

    @Test
    @DisplayName("Should confirm sales order")
    void should_ConfirmSalesOrder_When_RequestIsValid() throws Exception {
        SalesOrdersResponse response = buildResponse("so-1", "CONFIRMED");

        when(salesOrdersService.confirm("so-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/sales-orders/{id}/confirm", "so-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("so-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(salesOrdersService).confirm("so-1");
    }

    @Test
    @DisplayName("Should cancel sales order")
    void should_CancelSalesOrder_When_RequestIsValid() throws Exception {
        SalesOrdersResponse response = buildResponse("so-1", "CANCELLED");

        when(salesOrdersService.cancel("so-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/sales-orders/{id}/cancel", "so-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("so-1"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(salesOrdersService).cancel("so-1");
    }

    private SalesOrdersResponse buildResponse(String id, String status) {
        return SalesOrdersResponse.builder()
                .id(id)
                .soNumber("SO-001")
                .customerId("bp-1")
                .warehouseId("wh-1")
                .orderDate(LocalDate.parse("2026-03-20"))
                .requestedDeliveryDate(LocalDate.parse("2026-03-25"))
                .status(status)
                .subTotal(new BigDecimal("500.00"))
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("500.00"))
                .currency("VND")
                .notes("Test SO")
                .confirmedAt(null)
                .confirmedBy(null)
                .createdAt(LocalDateTime.parse("2026-03-01T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-01T11:00:00"))
                .lines(List.of())
                .build();
    }

    private PageResponse<SalesOrdersResponse> buildPageResponse(SalesOrdersResponse response, int page, int size) {
        return PageResponse.<SalesOrdersResponse>builder()
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
