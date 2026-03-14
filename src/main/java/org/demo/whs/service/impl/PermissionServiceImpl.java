package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    @Override
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        return null;
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
}
