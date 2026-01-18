package org.demo.whs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.exception.ErrorCode;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * JWT Access Denied Handler
 * Xử lý các request bị từ chối do không đủ quyền (403 Forbidden)
 *
 * Chức năng:
 * 1. Trả về JSON response thống nhất cho forbidden requests
 * 2. Log security events cho monitoring
 * 3. Không redirect sang error page (vì là REST API)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        log.warn("Access denied - uri={}, method={}, user={}",
                request.getRequestURI(),
                request.getMethod(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        BaseResponse<Void> errorResponse = BaseResponse.<Void>builder()
                .success(false)
                .errorCode(ErrorCode.AUTH_003.getCode())
                .message(ErrorCode.AUTH_003.getMessage())
                .data(null)
                .fieldErrors(null)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
