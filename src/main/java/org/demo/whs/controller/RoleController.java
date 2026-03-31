package org.demo.whs.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Role.CreateRoleRequest;
import org.demo.whs.entity.dto.request.Role.UpdateRoleRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Role.RoleResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.service.RoleService;
import org.demo.whs.service.UserRoleService;
import org.springframework.data.domain.PageRequest;
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
    private final UserRoleService userRoleService;

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_CREATE')")
    public ResponseEntity<BaseResponse<RoleResponse>> createRole(
            @RequestBody @Valid CreateRoleRequest request
    ) {

        RoleResponse response = roleService.createRole(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ROLE_READ')")
    public ResponseEntity<BaseResponse<PageResponse<RoleResponse>>> getRoles(
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(required = false) @Size(max = 100, message = "Search keyword max 50 chars") String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {

        PageResponse<RoleResponse> response = roleService.getRoles(isDefault, search, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ROLE_READ')")
    public ResponseEntity<BaseResponse<RoleResponse>> getRoleById(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id
    ) {

        RoleResponse response = roleService.getRoleById(id);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ROLE_UPDATE')")
    public ResponseEntity<BaseResponse<RoleResponse>> updateRole(

            @PathVariable
            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String id,

            @RequestBody @Valid UpdateRoleRequest request
    ) {

        RoleResponse response = roleService.updateRole(id, request);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_ROLE_DELETE')")
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

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAuthority('PERM_ROLE_READ')")
    public ResponseEntity<BaseResponse<PageResponse<AccountResponse>>> getRoleUsers(

            @PathVariable("id")
//            @Pattern(regexp = UUID_PATTERN, message = "Invalid role id format")
            String roleId,

            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("Get users of role {}", roleId);

        PageResponse<AccountResponse> response = userRoleService.getRoleUsers(roleId, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
