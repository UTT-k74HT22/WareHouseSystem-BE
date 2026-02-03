package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.StockAdjustmentsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing stock adjustments.
 */
@RequestMapping("/api/v1/stock-adjustments")
@RestController
@RequiredArgsConstructor
@Slf4j
public class StockAdjustmentsController {
    private final StockAdjustmentsService stockAdjustmentsService;
}
