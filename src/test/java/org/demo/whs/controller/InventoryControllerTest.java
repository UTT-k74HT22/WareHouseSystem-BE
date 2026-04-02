package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryReserveRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryUnreserveRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryByLocationResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryReserveResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventorySummaryResponse;
import org.demo.whs.entity.dto.response.Inventory.InventoryUnreserveResponse;
import org.demo.whs.entity.dto.response.Inventory.LocationInventoryItemResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.service.InventoryService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
@WithMockUser(authorities = {
        "PERM_INVENTORY_READ",
        "PERM_INVENTORY_RESERVATION_UPDATE",
        "PERM_INVENTORY_MUTATION_UPDATE"
})
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
                any(),
                any(PageRequest.class)
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
        // Since the validation is likely in the service or a dedicated validator, 
        // we mock the service to throw if it's called with invalid parameters.
        // Or if it's handled by Spring's parameter binding, it might fail earlier.
        // Assuming it's in the service:
        when(inventoryService.getInventories(any(), any(Pageable.class)))
                .thenThrow(new BadRequestException(ErrorCode.COM_001));

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "abcxyz")
                        .param("direction", "ASC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when direction invalid")
    void shouldReturn400WhenInvalidDirection() throws Exception {
        when(inventoryService.getInventories(any(), any(Pageable.class)))
                .thenThrow(new BadRequestException(ErrorCode.COM_001));

        mockMvc.perform(get("/api/v1/inventories")
                        .param("sortBy", "updatedAt")
                        .param("direction", "HELLO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 400 when page is negative")
    void shouldReturn400WhenPageNegative() throws Exception {
        when(inventoryService.getInventories(any(), any(Pageable.class)))
                .thenThrow(new BadRequestException(ErrorCode.COM_006));

        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_006"));
    }

    @Test
    @DisplayName("Should return 400 when size is zero")
    void shouldReturn400WhenSizeIsZero() throws Exception {
        when(inventoryService.getInventories(any(), any(Pageable.class)))
                .thenThrow(new BadRequestException(ErrorCode.COM_007));

        mockMvc.perform(get("/api/v1/inventories")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_007"));
    }

    @Test
    @DisplayName("Should return 400 when size exceeds max limit")
    void shouldReturn400WhenSizeTooLarge() throws Exception {
        when(inventoryService.getInventories(any(), any(Pageable.class)))
                .thenThrow(new BadRequestException(ErrorCode.COM_008));

        mockMvc.perform(get("/api/v1/inventories")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_008"));
    }

    @Test
    @DisplayName("Should return 200 and inventory summary when productId is valid")
    void shouldReturn200AndSummaryWhenProductIdIsValid() throws Exception {
        String productId = "550e8400-e29b-41d4-a716-446655440000";
        InventorySummaryResponse mockResponse = InventorySummaryResponse.builder()
                .productId(productId)
                .productSku("SKU001")
                .productName("Product 001")
                .totalOnHandQuantity(new BigDecimal("100.00"))
                .totalQuarantineQuantity(new BigDecimal("10.00"))
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
                .andExpect(jsonPath("$.data.total_available_quantity").value(70.00))
                .andExpect(jsonPath("$.data.warehouse_count").value(2));
    }

    @Test
    @DisplayName("Should return 400 when productId format is invalid")
    void shouldReturn400WhenProductIdIsInvalid() throws Exception {
        String invalidProductId = "invalid-uuid";
        
        when(inventoryService.getSummaryByProduct(invalidProductId))
                .thenThrow(new BadRequestException(ErrorCode.COM_001));

        mockMvc.perform(get("/api/v1/inventories/summary/{productId}", invalidProductId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
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

        when(inventoryService.getInventoryByLocation(any(InventoryFilterRequest.class)))
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

        when(inventoryService.checkAvailability(any(CheckAvailabilityRequest.class)))
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

    @Test
    @DisplayName("Should return 200 when reservation is successful")
    void shouldReturn200WhenReservationIsSuccessful() throws Exception {
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                .orderLineId("OL-1")
                .warehouseId("wh-1")
                .productId("prod-1")
                .quantity(new BigDecimal("10.00"))
                .build();

        InventoryReserveResponse mockResponse = InventoryReserveResponse.builder()
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .reservedQuantity(new BigDecimal("10.00"))
                .status("RESERVED")
                .build();

        when(inventoryService.reserve(any(InventoryReserveRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/inventories/reserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inventory_id").value("inv-1"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"));
    }

    @Test
    @DisplayName("Should return 400 when reservation request is invalid")
    void shouldReturn400WhenReservationRequestIsInvalid() throws Exception {
        InventoryReserveRequest request = InventoryReserveRequest.builder()
                // productId and warehouseId are missing
                .quantity(new BigDecimal("-5.00"))
                .build();

        mockMvc.perform(post("/api/v1/inventories/reserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 200 when unreservation is successful")
    void shouldReturn200WhenUnreservationIsSuccessful() throws Exception {
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .orderLineId("OL-1001")
                .quantity(new BigDecimal("10.00"))
                .build();

        InventoryUnreserveResponse mockResponse = InventoryUnreserveResponse.builder()
                .reservationId("res-1")
                .inventoryId("inv-1")
                .productId("prod-1")
                .unreservedQuantity(new BigDecimal("10.00"))
                .remainingReservedQuantity(BigDecimal.ZERO)
                .orderLineId("OL-1001")
                .status("RELEASED")
                .unreservedAt(LocalDateTime.now())
                .build();

        when(inventoryService.unreserve(any(InventoryUnreserveRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/inventories/unreserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reservation_id").value("res-1"))
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
    }

    @Test
    @DisplayName("Should return 400 when unreservation request is invalid")
    void shouldReturn400WhenUnreservationRequestIsInvalid() throws Exception {
        InventoryUnreserveRequest request = InventoryUnreserveRequest.builder()
                // productId and warehouseId are missing
                .quantity(new BigDecimal("-5.00"))
                .build();

        mockMvc.perform(post("/api/v1/inventories/unreserve")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should return 200 when increase is successful")
    void shouldReturn200WhenIncreaseIsSuccessful() throws Exception {
        InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("10.00"))
                .referenceType(org.demo.whs.entity.enums.ReferenceType.INBOUND_RECEIPT)
                .referenceId("ref-uuid")
                .referenceNumber("REC-001")
                .notes("Test notes")
                .build();

        InventoryResponse mockResponse = InventoryResponse.builder()
                .id("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("110.00"))
                .build();

        when(inventoryService.increase(any(InventoryIncreaseRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/inventories/increase")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("inv-1"))
                .andExpect(jsonPath("$.data.on_hand_quantity").value(110.00));
    }

    @Test
    @DisplayName("Should return 200 when decrease is successful")
    void shouldReturn200WhenDecreaseIsSuccessful() throws Exception {
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .quantity(new BigDecimal("10.00"))
                .referenceType(org.demo.whs.entity.enums.ReferenceType.OUTBOUND_SHIPMENT)
                .referenceId("ref-uuid")
                .referenceNumber("SHIP-001")
                .consumeReserved(false)
                .build();

        InventoryResponse mockResponse = InventoryResponse.builder()
                .id("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .onHandQuantity(new BigDecimal("90.00"))
                .reservedQuantity(new BigDecimal("10.00"))
                .build();

        when(inventoryService.decrease(any(org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/inventories/decrease")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("inv-1"))
                .andExpect(jsonPath("$.data.on_hand_quantity").value(90.00));
    }

    @Test
    @DisplayName("Should return 400 when decrease request is invalid")
    void shouldReturn400WhenDecreaseRequestIsInvalid() throws Exception {
        org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest request = org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest.builder()
                // Missing fields
                .quantity(new BigDecimal("-5.00"))
                .build();

        mockMvc.perform(post("/api/v1/inventories/decrease")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }
}
