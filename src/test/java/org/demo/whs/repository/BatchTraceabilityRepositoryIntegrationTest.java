package org.demo.whs.repository;

import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.OutboundShipmentLines;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
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
@DisplayName("Batch Traceability Repository Integration Tests")
class BatchTraceabilityRepositoryIntegrationTest {

    @Autowired
    private InboundReceiptLinesRepository inboundReceiptLinesRepository;

    @Autowired
    private OutboundShipmentLinesRepository outboundShipmentLinesRepository;

    @Autowired
    private StockMovementsRepository stockMovementsRepository;

    @Test
    void should_FindInboundReceiptLinesByBatchOrderedByCreatedAtDesc() {
        InboundReceiptLines older = inboundLine("L1", "IR1", "B1", 5);
        inboundReceiptLinesRepository.saveAndFlush(older);
        pauseForTimestampSeparation();

        InboundReceiptLines newer = inboundLine("L2", "IR2", "B1", 1);
        inboundReceiptLinesRepository.saveAndFlush(newer);

        List<InboundReceiptLines> results = inboundReceiptLinesRepository.findByBatchIdOrderByCreatedAtDesc("B1");

        assertThat(results).extracting(line -> line.getId().trim()).containsExactly("L2", "L1");
    }

    @Test
    void should_FindOutboundShipmentLinesByBatchOrderedByCreatedAtDesc() {
        OutboundShipmentLines older = outboundLine("O1", "OS1", "B1", 4);
        outboundShipmentLinesRepository.saveAndFlush(older);
        pauseForTimestampSeparation();

        OutboundShipmentLines newer = outboundLine("O2", "OS2", "B1", 2);
        outboundShipmentLinesRepository.saveAndFlush(newer);

        List<OutboundShipmentLines> results = outboundShipmentLinesRepository.findByBatchIdOrderByCreatedAtDesc("B1");

        assertThat(results).extracting(line -> line.getId().trim()).containsExactly("O2", "O1");
    }

    @Test
    void should_FindStockMovementsByBatchOrderedByMovementDateDesc() {
        StockMovements older = movement("M1", "B1", 10);
        StockMovements newer = movement("M2", "B1", 1);
        stockMovementsRepository.saveAllAndFlush(List.of(older, newer));

        List<StockMovements> results = stockMovementsRepository.findByBatchIdOrderByMovementDateDesc("B1");

        assertThat(results).extracting(movement -> movement.getId().trim()).containsExactly("M2", "M1");
    }

    private InboundReceiptLines inboundLine(String id, String receiptId, String batchId, long lineNumber) {
        InboundReceiptLines line = new InboundReceiptLines();
        line.setId(id);
        line.setInboundReceiptId(receiptId);
        line.setPurchaseOrderLineId("POL-1");
        line.setProductId("P1");
        line.setBatchId(batchId);
        line.setLocationId("LOC-1");
        line.setLineNumber((int) lineNumber);
        line.setQuantityReceived(new BigDecimal("5"));
        line.setQualityStatus(QualityStatus.PASS);
        return line;
    }

    private OutboundShipmentLines outboundLine(String id, String shipmentId, String batchId, long quantity) {
        OutboundShipmentLines line = new OutboundShipmentLines();
        line.setId(id);
        line.setOutboundShipmentId(shipmentId);
        line.setSalesOrderLineId("SOL-1");
        line.setProductId("P1");
        line.setBatchId(batchId);
        line.setLocationId("LOC-1");
        line.setLineNumber(1);
        line.setQuantityShipped(new BigDecimal(quantity));
        return line;
    }

    private StockMovements movement(String id, String batchId, long hoursAgo) {
        StockMovements movement = new StockMovements();
        movement.setId(id);
        movement.setMovementType(StockMovementsType.INBOUND);
        movement.setProductId("P1");
        movement.setWarehouseId("W1");
        movement.setLocationId("LOC-1");
        movement.setBatchId(batchId);
        movement.setQuantityChange(new BigDecimal("5"));
        movement.setQuantityBefore(BigDecimal.ZERO);
        movement.setQuantityAfter(new BigDecimal("5"));
        movement.setMovementDate(LocalDateTime.now().minusHours(hoursAgo));
        movement.setReferenceType(ReferenceType.INBOUND_RECEIPT);
        movement.setReferenceId("IR1");
        movement.setReferenceNumber("IR-001");
        return movement;
    }

    private void pauseForTimestampSeparation() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while preparing test data", exception);
        }
    }
}
