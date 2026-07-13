package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.UserRole.AssignRolesRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.service.UserRoleService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing User Roles.
 */
@RequestMapping("/api/v1/users/{userId}/roles")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserRoleController {

    private final UserRoleService userRoleService;

    /**
     * Assign roles to user.
     * API: POST /api/v1/users/{id}/roles
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_USER_ROLE_CREATE')")
    public ResponseEntity<BaseResponse<List<RoleResponse>>> assignRolesToUser(

            @PathVariable
            String userId,

            @RequestBody @Valid AssignRolesRequest request
    ) {

        log.info("Assign roles to user {}", userId);

        List<RoleResponse> response = userRoleService.assignRolesToUser(userId, request);

        return ResponseEntity.ok(
                BaseResponse.success(response, "Roles assigned successfully")
        );
    }

    /**
     * Remove role from user.
     * API: DELETE /api/v1/users/{id}/roles/{roleId}
     */
    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('PERM_USER_ROLE_DELETE')")
    public ResponseEntity<BaseResponse<Void>> removeRoleFromUser(

            @PathVariable
//            @Pattern(regexp = UUID_PATTERN, message = "Invalid user id format")
            String userId,

            @PathVariable
//            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String roleId
    ) {

        log.info("Remove role {} from user {}", roleId, userId);

        userRoleService.removeRoleFromUser(userId, roleId);

        return ResponseEntity.ok(
                BaseResponse.success(null, "Role removed successfully")
        );
    }

    /**
     * Get roles of user.
     * API: GET /api/v1/users/{id}/roles
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_ROLE_READ')")
    public ResponseEntity<BaseResponse<PageResponse<RoleResponse>>> getUserRoles(

            @PathVariable
//            @Pattern(regexp = UUID_PATTERN, message = "Invalid user id format")
            String userId,

            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        log.info("Get roles of user {}", userId);

        PageResponse<RoleResponse> response = userRoleService.getUserRoles(userId, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

}
