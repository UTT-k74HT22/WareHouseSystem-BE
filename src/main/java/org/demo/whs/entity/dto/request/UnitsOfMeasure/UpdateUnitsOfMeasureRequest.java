package org.demo.whs.entity.dto.request.UnitsOfMeasure;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.UnitsOfMeasureType;

/**
 * Request DTO for updating a unit of measure.
 * All fields are optional.
 */
@Getter
public class UpdateUnitsOfMeasureRequest {

    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private UnitsOfMeasureType type;
}
