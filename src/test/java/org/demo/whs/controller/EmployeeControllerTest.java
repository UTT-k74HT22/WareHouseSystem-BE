package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.Employee.UpdateEmployeeRequest;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    @DisplayName("Get employee by id success -> 200 OK")
    void getEmployeeById_Success() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder()
                .id("emp-1")
                .employeeCode("EMP-001")
                .build();

        when(employeeService.getById("emp-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/employees/{id}", "emp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("emp-1"));
    }

    @Test
    @DisplayName("Get employee by id not found -> 404 NOT FOUND")
    void getEmployeeById_NotFound() throws Exception {
        when(employeeService.getById("missing"))
                .thenThrow(new NotFoundException("Employee not found", ErrorCode.EMP_001));

        mockMvc.perform(get("/api/v1/employees/{id}", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("EMP_001"))
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    @DisplayName("Update employee success -> 200 OK")
    void updateEmployee_Success() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .department("Operations")
                .build();

        EmployeeResponse response = EmployeeResponse.builder()
                .id("emp-1")
                .department("Operations")
                .build();

        when(employeeService.update(eq("emp-1"), any(UpdateEmployeeRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/employees/{id}", "emp-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("emp-1"));
    }

    @Test
    @DisplayName("Update employee validation error -> 400 BAD REQUEST")
    void updateEmployee_ValidationError() throws Exception {
        String longDepartment = "a".repeat(101);
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .department(longDepartment)
                .build();

        mockMvc.perform(put("/api/v1/employees/{id}", "emp-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Soft delete employee success -> 200 OK")
    void deleteEmployee_Success() throws Exception {
        doNothing().when(employeeService).softDelete("emp-1");

        mockMvc.perform(delete("/api/v1/employees/{id}", "emp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("List employees success -> 200 OK")
    void listEmployees_Success() throws Exception {
        EmployeeResponse item = EmployeeResponse.builder()
                .id("emp-1")
                .employeeCode("EMP-001")
                .build();

        PageResponse<EmployeeResponse> pageResponse = PageResponse.<EmployeeResponse>builder()
                .content(java.util.List.of(item))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(employeeService.getEmployees(any(), any(), any(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/employees")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("emp-1"));
    }
}
