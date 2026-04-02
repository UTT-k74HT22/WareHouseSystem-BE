package org.demo.whs.service;

import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing User.
 */
public interface UserService {

    /**
     * Get all users with pagination and search.
     *
     * @param search search keyword (username, email, full name)
     * @param status user status filter
     * @param pageable pagination info
     * @return page of AccountResponse DTOs
     */
    PageResponse<AccountResponse> getAll(String search, String status, Pageable pageable);

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
