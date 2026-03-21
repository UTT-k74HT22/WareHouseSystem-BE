package org.demo.whs.service.impl;

import jdk.jfr.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Permission;
import org.demo.whs.entity.Role;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.RoleMapper;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public RoleResponse createRole(CreateRoleRequest request) {
        return null;
    }

    @Override
    public PageResponse<RoleResponse> getRoles(Pageable pageable) {
        return null;
    }

    /**
     * Lấy chi tiết Role + Permissions
     * @param roleId = Role.id trong bảng roles
     */
    @Override
    public RoleResponse getRoleById(String roleId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        List<Permission> permissions = roleRepository.findPermissionsByRoleId(roleId);

        return roleMapper.toResponseWithPermissions(role, permissions);
    }

    /**
     * Update an existing role.
     *
     * @param id      the ID of the role to update
     * @param request the request body containing updated role details
     * @return the updated role details
     */
    @Override
    @Transactional
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {

        log.info("Updating role with id={}", id);

        if (request == null) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ROLE_001));

        if (request.getName() != null && request.getName().isBlank()) {
            throw new BadRequestException(ErrorCode.ROLE_002);
        }

        if (Boolean.TRUE.equals(request.getIsDefault()) &&
                !Boolean.TRUE.equals(role.getIsDefault())) {
            roleRepository.updateAllIsDefaultToFalse();
        }

        roleMapper.updateEntity(request, role);

        Role updated = roleRepository.save(role);

        log.info("Role updated successfully id={}, name={}", updated.getId(), updated.getName());

        return roleMapper.toResponse(updated);
    }

    @Override
    public void deleteRole(String id) {

    }

}
