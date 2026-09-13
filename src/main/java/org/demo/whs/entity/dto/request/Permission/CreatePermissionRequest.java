package org.demo.whs.entity.dto.request.Permission;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.ActionType;

@Setter
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreatePermissionRequest {

    @NotBlank(message = "Permission name is required")
    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Resource is required")
    @Size(max = 50, message = "Resource must not exceed 50 characters")
    @Pattern(
            regexp = "^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*$",
            message = "Resource must use canonical UPPER_SNAKE_CASE format"
    )
    private String resource;

    @NotNull(message = "Action is required")
    private ActionType action;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
