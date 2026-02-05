package org.demo.whs.entity.dto.request.UnitsOfMeasure;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.demo.whs.entity.enums.UnitsOfMeasureType;

/**
 * Request DTO for creating or updating a unit of measure.
 */
@Getter
public class UnitsOfMeasureRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Type is required")
    private UnitsOfMeasureType type;
}
