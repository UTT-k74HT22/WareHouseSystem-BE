package org.demo.whs.mapper;

import org.demo.whs.entity.Employee;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.entity.enums.EmployeeStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper for converting between {@link Employee} entities and DTOs.
 */
@Component
public class EmployeeMapper {

    /**
     * Convert a {@link CreateEmployeeRequest} to an {@link Employee} entity.
     * <p>
     * The {@code accountId} is not part of the request and must be provided
     * separately (e.g. from the service layer) to link the employee to a user account.
     * </p>
     *
     * @param request   the incoming create request DTO
     * @param accountId the associated user account ID (must be provided by caller)
     * @return new Employee entity
     */
    public Employee toEntity(CreateEmployeeRequest request, String accountId) {
        if (request == null) return null;

        Employee employee = new Employee();

        employee.setAccountId(accountId);
        employee.setEmployeeCode(request.getEmployeeCode());
        employee.setDepartment(request.getDepartment());
        employee.setPosition(request.getPosition());
        employee.setHireDate(request.getHireDate());
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    /**
     * Convert an {@link Employee} entity plus its associated {@link UserProfile}
     * into a full {@link EmployeeResponse}.
     *
     * @param employee    the employee entity
     * @param userProfile linked user profile (may be null)
     * @return response DTO
     */
    public EmployeeResponse toResponse(Employee employee, UserProfile userProfile) {
        if (employee == null) return null;

        EmployeeResponse.EmployeeResponseBuilder builder = EmployeeResponse.builder()
                .id(employee.getId())
                .accountId(employee.getAccountId())
                .employeeCode(employee.getEmployeeCode())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .hireDate(employee.getHireDate())
                .terminationDate(employee.getTerminationDate())
                .salaryGrade(employee.getSalaryGrade())
                .warehouseId(employee.getWarehouseId())
                .status(employee.getStatus() != null ? employee.getStatus().name() : null)
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt());

        if (userProfile != null) {
            builder.firstName(userProfile.getFirstName())
                    .lastName(userProfile.getLastName())
                    .email(userProfile.getEmail())
                    .phoneNumber(userProfile.getPhoneNumber())
                    .address(userProfile.getAddress())
                    .dateOfBirth(userProfile.getDate_of_birth());
        }

        return builder.build();
    }

    /**
     * Convert a list of employees (each paired with its profile) to response list.
     * This overload is a convenience for single-source mapping when profiles are
     * loaded separately.
     *
     * @param employees list of employees
     * @return list of EmployeeResponse (no profile data)
     */
    public List<EmployeeResponse> toResponseList(List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }
        return employees.stream()
                .map(e -> toResponse(e, null))
                .toList();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Parse status string to enum. Defaults to ACTIVE when null/blank.
     *
     * @param status raw string value
     * @return {@link EmployeeStatus}
     */
    private EmployeeStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return EmployeeStatus.ACTIVE;
        }
        try {
            return EmployeeStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EmployeeStatus.ACTIVE;
        }
    }
}
