package org.demo.whs.entity.dto.request.Employee;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing employee's WMS / HR information.
 * <p>
 * All fields are optional — only non-null values will be applied
 * (partial-update / PATCH semantics handled in service layer).
 * </p>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UpdateEmployeeRequest {

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Position must not exceed 100 characters")
    private String position;

    private LocalDate hireDate;

    private LocalDate terminationDate;

    @Size(max = 20, message = "Salary grade must not exceed 20 characters")
    private String salaryGrade;

    /** Reassign to a different warehouse. */
    private String warehouseId;
}
