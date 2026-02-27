package org.demo.whs.entity.dto.request.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import lombok.Setter;
import org.demo.whs.entity.enums.BatchStatus;

import java.time.LocalDate;

/**
 * Request DTO for creating a new batch.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateBatchRequest {

    @NotBlank(message = "Batch number is required")
    @Size(max = 50)
    private String batchNumber;

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Manufacturing date is required")
    private LocalDate manufactureDate;

    private LocalDate expiryDate;

    @Size(max = 50)
    private String supplierBatchNumber;

    private String notes;

    @NotNull(message = "Status is required")
    private BatchStatus status;
}
