package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.exception.*;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.demo.whs.repository.specification.PermissionSpecification;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
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

    /**
     * Get permissions with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of permissions
     */
    @Override
    public PageResponse<PermissionResponse> getPermissions(
            String resource,
            ActionType action,
            String search,
            Pageable pageable
    ) {

        Page<PermissionResponse> page = permissionRepository
                .findAll(
                        PermissionSpecification.filter(resource, action, search),
                        pageable
                )
                .map(permissionMapper::toResponse);

        return PageResponse.from(page);
    }

    /**
     * Get a permission by its ID.
     *
     * @param id the ID of the permission
     * @return the permission response
     */
    @Override
    public PermissionResponse getPermissionById(String id) {

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PERM_001));

        return permissionMapper.toResponse(permission);
    }

    /**
     * Update an existing permission.
     *
     * @param id      the ID of the permission to update
     * @param request the request containing updated permission details
     * @return the updated permission response
     */
    @Override
    public PermissionResponse updatePermission(String id, UpdatePermissionRequest request) {

        Permission permission = permissionRepository.findById(id)
        .orElseThrow(() -> new NotFoundException(ErrorCode.PERM_001));

        validateUpdatePermission(request, permission);

        if (request.getName() != null) {
            permission.setName(request.getName());
        }

        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        Permission update = permissionRepository.save(permission);

        log.info("Permission updated successfully with id: {}", update.getId());

        return permissionMapper.toResponse(update);
    }

    /**
     * Delete a permission by its ID.
     *
     * @param id the ID of the permission to delete
     */
    @Override
    public void deletePermission(String id) {
        log.info("Start deleting permission with id: {}", id);

        Permission permission = findPermissionOrThrow(id);

        validatePermissionNotInUse(id);

        permissionRepository.delete(permission);

        log.info("Permission deleted successfully, id: {}", id);
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

    private void validateDuplicateName(String name, String id) {
        if (permissionRepository.existsByNameAndIdNot(name, id)) {
            throw new BadRequestException(ErrorCode.PERM_002);
        }
    }

    private void validateUpdatePermission(UpdatePermissionRequest request, Permission permission) {

        if (request == null) {
            throw new BadRequestException(ErrorCode.PERM_004);
        }

        if (request.getAction() != null) {
            throw new BadRequestException(ErrorCode.PERM_010);
        }

        if (request.getResource() != null) {
            throw new BadRequestException(ErrorCode.PERM_012);
        }

        if (request.getName() != null) {
            validateDuplicateName(request.getName(), permission.getId());
        }
    }

    private Permission findPermissionOrThrow(String id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PERM_001));
    }

    private void validatePermissionNotInUse(String permissionId) {
        List<String> roleIds = rolePermissionRepository.findRoleIdsByPermissionId(permissionId);

        if (!roleIds.isEmpty()) {
            throw new ConflictException(ErrorCode.PERM_007);
        }
    }
}
