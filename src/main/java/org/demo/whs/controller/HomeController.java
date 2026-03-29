package org.demo.whs.controller;

import org.demo.whs.security.SecurityUtils;
import org.demo.whs.utils.annotation.RateLimit;
import org.demo.whs.entity.enums.RateLimitType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Home Controller
 * Base controller for handling home-related requests
 *
 * API Version: v1
 */
@RequestMapping("/api/v1/home")
@RestController
@RateLimit(key = "home", limit = 100, duration = 60, type = RateLimitType.IP)
public class HomeController {

    @PreAuthorize("hasAuthority('PERM_SYSTEM_DIAGNOSTIC_READ')")
    @GetMapping("/test")
    public String testEndpoint() {
        return "Home Controller is working!";
    }

    @GetMapping("/me")
    public ResponseEntity<String> me() {
        String currentUser = SecurityUtils.getCurrentUsername();
        return ResponseEntity.ok("Current user: " + currentUser);
    }
}
