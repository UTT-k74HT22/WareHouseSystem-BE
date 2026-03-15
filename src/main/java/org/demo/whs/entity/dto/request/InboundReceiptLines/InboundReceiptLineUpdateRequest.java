package org.demo.whs.entity.dto.request.InboundReceiptLines;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.QualityStatus;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InboundReceiptLineUpdateRequest {

    @NotBlank(message = "Location ID is required")
    private String locationId;

    private String batchId;

    @NotNull(message = "Quantity received is required")
    @DecimalMin(value = "0.01", message = "Quantity received must be greater than 0")
    private BigDecimal quantityReceived;

    private QualityStatus qualityStatus;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
