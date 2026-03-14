package org.demo.whs.entity.dto.response.Permission;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import org.demo.whs.entity.enums.ActionType;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PermissionResponse {

    private String id;
    private String code;
    private String name;
    private String resource;
    private ActionType action;
    private String description;
    private LocalDateTime createdAt;
}
