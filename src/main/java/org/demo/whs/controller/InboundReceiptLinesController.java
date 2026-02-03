package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.InboundReceiptLinesService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing inbound receipt lines.
 */
@RequestMapping("/api/v1/inbound-receipt-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
public class InboundReceiptLinesController {
    private final InboundReceiptLinesService inboundReceiptLinesService;
}
