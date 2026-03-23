package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RolePermissionMapper;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.RolePermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    public List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request) {
        return List.of();
    }

    @Override
    public void removePermission(String roleId, String permissionId) {

    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> getRolePermissions(
            String roleId,
            String resource,
            Pageable pageable
    ) {

        if (roleId == null || roleId.isEmpty()) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        Page<Permission> page = rolePermissionRepository.findPermissionsByRoleId(roleId, resource, pageable);

        Page<PermissionResponse> responsePage = page.map(rolePermissionMapper::toResponse);

        return PageResponse.from(responsePage);
    }
}
