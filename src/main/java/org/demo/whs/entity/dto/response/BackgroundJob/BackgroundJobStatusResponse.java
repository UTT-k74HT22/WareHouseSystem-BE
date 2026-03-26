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
 * Focused response DTO for polling job progress/status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobStatusResponse {

    private String id;
    private String jobCode;
    private BackgroundJobStatus status;
    private String currentStep;
    private Integer progressPercent;
    private Long processedRows;
    private Long totalRows;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Boolean cancellable;
    private Boolean retryable;
    private Boolean downloadAvailable;
    private BackgroundJobFileResponse file;
}
