package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import org.demo.whs.entity.enums.EmployeeStatus;

import java.time.LocalDate;

/**
 * Employee: Represents a WMS warehouse staff member.
 * <p>
 * Extension of {@link UserProfile} using Class-Table Inheritance pattern.
 * Links 1-1 with {@link Account} for login identity and
 * 1-1 with {@link UserProfile} for personal information.
 * </p>
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Employee extends BaseEntity {

    /**
     * Links to the account used for login. Must be unique (1-1 relationship).
     */
    @Column(name = "account_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String accountId;

    /**
     * Unique staff identifier within the warehouse system (e.g. EMP-001).
     */
    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    /**
     * Organizational department (e.g. Warehouse, Logistics, Admin).
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * Operational role within WMS (e.g. PICKER, PACKER, RECEIVER, SUPERVISOR, MANAGER).
     */
    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    /**
     * Salary band / grade (e.g. L1, L2, SENIOR).
     */
    @Column(name = "salary_grade", length = 20)
    private String salaryGrade;

    /**
     * Primary warehouse this employee is assigned to.
     */
    @Column(name = "warehouse_id", columnDefinition = "CHAR(36)")
    private String warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EmployeeStatus status = EmployeeStatus.ACTIVE;
}
