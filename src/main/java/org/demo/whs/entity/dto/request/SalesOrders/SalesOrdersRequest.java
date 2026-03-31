package org.demo.whs.entity.dto.request.SalesOrders;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.dto.request.SalesOrderLines.SalesOrderLinesRequest;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SalesOrdersRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Warehouse ID is required")
    private String warehouseId;

    @NotNull(message = "Order date is required")
    private LocalDate orderDate;

    @FutureOrPresent(message = "Requested delivery date cannot be in the past")
    @NotNull(message = "Requested delivery date is required")
    private LocalDate requestedDeliveryDate;

    @NotBlank(message = "Currency is required")
    // NOTE: Keep in sync with CurrencyType enum
    @Pattern(regexp = "^(VND|USD|EUR|JPY)$", message = "Currency must be one of: VND, USD, EUR, JPY")
    private String currency;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one sales order line is required")
    @Valid
    private List<SalesOrderLinesRequest> lines;
}
