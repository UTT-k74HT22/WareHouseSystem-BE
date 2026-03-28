package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.EmailService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@ActiveProfiles("test")
@Import({EmailControllerSecurityTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
class EmailControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        EmailLog emailLog = new EmailLog();
        emailLog.setId("email-1");

        EmailLogResponse response = EmailLogResponse.builder()
                .id("email-1")
                .recipient("admin@example.com")
                .subject("Subject")
                .emailType(EmailType.NOTIFICATION)
                .status(EmailStatus.SENT)
                .retryCount(0)
                .hasAttachment(false)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .triggeredByUsername("admin")
                .build();

        when(emailService.sendEmail(any(SendEmailRequest.class))).thenReturn(emailLog);
        when(emailService.sendEmailAsync(any(SendEmailRequest.class))).thenReturn(emailLog);
        when(emailService.getEmailLog(anyString())).thenReturn(response);
        when(emailService.getAllEmailLogs(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));
        when(emailService.getEmailLogsByStatus(any(EmailStatus.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));
        when(emailService.getEmailLogsByType(any(EmailType.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));
        when(emailService.getEmailLogsByRecipient(anyString(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));
        when(emailService.retryEmail(anyString())).thenReturn(emailLog);
        when(emailService.getEmailStatistics()).thenReturn(Map.of("total", 1L));
        doNothing().when(emailService).processPendingEmails();
        doNothing().when(emailService).retryFailedEmails();
    }

    @Test
    @WithMockUser(authorities = {
            "PERM_EMAIL_CREATE",
            "PERM_EMAIL_READ",
            "PERM_EMAIL_UPDATE"
    })
    @DisplayName("Should allow admin role on all email endpoints")
    void should_AllowAdminRole_OnAllEmailEndpoints() throws Exception {
        for (MockHttpServletRequestBuilder request : buildAllEndpointRequests()) {
            mockMvc.perform(request)
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should forbid non-admin role on all email endpoints")
    void should_ForbidNonAdminRole_OnAllEmailEndpoints() throws Exception {
        for (MockHttpServletRequestBuilder request : buildAllEndpointRequests()) {
            mockMvc.perform(request)
                    .andExpect(status().isForbidden());
        }
    }

    private List<MockHttpServletRequestBuilder> buildAllEndpointRequests() throws Exception {
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .recipient("admin@example.com")
                .subject("Subject")
                .content("<p>content</p>")
                .emailType(EmailType.NOTIFICATION)
                .priority(5)
                .async(false)
                .build();

        return List.of(
                post("/api/v1/emails/send")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailRequest)),
                get("/api/v1/emails/{id}", "email-1"),
                get("/api/v1/emails"),
                get("/api/v1/emails/status/{status}", EmailStatus.SENT),
                get("/api/v1/emails/type/{type}", EmailType.NOTIFICATION),
                get("/api/v1/emails/recipient/{email}", "admin@example.com"),
                post("/api/v1/emails/{id}/retry", "email-1"),
                get("/api/v1/emails/statistics"),
                post("/api/v1/emails/process-pending"),
                post("/api/v1/emails/retry-failed")
        );
    }

    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .formLogin(AbstractHttpConfigurer::disable)
                    .httpBasic(AbstractHttpConfigurer::disable);
            return http.build();
        }
    }
}
