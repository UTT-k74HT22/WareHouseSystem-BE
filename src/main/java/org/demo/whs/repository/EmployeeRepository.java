package org.demo.whs.repository;

import org.demo.whs.entity.Employee;
import org.demo.whs.entity.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link Employee} entity.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /**
     * Check whether an employee code is already in use.
     *
     * @param employeeCode unique staff code
     * @return true if exists
     */
    boolean existsByEmployeeCode(String employeeCode);

    /**
     * Check whether an account already has an employee record linked.
     *
     * @param accountId account identifier
     * @return true if exists
     */
    boolean existsByAccountId(String accountId);

    /**
     * Find employee by account ID.
     *
     * @param accountId account identifier
     * @return optional employee
     */
    Optional<Employee> findByAccountId(String accountId);

    /**
     * Find employee by unique employee code.
     *
     * @param employeeCode staff code
     * @return optional employee
     */
    Optional<Employee> findByEmployeeCode(String employeeCode);

    /**
     * Find all employees assigned to a specific warehouse.
     *
     * @param warehouseId warehouse identifier
     * @return list of employees
     */
    List<Employee> findAllByWarehouseId(String warehouseId);

    /**
     * Paginated list with optional filters for warehouse, status, and position.
     * Null parameters are ignored (treated as "no filter").
     *
     * @param warehouseId optional warehouse filter
     * @param status      optional status filter
     * @param position    optional position filter
     * @param pageable    pagination config
     * @return page of employees
     */
    @Query("""
            SELECT e FROM Employee e
            WHERE (:warehouseId IS NULL OR e.warehouseId = :warehouseId)
              AND (:status IS NULL       OR e.status = :status)
              AND (:position IS NULL     OR LOWER(e.position) LIKE LOWER(CONCAT('%', :position, '%')))
            """)
    Page<Employee> findAllWithFilters(
            @Param("warehouseId") String warehouseId,
            @Param("status") EmployeeStatus status,
            @Param("position") String position,
            Pageable pageable
    );
}
