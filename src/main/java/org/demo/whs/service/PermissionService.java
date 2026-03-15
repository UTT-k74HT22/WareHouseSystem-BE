package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

    PermissionResponse createPermission(CreatePermissionRequest request);

    PageResponse<PermissionResponse> getPermissions(Pageable pageable);

    PermissionResponse getPermissionById(String id);

    PermissionResponse updatePermission(String id, UpdatePermissionRequest request);

    void deletePermission(String id);
}
