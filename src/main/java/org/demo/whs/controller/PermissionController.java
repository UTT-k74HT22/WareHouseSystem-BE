package org.demo.whs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Permission.CreatePermissionRequest;
import org.demo.whs.entity.dto.request.Permission.UpdatePermissionRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Permission.PermissionResponse;
import org.demo.whs.entity.enums.ActionType;
import org.demo.whs.service.PermissionService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/permissions")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class PermissionController {

    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PermissionResponse>> createPermission(
            @RequestBody @Valid CreatePermissionRequest request
    ) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BaseResponse<PageResponse<PermissionResponse>>> getPermissions(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) ActionType action,
            @RequestParam(required = false) String search,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        PageResponse<PermissionResponse> response = permissionService.getPermissions(resource, action, search, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<BaseResponse<PermissionResponse>> getPermissionById(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid permission id format") String id
    ) {
        PermissionResponse response = permissionService.getPermissionById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PermissionResponse>> updatePermission(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid permission id format") String id,
            @RequestBody @Valid UpdatePermissionRequest request
    ) {
        PermissionResponse response = permissionService.updatePermission(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deletePermission(
            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid permission id format") String id
    ) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Permission deleted successfully"));
    }
}
