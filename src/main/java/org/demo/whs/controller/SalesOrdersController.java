package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.SalesOrdersService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing sales orders.
 */
@RequestMapping("/api/v1/sales-orders")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Sales Orders", description = "APIs for managing sales orders")
@PreAuthorize("isAuthenticated()")
public class SalesOrdersController {
    private final SalesOrdersService salesOrdersService;
}
