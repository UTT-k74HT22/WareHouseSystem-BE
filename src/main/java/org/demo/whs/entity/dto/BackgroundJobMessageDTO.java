package org.demo.whs.entity.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.BackgroundJobType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobMessageDTO {

    private String jobId;
    private String jobCode;
    private BackgroundJobType jobType;
    private String businessType;
    private String requestedBy;
    private LocalDateTime createdAt;
}
