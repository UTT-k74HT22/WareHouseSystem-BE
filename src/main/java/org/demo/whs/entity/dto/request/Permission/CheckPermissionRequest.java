package org.demo.whs.entity.dto.request.Permission;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.ActionType;

/**
 * Request dùng để check permission của user hiện tại.
 *
 * Ví dụ:
 * {
 *   "resource": "USER",
 *   "action": "CREATE"
 * }
 */
@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CheckPermissionRequest {

    /**
     * Resource cần check (USER, ORDER, PRODUCT...)
     */
    @NotBlank(message = "Resource is required")
    @Pattern(
            regexp = "^[A-Z][A-Z0-9]*(?:_[A-Z0-9]+)*$",
            message = "Resource must use canonical UPPER_SNAKE_CASE format"
    )
    private String resource;

    /**
     * Action tương ứng (CREATE, UPDATE, DELETE...)
     */
    @NotNull(message = "Action is required")
    private ActionType action;
}
