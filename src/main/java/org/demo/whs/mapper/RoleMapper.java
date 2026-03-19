package org.demo.whs.mapper;

import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.repository.RoleRepository;
import org.springframework.stereotype.Component;

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