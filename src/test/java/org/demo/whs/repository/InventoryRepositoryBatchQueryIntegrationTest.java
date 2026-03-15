package org.demo.whs.repository;

import org.demo.whs.entity.Inventory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Inventory Repository Batch Query Integration Tests")
class InventoryRepositoryBatchQueryIntegrationTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void should_FindInventoryByBatchOrderedByLastMovementDesc() {
        Inventory older = inventory("P1", "W1", "L1", "B1", "10", "0", "1", 5);
        Inventory newer = inventory("P1", "W2", "L2", "B1", "6", "0", "0", 1);
        inventoryRepository.saveAllAndFlush(List.of(older, newer));

        List<Inventory> results = inventoryRepository.findByBatchIdOrderByLastMovementAtDesc("B1");

        assertThat(results).extracting(Inventory::getWarehouseId).containsExactly("W2", "W1");
    }

    @Test
    void should_FindInventoryByWarehouseAndBatchIds_When_FilterApplied() {
        Inventory match = inventory("P1", "W1", "L1", "B1", "10", "0", "1", 2);
        Inventory otherWarehouse = inventory("P1", "W2", "L1", "B1", "8", "0", "0", 2);
        Inventory otherBatch = inventory("P1", "W1", "L1", "B2", "5", "0", "0", 2);
        inventoryRepository.saveAllAndFlush(List.of(match, otherWarehouse, otherBatch));

        List<Inventory> results = inventoryRepository.findByWarehouseIdAndBatchIdIn("W1", List.of("B1"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getWarehouseId()).isEqualTo("W1");
        assertThat(results.get(0).getBatchId()).isEqualTo("B1");
    }

    @Test
    void should_FindInventoryByProductWarehouseAndBatchIds_When_FilterApplied() {
        Inventory match = inventory("P1", "W1", "L1", "B1", "10", "0", "1", 2);
        Inventory otherProduct = inventory("P2", "W1", "L1", "B1", "8", "0", "0", 2);
        Inventory otherBatch = inventory("P1", "W1", "L1", "B2", "5", "0", "0", 2);
        inventoryRepository.saveAllAndFlush(List.of(match, otherProduct, otherBatch));

        List<Inventory> results = inventoryRepository.findByProductIdAndWarehouseIdAndBatchIdIn("P1", "W1", List.of("B1"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo("P1");
        assertThat(results.get(0).getWarehouseId()).isEqualTo("W1");
        assertThat(results.get(0).getBatchId()).isEqualTo("B1");
    }

    private Inventory inventory(String productId, String warehouseId, String locationId, String batchId,
                                String onHand, String quarantine, String reserved, long hoursAgo) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setWarehouseId(warehouseId);
        inventory.setLocationId(locationId);
        inventory.setBatchId(batchId);
        inventory.setOnHandQuantity(new BigDecimal(onHand));
        inventory.setQuarantineQuantity(new BigDecimal(quarantine));
        inventory.setReservedQuantity(new BigDecimal(reserved));
        inventory.setLastMovementAt(LocalDateTime.now().minusHours(hoursAgo));
        return inventory;
    }
}
