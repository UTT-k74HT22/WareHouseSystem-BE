package org.demo.whs.entity.dto.request.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO for updating batch information.
 * All fields are optional.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateBatchRequest {

    /**
     * Batch number (optional)
     */
    @Size(max = 50, message = "Batch number must not exceed 50 characters")
    private String batchNumber;

    /**
     * Manufacturing date (optional)
     */
    private LocalDate manufacturingDate;

    /**
     * Expiry date (optional)
     */
    private LocalDate expiryDate;

    /**
     * Supplier batch number (optional)
     */
    @Size(max = 50)
    private String supplierBatchNumber;

    /**
     * Notes (optional)
     */
    @Size(max = 1000)
    private String notes;
}
