package org.demo.whs.entity.dto.response.Batch;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BatchTraceabilityResponse {

    private String batchId;
    private String batchNumber;
    private String productId;
    private String productSku;
    private String productName;
    private BatchStatus status;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private String supplierBatchNumber;
    private String notes;
    private BatchInventorySnapshotResponse inventorySnapshot;
    private List<InboundTraceabilityEvent> inboundReceipts;
    private List<OutboundTraceabilityEvent> outboundShipments;
    private List<StockMovementTraceabilityEvent> stockMovements;
    private List<String> workflowNotes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class InboundTraceabilityEvent {
        private String inboundReceiptId;
        private String receiptNumber;
        private LocalDate receiptDate;
        private InboundReceiptsStatus receiptStatus;
        private String purchaseOrderId;
        private String purchaseOrderNumber;
        private String supplierId;
        private String supplierCode;
        private String supplierName;
        private String warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal quantityReceived;
        private QualityStatus qualityStatus;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime confirmedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OutboundTraceabilityEvent {
        private String outboundShipmentId;
        private String shipmentNumber;
        private LocalDate shipmentDate;
        private OutboundShipmentsStatus shipmentStatus;
        private String salesOrderId;
        private String salesOrderNumber;
        private String customerId;
        private String customerCode;
        private String customerName;
        private String warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal quantityShipped;
        private LocalDateTime pickedAt;
        private String notes;
        private LocalDateTime createdAt;
        private LocalDateTime shippedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class StockMovementTraceabilityEvent {
        private String stockMovementId;
        private StockMovementsType movementType;
        private LocalDateTime movementDate;
        private String warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String locationId;
        private String locationCode;
        private String locationName;
        private BigDecimal quantityChange;
        private BigDecimal quantityBefore;
        private BigDecimal quantityAfter;
        private ReferenceType referenceType;
        private String referenceId;
        private String referenceNumber;
        private String notes;
    }
}
