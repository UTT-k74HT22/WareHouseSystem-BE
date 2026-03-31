package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.User.AccountResponse;
import org.demo.whs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
