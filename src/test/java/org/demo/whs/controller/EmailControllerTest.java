package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EmailController
 */
@WebMvcTest(EmailController.class)
@AutoConfigureMockMvc
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmailService emailService;

    private SendEmailRequest sendEmailRequest;
    private EmailLog emailLog;
    private EmailLogResponse emailLogResponse;

    @BeforeEach
    void setUp() {
        sendEmailRequest = SendEmailRequest.builder()
                .recipient("test@example.com")
                .subject("Test Email")
                .content("<h1>Test Content</h1>")
                .emailType(EmailType.NOTIFICATION)
                .async(false)
                .priority(5)
                .build();

        emailLog = EmailLog.builder()
                .recipient("test@example.com")
                .subject("Test Email")
                .content("<h1>Test Content</h1>")
                .emailType(EmailType.NOTIFICATION)
                .status(EmailStatus.SENT)
                .retryCount(0)
                .maxRetry(3)
                .priority(5)
                .hasAttachment(false)
                .build();

        emailLogResponse = EmailLogResponse.builder()
                .id("test-id-123")
                .recipient("test@example.com")
                .subject("Test Email")
                .emailType(EmailType.NOTIFICATION)
                .status(EmailStatus.SENT)
                .retryCount(0)
                .priority(5)
                .hasAttachment(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testSendEmail_Success() throws Exception {
        // Arrange
        when(emailService.sendEmail(any(SendEmailRequest.class))).thenReturn(emailLog);
        when(emailService.getEmailLog(any())).thenReturn(emailLogResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.recipient").value("test@example.com"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(emailService, times(1)).sendEmail(any(SendEmailRequest.class));
        verify(emailService, times(1)).getEmailLog(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testSendEmailAsync_Success() throws Exception {
        // Arrange
        sendEmailRequest.setAsync(true);
        emailLog.setStatus(EmailStatus.PENDING);
        emailLogResponse.setStatus(EmailStatus.PENDING);

        when(emailService.sendEmailAsync(any(SendEmailRequest.class))).thenReturn(emailLog);
        when(emailService.getEmailLog(any())).thenReturn(emailLogResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(emailService, times(1)).sendEmailAsync(any(SendEmailRequest.class));
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetEmailLog_Success() throws Exception {
        // Arrange
        when(emailService.getEmailLog("test-id-123")).thenReturn(emailLogResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/test-id-123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.recipient").value("test@example.com"));

        verify(emailService, times(1)).getEmailLog("test-id-123");
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetAllEmailLogs_Success() throws Exception {
        // Arrange
        List<EmailLogResponse> emailLogs = Arrays.asList(emailLogResponse);
        Page<EmailLogResponse> page = new PageImpl<>(emailLogs);

        when(emailService.getAllEmailLogs(any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/emails")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value("test-id-123"));

        verify(emailService, times(1)).getAllEmailLogs(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetEmailLogsByStatus_Success() throws Exception {
        // Arrange
        List<EmailLogResponse> emailLogs = Arrays.asList(emailLogResponse);
        Page<EmailLogResponse> page = new PageImpl<>(emailLogs);

        when(emailService.getEmailLogsByStatus(any(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/status/SENT")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(emailService, times(1)).getEmailLogsByStatus(any(), any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetEmailLogsByType_Success() throws Exception {
        // Arrange
        List<EmailLogResponse> emailLogs = Arrays.asList(emailLogResponse);
        Page<EmailLogResponse> page = new PageImpl<>(emailLogs);

        when(emailService.getEmailLogsByType(any(), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/type/NOTIFICATION")
                        .with(csrf())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(emailService, times(1)).getEmailLogsByType(any(), any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testRetryEmail_Success() throws Exception {
        // Arrange
        when(emailService.retryEmail("test-id-123")).thenReturn(emailLog);
        when(emailService.getEmailLog("test-id-123")).thenReturn(emailLogResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/test-id-123/retry")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id-123"));

        verify(emailService, times(1)).retryEmail("test-id-123");
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testGetEmailStatistics_Success() throws Exception {
        // Arrange
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", 100L);
        stats.put("pending", 10L);
        stats.put("sent", 80L);
        stats.put("failed", 5L);
        stats.put("retry", 5L);

        when(emailService.getEmailStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/v1/emails/statistics")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.sent").value(80))
                .andExpect(jsonPath("$.failed").value(5));

        verify(emailService, times(1)).getEmailStatistics();
    }

    @Test
    @WithMockUser(authorities = {"USER"})
    void testSendEmail_Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailRequest)))
                .andExpect(status().isForbidden());

        verify(emailService, never()).sendEmail(any());
    }

    @Test
    @WithMockUser(authorities = {"ADMIN"})
    void testSendEmail_InvalidRequest() throws Exception {
        // Arrange
        sendEmailRequest.setRecipient(""); // Invalid email

        // Act & Assert
        mockMvc.perform(post("/api/v1/emails/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendEmailRequest)))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).sendEmail(any());
    }
}
