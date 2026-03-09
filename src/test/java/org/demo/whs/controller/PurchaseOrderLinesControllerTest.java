package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.PurchaseOrderLinesService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseOrderLinesController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class PurchaseOrderLinesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PurchaseOrderLinesService purchaseOrderLinesService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("should_GetPurchaseOrderLines_When_PurchaseOrderIdExists")
    void should_GetPurchaseOrderLines_When_PurchaseOrderIdExists() throws Exception {
        String purchaseOrderId = "PO-001";
        PurchaseOrderLinesResponse line = PurchaseOrderLinesResponse.builder()
                .id("POL-001")
                .purchaseOrderId(purchaseOrderId)
                .productId("PROD-001")
                .quantityOrdered(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("100.00"))
                .lineTotal(new BigDecimal("1000.00"))
                .build();

        when(purchaseOrderLinesService.getByPurchaseOrderId(purchaseOrderId)).thenReturn(List.of(line));

        mockMvc.perform(get("/api/v1/purchase-order-lines/purchase-order/{purchaseOrderId}", purchaseOrderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("POL-001"))
                .andExpect(jsonPath("$.data[0].purchase_order_id").value(purchaseOrderId));
    }

    @Test
    @DisplayName("should_CreatePurchaseOrderLine_When_RequestIsValid")
    void should_CreatePurchaseOrderLine_When_RequestIsValid() throws Exception {
        PurchaseOrderLinesRequest request = PurchaseOrderLinesRequest.builder()
                .purchaseOrderId("PO-001")
                .productId("PROD-001")
                .quantityOrdered(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("100.00"))
                .notes("New line")
                .build();

        PurchaseOrderLinesResponse response = PurchaseOrderLinesResponse.builder()
                .id("POL-001")
                .purchaseOrderId("PO-001")
                .productId("PROD-001")
                .quantityOrdered(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        when(purchaseOrderLinesService.create(any(PurchaseOrderLinesRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/purchase-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("POL-001"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_CreateRequestIsInvalid")
    void should_ReturnBadRequest_When_CreateRequestIsInvalid() throws Exception {
        PurchaseOrderLinesRequest request = PurchaseOrderLinesRequest.builder()
                .purchaseOrderId("") // Blank
                .productId("PROD-001")
                .quantityOrdered(new BigDecimal("-1.00")) // Invalid
                .unitPrice(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/api/v1/purchase-order-lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_UpdatePurchaseOrderLine_When_RequestIsValid")
    void should_UpdatePurchaseOrderLine_When_RequestIsValid() throws Exception {
        String id = "POL-001";
        UpdatePurchaseOrderLinesRequest request = UpdatePurchaseOrderLinesRequest.builder()
                .productId("PROD-002")
                .quantityOrdered(new BigDecimal("20.00"))
                .unitPrice(new BigDecimal("150.00"))
                .notes("Updated line")
                .build();

        PurchaseOrderLinesResponse response = PurchaseOrderLinesResponse.builder()
                .id(id)
                .productId("PROD-002")
                .quantityOrdered(new BigDecimal("20.00"))
                .unitPrice(new BigDecimal("150.00"))
                .build();

        when(purchaseOrderLinesService.update(eq(id), any(UpdatePurchaseOrderLinesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/purchase-order-lines/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_id").value("PROD-002"));
    }

    @Test
    @DisplayName("should_ReturnNotFound_When_UpdateLineDoesNotExist")
    void should_ReturnNotFound_When_UpdateLineDoesNotExist() throws Exception {
        String id = "NON-EXISTENT";
        UpdatePurchaseOrderLinesRequest request = UpdatePurchaseOrderLinesRequest.builder()
                .quantityOrdered(new BigDecimal("20.00"))
                .build();

        when(purchaseOrderLinesService.update(eq(id), any(UpdatePurchaseOrderLinesRequest.class)))
                .thenThrow(new NotFoundException("Purchase order line not found", ErrorCode.POL_001));

        mockMvc.perform(put("/api/v1/purchase-order-lines/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("POL_001"));
    }

    @Test
    @DisplayName("should_DeletePurchaseOrderLine_When_IdExists")
    void should_DeletePurchaseOrderLine_When_IdExists() throws Exception {
        String id = "POL-001";
        doNothing().when(purchaseOrderLinesService).delete(id);

        mockMvc.perform(delete("/api/v1/purchase-order-lines/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
