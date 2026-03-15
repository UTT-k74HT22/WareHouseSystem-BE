package org.demo.whs.entity.dto.request.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Request DTO for quarantining a batch.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class QuarantineBatchRequest {

    @NotBlank(message = "Quarantine reason is required")
    @Size(max = 500, message = "Quarantine reason must not exceed 500 characters")
    private String reason;

    private LocalDate expectedResolutionDate;

    private Boolean notifyManager = Boolean.TRUE;
}
