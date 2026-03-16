package org.demo.whs.mapper;

import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Permission entity transformations.
 */
@Component
public class PermissionMapper {

    /**
     * Convert CreatePermissionRequest → Permission entity
     */
    public Permission createEntity(CreatePermissionRequest request) {

        if (request == null) {
            return null;
        }

        return Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();
    }

    /**
     * Update existing Permission entity from UpdatePermissionRequest
     */
    public void updateEntity(UpdatePermissionRequest request, Permission permission) {

        if (permission == null || request == null) {
            return;
        }


        if (request.getName() != null) {
            permission.setName(request.getName());
        }

        if (request.getResource() != null) {
            permission.setResource(request.getResource());
        }

        if (request.getAction() != null) {
            permission.setAction(request.getAction());
        }

        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
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