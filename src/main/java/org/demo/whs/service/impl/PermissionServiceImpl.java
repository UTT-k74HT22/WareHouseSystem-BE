package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.swing.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    /**
     * Creates a new permission in the system.
     * @param request permission creation request
     * @return created permission response
     */
    @Override
    public PermissionResponse createPermission(CreatePermissionRequest request) {

        validateCreatePermission(request);

        String code = generatePermissionCode(
                request.getResource(),
                request.getAction()
        );

        Permission permission = permissionMapper.createEntity(request);
        permission.setCode(code);

        Permission savePermission = permissionRepository.save(permission);

        log.info("Permission created successfully with code: {}", savePermission.getCode());

        return permissionMapper.toResponse(savePermission);
    }

    @Override
    public PageResponse<PermissionResponse> getPermissions(Pageable pageable) {
        return null;
    }

    @Override
    public PermissionResponse getPermissionById(String id) {
        return null;
    }

    @Override
    public PermissionResponse updatePermission(String id, UpdatePermissionRequest request) {
        return null;
    }

    @Override
    public void deletePermission(String id) {

    }

    /**
     * Validate dữ liệu khi tạo permission.
     *
     * @param request create permission request
     */
    private void validateCreatePermission(CreatePermissionRequest request) {

        validateName(request.getName());
        validateResource(request.getResource());
        validateAction(request.getAction());
        validateResourceActionUnique(request.getResource(), request.getAction());
    }

    /**
     * Validate permission name uniqueness.
     */
    private void validateName(String name) {

        if (permissionRepository.existsByName(name)) {
            throw new BadRequestException(ErrorCode.PERM_002);
        }
    }

    /**
     * Validate resource hợp lệ.
     */
    private void validateResource(String resource) {

        if (resource == null || resource.isBlank()) {
            throw new BadRequestException(ErrorCode.PERM_009);
        }
    }

    /**
     * Validate action hợp lệ.
     */
    private void validateAction(ActionType action) {

        if (action == null) {
            throw new BadRequestException(ErrorCode.PERM_010);
        }
    }

    /**
     * Validate resource + action uniqueness.
     */
    private void validateResourceActionUnique(String resource, ActionType action) {

        if (permissionRepository.existsByResourceAndAction(resource, action)) {
            throw new BadRequestException(ErrorCode.PERM_006);
        }
    }

    /**
     * Generate permission code based on resource and action.
     */
    private String generatePermissionCode(String resource, ActionType action) {

        return "PERM_" +
                resource.toUpperCase() +
                "_" +
                action.name();
    }
}
