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
import java.util.List;

/**
 * Detailed response DTO for a background job.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobDetailResponse {

    private String id;
    private String jobCode;
    private BackgroundJobType jobType;
    private String businessType;
    private BackgroundJobStatus status;
    private String currentStep;
    private String requestedBy;
    private String requestPayload;
    private String requestHash;
    private Integer progressPercent;
    private Long processedRows;
    private Long totalRows;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String createdBy;
    private String updatedBy;
    private Boolean cancellable;
    private Boolean retryable;
    private BackgroundJobFileResponse file;
    private List<BackgroundJobStepLogResponse> stepLogs;
}
