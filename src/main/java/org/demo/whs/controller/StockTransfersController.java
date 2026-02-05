package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.StockTransfersService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing stock transfers.
 */
@RequestMapping("/api/v1/stock-transfers")
@RestController
@RequiredArgsConstructor
@Slf4j
public class StockTransfersController {
    private final StockTransfersService stockTransfersService;
}
