package org.demo.whs.mapper;

import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.Permission;
import org.springframework.stereotype.Component;

/**
 * Mapper class for RolePermission transformations.
 */
@Component
public class RolePermissionMapper {

    /**
     * Convert roleId + permissionId → RoleHasPermission entity
     */
    public RoleHasPermission createEntity(String roleId, String permissionId) {

        if (roleId == null || permissionId == null) {
            return null;
        }

        return RoleHasPermission.builder()
                .roleId(roleId)
                .permissionId(permissionId)
                .build();
    }

    /**
     * Convert Permission → PermissionResponse
     */
    public PermissionResponse toResponse(Permission permission) {

        if (permission == null) {
            return null;
        }

        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .description(permission.getDescription())
                .createdBy(permission.getCreatedBy())
                .createdAt(permission.getCreatedAt())
                .updatedBy(permission.getUpdatedBy())
                .updatedAt(permission.getUpdatedAt())
                .build();
    }
}