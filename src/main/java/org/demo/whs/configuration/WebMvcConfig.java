package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Web MVC Configuration
 * 
 * NOTE: Rate limiting đã được chuyển sang Filter (RateLimitFilter)
 * Filter chạy TẠI CỔNG VÀO, trước cả Security Filter Chain
 * Không còn sử dụng Interceptor nữa
 *
 * CORS Configuration:
 * - Primary CORS config nằm trong SecurityConfig (security filter chain)
 * - Config này là backup cho các non-security endpoints (nếu có)
 * - Trong production, chỉ cho phép origins thực tế của frontend
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.max-age:3600}")
    private Long maxAge;

    /**
     * Configure CORS for the application
     * This is a backup configuration for WebMVC layer
     * Primary CORS is handled by SecurityConfig
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        String[] methods = Arrays.stream(allowedMethods.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders(allowedHeaders.equals("*")
                        ? new String[]{"*"}
                        : Arrays.stream(allowedHeaders.split(","))
                                .map(String::trim)
                                .toArray(String[]::new))
                .exposedHeaders("Authorization", "X-Total-Count", "X-RateLimit-Remaining", "X-RateLimit-Reset")
                .allowCredentials(true)
                .maxAge(maxAge);
    }
}
