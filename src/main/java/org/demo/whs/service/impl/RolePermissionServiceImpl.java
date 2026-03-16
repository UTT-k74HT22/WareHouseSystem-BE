package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.service.RolePermissionService;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class RolePermissionServiceImpl implements RolePermissionService {
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
}
