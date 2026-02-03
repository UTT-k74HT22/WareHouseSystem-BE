package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing inbound receipts.
 */
@RequestMapping("/api/v1/inbound-receipts")
@RestController
@RequiredArgsConstructor
@Slf4j
public class InboundReceiptsController {
    private final InboundReceiptsService inboundReceiptsService;
}
