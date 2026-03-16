package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.UserService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Service Implementation for managing User.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;

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
                .collect(java.util.stream.Collectors.toMap(
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
        return userProfileRepository.getAccountById(accountId);
    }
}
