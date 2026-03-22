package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.OutboundShipmentLinesService;
import org.demo.whs.service.RateLimitService;
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

@WebMvcTest(OutboundShipmentLinesController.class)
@ActiveProfiles("test")
@Import({OutboundShipmentLinesControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser(roles = {"ADMIN", "MANAGER"})
class OutboundShipmentLinesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OutboundShipmentLinesService outboundShipmentLinesService;

    @MockitoBean
    private RateLimitService rateLimitService;

    // ==================== CREATE ====================

    @Test
    @DisplayName("Should create outbound shipment line when request is valid")
    void should_CreateOutboundShipmentLine_When_RequestIsValid() throws Exception {
        OutboundShipmentLinesResponse response = buildResponse("line-1", "ship-1");

        when(outboundShipmentLinesService.create(any(OutboundShipmentLinesRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/outbound-shipment-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "outbound_shipment_id", "ship-1",
                                "sales_order_line_id", "so-line-1",
                                "product_id", "prod-1",
                                "location_id", "loc-1",
                                "quantity_shipped", 5.00,
                                "notes", "Pick note"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(outboundShipmentLinesService).create(any(OutboundShipmentLinesRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void should_Return400_When_CreateRequestHasMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/outbound-shipment-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "outbound_shipment_id", "ship-1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when quantity shipped is zero or negative")
    void should_Return400_When_QuantityShippedIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/outbound-shipment-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "outbound_shipment_id", "ship-1",
                                "sales_order_line_id", "so-line-1",
                                "product_id", "prod-1",
                                "location_id", "loc-1",
                                "quantity_shipped", 0
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/outbound-shipment-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "outbound_shipment_id", "ship-1",
                                "sales_order_line_id", "so-line-1",
                                "product_id", "prod-1",
                                "location_id", "loc-1",
                                "quantity_shipped", 5.00
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET BY SHIPMENT ID ====================

    @Test
    @DisplayName("Should get all lines for a specific outbound shipment")
    void should_GetLinesByShipmentId_When_RequestIsValid() throws Exception {
        OutboundShipmentLinesResponse response = buildResponse("line-1", "ship-1");

        when(outboundShipmentLinesService.getByShipmentId("ship-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/outbound-shipment-lines/shipment/{shipmentId}", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(outboundShipmentLinesService).getByShipmentId("ship-1");
    }

    @Test
    @DisplayName("Should return empty list when shipment has no lines")
    void should_ReturnEmptyList_When_ShipmentHasNoLines() throws Exception {
        when(outboundShipmentLinesService.getByShipmentId("ship-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/outbound-shipment-lines/shipment/{shipmentId}", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(outboundShipmentLinesService).getByShipmentId("ship-1");
    }

    // ==================== GET BY ID ====================

    @Test
    @DisplayName("Should get outbound shipment line by id")
    void should_GetOutboundShipmentLineById_When_RequestIsValid() throws Exception {
        OutboundShipmentLinesResponse response = buildResponse("line-1", "ship-1");

        when(outboundShipmentLinesService.getById("line-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/outbound-shipment-lines/{id}", "line-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(outboundShipmentLinesService).getById("line-1");
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("Should update outbound shipment line when request is valid")
    void should_UpdateOutboundShipmentLine_When_RequestIsValid() throws Exception {
        OutboundShipmentLinesResponse response = buildResponse("line-1", "ship-1");

        when(outboundShipmentLinesService.update(eq("line-1"), any(UpdateOutboundShipmentLinesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipment-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity_shipped", 8.00,
                                "location_id", "loc-2",
                                "notes", "Updated note"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"));

        verify(outboundShipmentLinesService).update(eq("line-1"), any(UpdateOutboundShipmentLinesRequest.class));
    }

    @Test
    @DisplayName("Should update with partial request - only quantity")
    void should_UpdateWithPartialRequest_When_OnlyQuantityProvided() throws Exception {
        OutboundShipmentLinesResponse response = buildResponse("line-1", "ship-1");

        when(outboundShipmentLinesService.update(eq("line-1"), any(UpdateOutboundShipmentLinesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipment-lines/{id}", "line-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "quantity_shipped", 10.00
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("line-1"));

        verify(outboundShipmentLinesService).update(eq("line-1"), any(UpdateOutboundShipmentLinesRequest.class));
    }

    // ==================== REMOVE ====================

    @Test
    @DisplayName("Should remove outbound shipment line")
    void should_RemoveOutboundShipmentLine_When_RequestIsValid() throws Exception {
        mockMvc.perform(delete("/api/v1/outbound-shipment-lines/{id}", "line-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Outbound shipment line removed successfully"));

        verify(outboundShipmentLinesService).remove("line-1");
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipmentLinesResponse buildResponse(String id, String shipmentId) {
        OutboundShipmentLinesResponse response = new OutboundShipmentLinesResponse();
        response.setId(id);
        response.setOutboundShipmentId(shipmentId);
        response.setSalesOrderLineId("so-line-1");
        response.setProductId("prod-1");
        response.setSku("SKU-001");
        response.setProductName("Product A");
        response.setLocationId("loc-1");
        response.setLocationName("Location A");
        response.setBatchId(null);
        response.setBatchNumber(null);
        response.setLineNumber(1);
        response.setQuantityShipped(new BigDecimal("5.00"));
        response.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return response;
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
