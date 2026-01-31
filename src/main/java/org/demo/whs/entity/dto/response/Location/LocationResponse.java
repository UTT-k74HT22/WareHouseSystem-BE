package org.demo.whs.entity.dto.response.Location;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Location entity.
 */
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LocationResponse {

    private String id;
    private String warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String code;
    private String name;
    private String zone;
    private LocationType type;
    private BigDecimal capacity;
    private LocationStatus status;
    private String notes;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
