package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.InventoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing inventory-related operations.
 */
@RequestMapping("/api/v1/inventories")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class InventoryController {
    private final InventoryService inventoryService;
}
