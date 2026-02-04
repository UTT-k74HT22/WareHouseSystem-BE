package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class OutboundShipmentLinesController {
}
