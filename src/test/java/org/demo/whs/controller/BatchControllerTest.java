package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.service.BatchService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@ActiveProfiles("test")
@WithMockUser
@Import({BatchControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class BatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BatchService batchService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should update batch when request is valid")
    void should_UpdateBatch_When_RequestIsValid() throws Exception {
        when(batchService.updateBatch(eq("batch-1"), any())).thenReturn(buildResponse("batch-1", BatchStatus.AVAILABLE));

        mockMvc.perform(put("/api/v1/batches/{id}", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "Updated batch note"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("batch-1"))
                .andExpect(jsonPath("$.message").value("Batch updated successfully"));

        verify(batchService).updateBatch(eq("batch-1"), any());
    }

    @Test
    @DisplayName("Should quarantine batch when request is valid")
    void should_QuarantineBatch_When_RequestIsValid() throws Exception {
        when(batchService.quarantineBatch(eq("batch-1"), any()))
                .thenReturn(buildResponse("batch-1", BatchStatus.QUARANTINE));

        mockMvc.perform(put("/api/v1/batches/{id}/quarantine", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Quality issue found",
                                "notify_manager", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("QUARANTINE"))
                .andExpect(jsonPath("$.message").value("Batch quarantined successfully"));

        verify(batchService).quarantineBatch(eq("batch-1"), any());
    }

    @Test
    @DisplayName("Should return 400 when quarantine request misses reason")
    void should_Return400_When_QuarantineRequestMissesReason() throws Exception {
        mockMvc.perform(put("/api/v1/batches/{id}/quarantine", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notify_manager", true
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(batchService, never()).quarantineBatch(any(), any());
    }

    @Test
    @DisplayName("Should release batch when request is valid")
    void should_ReleaseBatch_When_RequestIsValid() throws Exception {
        when(batchService.releaseBatch(eq("batch-1"), any()))
                .thenReturn(buildResponse("batch-1", BatchStatus.AVAILABLE));

        mockMvc.perform(put("/api/v1/batches/{id}/release", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "release_notes", "Quality issue resolved"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.message").value("Batch released successfully"));

        verify(batchService).releaseBatch(eq("batch-1"), any());
    }

    @Test
    @DisplayName("Should return 400 when release request misses notes")
    void should_Return400_When_ReleaseRequestMissesNotes() throws Exception {
        mockMvc.perform(put("/api/v1/batches/{id}/release", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));

        verify(batchService, never()).releaseBatch(any(), any());
    }

    @Test
    @DisplayName("Should return 400 when generic status patch is blocked")
    void should_Return400_When_GenericStatusPatchIsBlocked() throws Exception {
        when(batchService.changeBatchStatus(eq("batch-1"), any()))
                .thenThrow(new BadRequestException(ErrorCode.BATCH_011));

        mockMvc.perform(patch("/api/v1/batches/{id}/status", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "EXPIRED"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("BATCH_011"));

        verify(batchService).changeBatchStatus(eq("batch-1"), any());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Should return 403 when quarantine is called without authentication")
    void should_Return403_When_QuarantineWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/batches/{id}/quarantine", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Quality issue found"
                        ))))
                .andExpect(status().isForbidden());

        verify(batchService, never()).quarantineBatch(any(), any());
    }

    private BatchResponse buildResponse(String id, BatchStatus status) {
        return BatchResponse.builder()
                .id(id)
                .batchNumber("BATCH-001")
                .productId("prod-1")
                .manufacturingDate(LocalDate.now().minusDays(5))
                .expiryDate(LocalDate.now().plusDays(30))
                .status(status)
                .createdBy("user-1")
                .createdAt(LocalDateTime.now())
                .updatedBy("user-1")
                .updatedAt(LocalDateTime.now())
                .build();
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
