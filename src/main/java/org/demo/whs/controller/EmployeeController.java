package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.request.Employee.UpdateEmployeeRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Employee management.
 * API Version: v1
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EmployeeController {

    private final EmployeeService employeeService;


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<BaseResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody CreateEmployeeRequest createEmployeeRequest) {
        log.info("Received request to create employee with employeeCode={}", createEmployeeRequest.getEmployeeCode());
        EmployeeResponse response = employeeService.create(createEmployeeRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<EmployeeResponse>> getEmployeeById(@PathVariable String id) {
        log.info("Received request to fetch employee by id={}", id);
        EmployeeResponse response = employeeService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<EmployeeResponse>> updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        log.info("Received request to update employee by id={}", id);
        EmployeeResponse response = employeeService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> deleteEmployee(@PathVariable String id) {
        log.info("Received request to soft delete employee by id={}", id);
        employeeService.softDelete(id);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
