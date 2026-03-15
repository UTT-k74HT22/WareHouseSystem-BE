package org.demo.whs.repository;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.enums.BatchStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Batch Repository Query Integration Tests")
class BatchRepositoryQueryIntegrationTest {

    @Autowired
    private BatchRepository batchRepository;

    @Test
    void should_FindExpiringBatches_When_StatusAndExpiryMatch() {
        Batch available = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 3, 30);
        Batch quarantine = batch("B2", "P1", "BATCH-002", BatchStatus.QUARANTINE, 5, 20);
        Batch recalled = batch("B3", "P1", "BATCH-003", BatchStatus.RECALLED, 2, 10);
        Batch future = batch("B4", "P1", "BATCH-004", BatchStatus.AVAILABLE, 45, 5);

        batchRepository.saveAllAndFlush(List.of(available, quarantine, recalled, future));

        List<Batch> results = batchRepository.findExpiringBatches(
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                List.of(BatchStatus.AVAILABLE, BatchStatus.QUARANTINE)
        );

        assertThat(results).extracting(batch -> batch.getId().trim()).containsExactly("B1", "B2");
    }

    @Test
    void should_FindBatchesByProduct_When_OrderedByManufacturingAndExpiry() {
        Batch oldest = batch("B1", "P1", "BATCH-001", BatchStatus.AVAILABLE, 20, 40);
        Batch middle = batch("B2", "P1", "BATCH-002", BatchStatus.RECALLED, 15, 20);
        Batch newest = batch("B3", "P1", "BATCH-003", BatchStatus.QUARANTINE, 10, 5);
        Batch otherProduct = batch("B4", "P2", "BATCH-004", BatchStatus.AVAILABLE, 12, 25);

        batchRepository.saveAllAndFlush(List.of(newest, middle, oldest, otherProduct));

        List<Batch> results = batchRepository.findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc("P1");

        assertThat(results).extracting(batch -> batch.getId().trim()).containsExactly("B1", "B2", "B3");
    }

    private Batch batch(String id, String productId, String batchNumber, BatchStatus status, long expiryInDays, long manufacturingDaysAgo) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setProductId(productId);
        batch.setBatchNumber(batchNumber);
        batch.setStatus(status);
        batch.setManufacturingDate(LocalDate.now().minusDays(manufacturingDaysAgo));
        batch.setExpiryDate(LocalDate.now().plusDays(expiryInDays));
        return batch;
    }
}
