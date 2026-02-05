package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.PurchaseOrdersService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing purchase orders.
 */
@RequestMapping("/api/v1/purchase-orders")
@RestController
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrdersController {
    private final PurchaseOrdersService purchaseOrdersService;

}
