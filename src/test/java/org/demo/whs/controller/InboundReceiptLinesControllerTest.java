package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.InboundReceiptLinesService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
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

@WebMvcTest(InboundReceiptLinesController.class)
@ActiveProfiles("test")
@WithMockUser
@Import({InboundReceiptLinesControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class InboundReceiptLinesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InboundReceiptLinesService inboundReceiptLinesService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should create inbound receipt line when request is valid")
    void should_CreateInboundReceiptLine_When_RequestIsValid() throws Exception {
        when(inboundReceiptLinesService.create(any())).thenReturn(buildResponse("line-1", 1, "PASS"));

        mockMvc.perform(post("/api/v1/inbound-receipt-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inbound_receipt_id", "receipt-1",
                                "purchase_order_line_id", "pol-1",
                                "location_id", "loc-1",
                                "quantity_received", "5.00"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"))
                .andExpect(jsonPath("$.data.line_number").value(1))
                .andExpect(jsonPath("$.data.quality_status").value("PASS"));

        verify(inboundReceiptLinesService).create(any());
    }

    @Test
    @DisplayName("Should return 400 when create request misses inbound_receipt_id")
    void should_Return400_When_CreateRequestMissesInboundReceiptId() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipt-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "purchase_order_line_id", "pol-1",
                                "location_id", "loc-1",
                                "quantity_received", "5.00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptLinesService, never()).create(any());
    }

    @Test
    @DisplayName("Should return 400 when create request has non-positive quantity")
    void should_Return400_When_CreateRequestHasNonPositiveQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipt-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inbound_receipt_id", "receipt-1",
                                "purchase_order_line_id", "pol-1",
                                "location_id", "loc-1",
                                "quantity_received", "0.00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptLinesService, never()).create(any());
    }

    @Test
    @DisplayName("Should return 400 when create request notes exceed max length")
    void should_Return400_When_CreateRequestNotesExceedMaxLength() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipt-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inbound_receipt_id", "receipt-1",
                                "purchase_order_line_id", "pol-1",
                                "location_id", "loc-1",
                                "quantity_received", "5.00",
                                "notes", "a".repeat(501)
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptLinesService, never()).create(any());
    }

    @Test
    @DisplayName("Should update inbound receipt line when request is valid")
    void should_UpdateInboundReceiptLine_When_RequestIsValid() throws Exception {
        when(inboundReceiptLinesService.update(eq("line-1"), any())).thenReturn(buildResponse("line-1", 2, "QUARANTINE"));

        mockMvc.perform(put("/api/v1/inbound-receipt-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "location_id", "loc-2",
                                "batch_id", "batch-1",
                                "quantity_received", "8.00",
                                "quality_status", "QUARANTINE",
                                "notes", "Damaged on arrival"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"))
                .andExpect(jsonPath("$.data.line_number").value(2))
                .andExpect(jsonPath("$.data.quality_status").value("QUARANTINE"));

        verify(inboundReceiptLinesService).update(eq("line-1"), any());
    }

    @Test
    @DisplayName("Should return 400 when update request misses location_id")
    void should_Return400_When_UpdateRequestMissesLocationId() throws Exception {
        mockMvc.perform(put("/api/v1/inbound-receipt-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity_received", "8.00"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(inboundReceiptLinesService, never()).update(any(), any());
    }

    @Test
    @DisplayName("Should delete inbound receipt line when request is valid")
    void should_DeleteInboundReceiptLine_When_RequestIsValid() throws Exception {
        doNothing().when(inboundReceiptLinesService).delete("line-1");

        mockMvc.perform(delete("/api/v1/inbound-receipt-lines/{id}", "line-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(inboundReceiptLinesService).delete("line-1");
    }

    @Test
    @DisplayName("Should get inbound receipt lines by receipt id")
    void should_GetInboundReceiptLinesByReceiptId_When_RequestIsValid() throws Exception {
        when(inboundReceiptLinesService.findByInboundReceiptId("receipt-1"))
                .thenReturn(List.of(buildResponse("line-1", 1, "PASS"), buildResponse("line-2", 2, "QUARANTINE")));

        mockMvc.perform(get("/api/v1/inbound-receipt-lines")
                        .param("inboundReceiptId", "receipt-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("line-1"))
                .andExpect(jsonPath("$.data[1].id").value("line-2"));

        verify(inboundReceiptLinesService).findByInboundReceiptId("receipt-1");
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/inbound-receipt-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "inbound_receipt_id", "receipt-1",
                                "purchase_order_line_id", "pol-1",
                                "location_id", "loc-1",
                                "quantity_received", "5.00"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    private InboundReceiptLinesResponse buildResponse(String id, int lineNumber, String qualityStatus) {
        return InboundReceiptLinesResponse.builder()
                .id(id)
                .inboundReceiptId("receipt-1")
                .purchaseOrderLineId("pol-1")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Product 1")
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .locationId("loc-1")
                .locationCode("A-01-01")
                .locationName("Inbound Staging A1")
                .lineNumber(lineNumber)
                .quantityReceived(new BigDecimal("5.00"))
                .qualityStatus(qualityStatus)
                .notes("test")
                .createdAt(LocalDateTime.parse("2026-03-15T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-03-15T10:05:00"))
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
