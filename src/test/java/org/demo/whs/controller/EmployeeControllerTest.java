package org.demo.whs.controller;

import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import({GlobalExceptionHandle.class})
@ImportAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private org.demo.whs.service.RateLimitService rateLimitService;

    @Test
    @DisplayName("should_Return200_When_GetEmployeeByIdSuccess")
    void should_Return200_When_GetEmployeeByIdSuccess() throws Exception {
        EmployeeResponse response = EmployeeResponse.builder()
                .id("emp-1")
                .employeeCode("EMP-001")
                .firstName("Dung")
                .build();

        when(employeeService.getById("emp-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/employees/{id}", "emp-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("emp-1"))
                .andExpect(jsonPath("$.data.employee_code").value("EMP-001"));
    }

    @Test
    @DisplayName("should_Return404_When_GetEmployeeByIdNotFound")
    void should_Return404_When_GetEmployeeByIdNotFound() throws Exception {
        when(employeeService.getById("missing-id"))
                .thenThrow(new NotFoundException("Employee not found", ErrorCode.EMP_001));

        mockMvc.perform(get("/api/v1/employees/{id}", "missing-id")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    @DisplayName("should_Return400_When_IdIsBlank")
    void should_Return400_When_IdIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/employees/{id}", " ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
