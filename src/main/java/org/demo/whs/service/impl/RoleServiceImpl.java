package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        return null;
    }

    @Override
    public PageResponse<RoleResponse> getRoles(Pageable pageable) {
        return null;
    }

    @Override
    public RoleResponse getRoleById(String id) {
        return null;
    }

    @Override
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {
        return null;
    }

    @Override
    public void deleteRole(String id) {

    }

    @Override
    public List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request) {
        return List.of();
    }

    @Override
    public void removePermission(String roleId, String permissionId) {

    }

    @Override
    public PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable) {
        return null;
    }

    @Override
    public List<RoleResponse> assignRolesToUser(String userId, AssignRolesRequest request) {
        return List.of();
    }

    @Override
    public void removeRoleFromUser(String userId, String roleId) {

    }

    @Override
    public PageResponse<RoleResponse> getUserRoles(String userId, Pageable pageable) {
        return null;
    }

    @Override
    public PageResponse<AccountResponse> getRoleUsers(String roleId, Pageable pageable) {
        return null;
    }
}
