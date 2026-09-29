package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.demo.whs.entity.dto.request.User.ResetUserPasswordRequest;
import org.demo.whs.entity.dto.request.User.UpdateUserRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.entity.enums.AccountStatus;
import org.demo.whs.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Controller for managing User.
 */
@RequestMapping("/api/v1/users")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserController {

    private final UserService userService;

    /**
     * Get all users with pagination and search.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_USER_READ')")
    public ResponseEntity<BaseResponse<PageResponse<AccountResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Received request to get all users with search: {}, status: {}", search, status);
        PageResponse<AccountResponse> response = userService.getAll(search, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Get all users with the role of Manager.
     *
     * @return ResponseEntity containing BaseResponse with list of AccountResponse DTOs
     */
    @GetMapping("/managers")
    @PreAuthorize("hasAuthority('PERM_USER_READ')")
    public ResponseEntity<BaseResponse<List<AccountResponse>>> getAllManagers() {
        log.info("Received request to get all users with role Manager");
        List<AccountResponse> managers = userService.getAllUserWithRoleManager();
        BaseResponse<List<AccountResponse>> response = BaseResponse.success(managers);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by account ID.
     *
     * @param accountId the account ID
     * @return ResponseEntity containing BaseResponse with AccountResponse DTO
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('PERM_USER_READ')")
    public ResponseEntity<BaseResponse<AccountResponse>> getUserById(@PathVariable String accountId) {
        log.info("Received request to get user with account ID: {}", accountId);
        AccountResponse user = userService.getUserById(accountId);
        BaseResponse<AccountResponse> response = BaseResponse.success(user);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially update account and profile information.
     *
     * @param accountId the account ID
     * @param request   fields to update (null fields are ignored)
     * @return ResponseEntity containing BaseResponse with updated AccountResponse DTOs
     */
    @PutMapping("/{accountId}")
    @PreAuthorize("hasAuthority('PERM_USER_UPDATE')")
    public ResponseEntity<BaseResponse<AccountResponse>> updateUser(
            @PathVariable String accountId,
            @RequestBody @Valid UpdateUserRequest request) {
        log.info("Received request to update user with account ID: {}", accountId);
        AccountResponse user = userService.update(accountId, request);
        return ResponseEntity.ok(BaseResponse.success(user, "User updated successfully"));
    }

    /**
     * Reset account password (admin operation).
     *
     * @param accountId the account ID
     * @param request   the new password
     * @return ResponseEntity containing BaseResponse with AccountResponse DTOs
     */
    @PostMapping("/{accountId}/reset-password")
    @PreAuthorize("hasAuthority('PERM_USER_UPDATE')")
    public ResponseEntity<BaseResponse<AccountResponse>> resetUserPassword(
            @PathVariable String accountId,
            @RequestBody @Valid ResetUserPasswordRequest request) {
        log.info("Received request to reset password of user {}", accountId);
        AccountResponse user = userService.resetPassword(accountId, request);
        return ResponseEntity.ok(BaseResponse.success(user, "User password reset successfully"));
    }
}
