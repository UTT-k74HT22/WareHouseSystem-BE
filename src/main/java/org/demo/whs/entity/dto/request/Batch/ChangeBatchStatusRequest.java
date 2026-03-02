package org.demo.whs.entity.dto.request.Batch;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.BatchStatus;

/**
 * Request DTO for changing batch status.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChangeBatchStatusRequest {

    @NotNull(message = "Status is required")
    private BatchStatus status;

}
