package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import org.demo.whs.interceptor.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration để register RateLimitInterceptor
 * Interceptor sẽ được áp dụng cho tất cả các requests
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    private final RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**") // Áp dụng cho tất cả endpoints
                .excludePathPatterns(
                    "/actuator/**",      // Exclude actuator endpoints
                    "/swagger-ui/**",    // Exclude Swagger UI
                    "/v3/api-docs/**",   // Exclude OpenAPI docs
                    "/error"             // Exclude error page
                );
    }
}
