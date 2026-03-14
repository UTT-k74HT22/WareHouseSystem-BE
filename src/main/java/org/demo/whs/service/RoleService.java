package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoleService {

    // ROLE CRUD
    RoleResponse createRole(CreateRoleRequest request);

    PageResponse<RoleResponse> getRoles(Pageable pageable);

    RoleResponse getRoleById(String id);

    RoleResponse updateRole(String id, UpdateRoleRequest request);

    void deleteRole(String id);


    // ROLE - PERMISSION
    List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request);

    void removePermission(String roleId, String permissionId);

    PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable);


    // USER - ROLE
    List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request);

    void removeRoleFromUser(String userId, String roleId);

    PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable);

    PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable);
}