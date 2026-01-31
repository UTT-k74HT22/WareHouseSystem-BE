package org.demo.whs.entity.dto.request.Location;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;

/**
 * Request DTO for searching locations with multiple filters.
 * All fields are optional.
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SearchLocationRequest {

    private String warehouseId;
    private String code;
    private String name;
    private String zone;
    private LocationType type;
    private LocationStatus status;
}
