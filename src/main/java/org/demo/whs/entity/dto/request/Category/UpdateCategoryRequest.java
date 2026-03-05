package org.demo.whs.entity.dto.request.Category;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateCategoryRequest {

    @Size(max = 20, message = "Category code must not exceed 20 characters")
    @Pattern(
            regexp = "^[A-Z0-9_-]+$",
            message = "Category code must contain only uppercase letters, numbers, underscores, and hyphens"
    )
    private String code;

    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
