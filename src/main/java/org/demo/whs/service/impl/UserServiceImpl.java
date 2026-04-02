package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.UserProfile;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.mapper.AccountMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * Get all users with pagination and search.
     */
    @Override
    public PageResponse<AccountResponse> getAll(String search, String status, Pageable pageable) {
        log.info("Fetching users with search: {}, status: {}, page: {}", search, status, pageable);

        Page<org.demo.whs.entity.Account> page = accountRepository.findAllWithSearch(search, status, pageable);

        List<AccountResponse> content = page.getContent().stream()
                .map(account -> {
                    UserProfile profile = userProfileRepository.findByAccountId(account.getId()).orElse(null);
                    return accountMapper.toAccountResponseWithProfile(account, profile);
                })
                .collect(Collectors.toList());

        return PageResponse.from(page, content);
    }

    /**
     * Get all users with the role of Manager.
     *
     * @return the list of AccountResponse DTOs
     */
    @Override
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
    public AccountResponse getUserById(String accountId) {
        log.info("Fetching user with account ID: {}", accountId);
        org.demo.whs.entity.Account account = accountRepository.findById(accountId).orElse(null);
        if (account == null) {
            return null;
        }
        UserProfile profile = userProfileRepository.findByAccountId(accountId).orElse(null);
        return accountMapper.toAccountResponseWithProfile(account, profile);
    }
}
