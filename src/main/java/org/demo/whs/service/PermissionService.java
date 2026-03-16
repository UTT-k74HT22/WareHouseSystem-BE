package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.springframework.data.domain.Pageable;

public interface PermissionService {

    /**
     * Create a new permission.
     *
     * @param request the request containing permission details
     * @return the created permission response
     */
    PermissionResponse createPermission(CreatePermissionRequest request);

    /**
     * Get permissions with pagination.
     *
     * @param pageable pagination information
     * @return paginated list of permissions
     */
    PageResponse<PermissionResponse> getPermissions(
            String resource,
            ActionType action,
            String search,
            Pageable pageable);

    /**
     * Get a permission by its ID.
     *
     * @param id the ID of the permission
     * @return the permission response
     */
    PermissionResponse getPermissionById(String id);

    /**
     * Update an existing permission.
     *
     * @param id      the ID of the permission to update
     * @param request the request containing updated permission details
     * @return the updated permission response
     */
    PermissionResponse updatePermission(String id, UpdatePermissionRequest request);

    /**
     * Delete a permission by its ID.
     *
     * @param id the ID of the permission to delete
     */
    void deletePermission(String id);
}
