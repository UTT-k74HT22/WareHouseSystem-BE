package org.demo.whs.service.impl;

import org.demo.whs.configuration.EmailProperties;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.dto.request.SendEmailRequest;
import org.demo.whs.entity.dto.response.EmailLogResponse;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.EmailLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailLogRepository emailLogRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmailProperties emailProperties;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private EmailProducerService emailProducerService;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private SendEmailRequest sendEmailRequest;
    private EmailLog emailLog;

    @BeforeEach
    void setUp() {
        // Setup email properties
        when(emailProperties.isEnabled()).thenReturn(true);
        when(emailProperties.getFrom()).thenReturn("noreply@warehouse.com");
        when(emailProperties.getFromName()).thenReturn("Warehouse Management System");
        when(emailProperties.getMaxRetry()).thenReturn(3);
        when(emailProperties.isAsyncByDefault()).thenReturn(false);

        // Setup send email request
        sendEmailRequest = SendEmailRequest.builder()
                .recipient("test@example.com")
                .subject("Test Email")
                .content("<h1>Test Content</h1>")
                .emailType(EmailType.NOTIFICATION)
                .priority(5)
                .build();

        // Setup email log
        emailLog = EmailLog.builder()
                .recipient("test@example.com")
                .subject("Test Email")
                .content("<h1>Test Content</h1>")
                .emailType(EmailType.NOTIFICATION)
                .status(EmailStatus.PENDING)
                .retryCount(0)
                .maxRetry(3)
                .priority(5)
                .hasAttachment(false)
                .build();
        emailLog.setCreatedAt(LocalDateTime.now());
        emailLog.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testSendEmail_Success() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);

        // Act
        EmailLog result = emailService.sendEmail(sendEmailRequest);

        // Assert
        assertNotNull(result);
        verify(emailLogRepository, times(2)).save(any(EmailLog.class));
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailAsync_Success() {
        // Arrange
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        doNothing().when(emailProducerService).sendEmailToQueue(any(EmailLog.class));

        // Act
        EmailLog result = emailService.sendEmailAsync(sendEmailRequest);

        // Assert
        assertNotNull(result);
        verify(emailLogRepository, times(1)).save(any(EmailLog.class));
        verify(emailProducerService, times(1)).sendEmailToQueue(any(EmailLog.class));
    }

    @Test
    void testSendSimpleEmail() {
        // Arrange
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendSimpleEmail(
                "test@example.com",
                "Test Subject",
                "Test Content",
                EmailType.NOTIFICATION
        );

        // Assert
        verify(emailLogRepository, atLeastOnce()).save(any(EmailLog.class));
    }

    @Test
    void testGetEmailLog_Success() {
        // Arrange
        when(emailLogRepository.findById("test-id-123")).thenReturn(Optional.of(emailLog));

        // Act
        EmailLogResponse result = emailService.getEmailLog("test-id-123");

        // Assert
        assertNotNull(result);
        assertEquals("test-id-123", result.getId());
        assertEquals("test@example.com", result.getRecipient());
        verify(emailLogRepository, times(1)).findById("test-id-123");
    }

    @Test
    void testGetEmailLog_NotFound() {
        // Arrange
        when(emailLogRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> {
            emailService.getEmailLog("invalid-id");
        });
        verify(emailLogRepository, times(1)).findById("invalid-id");
    }

    @Test
    void testGetAllEmailLogs() {
        // Arrange
        List<EmailLog> emailLogs = Arrays.asList(emailLog);
        Page<EmailLog> emailLogPage = new PageImpl<>(emailLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailLogRepository.findAll(pageable)).thenReturn(emailLogPage);

        // Act
        Page<EmailLogResponse> result = emailService.getAllEmailLogs(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(emailLogRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetEmailLogsByStatus() {
        // Arrange
        List<EmailLog> emailLogs = Arrays.asList(emailLog);
        Page<EmailLog> emailLogPage = new PageImpl<>(emailLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailLogRepository.findByStatus(EmailStatus.PENDING, pageable))
                .thenReturn(emailLogPage);

        // Act
        Page<EmailLogResponse> result = emailService.getEmailLogsByStatus(EmailStatus.PENDING, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(emailLogRepository, times(1)).findByStatus(EmailStatus.PENDING, pageable);
    }

    @Test
    void testGetEmailLogsByType() {
        // Arrange
        List<EmailLog> emailLogs = Arrays.asList(emailLog);
        Page<EmailLog> emailLogPage = new PageImpl<>(emailLogs);
        Pageable pageable = PageRequest.of(0, 10);

        when(emailLogRepository.findByEmailType(EmailType.NOTIFICATION, pageable))
                .thenReturn(emailLogPage);

        // Act
        Page<EmailLogResponse> result = emailService.getEmailLogsByType(EmailType.NOTIFICATION, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(emailLogRepository, times(1)).findByEmailType(EmailType.NOTIFICATION, pageable);
    }

    @Test
    void testRetryEmail_Success() {
        // Arrange
        emailLog.setStatus(EmailStatus.FAILED);
        emailLog.setRetryCount(1);

        when(emailLogRepository.findById("test-id-123")).thenReturn(Optional.of(emailLog));
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        doNothing().when(emailProducerService).sendEmailToQueue(any(EmailLog.class));

        // Act
        EmailLog result = emailService.retryEmail("test-id-123");

        // Assert
        assertNotNull(result);
        assertEquals(EmailStatus.RETRY, result.getStatus());
        verify(emailLogRepository, times(1)).findById("test-id-123");
        verify(emailProducerService, times(1)).sendEmailToQueue(any(EmailLog.class));
    }

    @Test
    void testRetryEmail_MaxRetryExceeded() {
        // Arrange
        emailLog.setRetryCount(3);
        emailLog.setMaxRetry(3);

        when(emailLogRepository.findById("test-id-123")).thenReturn(Optional.of(emailLog));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            emailService.retryEmail("test-id-123");
        });
        verify(emailLogRepository, times(1)).findById("test-id-123");
        verify(emailProducerService, never()).sendEmailToQueue(any(EmailLog.class));
    }

    @Test
    void testGetEmailStatistics() {
        // Arrange
        when(emailLogRepository.count()).thenReturn(100L);
        when(emailLogRepository.countByStatus(EmailStatus.PENDING)).thenReturn(10L);
        when(emailLogRepository.countByStatus(EmailStatus.SENT)).thenReturn(80L);
        when(emailLogRepository.countByStatus(EmailStatus.FAILED)).thenReturn(5L);
        when(emailLogRepository.countByStatus(EmailStatus.RETRY)).thenReturn(5L);

        // Act
        Map<String, Long> result = emailService.getEmailStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.get("total"));
        assertEquals(10L, result.get("pending"));
        assertEquals(80L, result.get("sent"));
        assertEquals(5L, result.get("failed"));
        assertEquals(5L, result.get("retry"));
    }

    @Test
    void testSendEmail_WhenEmailDisabled() {
        // Arrange
        when(emailProperties.isEnabled()).thenReturn(false);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);

        // Act
        EmailLog result = emailService.sendEmail(sendEmailRequest);

        // Assert
        assertNotNull(result);
        verify(mailSender, times(1)).createMimeMessage();
        // Email should not be sent when disabled
    }

    @Test
    void testSendTemplateEmail() {
        // Arrange
        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", "John Doe");

        when(emailLogRepository.save(any(EmailLog.class))).thenReturn(emailLog);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any())).thenReturn("<h1>Processed Template</h1>");

        // Act
        emailService.sendTemplateEmail(
                "test@example.com",
                "Welcome",
                "email/welcome-email",
                variables,
                EmailType.WELCOME
        );

        // Assert
        verify(emailLogRepository, atLeastOnce()).save(any(EmailLog.class));
    }
}
