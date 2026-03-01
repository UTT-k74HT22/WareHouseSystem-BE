package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
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
}
