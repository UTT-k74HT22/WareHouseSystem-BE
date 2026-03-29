package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchInventorySnapshotResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.enums.BatchStatus;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BatchController.class)
@ActiveProfiles("test")
@WithMockUser(authorities = "PERM_BATCH_READ")
@Import({BatchQueryControllerTest.TestSecurityConfig.class, GlobalExceptionHandle.class})
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
class BatchQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BatchService batchService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("Should get batch traceability when request is valid")
    void should_GetBatchTraceability_When_RequestIsValid() throws Exception {
        when(batchService.getBatchTraceability("batch-1")).thenReturn(buildTraceabilityResponse());

        mockMvc.perform(get("/api/v1/batches/{id}/traceability", "batch-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batch_id").value("batch-1"))
                .andExpect(jsonPath("$.data.inventory_snapshot.total_available_quantity").value(7))
                .andExpect(jsonPath("$.message").value("Batch traceability retrieved successfully"));

        verify(batchService).getBatchTraceability("batch-1");
    }

    @Test
    @DisplayName("Should get expiring batches when request is valid")
    void should_GetExpiringBatches_When_RequestIsValid() throws Exception {
        when(batchService.getExpiringBatches(30, "W1")).thenReturn(List.of(buildExpiringResponse()));

        mockMvc.perform(get("/api/v1/batches/expiring")
                        .param("threshold_days", "30")
                        .param("warehouse_id", "W1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].batch_id").value("batch-1"))
                .andExpect(jsonPath("$.data[0].urgency").value("CRITICAL"));

        verify(batchService).getExpiringBatches(30, "W1");
    }

    @Test
    @DisplayName("Should get FIFO recommendations when request is valid")
    void should_GetFifoRecommendations_When_RequestIsValid() throws Exception {
        when(batchService.getFifoRecommendations("prod-1", "W1", 5)).thenReturn(List.of(buildFifoResponse()));

        mockMvc.perform(get("/api/v1/batches/fifo-recommendations")
                        .param("product_id", "prod-1")
                        .param("warehouse_id", "W1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].recommendation_rank").value(1))
                .andExpect(jsonPath("$.data[0].warehouse_id").value("W1"));

        verify(batchService).getFifoRecommendations("prod-1", "W1", 5);
    }

    @Test
    @DisplayName("Should return 400 when FIFO limit is invalid")
    void should_Return400_When_FifoLimitIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/batches/fifo-recommendations")
                        .param("product_id", "prod-1")
                        .param("warehouse_id", "W1")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("Should get batches by product when request is valid")
    void should_GetBatchesByProduct_When_RequestIsValid() throws Exception {
        when(batchService.getBatchesByProduct("prod-1", "W1")).thenReturn(List.of(buildByProductResponse()));

        mockMvc.perform(get("/api/v1/batches/by-product/{productId}", "prod-1")
                        .param("warehouse_id", "W1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].batch_id").value("batch-1"))
                .andExpect(jsonPath("$.data[0].inventory_snapshot.total_on_hand_quantity").value(8));

        verify(batchService).getBatchesByProduct("prod-1", "W1");
    }

    private BatchTraceabilityResponse buildTraceabilityResponse() {
        return BatchTraceabilityResponse.builder()
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Paracetamol")
                .status(BatchStatus.AVAILABLE)
                .manufacturingDate(LocalDate.now().minusDays(10))
                .expiryDate(LocalDate.now().plusDays(20))
                .inventorySnapshot(buildInventorySnapshot("10", "1", "2", "7"))
                .workflowNotes(List.of("[QUARANTINE] reason=Quality hold"))
                .build();
    }

    private BatchExpiringResponse buildExpiringResponse() {
        return BatchExpiringResponse.builder()
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Paracetamol")
                .status(BatchStatus.AVAILABLE)
                .expiryDate(LocalDate.now().plusDays(5))
                .daysToExpiry(5L)
                .urgency("CRITICAL")
                .inventorySnapshot(buildInventorySnapshot("8", "0", "1", "7"))
                .build();
    }

    private BatchFifoRecommendationResponse buildFifoResponse() {
        return BatchFifoRecommendationResponse.builder()
                .recommendationRank(1)
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Paracetamol")
                .warehouseId("W1")
                .warehouseCode("WH-01")
                .warehouseName("Main Warehouse")
                .status(BatchStatus.AVAILABLE)
                .manufacturingDate(LocalDate.now().minusDays(30))
                .expiryDate(LocalDate.now().plusDays(10))
                .daysToExpiry(10L)
                .inventorySnapshot(buildInventorySnapshot("5", "0", "0", "5"))
                .build();
    }

    private BatchByProductResponse buildByProductResponse() {
        return BatchByProductResponse.builder()
                .batchId("batch-1")
                .batchNumber("BATCH-001")
                .productId("prod-1")
                .productSku("SKU-1")
                .productName("Paracetamol")
                .status(BatchStatus.AVAILABLE)
                .expiryDate(LocalDate.now().plusDays(10))
                .inventorySnapshot(buildInventorySnapshot("8", "1", "0", "7"))
                .build();
    }

    private BatchInventorySnapshotResponse buildInventorySnapshot(String onHand, String quarantine, String reserved, String available) {
        return BatchInventorySnapshotResponse.builder()
                .totalOnHandQuantity(new BigDecimal(onHand))
                .totalQuarantineQuantity(new BigDecimal(quarantine))
                .totalReservedQuantity(new BigDecimal(reserved))
                .totalAvailableQuantity(new BigDecimal(available))
                .warehouseCount(1L)
                .locationCount(1L)
                .warehouses(List.of())
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
