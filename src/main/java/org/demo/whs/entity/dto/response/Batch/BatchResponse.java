package org.demo.whs.entity.dto.response.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder(toBuilder = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchResponse {

    private String id;
    private String batchNumber;
    private String productId;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String supplierBatchNumber;
    private String notes;
    private BatchStatus status;
    private BigDecimal totalOnHandQuantity;
    private BigDecimal totalQuarantineQuantity;
    private BigDecimal totalReservedQuantity;
    private BigDecimal totalAvailableQuantity;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
