package org.demo.whs.entity.dto.request.SalesOrders;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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
public class SalesOrdersFilterRequest {

    private String soNumber;

    private String customerId;

    private String warehouseId;

    private String status;

    private LocalDate orderDateFrom;

    private LocalDate orderDateTo;

    private LocalDate requestedDeliveryDateFrom;

    private LocalDate requestedDeliveryDateTo;
}
