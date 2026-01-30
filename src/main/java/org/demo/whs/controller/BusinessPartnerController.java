package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.BusinessPartnerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/business-partner")
@RestController
@RequiredArgsConstructor
@Slf4j
public class BusinessPartnerController {

    public final BusinessPartnerService businessPartnerService;

    @GetMapping
    public ResponseEntity<String> getBusinessPartners() {
        log.info("Fetching business partners");
        // Placeholder response
        return ResponseEntity.ok("List of business partners");
    }

}
