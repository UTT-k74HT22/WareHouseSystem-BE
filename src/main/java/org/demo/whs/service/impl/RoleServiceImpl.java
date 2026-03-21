package org.demo.whs.service.impl;

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

    @Override
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {
        return null;
    }

    @Override
    public void deleteRole(String id) {

    }

}
