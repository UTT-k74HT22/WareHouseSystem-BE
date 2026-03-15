package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.request.Employee.UpdateEmployeeRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.entity.enums.EmployeeStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.EmployeeMapper;
import org.demo.whs.repository.*;
import org.demo.whs.service.EmployeeService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.demo.whs.mapper.AccountHasRoleMapper.getAccountHasRole;
import static org.demo.whs.mapper.AccountMapper.getAccount;
import static org.demo.whs.mapper.UserProfileMapper.getUserProfile;

/**
 * Service implementation for Employee management.
 * <p>
 * Handles CRUD + status-change operations for WMS warehouse staff.
 * Personal identity data is read from {@code user_profiles};
 * WMS operational data lives in {@code employees}.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final WareHouseRepository wareHouseRepository;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;
    private final IdentifierGenerator identifierGenerator;

    /**
     * Create and onboard a new warehouse employee.
     * <p>
     * Steps:
     * <ol>
     *   <li>Validate uniqueness of username and employeeCode.</li>
     *   <li>Validate that the requested role exists.</li>
     *   <li>Create {@link Account} with hashed password.</li>
     *   <li>Assign the requested {@link Role} via {@link AccountHasRole}.</li>
     *   <li>Create {@link UserProfile} with personal data.</li>
     *   <li>Create {@link Employee} with WMS operational data.</li>
     * </ol>
     * All steps are wrapped in a single transaction; any failure rolls back the whole operation.
     * Warehouse assignment is handled separately via the update API.
     * </p>
     *
     * @param request creation data
     * @return created employee response
     */
    @Override
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        String employeeCode = identifierGenerator.generateSystemManaged(
                request.getEmployeeCode(),
                "Employee code",
                "EMP",
                20,
                employeeRepository::existsByEmployeeCode
        );
        log.info("Creating employee with employeeCode={}", employeeCode);

        validateRequest(request, employeeCode);

        // 2. Validate role exists
        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new NotFoundException(
                        "Role not found: " + request.getRole(), ErrorCode.ROLE_001));

        // 3. Create account with hashed password
        Account account = getAccount(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        accountRepository.save(account);
        log.info("Account created with username={}", request.getUsername());

        // 4. Assign role to account
        AccountHasRole accountHasRole = getAccountHasRole(account, role);
        accountHasRoleRepository.save(accountHasRole);

        // 5. Create user profile
        UserProfile userProfile = getUserProfile(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhoneNumber(),
                account
        );
        userProfileRepository.save(userProfile);

        Employee employee = employeeMapper.toEntity(request, account.getId());
        employee.setEmployeeCode(employeeCode);
        employeeRepository.save(employee);

        log.info("Employee created successfully with id={}, employeeCode={}", employee.getId(), employeeCode);
        return employeeMapper.toResponse(employee, userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(String id) {
        log.info("Fetching employee by id={}", id);

        Employee employee = findEmployeeById(id);
        UserProfile userProfile = userProfileRepository.findByAccountId(employee.getAccountId())
                .orElse(null);

        return employeeMapper.toResponse(employee, userProfile);
    }

    @Override
    @Transactional
    public EmployeeResponse update(String id, UpdateEmployeeRequest request) {
        log.info("Updating employee by id={}", id);

        Employee employee = findEmployeeById(id);

        if (request.getWarehouseId() != null && !request.getWarehouseId().isBlank()) {
            validateWarehouseAssignment(request.getWarehouseId());
        }

        employeeMapper.updateEntity(employee, request);
        Employee updatedEmployee = employeeRepository.save(employee);

        UserProfile userProfile = userProfileRepository.findByAccountId(updatedEmployee.getAccountId())
                .orElse(null);

        log.info("Employee updated successfully: id={}", updatedEmployee.getId());
        return employeeMapper.toResponse(updatedEmployee, userProfile);
    }

    @Override
    @Transactional
    public void softDelete(String id) {
        log.info("Soft deleting employee by id={}", id);

        Employee employee = findEmployeeById(id);
        if (employee.getStatus() == EmployeeStatus.TERMINATED) {
            throw new BadRequestException(ErrorCode.EMP_005);
        }

        employee.setStatus(EmployeeStatus.TERMINATED);
        if (employee.getTerminationDate() == null) {
            employee.setTerminationDate(java.time.LocalDate.now());
        }

        employeeRepository.save(employee);
        log.info("Employee soft deleted successfully: id={}", employee.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getEmployees(String keyword, String status, String warehouseId, Pageable pageable) {
        validatePageable(pageable);

        EmployeeStatus statusFilter = parseStatusOrDefault(status);
        String normalizedKeyword = normalizeKeyword(keyword);

        Page<Employee> employeePage = employeeRepository.findAllWithFilters(
                warehouseId,
                statusFilter,
                normalizedKeyword,
                pageable
        );

        List<EmployeeResponse> responses = mapEmployeeResponses(employeePage.getContent());
        return PageResponse.from(employeePage, responses);
    }

    private void validateRequest(CreateEmployeeRequest request, String employeeCode) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(ErrorCode.COM_005);
        }

        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new BadRequestException(ErrorCode.EMP_002);
        }
    }

    private Employee findEmployeeById(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found", ErrorCode.EMP_001));
    }

    private void validateWarehouseAssignment(String warehouseId) {
        Warehouses warehouse = wareHouseRepository.findById(warehouseId)
                .orElseThrow(() -> new BadRequestException(ErrorCode.EMP_006));

        if (warehouse.getStatus() != WareHouseStatus.ACTIVE) {
            throw new BadRequestException(ErrorCode.EMP_006);
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
        if (pageable.getPageSize() <= 0 || pageable.getPageSize() > 100) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
    }

    private EmployeeStatus parseStatusOrDefault(String status) {
        if (status == null || status.isBlank()) {
            return EmployeeStatus.ACTIVE;
        }
        try {
            return EmployeeStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<EmployeeResponse> mapEmployeeResponses(List<Employee> employees) {
        if (employees == null || employees.isEmpty()) {
            return List.of();
        }

        List<String> accountIds = employees.stream()
                .map(Employee::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findByAccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(UserProfile::getAccountId, profile -> profile));

        return employees.stream()
                .map(employee -> employeeMapper.toResponse(employee, profileMap.get(employee.getAccountId())))
                .toList();
    }
}
