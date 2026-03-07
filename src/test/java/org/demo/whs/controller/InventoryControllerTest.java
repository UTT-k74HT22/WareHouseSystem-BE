package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@WithMockUser
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    @MockBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should return 200 when request is valid")
    void shouldReturn200WhenValidRequest() throws Exception {

        PageResponse<InventoryResponse> mockPage =
                PageResponse.<InventoryResponse>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0L)
                        .totalPages(0)
                        .isFirst(true)
                        .isLast(true)
                        .build();

        when(inventoryService.getInventories(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(PageRequest.class)
        )).thenReturn(mockPage);

        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "updatedAt")
                        .param("direction", "ASC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("Should return 400 when sort field invalid")
    void shouldReturn400WhenInvalidSortField() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "abcxyz")
                        .param("direction", "ASC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when direction invalid")
    void shouldReturn400WhenInvalidDirection() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "updatedAt")
                        .param("direction", "HELLO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when page is negative")
    void shouldReturn400WhenPageNegative() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_006"));
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void shouldReturn400WhenSizeIsZero() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_007"));
    }

    @Test
    @DisplayName("Should return 400 when size exceeds max limit")
    void shouldReturn400WhenSizeTooLarge() throws Exception {

        mockMvc.perform(get("/api/v1/inventories")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_008"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 and inventory summary when productId is valid")
    void shouldReturn200AndSummaryWhenProductIdIsValid() throws Exception {
        String productId = "550e8400-e29b-41d4-a716-446655440000";
        InventorySummaryResponse mockResponse = InventorySummaryResponse.builder()
                .productId(productId)
                .productSku("SKU001")
                .productName("Product 001")
                .totalOnHandQuantity(new BigDecimal("100.00"))
                .totalReservedQuantity(new BigDecimal("20.00"))
                .warehouseCount(2L)
                .locationCount(5L)
                .build();

        when(inventoryService.getSummaryByProduct(productId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/inventories/summary/{productId}", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_id").value(productId))
                .andExpect(jsonPath("$.data.total_on_hand_quantity").value(100.00))
                .andExpect(jsonPath("$.data.warehouse_count").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when productId format is invalid")
    void shouldReturn400WhenProductIdIsInvalid() throws Exception {
        String invalidProductId = "invalid-uuid";

        mockMvc.perform(get("/api/v1/inventories/summary/{productId}", invalidProductId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 and inventory by location when requested")
    void shouldReturn200AndInventoryByLocation() throws Exception {
        InventoryByLocationResponse mockResponse = InventoryByLocationResponse.builder()
                .locationId("loc-1")
                .locationCode("LOC-001")
                .locationName("Location 001")
                .warehouseId("wh-1")
                .warehouseName("Warehouse 001")
                .items(List.of(
                        LocationInventoryItemResponse.builder()
                                .productId("prod-1")
                                .productSku("SKU-001")
                                .productName("Product 001")
                                .onHandQuantity(new BigDecimal("100.00"))
                                .reservedQuantity(BigDecimal.ZERO)
                                .availableQuantity(new BigDecimal("100.00"))
                                .build()
                ))
                .build();

        when(inventoryService.getInventoryByLocation(ArgumentMatchers.any(InventoryFilterRequest.class)))
                .thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/v1/inventories/by-location")
                        .param("warehouseId", "wh-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].location_id").value("loc-1"))
                .andExpect(jsonPath("$.data[0].location_code").value("LOC-001"))
                .andExpect(jsonPath("$.data[0].items[0].product_sku").value("SKU-001"))
                .andExpect(jsonPath("$.data[0].items[0].on_hand_quantity").value(100.00));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 200 when checking availability is successful")
    void shouldReturn200WhenCheckAvailabilityIsSuccessful() throws Exception {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId("prod-1")
                .quantity(new BigDecimal("10.00"))
                .build();

        CheckAvailabilityResponse mockResponse = CheckAvailabilityResponse.builder()
                .productId("prod-1")
                .requestedQuantity(new BigDecimal("10.00"))
                .availableQuantity(new BigDecimal("100.00"))
                .isAvailable(true)
                .message("Stock available")
                .build();

        when(inventoryService.checkAvailability(ArgumentMatchers.any(CheckAvailabilityRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/inventories/check-availability")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_id").value("prod-1"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.available_quantity").value(100.00))
                .andExpect(jsonPath("$.data.message").value("Stock available"));
    }

    @Test
    @WithMockUser
    @DisplayName("Should return 400 when check availability request is invalid")
    void shouldReturn400WhenCheckAvailabilityRequestIsInvalid() throws Exception {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                // productId is missing
                .quantity(new BigDecimal("-1.00")) // negative quantity
                .build();

        mockMvc.perform(post("/api/v1/inventories/check-availability")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }
}
