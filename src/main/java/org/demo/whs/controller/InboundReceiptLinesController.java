package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.InboundReceiptLinesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing inbound receipt lines.
 */
@RequestMapping("/api/v1/inbound-receipt-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Inbound Receipt Lines", description = "APIs for managing inbound receipt lines")
@PreAuthorize("isAuthenticated()")
public class InboundReceiptLinesController {
    private final InboundReceiptLinesService inboundReceiptLinesService;
}
