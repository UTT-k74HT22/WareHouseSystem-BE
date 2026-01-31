package org.demo.whs.entity.dto.request.Location;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.Getter;
import org.demo.whs.entity.enums.LocationType;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing location.
 * All fields are optional - user can update or leave blank.
 * Warehouse ID and code cannot be changed after creation.
 */
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateLocationRequest {

    @Size(max = 100, message = "Location name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Zone must not exceed 50 characters")
    private String zone;

    private LocationType type;

    @DecimalMin(value = "0.0", inclusive = false, message = "Capacity must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Capacity must have at most 13 integer digits and 2 decimal places")
    private BigDecimal capacity;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
