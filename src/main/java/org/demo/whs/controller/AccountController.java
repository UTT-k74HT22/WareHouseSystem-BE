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
     * Endpoint to retrieve accounts with pagination.
     *
     * @param page the page number to retrieve (default: 0)
     * @param size the number of items per page (default: 10)
     * @return a paginated response containing account information
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<AccountResponse>>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.debug("get Account list: page={}, size={}", page, size);
        return ResponseEntity.ok(
                BaseResponse.success(accountService.getList(page, size))
        );
    }
}
