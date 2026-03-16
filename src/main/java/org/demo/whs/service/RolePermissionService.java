package org.demo.whs.service;

import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RolePermissionService {

    /**
     * Assign permissions to a role
     *
     * @param roleId  the ID of the role
     * @param request the request containing permission IDs to assign
     * @return list of assigned permissions
     */
    List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request);

    /**
     * Remove a permission from a role
     *
     * @param roleId       the ID of the role
     * @param permissionId the ID of the permission to remove
     */
    void removePermission(String roleId, String permissionId);

    /**
     * Get permissions assigned to a role with pagination
     *
     * @param roleId   the ID of the role
     * @param pageable pagination information
     * @return paginated list of permissions assigned to the role
     */
    PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable);
}
