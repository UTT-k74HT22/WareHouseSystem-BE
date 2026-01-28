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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests cho EmailController
 */
@WebMvcTest(EmailController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RateLimitService rateLimitService;

    // ===== Helper methods =====

    private SendEmailRequest buildSendEmailRequest() {
        return SendEmailRequest.builder()
                .recipient("test@example.com")
                .subject("Test Email")
                .content("<h1>Test</h1>")
                .emailType(EmailType.NOTIFICATION)
                .priority(5)
                .async(false)
                .build();
    }

    private EmailLogResponse buildEmailLogResponse() {
        return EmailLogResponse.builder()
                .id("test-id-123")
                .recipient("test@example.com")
                .subject("Test Email")
                .emailType(EmailType.NOTIFICATION)
                .status(EmailStatus.SENT)
                .retryCount(0)
                .hasAttachment(false)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .triggeredByUsername("admin")
                .build();
    }

    private EmailLog buildEmailLogEntity() {
        EmailLog entity = new EmailLog();
        entity.setId("test-id-123");
        entity.setRecipient("test@example.com");
        entity.setSubject("Test Email");
        entity.setEmailType(EmailType.NOTIFICATION);
        entity.setStatus(EmailStatus.SENT);
        entity.setRetryCount(0);
        entity.setMaxRetry(3);
        entity.setHasAttachment(false);
        entity.setPriority(5);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }


    // =============== TEST: SEND EMAIL ===============

    @Test
    @DisplayName("Send email thành công → 201 CREATED")
    void testSendEmail_Success() throws Exception {
        // EmailLog mà service trả về
        EmailLog emailLog = buildEmailLogEntity();
        EmailLogResponse response = buildEmailLogResponse();

        when(emailService.sendEmail(any(SendEmailRequest.class))).thenReturn(emailLog);
        when(emailService.getEmailLog(eq("test-id-123"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/emails/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildSendEmailRequest())))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.recipient").value("test@example.com"));
    }

    // Nếu bạn vẫn muốn test 403 vì thiếu quyền ADMIN thì:
    // cần bật security/method-security. Hiện tại addFilters=false + không import SecurityConfig
    // nên PreAuthorize sẽ không chạy → nếu muốn 403 thì ta sẽ viết Integration Test riêng.

    // =============== TEST: GET ALL EMAIL LOGS ===============

    @Test
    @DisplayName("Get all email logs thành công → 200 OK")
    void testGetAllEmailLogs_Success() throws Exception {
        EmailLogResponse response = buildEmailLogResponse();
        when(emailService.getAllEmailLogs(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/emails")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("test-id-123"))
                .andExpect(jsonPath("$.content[0].recipient").value("test@example.com"));
    }

    // =============== TEST: GET BY ID ===============

    @Test
    @DisplayName("Get email log by id thành công → 200 OK")
    void testGetEmailLog_Success() throws Exception {
        EmailLogResponse response = buildEmailLogResponse();
        when(emailService.getEmailLog("test-id-123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/emails/{id}", "test-id-123"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.recipient").value("test@example.com"));
    }

    // =============== TEST: STATISTICS ===============

    @Test
    @DisplayName("Get email statistics thành công → 200 OK")
    void testGetEmailStatistics_Success() throws Exception {
        Map<String, Long> stats = Map.of(
                "total", 100L,
                "pending", 10L,
                "sent", 80L,
                "failed", 5L,
                "retry", 5L
        );
        when(emailService.getEmailStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/emails/statistics"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.sent").value(80));
    }
}
