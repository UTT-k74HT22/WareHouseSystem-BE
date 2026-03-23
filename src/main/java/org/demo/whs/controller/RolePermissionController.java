package org.demo.whs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.RolePermission.AssignPermissionsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.service.RolePermissionService;
import org.demo.whs.service.RoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
public class RolePermissionController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private final RolePermissionService rolePermissionService;

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

        List<PermissionResponse> response = rolePermissionService.assignPermissions(id, request);

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

        rolePermissionService.removePermission(id, permId);

        return ResponseEntity.ok(
                BaseResponse.success(null, "Permission removed successfully")
        );
    }

    /**
     * Get role permissions
     */
    @GetMapping("/{id}/permissions")
//    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ResponseEntity<BaseResponse<PageResponse<PermissionResponse>>> getRolePermissions(

            @PathVariable
//            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @RequestParam(required = false) String resource,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        PageResponse<PermissionResponse> response =
                rolePermissionService.getRolePermissions(id, resource, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}