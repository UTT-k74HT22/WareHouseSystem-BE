package org.demo.whs.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.service.WareHouseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/warehouse")
@RestController
@RequiredArgsConstructor
@Slf4j
public class WareHouseController {

    public final WareHouseService wareHouseService;

    @GetMapping
    public ResponseEntity<String> getWareHouse() {
        log.info("Fetching warehouses");
        // Placeholder response
        return ResponseEntity.ok("List of warehouses");
    }
}
