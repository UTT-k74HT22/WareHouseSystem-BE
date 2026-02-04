package org.demo.whs.service;

import org.demo.whs.entity.dto.response.User.AccountResponse;

import java.util.List;

/**
 * Service Interface for managing User.
 */
public interface UserService {

    /**
     * Get all users with the role of Manager.
     *
     * @return the list of AccountResponse DTOs
     */
    List<AccountResponse> getAllUserWithRoleManager();

    /**
     * Get user by account ID.
     *
     * @param accountId the account ID
     * @return the AccountResponse DTO
     */
    AccountResponse getUserById(String accountId);
}
