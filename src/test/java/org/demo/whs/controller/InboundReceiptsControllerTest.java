package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.InboundReceiptsService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InboundReceiptsController.class)
@ActiveProfiles("test")
@Import({InboundReceiptsControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser
class InboundReceiptsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InboundReceiptsService inboundReceiptsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create inbound receipt when request is valid")
    void should_CreateInboundReceipt_When_RequestIsValid() throws Exception {
        InboundReceiptsResponse response = buildResponse("rec-1", "DRAFT");

        when(inboundReceiptsService.create(any(InboundReceiptsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inbound-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "purchase_order_id", "po-1",
                                "receipt_date", "2026-03-15",
                                "delivery_note_number", "DN-20260315-001",
                                "notes", "Test receipt"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("rec-1"))
                .andExpect(jsonPath("$.data.receipt_number").value("GR-20260315-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(inboundReceiptsService).create(any(InboundReceiptsRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing purchase_order_id")
    void should_Return400_When_CreateRequestHasMissingPurchaseOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "delivery_note_number", "DN-001"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).create(any());
    }

    @Test
    @DisplayName("Should return 400 when delivery_note_number exceeds max length")
    void should_Return400_When_DeliveryNoteNumberExceedsMaxLength() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "purchase_order_id", "po-1",
                                "delivery_note_number", "a".repeat(101)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).create(any());
    }

    @Test
    @DisplayName("Should return 400 when notes exceed max length")
    void should_Return400_When_NotesExceedMaxLength() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "purchase_order_id", "po-1",
                                "notes", "a".repeat(1001)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).create(any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "purchase_order_id", "po-1"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get inbound receipt by id")
    void should_GetInboundReceiptById_When_RequestIsValid() throws Exception {
        InboundReceiptsResponse response = buildResponse("rec-1", "DRAFT");

        when(inboundReceiptsService.getById("rec-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/inbound-receipts/{id}", "rec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("rec-1"))
                .andExpect(jsonPath("$.data.receipt_number").value("GR-20260315-001"));

        verify(inboundReceiptsService).getById("rec-1");
    }

    @Test
    @DisplayName("Should get all inbound receipts when no filter is provided")
    void should_GetAllInboundReceipts_When_NoFilterProvided() throws Exception {
        PageResponse<InboundReceiptsResponse> pageResponse = buildPageResponse(
                buildResponse("rec-1", "DRAFT"), 0, 10);

        when(inboundReceiptsService.getAll(any(InboundReceiptsFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("rec-1"));

        verify(inboundReceiptsService).getAll(any(InboundReceiptsFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should filter inbound receipts when filters are provided")
    void should_FilterInboundReceipts_When_FiltersAreProvided() throws Exception {
        PageResponse<InboundReceiptsResponse> pageResponse = buildPageResponse(
                buildResponse("rec-2", "DRAFT"), 0, 5);

        when(inboundReceiptsService.getAll(any(InboundReceiptsFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("receipt_number", "GR-2026")
                        .param("purchase_order_id", "po-1")
                        .param("warehouse_id", "wh-1")
                        .param("status", "DRAFT")
                        .param("receipt_date_from", "2026-03-01")
                        .param("receipt_date_to", "2026-03-31")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content[0].status").value("DRAFT"));

        verify(inboundReceiptsService).getAll(any(InboundReceiptsFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should return 400 when sort field is invalid")
    void should_Return400_When_SortFieldIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("sortBy", "invalidField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when page is negative")
    void should_Return400_When_PageIsNegative() throws Exception {
        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_006"));

        verify(inboundReceiptsService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void should_Return400_When_SizeIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_007"));

        verify(inboundReceiptsService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when size exceeds 100")
    void should_Return400_When_SizeExceeds100() throws Exception {
        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_008"));

        verify(inboundReceiptsService, never()).getAll(any(), any());
    }

    @Test
    @DisplayName("Should sort inbound receipts by valid field")
    void should_SortInboundReceipts_When_SortFieldIsValid() throws Exception {
        PageResponse<InboundReceiptsResponse> pageResponse = buildPageResponse(
                buildResponse("rec-1", "DRAFT"), 0, 10);

        when(inboundReceiptsService.getAll(any(InboundReceiptsFilterRequest.class), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/inbound-receipts")
                        .param("sortBy", "receiptDate")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inboundReceiptsService).getAll(any(InboundReceiptsFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should update inbound receipt when request is valid")
    void should_UpdateInboundReceipt_When_RequestIsValid() throws Exception {
        InboundReceiptsResponse response = buildResponse("rec-1", "DRAFT");

        when(inboundReceiptsService.update(eq("rec-1"), any(UpdateInboundReceiptsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/inbound-receipts/{id}", "rec-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "receipt_date", "2026-03-20",
                                "delivery_note_number", "DN-20260320-001",
                                "notes", "Updated notes"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("rec-1"));

        verify(inboundReceiptsService).update(eq("rec-1"), any(UpdateInboundReceiptsRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when update request has invalid delivery_note_number length")
    void should_Return400_When_UpdateRequestHasInvalidDeliveryNoteNumberLength() throws Exception {
        mockMvc.perform(put("/api/v1/inbound-receipts/{id}", "rec-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "delivery_note_number", "a".repeat(101)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).update(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when notes exceed max length in update")
    void should_Return400_When_NotesExceedMaxLengthInUpdate() throws Exception {
        mockMvc.perform(put("/api/v1/inbound-receipts/{id}", "rec-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "a".repeat(1001)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptsService, never()).update(any(), any());
    }

    @Test
    @DisplayName("Should delete inbound receipt")
    void should_DeleteInboundReceipt_When_RequestIsValid() throws Exception {
        doNothing().when(inboundReceiptsService).delete("rec-1");

        mockMvc.perform(delete("/api/v1/inbound-receipts/{id}", "rec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inboundReceiptsService).delete("rec-1");
    }

    @Test
    @DisplayName("Should confirm inbound receipt")
    void should_ConfirmInboundReceipt_When_RequestIsValid() throws Exception {
        InboundReceiptsResponse response = buildResponse("rec-1", "CONFIRMED");

        when(inboundReceiptsService.confirm("rec-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/inbound-receipts/{id}/confirm", "rec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("rec-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(inboundReceiptsService).confirm("rec-1");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when delete is called without authentication")
    void should_Return401_When_DeleteWithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/inbound-receipts/{id}", "rec-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when confirm is called without authentication")
    void should_Return401_When_ConfirmWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/inbound-receipts/{id}/confirm", "rec-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get inbound receipts by purchase order id")
    void should_GetInboundReceiptsByPurchaseOrderId_When_RequestIsValid() throws Exception {
        List<InboundReceiptsResponse> responses = List.of(
                buildResponse("rec-1", "DRAFT"),
                buildResponse("rec-2", "CONFIRMED")
        );

        when(inboundReceiptsService.getByPurchaseOrderId("po-1")).thenReturn(responses);

        mockMvc.perform(get("/api/v1/inbound-receipts/by-po/{purchaseOrderId}", "po-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("rec-1"))
                .andExpect(jsonPath("$.data[1].id").value("rec-2"));

        verify(inboundReceiptsService).getByPurchaseOrderId("po-1");
    }

    @Test
    @DisplayName("Should return empty list when no receipts found for purchase order")
    void should_ReturnEmptyList_When_NoReceiptsFoundForPurchaseOrder() throws Exception {
        when(inboundReceiptsService.getByPurchaseOrderId("po-999")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/inbound-receipts/by-po/{purchaseOrderId}", "po-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(inboundReceiptsService).getByPurchaseOrderId("po-999");
    }

    private InboundReceiptsResponse buildResponse(String id, String status) {
        return InboundReceiptsResponse.builder()
                .id(id)
                .receiptNumber("GR-20260315-001")
                .purchaseOrderId("po-1")
                .purchaseOrderNumber("PO-20260315-ABC123")
                .warehouseId("wh-1")
                .warehouseName("Main Warehouse")
                .receiptDate(LocalDate.parse("2026-03-15"))
                .status(status)
                .deliveryNoteNumber("DN-20260315-001")
                .notes("Test receipt")
                .confirmedAt(null)
                .confirmedBy(null)
                .lines(List.of())
                .createdAt(LocalDateTime.parse("2026-03-15T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-15T11:00:00"))
                .build();
    }

    private PageResponse<InboundReceiptsResponse> buildPageResponse(
            InboundReceiptsResponse response, int page, int size) {
        return PageResponse.<InboundReceiptsResponse>builder()
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
