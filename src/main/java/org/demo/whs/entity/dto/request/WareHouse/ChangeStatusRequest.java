package org.demo.whs.entity.dto.request.WareHouse;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.demo.whs.entity.enums.WareHouseStatus;

/**
 * Request DTO for changing warehouse status.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChangeStatusRequest {

    @NotNull(message = "Status is required")
    private WareHouseStatus status;
}
