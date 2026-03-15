package org.demo.whs.entity.dto.response.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.BatchStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchExpiringResponse {

    private String batchId;
    private String batchNumber;
    private String productId;
    private String productSku;
    private String productName;
    private BatchStatus status;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private Long daysToExpiry;
    private String urgency;
    private BatchInventorySnapshotResponse inventorySnapshot;
}
