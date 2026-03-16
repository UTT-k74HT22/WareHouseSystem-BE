package org.demo.whs.service;

import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserRoleService {

    /**
     * Assign roles to a user.
     *
     * @param userId  the ID of the user
     * @param request the request containing role IDs to assign
     * @return list of assigned roles
     */
    List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request);

    /**
     * Remove a role from a user.
     *
     * @param userId the ID of the user
     * @param roleId the ID of the role to remove
     */
    void removeRoleFromUser(String userId, String roleId);

    /**
     * Get roles assigned to a user with pagination.
     *
     * @param userId   the ID of the user
     * @param pageable pagination information
     * @return paginated list of roles assigned to the user
     */
    PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable);

    /**
     * Get users assigned to a role with pagination.
     *
     * @param roleId   the ID of the role
     * @param pageable pagination information
     * @return paginated list of users assigned to the role
     */
    PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable);
}
