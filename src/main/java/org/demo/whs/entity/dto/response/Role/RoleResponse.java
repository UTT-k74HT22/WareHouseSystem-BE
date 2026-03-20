package org.demo.whs.entity.dto.response.Role;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.RoleType;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoleResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    private Long permissionCount;
    private Long userCount;
}
