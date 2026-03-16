package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.PermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

        validatePermissionNameUnique(request.getName());

        Permission permission = permissionMapper.createEntity(request);

        Permission savePermission = permissionRepository.save(permission);

        log.info("Permission created successfully with name: {}", savePermission.getName());

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

    private void validatePermissionNameUnique(String name) {
        if (permissionRepository.existsByName(name)) {
            throw new BadRequestException(ErrorCode.PERM_002);
        }
    }
}
