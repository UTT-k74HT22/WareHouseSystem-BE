package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RolePermissionRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.RolePermissionService;
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

    @Override
    public List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request) {
        return List.of();
    }

    @Override
    @Transactional
    public void removePermission(String roleId, String permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);

        if (roleId == null || roleId.isBlank()) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        if (permissionId == null || permissionId.isBlank()) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        int deleted = rolePermissionRepository
                .deleteByIdRoleIdAndIdPermissionId(roleId, permissionId);

        if (deleted == 0) {
            log.warn("Permission {} is not assigned to role {}", permissionId, roleId);
            throw new NotFoundException(ErrorCode.PERM_008);
        }

        log.info("Removed permission {} from role {}", permissionId, roleId);
    }

    @Override
    public PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable) {
        return null;
    }
}
