package org.demo.whs.entity.dto.request.UserRole;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AssignRolesRequest {

    @NotEmpty(message = "Role IDs are required")
    private List<String> roleIds;
}
