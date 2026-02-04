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
}
