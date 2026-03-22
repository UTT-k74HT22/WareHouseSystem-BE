package org.demo.whs.entity.dto.response.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.enums.BackgroundJobType;

import java.time.LocalDateTime;

/**
 * Lightweight response DTO for listing background jobs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobSummaryResponse {

    private String id;
    private String jobCode;
    private BackgroundJobType jobType;
    private String businessType;
    private BackgroundJobStatus status;
    private String currentStep;
    private Integer progressPercent;
    private Long processedRows;
    private Long totalRows;
    private String requestedBy;
    private String resultFileName;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Boolean cancellable;
    private Boolean retryable;
}
