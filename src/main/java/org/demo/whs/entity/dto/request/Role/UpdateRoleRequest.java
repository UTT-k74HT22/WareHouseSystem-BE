package org.demo.whs.entity.dto.request.Role;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateRoleRequest {

    @Size(max = 50, message = "Role code must not exceed 50 characters")
    private String code;

    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private Boolean isDefault;
}
