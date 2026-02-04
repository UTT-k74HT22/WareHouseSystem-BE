package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.RoleType;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

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
        log.info("Fetching all users with role Manager");
        String role = RoleType.MANAGER.toString();
        return userProfileRepository.getAccountsByRole(role);
    }
}
