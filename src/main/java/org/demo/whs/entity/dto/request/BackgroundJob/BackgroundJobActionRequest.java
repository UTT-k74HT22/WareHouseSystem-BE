package org.demo.whs.entity.dto.request.BackgroundJob;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Shared request DTO for background job actions (retry / cancel).
 * Body is optional; only a reason can be supplied.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BackgroundJobActionRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
