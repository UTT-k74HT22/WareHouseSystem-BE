package org.demo.whs.entity.dto.request.Permission;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.demo.whs.entity.enums.ActionType;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdatePermissionRequest {

    @Size(max = 100, message = "Permission name must not exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Resource must not exceed 50 characters")
    private String resource;

    private ActionType action;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;
}
