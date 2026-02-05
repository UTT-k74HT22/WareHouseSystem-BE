package org.demo.whs.entity.dto.response.UnitsOfMeasure;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.UnitsOfMeasureType;

/**
 * Response DTO for units of measure.
 */
@Getter
@Builder
@AllArgsConstructor
@NotNull
public class UnitsOfMeasureResponse {
    private String id;
    private String code;
    private String name;
    private String description;
    private UnitsOfMeasureType type;
}
