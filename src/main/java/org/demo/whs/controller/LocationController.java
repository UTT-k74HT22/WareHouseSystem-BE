package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/location")
@RestController
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    public final LocationService locationService;

    @GetMapping
    public ResponseEntity<String> getLocations() {
        log.info("Fetching locations");
        // Placeholder response
        return ResponseEntity.ok("List of locations");
    }

}
