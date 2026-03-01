package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.*;
import org.demo.whs.service.AccountRegistrationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static org.demo.whs.mapper.AccountHasRoleMapper.getAccountHasRole;
import static org.demo.whs.mapper.AccountMapper.getAccount;
import static org.demo.whs.mapper.UserProfileMapper.getUserProfile;

/**
 * Default implementation of {@link AccountRegistrationService}.
 * <p>
 * Encapsulates the shared "create account + assign role + create user profile" flow
 * so that both {@code EmployeeServiceImpl} and future onboarding services (e.g.,
 * Customer portal user creation) can reuse this logic without duplication.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountRegistrationServiceImpl implements AccountRegistrationService {

    private final AccountRepository accountRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     * <p>
     * All persistence operations are wrapped in a single transaction.
     * Any failure (e.g., duplicate username, unknown role) will roll back
     * the entire unit of work.
     * </p>
     */
    @Override
    @Transactional
    public RegisterAccount register(String username, String rawPassword,
                                     RoleType role, String firstName, String lastName, String email, String phoneNumber) {
        log.info("[PROVISIONING][ACCOUNT] Start provisioning for username={}", username);

        // 1. Validate username uniqueness
        if (accountRepository.existsByUsername(username)) {
            log.warn("[PROVISIONING][ACCOUNT] Username already exists, username={}", username);
            throw new BadRequestException(ErrorCode.COM_005);
        }

        // 2. Resolve the requested role
        Role resolvedRole = roleRepository.findByName(role)
                .orElseThrow(() -> {
                    log.warn("[PROVISIONING][ACCOUNT] Role not found, role={}", role);
                    return new NotFoundException("Role not found: " + role, ErrorCode.ROLE_001);
                });
        log.info("[PROVISIONING][ACCOUNT] Role resolved, role={}", role);

        // 3. Create and persist account with hashed password
        Account account = getAccount(username, passwordEncoder.encode(rawPassword));
        accountRepository.save(account);
        log.info("[PROVISIONING][ACCOUNT] Account created, accountId={}", account.getId());

        // 4. Assign role to account via join table
        AccountHasRole accountHasRole = getAccountHasRole(account, resolvedRole);
        accountHasRoleRepository.save(accountHasRole);
        log.info("[PROVISIONING][ACCOUNT] Role assigned, accountId={}, role={}", account.getId(), role);

        // 5. Create and persist user profile
        UserProfile userProfile = getUserProfile(firstName, lastName, email, phoneNumber, account);
        userProfileRepository.save(userProfile);
        log.info("[PROVISIONING][ACCOUNT] UserProfile created, accountId={}", account.getId());

        log.info("[PROVISIONING][ACCOUNT] Provisioning complete, username={}", username);
        return new RegisterAccount(account, userProfile);
    }
}
