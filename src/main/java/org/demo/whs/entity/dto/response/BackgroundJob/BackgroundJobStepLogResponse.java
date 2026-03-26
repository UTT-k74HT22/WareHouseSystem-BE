package org.demo.whs.entity.dto.response.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.BackgroundJobStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for a single background job step log entry.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobStepLogResponse {

    private String id;
    private String stepName;
    private BackgroundJobStatus status;
    private String message;
    private Integer progressPercent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
