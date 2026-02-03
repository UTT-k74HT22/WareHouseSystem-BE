package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.PurchaseOrderLinesService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing purchase order lines.
 */
@RequestMapping("/api/v1/purchase-order-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderLinesController {
    private final PurchaseOrderLinesService purchaseOrderLinesService;
}
