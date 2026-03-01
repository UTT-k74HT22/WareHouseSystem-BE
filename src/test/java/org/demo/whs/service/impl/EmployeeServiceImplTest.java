package org.demo.whs.service.impl;

import org.demo.whs.entity.Employee;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.entity.enums.EmployeeStatus;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.EmployeeMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.EmployeeRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeServiceImpl Unit Tests")
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountHasRoleRepository accountHasRoleRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("should_ReturnEmployeeResponse_When_EmployeeExists")
    void should_ReturnEmployeeResponse_When_EmployeeExists() {
        String employeeId = "emp-1";
        String accountId = "acc-1";

        Employee employee = Employee.builder()
                .accountId(accountId)
                .employeeCode("EMP-001")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);

        UserProfile profile = UserProfile.builder()
                .accountId(accountId)
                .firstName("Dung")
                .lastName("Hoang")
                .email("dung@example.com")
                .build();

        EmployeeResponse expected = EmployeeResponse.builder()
                .id(employeeId)
                .accountId(accountId)
                .employeeCode("EMP-001")
                .firstName("Dung")
                .lastName("Hoang")
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProfileRepository.findByAccountId(accountId)).thenReturn(Optional.of(profile));
        when(employeeMapper.toResponse(employee, profile)).thenReturn(expected);

        EmployeeResponse result = employeeService.getById(employeeId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(employeeId);
        assertThat(result.getEmployeeCode()).isEqualTo("EMP-001");
        verify(employeeRepository).findById(employeeId);
        verify(userProfileRepository).findByAccountId(accountId);
        verify(employeeMapper).toResponse(employee, profile);
    }

    @Test
    @DisplayName("should_ThrowNotFoundException_When_EmployeeNotFound")
    void should_ThrowNotFoundException_When_EmployeeNotFound() {
        String employeeId = "emp-missing";
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(employeeId))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND)
                .hasMessage("Employee not found")
                .satisfies(ex -> assertThat(((NotFoundException) ex).getErrorCode()).isEqualTo("EMP_001"));

        verify(employeeRepository).findById(employeeId);
    }

    @Test
    @DisplayName("should_ReturnEmployeeResponseWithoutProfile_When_UserProfileMissing")
    void should_ReturnEmployeeResponseWithoutProfile_When_UserProfileMissing() {
        String employeeId = "emp-2";
        String accountId = "acc-2";

        Employee employee = Employee.builder()
                .accountId(accountId)
                .employeeCode("EMP-002")
                .status(EmployeeStatus.ACTIVE)
                .build();
        employee.setId(employeeId);

        EmployeeResponse expected = EmployeeResponse.builder()
                .id(employeeId)
                .accountId(accountId)
                .employeeCode("EMP-002")
                .build();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProfileRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
        when(employeeMapper.toResponse(employee, null)).thenReturn(expected);

        EmployeeResponse result = employeeService.getById(employeeId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(employeeId);
        verify(employeeRepository).findById(employeeId);
        verify(userProfileRepository).findByAccountId(accountId);
        verify(employeeMapper).toResponse(employee, null);
    }
}
