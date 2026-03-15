package org.demo.whs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/roles")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class RoleController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private final RoleService roleService;

    /**
     * Create role
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoleResponse>> createRole(
            @RequestBody @Valid CreateRoleRequest request
    ) {

        RoleResponse response = roleService.createRole(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    /**
     * Get roles
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BaseResponse<PageResponse<RoleResponse>>> getRoles(

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        PageResponse<RoleResponse> response = roleService.getRoles(pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get role by id
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BaseResponse<RoleResponse>> getRoleById(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id
    ) {

        RoleResponse response = roleService.getRoleById(id);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Update role
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoleResponse>> updateRole(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @RequestBody @Valid UpdateRoleRequest request
    ) {

        RoleResponse response = roleService.updateRole(id, request);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Delete role
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteRole(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id
    ) {

        roleService.deleteRole(id);

        return ResponseEntity.ok(
                BaseResponse.success(null, "Role deleted successfully")
        );
    }

    /**
     * Assign permissions to role
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<PermissionResponse>>> assignPermissions(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @RequestBody @Valid AssignPermissionsRequest request
    ) {

        List<PermissionResponse> response = roleService.assignPermissions(id, request);

        return ResponseEntity.ok(
                BaseResponse.success(response, "Permissions assigned successfully")
        );
    }

    /**
     * Remove permission from role
     */
    @DeleteMapping("/{id}/permissions/{permId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> removePermission(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid permission id format")
            String permId
    ) {

        roleService.removePermission(id, permId);

        return ResponseEntity.ok(
                BaseResponse.success(null, "Permission removed successfully")
        );
    }

    /**
     * Get role permissions
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BaseResponse<PageResponse<PermissionResponse>>> getRolePermissions(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        PageResponse<PermissionResponse> response =
                roleService.getRolePermissions(id, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get users of role
     */
    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BaseResponse<PageResponse<AccountResponse>>> getRoleUsers(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        PageResponse<AccountResponse> response =
                roleService.getRoleUsers(id, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}