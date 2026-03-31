package org.demo.whs.entity.dto.response.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Response DTO describing the generated file of a background job.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobFileResponse {

    private String fileName;
    private String storageObjectKey;
    private String mimeType;
    private Long fileSize;
    private String downloadUrl;
    private Instant downloadUrlExpiresAt;
}
