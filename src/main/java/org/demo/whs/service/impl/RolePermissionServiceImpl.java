package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.RoleHasPermission;
import org.demo.whs.entity.Role;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional
    public List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request) {

        if (roleId == null || roleId.isBlank()
                || request == null
                || request.getPermissionIds() == null
                || request.getPermissionIds().isEmpty()) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        List<String> permissionIds = request.getPermissionIds()
                .stream()
                .distinct()
                .toList();

        log.info("Assign permissions {} to role {}", permissionIds, roleId);

        roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);

        if (permissions.size() != permissionIds.size()) {
            throw new NotFoundException(ErrorCode.PERM_001);
        }

        List<String> existingPermissionIds =
                rolePermissionRepository.findPermissionIdsByRoleId(roleId);

        if (existingPermissionIds == null) {
            existingPermissionIds = List.of();
        }

        Set<String> existingSet = new HashSet<>(existingPermissionIds);

        List<RoleHasPermission> newEntities = permissions.stream()
                .map(Permission::getId)
                .filter(Objects::nonNull)
                .filter(id -> !existingSet.contains(id))
                .map(id -> rolePermissionMapper.createEntity(roleId, id))
                .toList();

        if (!newEntities.isEmpty()) {
            rolePermissionRepository.saveAll(newEntities);
        }

        log.info("Assigned {} new permissions to role {}", newEntities.size(), roleId);

        return permissions.stream()
                .map(rolePermissionMapper::toResponse)
                .toList();
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
