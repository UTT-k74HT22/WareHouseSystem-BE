package org.demo.whs.entity.dto.request.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for releasing a batch from quarantine.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ReleaseBatchRequest {

    @NotBlank(message = "Release notes are required")
    @Size(max = 1000, message = "Release notes must not exceed 1000 characters")
    private String releaseNotes;
}
