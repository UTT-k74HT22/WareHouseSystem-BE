package org.demo.whs.service;

import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service Interface for managing User-Role assignments.
 */
public interface UserRoleService {

    /**
     * Assign roles to user (add new roles, remove unselected roles).
     *
     * @param userId the user ID
     * @param request the request containing role IDs
     * @return list of assigned role responses
     */
    List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request);

    /**
     * Remove a single role from user.
     *
     * @param userId the user ID
     * @param roleId the role ID to remove
     */
    void removeRoleFromUser(String userId, String roleId);

    /**
     * Remove all roles from user.
     *
     * @param userId the user ID
     */
    void removeAllRolesFromUser(String userId);

    /**
     * Get roles of a user.
     *
     * @param userId the user ID
     * @param pageable pagination info
     * @return page of role responses
     */
    PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable);

    /**
     * Get users of a role.
     *
     * @param roleId the role ID
     * @param pageable pagination info
     * @return page of account responses
     */
    PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable);
}
