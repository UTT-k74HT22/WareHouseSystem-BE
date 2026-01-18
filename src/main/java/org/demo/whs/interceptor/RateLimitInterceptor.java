package org.demo.whs.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.utils.annotation.RateLimit;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.exception.RateLimitExceededException;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.security.RateLimitFilter;
import org.demo.whs.service.RateLimitService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor để kiểm tra rate limit trước khi request được xử lý
 * 
 * @deprecated Sử dụng {@link RateLimitFilter} thay thế
 * Lý do: Filter chạy TẠI CỔNG VÀO (trước Security Filter Chain),
 * trong khi Interceptor chạy SAU controllers mapping
 * 
 * Flow:
 * 1. Lấy annotation @RateLimit từ method hoặc class
 * 2. Xác định identifier dựa trên RateLimitType (IP, USER, API, GLOBAL)
 * 3. Gọi RateLimitService để check rate limit
 * 4. Nếu exceeded, throw RateLimitExceededException
 * 5. Nếu allowed, thêm rate limit headers vào response và cho phép request tiếp tục
 */
@Deprecated(since = "2.0", forRemoval = true)
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    
    /**
     * Header names cho rate limit info
     */
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        
        // Chỉ xử lý nếu handler là HandlerMethod (controller method)
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        
        // Lấy @RateLimit annotation từ method hoặc class
        RateLimit rateLimitAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimitAnnotation == null) {
            rateLimitAnnotation = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        }
        
        // Nếu không có annotation, skip rate limit check
        if (rateLimitAnnotation == null) {
            return true;
        }
        
        // Xác định identifier dựa trên RateLimitType
        String identifier = getIdentifier(request, rateLimitAnnotation.type());
        
        log.debug("Rate limit check for endpoint: {}, type: {}, identifier: {}", 
            request.getRequestURI(), rateLimitAnnotation.type(), identifier);
        
        // Check rate limit - using deprecated method signature
        // NOTE: Using empty routeKey since interceptor doesn't have access to best matching pattern
        String routeKey = request.getMethod() + ":" + request.getRequestURI();
        RateLimitDTO rateLimitDTO = rateLimitService.checkRateLimit(rateLimitAnnotation, identifier, routeKey);
        
        // Thêm rate limit headers vào response
        addRateLimitHeaders(response, rateLimitDTO);
        
        // Nếu rate limit exceeded, throw exception
        if (!rateLimitDTO.isAllowed()) {
            log.warn("Rate limit exceeded for endpoint: {}, identifier: {}, retry after: {}s", 
                request.getRequestURI(), identifier, rateLimitDTO.getRetryAfter());
            
            throw new RateLimitExceededException(
                rateLimitAnnotation.message(), 
                rateLimitDTO.getRetryAfter()
            );
        }
        
        log.debug("Rate limit check passed for endpoint: {}, remaining: {}", 
            request.getRequestURI(), rateLimitDTO.getRemaining());
        
        return true;
    }
    
    /**
     * Xác định identifier dựa trên RateLimitType
     * 
     * @param request HTTP request
     * @param type Rate limit type
     * @return Identifier string
     */
    private String getIdentifier(HttpServletRequest request, RateLimitType type) {
        return switch (type) {
            case IP -> getClientIpAddress(request);
            case USER -> getUserIdentifier();
            case API -> request.getRequestURI();
            case GLOBAL -> "global";
        };
    }
    
    /**
     * Lấy IP address của client
     * Xử lý cả trường hợp có proxy/load balancer
     * 
     * @param request HTTP request
     * @return IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Lấy user identifier từ SecurityContext
     * Nếu user chưa authenticated, sử dụng "anonymous"
     * 
     * @return Username hoặc "anonymous"
     */
    private String getUserIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        
        return "anonymous";
    }
    
    /**
     * Thêm rate limit headers vào response
     * Headers giúp client biết được rate limit status
     * 
     * @param response HTTP response
     * @param rateLimitDTO Rate limit info
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitDTO rateLimitDTO) {
        response.setHeader(HEADER_LIMIT, String.valueOf(rateLimitDTO.getLimit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(rateLimitDTO.getRemaining()));
        response.setHeader(HEADER_RESET, String.valueOf(rateLimitDTO.getResetTime()));
        
        if (!rateLimitDTO.isAllowed()) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(rateLimitDTO.getRetryAfter()));
        }
    }
}
