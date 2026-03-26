package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.OutboundShipmentsService;
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

@WebMvcTest(OutboundShipmentsController.class)
@ActiveProfiles("test")
@Import({OutboundShipmentsControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@WithMockUser(roles = {"ADMIN", "MANAGER"})
class OutboundShipmentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OutboundShipmentsService outboundShipmentsService;

    @MockitoBean
    private RateLimitService rateLimitService;

    // ==================== CREATE ====================

    @Test
    @DisplayName("Should create outbound shipment when request is valid")
    void should_CreateOutboundShipment_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsService.create(any(OutboundShipmentsRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/outbound-shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1",
                                "warehouse_id", "wh-1",
                                "shipment_date", "2026-03-22",
                                "carrier", "GHN",
                                "notes", "Test shipment"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        verify(outboundShipmentsService).create(any(OutboundShipmentsRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when create request has missing required fields")
    void should_Return400_When_CreateRequestHasMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/outbound-shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 401 when create is called without authentication")
    void should_Return401_When_CreateWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/outbound-shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sales_order_id", "so-1",
                                "warehouse_id", "wh-1",
                                "shipment_date", "2026-03-22"
                        ))))
                .andExpect(status().isUnauthorized());
    }

    // ==================== GET BY ID ====================

    @Test
    @DisplayName("Should get outbound shipment by id")
    void should_GetOutboundShipmentById_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsService.getById("ship-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/outbound-shipments/{id}", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(outboundShipmentsService).getById("ship-1");
    }

    // ==================== GET ALL ====================

    @Test
    @DisplayName("Should get all outbound shipments when no filter is provided")
    void should_GetAllOutboundShipments_When_NoFilterProvided() throws Exception {
        PageResponse<OutboundShipmentsResponse> pageResponse = buildPageResponse(
                buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.DRAFT), 0, 10);

        when(outboundShipmentsService.getAll(any(OutboundShipmentsFilterRequest.class), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/outbound-shipments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.content[0].id").value("ship-1"));

        verify(outboundShipmentsService).getAll(any(OutboundShipmentsFilterRequest.class), any());
    }

    @Test
    @DisplayName("Should filter outbound shipments when filters are provided")
    void should_FilterOutboundShipments_When_FiltersAreProvided() throws Exception {
        PageResponse<OutboundShipmentsResponse> pageResponse = buildPageResponse(
                buildResponse("ship-2", "SHIP-002", OutboundShipmentsStatus.PICKING), 0, 5);

        when(outboundShipmentsService.getAll(any(OutboundShipmentsFilterRequest.class), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/outbound-shipments")
                        .param("shipment_number", "SHIP-002")
                        .param("sales_order_id", "so-1")
                        .param("warehouse_id", "wh-1")
                        .param("status", "PICKING")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.content[0].status").value("PICKING"));

        verify(outboundShipmentsService).getAll(any(OutboundShipmentsFilterRequest.class), any());
    }

    // ==================== UPDATE ====================

    @Test
    @DisplayName("Should update outbound shipment when request is valid")
    void should_UpdateOutboundShipment_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.DRAFT);

        when(outboundShipmentsService.update(eq("ship-1"), any(UpdateOutboundShipmentsRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}", "ship-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "shipment_date", "2026-03-25",
                                "carrier", "GHTK",
                                "notes", "Updated notes"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"));

        verify(outboundShipmentsService).update(eq("ship-1"), any(UpdateOutboundShipmentsRequest.class));
    }

    // ==================== START PICKING ====================

    @Test
    @DisplayName("Should start picking for outbound shipment")
    void should_StartPicking_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.PICKING);

        when(outboundShipmentsService.startPicking("ship-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}/start-picking", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"))
                .andExpect(jsonPath("$.data.status").value("PICKING"));

        verify(outboundShipmentsService).startPicking("ship-1");
    }

    // ==================== MARK AS PACKED ====================

    @Test
    @DisplayName("Should mark outbound shipment as packed")
    void should_MarkAsPacked_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.PACKED);

        when(outboundShipmentsService.markAsPacked("ship-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}/mark-as-packed", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"))
                .andExpect(jsonPath("$.data.status").value("PACKED"));

        verify(outboundShipmentsService).markAsPacked("ship-1");
    }

    // ==================== SHIP ====================

    @Test
    @DisplayName("Should ship outbound shipment")
    void should_Ship_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.STAGING);

        when(outboundShipmentsService.ship("ship-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}/ship", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"))
                .andExpect(jsonPath("$.data.status").value("STAGING"));

        verify(outboundShipmentsService).ship("ship-1");
    }

    @Test
    @DisplayName("Should confirm dispatch for outbound shipment")
    void should_ConfirmDispatch_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.SHIPPED);

        when(outboundShipmentsService.confirmDispatch("ship-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}/confirm-dispatch", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        verify(outboundShipmentsService).confirmDispatch("ship-1");
    }

    // ==================== CANCEL ====================

    @Test
    @DisplayName("Should cancel outbound shipment")
    void should_Cancel_When_RequestIsValid() throws Exception {
        OutboundShipmentsResponse response = buildResponse("ship-1", "SHIP-001", OutboundShipmentsStatus.CANCELLED);

        when(outboundShipmentsService.cancel("ship-1")).thenReturn(response);

        mockMvc.perform(put("/api/v1/outbound-shipments/{id}/cancel", "ship-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("ship-1"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        verify(outboundShipmentsService).cancel("ship-1");
    }

    // ==================== HELPER METHODS ====================

    private OutboundShipmentsResponse buildResponse(String id, String shipmentNumber, OutboundShipmentsStatus status) {
        OutboundShipmentsResponse response = new OutboundShipmentsResponse();
        response.setId(id);
        response.setShipmentNumber(shipmentNumber);
        response.setSalesOrderId("so-1");
        response.setWarehouseId("wh-1");
        response.setShipmentDate(LocalDate.of(2026, 3, 22));
        response.setStatus(status);
        response.setCarrier("GHN");
        response.setNotes("Test shipment");
        response.setCreatedAt(LocalDateTime.of(2026, 3, 22, 10, 0, 0));
        response.setUpdatedAt(LocalDateTime.of(2026, 3, 22, 11, 0, 0));
        return response;
    }

    private PageResponse<OutboundShipmentsResponse> buildPageResponse(OutboundShipmentsResponse response, int page, int size) {
        org.springframework.data.domain.PageImpl<OutboundShipmentsResponse> springPage = 
                new org.springframework.data.domain.PageImpl<>(List.of(response), 
                        org.springframework.data.domain.PageRequest.of(page, size), 1);
        return PageResponse.from(springPage, List.of(response));
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
