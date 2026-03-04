package org.demo.whs.entity.dto.request.Category;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.demo.whs.entity.enums.CategoryStatus;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateCategoryStatusRequest {

    @NotNull(message = "Category status is required")
    private CategoryStatus status;
}
