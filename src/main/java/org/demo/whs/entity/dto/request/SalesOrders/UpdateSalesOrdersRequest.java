package org.demo.whs.entity.dto.request.SalesOrders;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateSalesOrdersRequest {

    private String customerId;

    private String warehouseId;

    private LocalDate orderDate;

    @FutureOrPresent(message = "Requested delivery date cannot be in the past")
    private LocalDate requestedDeliveryDate;

    @Pattern(regexp = "^(VND|USD|EUR|JPY)$", message = "Currency must be one of: VND, USD, EUR, JPY")
    private String currency;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
