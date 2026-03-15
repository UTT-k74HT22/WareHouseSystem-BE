package org.demo.whs.service.impl;

import org.demo.whs.entity.Employee;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.request.Employee.UpdateEmployeeRequest;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.entity.enums.EmployeeStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.EmployeeMapper;
import org.demo.whs.repository.AccountHasRoleRepository;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.EmployeeRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    private WareHouseRepository wareHouseRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    @Test
    @DisplayName("Should generate employee code when request does not provide one")
    void create_ShouldGenerateEmployeeCode_When_RequestDoesNotProvideOne() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("john.doe")
                .password("Password1!")
                .role(org.demo.whs.entity.enums.RoleType.ADMIN)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        Role role = new Role();
        role.setId("role-1");
        role.setName(request.getRole().name());

        Employee employee = new Employee();
        employee.setAccountId("acc-1");

        when(roleRepository.findByName(request.getRole())).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed-password");
        when(employeeMapper.toEntity(request, "acc-1")).thenReturn(employee);
        when(employeeMapper.toResponse(eq(employee), any(UserProfile.class))).thenAnswer(invocation ->
                EmployeeResponse.builder()
                        .employeeCode(employee.getEmployeeCode())
                        .accountId(employee.getAccountId())
                        .build());
        when(accountRepository.save(any())).thenAnswer(invocation -> {
            org.demo.whs.entity.Account account = invocation.getArgument(0);
            account.setId("acc-1");
            return account;
        });

        EmployeeResponse response = employeeService.create(request);

        assertThat(response.getEmployeeCode()).startsWith("EMP-");
        assertThat(response.getEmployeeCode()).hasSizeLessThanOrEqualTo(20);
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Should reject create request when employee code is provided")
    void create_ShouldReject_When_RequestProvidesEmployeeCode() {
        CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                .username("john.doe")
                .password("Password1!")
                .role(org.demo.whs.entity.enums.RoleType.ADMIN)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .employeeCode("EMP-001")
                .build();

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should get employee by id successfully")
        void getById_Success() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setAccountId("acc-1");
            employee.setStatus(EmployeeStatus.ACTIVE);

            UserProfile profile = new UserProfile();
            profile.setAccountId("acc-1");
            profile.setFirstName("John");
            profile.setLastName("Doe");

            EmployeeResponse expected = EmployeeResponse.builder()
                    .id("emp-1")
                    .accountId("acc-1")
                    .firstName("John")
                    .lastName("Doe")
                    .status(EmployeeStatus.ACTIVE.name())
                    .build();

            when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.of(profile));
            when(employeeMapper.toResponse(employee, profile)).thenReturn(expected);

            EmployeeResponse result = employeeService.getById("emp-1");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("emp-1");
            assertThat(result.getAccountId()).isEqualTo("acc-1");

            verify(employeeRepository).findById("emp-1");
            verify(userProfileRepository).findByAccountId("acc-1");
            verify(employeeMapper).toResponse(employee, profile);
        }

        @Test
        @DisplayName("Should throw NotFoundException when employee does not exist")
        void getById_NotFound() {
            when(employeeRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.getById("missing"))
                    .isInstanceOf(NotFoundException.class);

            verify(employeeRepository).findById("missing");
            verify(userProfileRepository, never()).findByAccountId(any());
        }
    }

    @Nested
    @DisplayName("Update Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update employee successfully")
        void update_Success() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setAccountId("acc-1");
            employee.setStatus(EmployeeStatus.ACTIVE);

            UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                    .department("Operations")
                    .warehouseId("wh-1")
                    .build();

            Warehouses warehouse = new Warehouses();
            warehouse.setId("wh-1");
            warehouse.setStatus(WareHouseStatus.ACTIVE);

            EmployeeResponse expected = EmployeeResponse.builder()
                    .id("emp-1")
                    .department("Operations")
                    .warehouseId("wh-1")
                    .build();

            when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
            when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
            doNothing().when(employeeMapper).updateEntity(employee, request);
            when(employeeRepository.save(employee)).thenReturn(employee);
            when(userProfileRepository.findByAccountId("acc-1")).thenReturn(Optional.empty());
            when(employeeMapper.toResponse(employee, null)).thenReturn(expected);

            EmployeeResponse result = employeeService.update("emp-1", request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("emp-1");

            verify(employeeRepository).findById("emp-1");
            verify(wareHouseRepository).findById("wh-1");
            verify(employeeMapper).updateEntity(employee, request);
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("Should throw BadRequestException when warehouse is missing")
        void update_WarehouseMissing() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setAccountId("acc-1");

            UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                    .warehouseId("wh-404")
                    .build();

            when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
            when(wareHouseRepository.findById("wh-404")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> employeeService.update("emp-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMP_006.getCode());

            verify(employeeRepository).findById("emp-1");
            verify(wareHouseRepository).findById("wh-404");
            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Soft Delete Tests")
    class SoftDeleteTests {

        @Test
        @DisplayName("Should soft delete employee successfully")
        void softDelete_Success() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setStatus(EmployeeStatus.ACTIVE);

            when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));
            when(employeeRepository.save(employee)).thenReturn(employee);

            employeeService.softDelete("emp-1");

            assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
            assertThat(employee.getTerminationDate()).isNotNull();

            verify(employeeRepository).findById("emp-1");
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("Should throw BadRequestException when employee already terminated")
        void softDelete_AlreadyTerminated() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setStatus(EmployeeStatus.TERMINATED);
            employee.setTerminationDate(LocalDate.now().minusDays(1));

            when(employeeRepository.findById("emp-1")).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> employeeService.softDelete("emp-1"))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMP_005.getCode());

            verify(employeeRepository).findById("emp-1");
            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("List Employees Tests")
    class ListEmployeesTests {

        @Test
        @DisplayName("Should default status to ACTIVE when not provided")
        void getEmployees_DefaultStatusActive() {
            Employee employee = new Employee();
            employee.setId("emp-1");
            employee.setAccountId("acc-1");

            UserProfile profile = new UserProfile();
            profile.setAccountId("acc-1");

            EmployeeResponse response = EmployeeResponse.builder()
                    .id("emp-1")
                    .build();

            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
            Page<Employee> page = new PageImpl<>(List.of(employee), pageable, 1);

            when(employeeRepository.findAllWithFilters(any(), any(), any(), any(Pageable.class)))
                    .thenReturn(page);
            when(userProfileRepository.findByAccountIdIn(any())).thenReturn(List.of(profile));
            when(employeeMapper.toResponse(employee, profile)).thenReturn(response);

            employeeService.getEmployees(null, null, null, pageable);

            verify(employeeRepository).findAllWithFilters(eq(null), eq(EmployeeStatus.ACTIVE), eq(null), eq(pageable));
            verify(employeeMapper).toResponse(employee, profile);
        }

        @Test
        @DisplayName("Should throw BadRequestException for invalid status")
        void getEmployees_InvalidStatus() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> employeeService.getEmployees(null, "INVALID", null, pageable))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

            verify(employeeRepository, never()).findAllWithFilters(any(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when page size exceeds limit")
        void getEmployees_SizeTooLarge() {
            Pageable pageable = PageRequest.of(0, 101);

            assertThatThrownBy(() -> employeeService.getEmployees(null, null, null, pageable))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

            verify(employeeRepository, never()).findAllWithFilters(any(), any(), any(), any(Pageable.class));
        }
    }
}
