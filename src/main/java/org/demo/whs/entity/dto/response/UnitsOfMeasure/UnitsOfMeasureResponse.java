package org.demo.whs.entity.dto.response.UnitsOfMeasure;

import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.UnitsOfMeasureType;

/**
 * Response DTO for units of measure.
 */
@Getter
@Builder
public class UnitsOfMeasureResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private UnitsOfMeasureType type;
}
