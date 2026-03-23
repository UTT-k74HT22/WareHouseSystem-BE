package org.demo.whs.entity.dto.request.RolePermission;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AssignPermissionsRequest {

    @NotEmpty(message = "Permission IDs are required")
    private List<String> permissionIds;
}
