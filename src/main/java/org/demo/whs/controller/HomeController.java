package org.demo.whs.controller;

import org.demo.whs.annotation.RateLimit;
import org.demo.whs.entity.enums.RateLimitType;
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public String testEndpoint() {
        return "Home Controller is working!";
    }
}
