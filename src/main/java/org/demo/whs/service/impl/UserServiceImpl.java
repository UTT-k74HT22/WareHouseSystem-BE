package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.request.User.ResetUserPasswordRequest;
import org.demo.whs.entity.dto.request.User.UpdateUserRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing User.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get all users with pagination and search.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> getAll(String search, AccountStatus status, Pageable pageable) {
        log.info("Fetching users with search: {}, status: {}, page: {}", search, status, pageable);

        Page<org.demo.whs.entity.Account> page = accountRepository.findAllWithSearch(search, status, pageable);

        List<String> accountIds = page.getContent().stream()
                .map(org.demo.whs.entity.Account::getId)
                .toList();

        Map<String, UserProfile> profileMap = userProfileRepository.findByAccountIdIn(accountIds).stream()
                .collect(Collectors.toMap(UserProfile::getAccountId, Function.identity(), (a, b) -> a));

        List<AccountResponse> content = page.getContent().stream()
                .map(account -> accountMapper.toAccountResponseWithProfile(
                        account, profileMap.get(account.getId())))
                .collect(Collectors.toList());

        return PageResponse.from(page, content);
    }

    /**
     * Get all users with the role of Manager.
     *
     * @return the list of AccountResponse DTOs
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllUserWithRoleManager() {
        log.info("Fetching all users with roles ADMIN and MANAGER");

        return Stream.concat(
                        userProfileRepository.getAccountsByRole(RoleType.ADMIN.toString()).stream(),
                        userProfileRepository.getAccountsByRole(RoleType.MANAGER.toString()).stream()
                )
                .collect(Collectors.toMap(
                        AccountResponse::getAccountId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    /**
     * Get user by account ID.
     *
     * @param accountId the account ID
     * @return the AccountResponse DTO
     */
    @Override
    @Transactional(readOnly = true)
    public AccountResponse getUserById(String accountId) {
        log.info("Fetching user with account ID: {}", accountId);
        org.demo.whs.entity.Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_ROLE_001));
        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);
        return accountMapper.toAccountResponseWithProfile(account, profile);
    }

    @Override
    @Transactional
    public AccountResponse update(String accountId, UpdateUserRequest request) {
        log.info("Updating user {}", accountId);

        if (request.getStatus() == AccountStatus.DELETED) {
            throw new BadRequestException("Không thể đánh dấu tài khoản là đã xóa", ErrorCode.COM_001);
        }

        org.demo.whs.entity.Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_ROLE_001));

        if (request.getStatus() != null) {
            String currentAccountId = SecurityUtils.getCurrentAccountId();
            if (currentAccountId != null && currentAccountId.equals(accountId)
                    && request.getStatus() != AccountStatus.ACTIVE) {
                throw new BadRequestException("Không thể khóa chính tài khoản của bạn", ErrorCode.AUTH_003);
            }
            account.setStatus(request.getStatus());
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(account.getUsername())) {
            accountRepository.findByUsername(request.getUsername())
                    .filter(existing -> !existing.getId().equals(accountId))
                    .ifPresent(existing -> {
                        throw new BadRequestException(ErrorCode.COM_005);
                    });
            account.setUsername(request.getUsername());
        }

        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);

        String email = trimOrNull(request.getEmail());
        String firstName = trimOrNull(request.getFirstName());
        String lastName = trimOrNull(request.getLastName());

        if (profile == null) {
            if (email != null || firstName != null || lastName != null) {
                if (email == null || firstName == null || lastName == null) {
                    throw new BadRequestException(
                            "Chưa có hồ sơ người dùng; cần cung cấp email, họ và tên để tạo mới",
                            ErrorCode.COM_001);
                }
                UserProfile created = UserProfile.builder()
                        .accountId(accountId)
                        .email(email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .build();
                profile = userProfileRepository.save(created);
            }
        } else {
            if (email != null && !email.equals(profile.getEmail())) {
                userProfileRepository.findByEmail(email)
                        .filter(existing -> !existing.getAccountId().equals(accountId))
                        .ifPresent(existing -> {
                            throw new BadRequestException(ErrorCode.COM_005);
                        });
                profile.setEmail(email);
            }
            if (firstName != null) {
                profile.setFirstName(firstName);
            }
            if (lastName != null) {
                profile.setLastName(lastName);
            }
            profile = userProfileRepository.save(profile);
        }

        org.demo.whs.entity.Account saved = accountRepository.save(account);
        return accountMapper.toAccountResponseWithProfile(saved, profile);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional
    public AccountResponse resetPassword(String accountId, ResetUserPasswordRequest request) {
        log.info("Resetting password of user {}", accountId);

        org.demo.whs.entity.Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_ROLE_001));
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        org.demo.whs.entity.Account saved = accountRepository.save(account);

        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);
        return accountMapper.toAccountResponseWithProfile(saved, profile);
    }
}
