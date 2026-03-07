package org.demo.whs.mapper;

import org.demo.whs.entity.dto.request.Inventory.CheckAvailabilityRequest;
import org.demo.whs.entity.dto.response.Inventory.CheckAvailabilityResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryMapper Unit Tests")
class InventoryMapperTest {

    private final InventoryMapper inventoryMapper = new InventoryMapper();

    @Test
    @DisplayName("should_SetAvailabilityFlag_When_MappingCheckAvailabilityResponse")
    void should_SetAvailabilityFlag_When_MappingCheckAvailabilityResponse() {
        CheckAvailabilityRequest request = CheckAvailabilityRequest.builder()
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .quantity(new BigDecimal("10.00"))
                .build();

        CheckAvailabilityResponse response = inventoryMapper.toCheckAvailabilityResponse(
                request,
                new BigDecimal("25.00"),
                true
        );

        assertThat(response.isAvailable()).isTrue();
        assertThat(response.getAvailableQuantity()).isEqualByComparingTo("25.00");
        assertThat(response.getRequestedQuantity()).isEqualByComparingTo("10.00");
        assertThat(response.getMessage()).isEqualTo("Stock available");
    }
}
