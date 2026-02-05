package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.SalesOrdersService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing sales order lines.
 */
@RequestMapping("/api/v1/sales-order-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
public class SalesOrderLinesController {
    private final SalesOrdersService salesOrdersService;
}
