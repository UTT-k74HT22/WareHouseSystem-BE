package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.request.Employee.UpdateEmployeeRequest;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;

/**
 * Service contract for Employee management operations.
 */
public interface EmployeeService {

    /**
     * Create and onboard a new warehouse employee.
     * The referenced account must exist and must not be linked to another employee.
     *
     * @param request creation data
     * @return created employee response
     */
    EmployeeResponse create(CreateEmployeeRequest request);

    /**
     * Get employee details by id.
     *
     * @param id employee id
     * @return employee response
     */
    EmployeeResponse getById(String id);

    /**
     * Update employee information by id.
     *
     * @param id      employee id
     * @param request update request
     * @return updated employee response
     */
    EmployeeResponse update(String id, UpdateEmployeeRequest request);

    /**
     * Soft delete (terminate) an employee by id.
     *
     * @param id employee id
     */
    void softDelete(String id);
}
