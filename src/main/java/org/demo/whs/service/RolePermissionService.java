package org.demo.whs.service;

import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RolePermissionService {

    // ROLE - PERMISSION
    List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request);

    void removePermission(String roleId, String permissionId);

    PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable);

}
