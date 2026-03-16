package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.repository.PermissionRepository;
import org.demo.whs.repository.RoleRepository;
import org.demo.whs.service.RolePermissionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<PermissionResponse> assignPermissions(String roleId, AssignPermissionsRequest request) {
        return List.of();
    }

    @Override
    public void removePermission(String roleId, String permissionId) {

    }

    @Override
    public PageResponse<PermissionResponse> getRolePermissions(String roleId, Pageable pageable) {
        return null;
    }
}
