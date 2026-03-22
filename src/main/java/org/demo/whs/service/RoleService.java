package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.springframework.data.domain.Pageable;


public interface RoleService {

    /**
     * Create a new role.
     *
     * @param request the request body containing role details
     * @return the created role details
     */
    RoleResponse createRole(CreateRoleRequest request);

    /**
     * Get roles with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of roles
     */
    PageResponse<RoleResponse> getRoles(Boolean isDefault, String search, Pageable pageable);

    /**
     * Get a role by its ID.
     *
     * @param roleId the ID of the role
     * @return the role details
     */
    RoleResponse getRoleById(String roleId);

    /**
     * Update an existing role.
     *
     * @param id      the ID of the role to update
     * @param request the request body containing updated role details
     * @return the updated role details
     */
    RoleResponse updateRole(String id, UpdateRoleRequest request);

    /**
     * Delete a role by its ID.
     *
     * @param id the ID of the role to delete
     */
    void deleteRole(String id);

}