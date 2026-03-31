package org.demo.whs.entity.dto.request.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.enums.BackgroundJobType;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Request DTO for filtering background jobs in list/search endpoints.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobFilterRequest {

    @Size(max = 36, message = "Requested by must not exceed 36 characters")
    private String requestedBy;

    private Set<BackgroundJobType> jobTypes;

    private Set<BackgroundJobStatus> statuses;

    @Size(max = 100, message = "Business type must not exceed 100 characters")
    private String businessType;

    @Size(max = 64, message = "Job code must not exceed 64 characters")
    private String jobCode;

    private LocalDateTime createdFrom;

    private LocalDateTime createdTo;

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 200, message = "Size must not exceed 200")
    private Integer size = 20;
}
