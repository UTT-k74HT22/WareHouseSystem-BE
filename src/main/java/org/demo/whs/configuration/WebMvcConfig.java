package org.demo.whs.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration
 * 
 * NOTE: Rate limiting đã được chuyển sang Filter (RateLimitFilter)
 * Filter chạy TẠI CỔNG VÀO, trước cả Security Filter Chain
 * Không còn sử dụng Interceptor nữa
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    
    // RateLimitInterceptor đã deprecated - sử dụng RateLimitFilter
}
