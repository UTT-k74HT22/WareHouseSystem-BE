package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.Employee.CreateEmployeeRequest;
import org.demo.whs.entity.dto.response.Employee.EmployeeResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.EmployeeMapper;
import org.demo.whs.repository.*;
import org.demo.whs.service.EmployeeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

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
        log.info("Creating employee with employeeCode={}", request.getEmployeeCode());

        // 1. Validate uniqueness of username and employeeCode
        validateRequest(request);

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

        // 6. Create employee record
        Employee employee = employeeMapper.toEntity(request, account.getId());
        employeeRepository.save(employee);

        log.info("Employee created successfully with id={}, employeeCode={}", employee.getId(), request.getEmployeeCode());
        return employeeMapper.toResponse(employee, userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getById(String id) {
        log.info("Fetching employee with id={}", id);

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found", ErrorCode.EMP_001));

        UserProfile userProfile = userProfileRepository.findByAccountId(employee.getAccountId())
                .orElse(null);

        return employeeMapper.toResponse(employee, userProfile);
    }

    private void validateRequest(CreateEmployeeRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException(ErrorCode.COM_005);
        }

        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new BadRequestException(ErrorCode.EMP_002);
        }
    }
}
