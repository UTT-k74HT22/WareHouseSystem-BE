package org.demo.whs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.utils.annotation.RateLimit;
import org.demo.whs.entity.dto.RateLimitDTO;
import org.demo.whs.entity.dto.response.RateLimitErrorResponse;
import org.demo.whs.entity.enums.RateLimitType;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.RateLimitService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter để kiểm tra rate limit TẠI CỔNG VÀO - trước cả Security Filter Chain
 * 
 * Architecture Flow:
 * 1. Request → RateLimitFilter (check rate limit)
 * 2. → Security Filter Chain (authentication/authorization)  
 * 3. → Controllers
 * 
 * Improvements:
 * - Filter runs BEFORE controllers (proper entry point)
 * - Trusted proxy IP extraction (chỉ tin X-Forwarded-For từ proxy)
 * - Route key = METHOD + best matching pattern (tránh cardinality explosion)
 * - Per-endpoint fallback mode (fail-open vs fail-closed)
 * - Semantically correct headers (Retry-After chỉ khi 429)
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RateLimitService rateLimitService;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;
    
    public RateLimitFilter(
            RateLimitService rateLimitService,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
            ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Header names cho rate limit info
     */
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";
    
    /**
     * Trusted proxy IPs - CHỈ TIN X-Forwarded-For từ những IP này
     * Trong production, config qua application.yml
     */
    private static final Set<String> TRUSTED_PROXIES = new HashSet<>(Arrays.asList(
        "127.0.0.1",
        "::1",
        "10.0.0.0/8",     // Private network
        "172.16.0.0/12",  // Private network
        "192.168.0.0/16"  // Private network
        // TODO: Add your actual load balancer/reverse proxy IPs
    ));
    
    /**
     * Excluded paths - không apply rate limit
     */
    private static final Set<String> EXCLUDED_PATHS = new HashSet<>(Arrays.asList(
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/error",
        "/favicon.ico"
    ));
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest requestToUse = wrapRefreshTokenRequest(request);
        String requestURI = requestToUse.getRequestURI();
        
        // Skip excluded paths
        if (isExcludedPath(requestURI)) {
            filterChain.doFilter(requestToUse, response);
            return;
        }
        
        try {
            // Lấy handler method để tìm @RateLimit annotation
            HandlerExecutionChain executionChain = handlerMapping.getHandler(requestToUse);
            if (executionChain == null || !(executionChain.getHandler() instanceof HandlerMethod)) {
                filterChain.doFilter(requestToUse, response);
                return;
            }
            
            HandlerMethod handlerMethod = (HandlerMethod) executionChain.getHandler();
            
            // Lấy @RateLimit annotation từ method hoặc class
            RateLimit rateLimitAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimitAnnotation == null) {
                rateLimitAnnotation = handlerMethod.getBeanType().getAnnotation(RateLimit.class);
            }
            
            // Nếu không có annotation, skip rate limit check
            if (rateLimitAnnotation == null) {
                filterChain.doFilter(requestToUse, response);
                return;
            }
            
            // Xác định identifier dựa trên RateLimitType
            String identifier = getIdentifier(
                requestToUse,
                rateLimitAnnotation.type(),
                rateLimitAnnotation.key()
            );
            
            // Tạo route key = METHOD + best matching pattern
            String routeKey = buildRouteKey(requestToUse, handlerMethod);
            
            log.debug("Rate limit check for endpoint: {}, type: {}, identifier: {}, routeKey: {}", 
                requestURI, rateLimitAnnotation.type(), identifier, routeKey);
            
            // Check rate limit
            RateLimitDTO rateLimitDTO = rateLimitService.checkRateLimit(
                rateLimitAnnotation, identifier, routeKey);
            
            // Thêm rate limit headers (luôn có, cả khi allowed và blocked)
            addRateLimitHeaders(response, rateLimitDTO);
            
            // Nếu rate limit exceeded, trả về 429
            if (!rateLimitDTO.isAllowed()) {
                log.warn("Rate limit exceeded for endpoint: {}, identifier: {}, retry after: {}s", 
                    requestURI, identifier, rateLimitDTO.getRetryAfter());
                
                handleRateLimitExceeded(response, rateLimitAnnotation.message(), rateLimitDTO);
                return;
            }
            
            log.debug("Rate limit check passed for endpoint: {}, remaining: {}", 
                requestURI, rateLimitDTO.getRemaining());
            
            // Allow request to proceed
            filterChain.doFilter(requestToUse, response);
            
        } catch (Exception e) {
            log.error("Error in rate limit filter for URI: {}", requestURI, e);
            // Fail-open: allow request on errors to prevent blocking entire system
            filterChain.doFilter(requestToUse, response);
        }
    }
    
    /**
     * Build route key = METHOD + best matching pattern
     * Sử dụng pattern thay vì actual URI để tránh cardinality explosion
     * 
     * Examples:
     * - POST:/api/v1/auth/login
     * - GET:/api/v1/products/{id}
     * - PUT:/api/v1/users/{userId}
     * 
     * @param request HTTP request
     * @param handlerMethod Handler method
     * @return Route key (METHOD:pattern)
     */
    private String buildRouteKey(HttpServletRequest request, HandlerMethod handlerMethod) {
        String method = request.getMethod();
        
        // Lấy best matching pattern từ handler mapping
        try {
            String pattern = (String) request.getAttribute(
                org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            
            if (pattern != null && !pattern.isEmpty()) {
                return method + ":" + pattern;
            }
        } catch (Exception e) {
            log.debug("Could not get best matching pattern, using URI: {}", e.getMessage());
        }
        
        // Fallback: sử dụng request URI
        return method + ":" + request.getRequestURI();
    }
    
    /**
     * Check if path should be excluded from rate limiting
     * 
     * @param path Request path
     * @return true if excluded
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }
    
    /**
     * Xác định identifier dựa trên RateLimitType
     * 
     * @param request HTTP request
     * @param type Rate limit type
     * @return Identifier string
     */
    private String getIdentifier(HttpServletRequest request, RateLimitType type, String rateLimitKey) {
        return switch (type) {
            case IP -> getTrustedClientIp(request);
            case USER -> getUserIdentifier(request, rateLimitKey);
            case API -> request.getRequestURI();
            case GLOBAL -> "global";
        };
    }
    
    /**
     * Lấy TRUSTED client IP address
     * CHỈ TIN X-Forwarded-For nếu request từ trusted proxy
     * 
     * Security: Tránh IP spoofing bằng cách chỉ tin proxy đã biết
     * 
     * @param request HTTP request
     * @return Trusted IP address
     */
    private String getTrustedClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        
        // Chỉ tin X-Forwarded-For nếu request từ trusted proxy
        if (isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                // X-Forwarded-For format: client, proxy1, proxy2
                // Lấy IP đầu tiên (client IP)
                String clientIp = forwardedFor.split(",")[0].trim();
                if (!clientIp.isEmpty() && !"unknown".equalsIgnoreCase(clientIp)) {
                    log.debug("Using X-Forwarded-For IP: {} (from trusted proxy: {})", 
                        clientIp, remoteAddr);
                    return clientIp;
                }
            }
            
            // Fallback: X-Real-IP (nginx)
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isEmpty() && !"unknown".equalsIgnoreCase(realIp)) {
                log.debug("Using X-Real-IP: {} (from trusted proxy: {})", realIp, remoteAddr);
                return realIp;
            }
        } else {
            log.debug("Request not from trusted proxy ({}), ignoring forwarded headers", remoteAddr);
        }
        
        // Default: remote address
        return remoteAddr;
    }
    
    /**
     * Check if IP is a trusted proxy
     * 
     * @param ip IP address
     * @return true if trusted
     */
    private boolean isTrustedProxy(String ip) {
        // Simple implementation - in production, use proper CIDR matching
        return TRUSTED_PROXIES.contains(ip) || 
               ip.startsWith("127.") || 
               ip.equals("::1") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("192.168.");
    }
    
    /**
     * Lấy user identifier từ SecurityContext
     * 
     * Nếu SecurityContext đã có authentication thì ưu tiên username.
     * Nếu chưa có (ví dụ endpoint permitAll như refresh-token), dùng strategy fallback.
     * 
     * @return Username hoặc "anonymous"
     */
    private String getUserIdentifier(HttpServletRequest request, String rateLimitKey) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }

        if (isRefreshTokenRequest(request, rateLimitKey)) {
            return getRefreshTokenStableIdentifier(request);
        }

        return "anonymous";
    }

    /**
     * Build a stable rate-limit identifier for refresh-token endpoint
     * when user authentication context is not available yet.
     *
     * Strategy: refresh-ip:{clientIp}:token:{sha256(refreshToken)}
     */
    private String getRefreshTokenStableIdentifier(HttpServletRequest request) {
        String clientIp = getTrustedClientIp(request);
        String refreshToken = extractRefreshTokenFromBody(request);

        if (refreshToken == null || refreshToken.isBlank()) {
            return "refresh-ip:" + clientIp;
        }

        return "refresh-ip:" + clientIp + ":token:" + sha256(refreshToken);
    }

    private boolean isRefreshTokenRequest(HttpServletRequest request, String rateLimitKey) {
        if ("refresh-token".equals(rateLimitKey)) {
            return true;
        }
        String requestUri = request.getRequestURI();
        return requestUri != null && requestUri.endsWith("/api/v1/auth/refresh-token");
    }

    private HttpServletRequest wrapRefreshTokenRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.endsWith("/api/v1/auth/refresh-token")) {
            return request;
        }
        if (request instanceof CachedBodyHttpServletRequest) {
            return request;
        }
        try {
            return new CachedBodyHttpServletRequest(request);
        } catch (IOException ex) {
            log.warn("Failed to cache refresh-token request body for rate limiting", ex);
            return request;
        }
    }

    private String extractRefreshTokenFromBody(HttpServletRequest request) {
        try {
            byte[] requestBodyBytes = request.getInputStream().readAllBytes();
            if (requestBodyBytes.length == 0) {
                return null;
            }

            String requestBody = new String(requestBodyBytes, StandardCharsets.UTF_8);
            if (requestBody.isBlank()) {
                return null;
            }

            String refreshToken = objectMapper.readTree(requestBody).path("refresh_token").asText(null);
            if (refreshToken == null || refreshToken.isBlank()) {
                refreshToken = objectMapper.readTree(requestBody).path("refreshToken").asText(null);
            }
            return refreshToken;
        } catch (Exception ex) {
            log.debug("Cannot extract refresh token from request body for rate-limit key: {}", ex.getMessage());
            return null;
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }
    
    /**
     * Thêm rate limit headers vào response
     * Headers giúp client biết được rate limit status
     * 
     * Semantics:
     * - X-RateLimit-Limit: luôn có
     * - X-RateLimit-Remaining: luôn có (0 khi exceeded)
     * - X-RateLimit-Reset: luôn có (Unix timestamp)
     * - Retry-After: CHỈ KHI 429 (rate limit exceeded)
     * 
     * @param response HTTP response
     * @param rateLimitDTO Rate limit info
     */
    private void addRateLimitHeaders(HttpServletResponse response, RateLimitDTO rateLimitDTO) {
        response.setHeader(HEADER_LIMIT, String.valueOf(rateLimitDTO.getLimit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(rateLimitDTO.getRemaining()));
        response.setHeader(HEADER_RESET, String.valueOf(rateLimitDTO.getResetTime()));
        
        // Retry-After CHỈ khi exceeded (429)
        if (!rateLimitDTO.isAllowed() && rateLimitDTO.getRetryAfter() != null) {
            response.setHeader(HEADER_RETRY_AFTER, String.valueOf(rateLimitDTO.getRetryAfter()));
        }
    }
    
    /**
     * Handle rate limit exceeded - trả về 429 response
     * 
     * @param response HTTP response
     * @param message Error message
     * @param rateLimitDTO Rate limit info
     * @throws IOException if error writing response
     */
    private void handleRateLimitExceeded(
            HttpServletResponse response, 
            String message, 
            RateLimitDTO rateLimitDTO) throws IOException {
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        RateLimitErrorResponse errorResponse = new RateLimitErrorResponse(
            ErrorCode.RATE_LIMIT_EXCEEDED.getCode(),
            message,
            rateLimitDTO.getRetryAfter(),
            rateLimitDTO.getLimit(),
            rateLimitDTO.getRemaining(),
            rateLimitDTO.getResetTime()
        );
        
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
