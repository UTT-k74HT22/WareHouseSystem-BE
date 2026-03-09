package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing inbound receipts.
 */
@RequestMapping("/api/v1/inbound-receipts")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Inbound Receipts", description = "APIs for managing inbound receipts")
@PreAuthorize("isAuthenticated()")
public class InboundReceiptsController {
    private final InboundReceiptsService inboundReceiptsService;
}
