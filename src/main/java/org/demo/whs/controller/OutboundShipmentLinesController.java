package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.OutboundShipmentLinesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing outbound shipment lines.
 */
@RequestMapping("/api/v1/outbound-shipment-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Outbound Shipment Lines", description = "APIs for managing outbound shipment lines")
@PreAuthorize("isAuthenticated()")
public class OutboundShipmentLinesController {
    private final OutboundShipmentLinesService outboundShipmentLinesService;
}
