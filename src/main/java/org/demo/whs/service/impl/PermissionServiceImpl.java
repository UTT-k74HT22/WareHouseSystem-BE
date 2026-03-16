package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
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

    @Override
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        return null;
    }

    @Override
    public PageResponse<PermissionResponse> getPermissions(Pageable pageable) {
        return null;
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

    @Override
    public PermissionResponse updatePermission(String id, UpdatePermissionRequest request) {
        return null;
    }

    @Override
    public void deletePermission(String id) {

    }
}
