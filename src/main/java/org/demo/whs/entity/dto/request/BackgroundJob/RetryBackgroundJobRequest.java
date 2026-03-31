package org.demo.whs.entity.dto.request.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for retrying a background job.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RetryBackgroundJobRequest {

    @Size(max = 500, message = "Retry reason must not exceed 500 characters")
    private String reason;
}
