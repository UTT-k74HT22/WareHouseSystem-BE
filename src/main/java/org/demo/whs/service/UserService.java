package org.demo.whs.service;

import org.demo.whs.entity.dto.request.User.ResetUserPasswordRequest;
import org.demo.whs.entity.dto.request.User.UpdateUserRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
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
    PageResponse<AccountResponse> getAll(String search, AccountStatus status, Pageable pageable);

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

    /**
     * Update account and profile information (partial update).
     * Username and email must stay unique across the system.
     * Status change is included: DELETED is rejected and an account cannot lock itself.
     *
     * @param accountId the account ID
     * @param request   fields to update (null fields are ignored)
     * @return the updated AccountResponse DTO
     */
    AccountResponse update(String accountId, UpdateUserRequest request);

    /**
     * Reset account password (admin operation).
     *
     * @param accountId the account ID
     * @param request   the new password (raw, will be hashed)
     * @return the AccountResponse DTO
     */
    AccountResponse resetPassword(String accountId, ResetUserPasswordRequest request);
}
