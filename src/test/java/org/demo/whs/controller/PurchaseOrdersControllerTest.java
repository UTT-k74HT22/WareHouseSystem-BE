package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersFilterRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.PurchaseOrdersService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseOrdersController.class)
@ActiveProfiles("test")
@Import({PurchaseOrdersControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser
class PurchaseOrdersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseOrdersService purchaseOrdersService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create purchase order when request is valid")
    void should_CreatePurchaseOrder_When_RequestIsValid() throws Exception {
        PurchaseOrdersResponse response = buildResponse("po-1", "DRAFT");

        when(purchaseOrdersService.create(any(PurchaseOrdersRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/purchase-orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supplier_id", "sup-1",
                                "warehouse_id", "wh-1",
                                "order_date", "2026-03-15",
                                "expected_delivery_date", "2027-03-25",
                                "currency", "VND",
                                "payment_terms", "Net 30",
                                "notes", "Test order"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("po-1"))
                .andExpect(jsonPath("$.data.purchase_order_number").value("PO-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(purchaseOrdersService).create(any(PurchaseOrdersRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void should_Return400_When_CreateRequestHasMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supplier_id", "sup-1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(purchaseOrdersService, never()).create(any());
    }

    @Test
    @DisplayName("Should return 400 when currency is invalid")
    void should_Return400_When_CurrencyIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supplier_id", "sup-1",
                                "warehouse_id", "wh-1",
                                "order_date", "2026-03-15",
                                "currency", "INVALID",
                                "notes", "test"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(purchaseOrdersService, never()).create(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/purchase-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supplier_id", "sup-1",
                                "warehouse_id", "wh-1",
                                "order_date", "2026-03-15",
                                "currency", "VND"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get purchase order by id")
    void should_GetPurchaseOrderById_When_RequestIsValid() throws Exception {
        PurchaseOrdersResponse response = buildResponse("po-1", "DRAFT");

        when(purchaseOrdersService.getById("po-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/purchase-orders/{id}", "po-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("po-1"))
                .andExpect(jsonPath("$.data.purchase_order_number").value("PO-001"));

        verify(purchaseOrdersService).getById("po-1");
    }

    @Test
    @DisplayName("Should get all purchase orders when no filter is provided")
    void should_GetAllPurchaseOrders_When_NoFilterProvided() throws Exception {
        PageResponse<PurchaseOrdersResponse> pageResponse = buildPageResponse(
                buildResponse("po-1", "DRAFT"), 0, 10);

        when(purchaseOrdersService.getAll(any(PurchaseOrdersFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("po-1"));

        verify(purchaseOrdersService).getAll(any(PurchaseOrdersFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should filter purchase orders when filters are provided")
    void should_FilterPurchaseOrders_When_FiltersAreProvided() throws Exception {
        PageResponse<PurchaseOrdersResponse> pageResponse = buildPageResponse(
                buildResponse("po-2", "CONFIRMED"), 0, 5);

        when(purchaseOrdersService.getAll(any(PurchaseOrdersFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("purchase_order_number", "PO-002")
                        .param("supplier_id", "sup-1")
                        .param("warehouse_id", "wh-1")
                        .param("status", "CONFIRMED")
                        .param("order_date_from", "2026-03-01")
                        .param("order_date_to", "2026-03-31")
                        .param("expected_delivery_date_from", "2026-03-10")
                        .param("expected_delivery_date_to", "2026-04-10")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"));

        verify(purchaseOrdersService).getAll(any(PurchaseOrdersFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should return 400 when sort field is invalid")
    void should_Return400_When_SortFieldIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(purchaseOrdersService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when page is negative")
    void should_Return400_When_PageIsNegative() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_006"));

        verify(purchaseOrdersService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void should_Return400_When_SizeIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_007"));

        verify(purchaseOrdersService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when size exceeds 100")
    void should_Return400_When_SizeExceeds100() throws Exception {
        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_008"));

        verify(purchaseOrdersService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should sort purchase orders by valid field")
    void should_SortPurchaseOrders_When_SortFieldIsValid() throws Exception {
        PageResponse<PurchaseOrdersResponse> pageResponse = buildPageResponse(
                buildResponse("po-1", "DRAFT"), 0, 10);

        when(purchaseOrdersService.getAll(any(PurchaseOrdersFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/purchase-orders")
                        .param("sortBy", "orderDate")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(purchaseOrdersService).getAll(any(PurchaseOrdersFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should update purchase order when request is valid")
    void should_UpdatePurchaseOrder_When_RequestIsValid() throws Exception {
        PurchaseOrdersResponse response = buildResponse("po-1", "DRAFT");

        when(purchaseOrdersService.update(eq("po-1"), any(UpdatePurchaseOrdersRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/purchase-orders/{id}", "po-1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "supplier_id", "sup-2",
                                "warehouse_id", "wh-2",
                                "expected_delivery_date", "2027-03-25",
                                "currency", "USD",
                                "payment_terms", "Net 60",
                                "notes", "Updated notes"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("po-1"));

        verify(purchaseOrdersService).update(eq("po-1"), any(UpdatePurchaseOrdersRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update request has invalid currency")
    void should_Return400_When_UpdateRequestHasInvalidCurrency() throws Exception {
        mockMvc.perform(put("/api/v1/purchase-orders/{id}", "po-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "currency", "INVALID"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(purchaseOrdersService, never()).update(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when notes exceed max length in update")
    void should_Return400_When_NotesExceedMaxLengthInUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/purchase-orders/{id}", "po-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "a".repeat(1001)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(purchaseOrdersService, never()).update(any(), any());
    }

    @Test
    @DisplayName("Should delete purchase order")
    void should_DeletePurchaseOrder_When_RequestIsValid() throws Exception {
        doNothing().when(purchaseOrdersService).delete("po-1");

        mockMvc.perform(delete("/api/v1/purchase-orders/{id}", "po-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(purchaseOrdersService).delete("po-1");
    }

    @Test
    @DisplayName("Should confirm purchase order")
    void should_ConfirmPurchaseOrder_When_RequestIsValid() throws Exception {
        PurchaseOrdersResponse response = buildResponse("po-1", "CONFIRMED");

        when(purchaseOrdersService.confirm("po-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/purchase-orders/{id}/confirm", "po-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("po-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(purchaseOrdersService).confirm("po-1");
    }

    private PurchaseOrdersResponse buildResponse(String id, String status) {
        return PurchaseOrdersResponse.builder()
                .id(id)
                .purchaseOrderNumber("PO-001")
                .supplierId("sup-1")
                .warehouseId("wh-1")
                .orderDate(LocalDate.parse("2026-03-15"))
                .expectedDeliveryDate(LocalDate.parse("2026-03-20"))
                .status(status)
                .subTotal(new BigDecimal("1000.00"))
                .taxAmount(new BigDecimal("100.00"))
                .totalAmount(new BigDecimal("1100.00"))
                .currency("VND")
                .paymentTerms("Net 30")
                .notes("Test order")
                .confirmedAt(null)
                .confirmedBy(null)
                .createdAt(LocalDateTime.parse("2026-03-01T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-01T11:00:00"))
                .build();
    }

    private PageResponse<PurchaseOrdersResponse> buildPageResponse(
            PurchaseOrdersResponse response, int page, int size) {
        return PageResponse.<PurchaseOrdersResponse>builder()
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
