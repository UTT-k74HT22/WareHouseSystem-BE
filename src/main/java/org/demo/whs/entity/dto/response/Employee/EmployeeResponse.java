package org.demo.whs.entity.dto.response.Employee;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO representing a full employee view.
 * <p>
 * Combines data from {@code employees} (WMS fields) and
 * {@code user_profiles} (personal identity fields).
 * </p>
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmployeeResponse {

    private String id;

    /** Account / login identity reference. */
    private String accountId;

    /** Unique WMS staff code (e.g. EMP-001). */
    private String employeeCode;

    // ── From user_profiles (personal identity) ─────────────────────────────
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;

    // ── WMS / HR fields (from employees) ────────────────────────────────────
    private String department;
    private String position;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private String salaryGrade;

    /** Primary assigned warehouse ID. */
    private String warehouseId;

    /** Employee lifecycle status: ACTIVE | ON_LEAVE | TERMINATED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
