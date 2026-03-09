package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.SalesOrderLinesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing sales order lines.
 */
@RequestMapping("/api/v1/sales-order-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Sales Order Lines", description = "APIs for managing sales order lines")
@PreAuthorize("isAuthenticated()")
public class SalesOrderLinesController {
    private final SalesOrderLinesService salesOrderLinesService;
}
