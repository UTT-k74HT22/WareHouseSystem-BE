package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.SalesOrdersService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing sales orders.
 */
@RequestMapping("/api/v1/sales-orders")
@RestController
@RequiredArgsConstructor
@Slf4j
public class SalesOrdersController {
    private final SalesOrdersService salesOrdersService;
}
