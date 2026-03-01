package org.demo.whs.entity.dto.request.Employee;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for changing an employee's lifecycle status (PATCH endpoint).
 * Allowed values: ACTIVE, ON_LEAVE, TERMINATED
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateEmployeeStatusRequest {

    @NotBlank(message = "Status is required")
    private String status;
}
