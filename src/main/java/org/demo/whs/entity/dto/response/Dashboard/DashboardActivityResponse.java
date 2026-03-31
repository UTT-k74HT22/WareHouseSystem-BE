package org.demo.whs.entity.dto.response.Dashboard;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DashboardActivityResponse {

    private String movementId;
    private String movementType;
    private String warehouseName;
    private String locationName;
    private String productSku;
    private String productName;
    private String referenceNumber;
    private BigDecimal quantityChange;
    private LocalDateTime movementDate;
}
