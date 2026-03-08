package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.OutboundShipmentsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing outbound shipment-related operations.
 */
@RequestMapping("/api/v1/outbound-shipments")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Outbound Shipments", description = "APIs for managing outbound shipments")
@PreAuthorize("isAuthenticated()")
public class OutboundShipmentsController {
    private final OutboundShipmentsService outboundShipmentsService;
}
