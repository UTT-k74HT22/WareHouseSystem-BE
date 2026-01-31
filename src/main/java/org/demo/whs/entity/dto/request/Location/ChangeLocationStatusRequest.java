package org.demo.whs.entity.dto.request.Location;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.LocationStatus;

/**
 * Request DTO for changing location status.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChangeLocationStatusRequest {

    @NotNull(message = "Status is required")
    private LocationStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
