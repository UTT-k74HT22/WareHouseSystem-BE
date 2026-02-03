package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.Account.AccountResponse;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing accounts.
 * Provides endpoints for retrieving account information.
 */
@RequestMapping("api/v1/accounts")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class AccountController {

    private final AccountService accountService;

    /**
     * Retrieves all accounts.
     *
     * @return list of account responses
     */
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<List<AccountResponse>>> getAll() {
        log.debug("Fetching all accounts");
        return ResponseEntity.ok(
                BaseResponse.success(accountService.getAll())
        );
    }
}