package org.demo.whs.mapper;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.repository.RoleRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Mapper class for Role entity transformations.
 */
@Component
public class RoleMapper {

    /**
     * Convert CreateRoleRequest → Role entity
     */
    public Role createEntity(CreateRoleRequest request) {

        if (request == null) {
            return null;
        }

        return Role.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(request.getIsDefault())
                .build();
    }

    /**
     * Update existing Role entity from UpdateRoleRequest
     */
    public void updateEntity(UpdateRoleRequest request, Role role) {

        if (role == null || request == null) {
            return;
        }

        if (request.getCode() != null) {
            role.setCode(request.getCode());
        }

        if (request.getName() != null) {
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
    }

    public RoleResponse toResponseWithPermissions(Role role, List<Permission> permissions) {
        if (role == null) return null;

        List<PermissionResponse> perms = (permissions == null) ? Collections.emptyList() :
                permissions.stream()
                        .filter(Objects::nonNull)
                        .map(this::mapPermission)
                        .collect(Collectors.toList());

        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isDefault(role.getIsDefault())
                .createdBy(role.getCreatedBy())
                .createdAt(role.getCreatedAt())
                .updatedBy(role.getUpdatedBy())
                .updatedAt(role.getUpdatedAt())
                .permissions(perms)
                .build();
    }

    /** ---------------- Helper ---------------- */

    /**
     * Map Permission → PermissionResponse
     */
    private PermissionResponse mapPermission(Permission permission) {
        if (permission == null) return null;

        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .resource(permission.getResource())
                .action(permission.getAction())
                .build();
    }

    /**
     * Convert Role → RoleResponse
     */
    public RoleResponse toResponse(Role role) {

        if (role == null) {
            return null;
        }

        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .isDefault(role.getIsDefault())
                .createdBy(role.getCreatedBy())
                .createdAt(role.getCreatedAt())
                .updatedBy(role.getUpdatedBy())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}