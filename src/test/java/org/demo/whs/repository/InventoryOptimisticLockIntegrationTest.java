package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Inventory Optimistic Lock Integration Tests")
class InventoryOptimisticLockIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void should_PreventStaleVersionUpdate_When_ConcurrentInventoryUpdatesOccur() {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        String inventoryId = txTemplate.execute(status -> {
            Inventory inventory = Inventory.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .batchId("batch-1")
                    .onHandQuantity(new BigDecimal("100.00"))
                    .reservedQuantity(new BigDecimal("10.00"))
                    .version(0)
                    .build();
            return inventoryRepository.saveAndFlush(inventory).getId();
        });

        Integer staleVersion = txTemplate.execute(status -> inventoryRepository.findById(inventoryId)
                .map(Inventory::getVersion)
                .orElseThrow());

        txTemplate.executeWithoutResult(status -> {
            Inventory current = inventoryRepository.findById(inventoryId).orElseThrow();
            current.setOnHandQuantity(new BigDecimal("80.00"));
            inventoryRepository.saveAndFlush(current);
        });

        Integer updatedRows = txTemplate.execute(status -> jdbcTemplate.update(
                "UPDATE inventory SET on_hand_quantity = ?, version = version + 1 WHERE id = ? AND version = ?",
                new BigDecimal("70.00"),
                inventoryId,
                staleVersion
        ));

        assertThat(updatedRows).isZero();
    }
}
