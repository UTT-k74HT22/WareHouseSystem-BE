package org.demo.whs.repository;

import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.enums.ReasonType;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("StockAdjustments Constraint Integration Tests")
class StockAdjustmentsConstraintIntegrationTest {

    @Autowired
    private StockAdjustmentsRepository stockAdjustmentsRepository;

    @Test
    void should_RejectAdjustment_When_AdjustmentQuantityIsZero() {
        StockAdjustments adjustment = baseAdjustment();
        adjustment.setQuantityBefore(new BigDecimal("100.00"));
        adjustment.setQuantityAfter(new BigDecimal("100.00"));
        adjustment.setAdjustmentQuantity(BigDecimal.ZERO);

        assertThatThrownBy(() -> stockAdjustmentsRepository.saveAndFlush(adjustment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_RejectAdjustment_When_ApprovedWithoutApprovalMetadata() {
        StockAdjustments adjustment = baseAdjustment();
        adjustment.setStatus(StockAdjustmentsStatus.APPROVED);
        adjustment.setQuantityBefore(new BigDecimal("100.00"));
        adjustment.setQuantityAfter(new BigDecimal("120.00"));
        adjustment.setAdjustmentQuantity(new BigDecimal("20.00"));

        assertThatThrownBy(() -> stockAdjustmentsRepository.saveAndFlush(adjustment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_SaveAdjustment_When_ApprovedWithApprovalMetadata() {
        StockAdjustments adjustment = baseAdjustment();
        adjustment.setStatus(StockAdjustmentsStatus.APPROVED);
        adjustment.setQuantityBefore(new BigDecimal("100.00"));
        adjustment.setQuantityAfter(new BigDecimal("130.00"));
        adjustment.setAdjustmentQuantity(new BigDecimal("30.00"));
        adjustment.setApprovedBy("acc-1");
        adjustment.setApprovedAt(LocalDateTime.now());

        StockAdjustments saved = stockAdjustmentsRepository.saveAndFlush(adjustment);

        assertThat(saved.getId()).isNotBlank();
    }

    private StockAdjustments baseAdjustment() {
        return StockAdjustments.builder()
                .adjustmentNumber("ADJ-TEST-" + UUID.randomUUID())
                .inventoryId("inv-1")
                .productId("prod-1")
                .warehouseId("wh-1")
                .locationId("loc-1")
                .batchId("batch-1")
                .quantityBefore(new BigDecimal("100.00"))
                .quantityAfter(new BigDecimal("120.00"))
                .adjustmentQuantity(new BigDecimal("20.00"))
                .reason(ReasonType.COUNT_ERROR)
                .status(StockAdjustmentsStatus.PENDING_APPROVAL)
                .notes("test")
                .requiresApproval(true)
                .build();
    }
}
