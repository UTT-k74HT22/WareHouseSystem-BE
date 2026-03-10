package org.demo.whs.mapper;

import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper class for Inbound Receipts.
 */
@Component
public class InboundReceiptsMapper {

    public InboundReceipts toEntity(InboundReceiptsRequest request) {
        if (request == null) {
            return null;
        }

        return InboundReceipts.builder()
                .purchaseOrderId(request.getPurchaseOrderId())
                .receiptDate(request.getReceiptDate() == null ? LocalDate.now() : request.getReceiptDate())
                .status(InboundReceiptsStatus.DRAFT)
                .deliveryNoteNumber(request.getDeliveryNoteNumber())
                .notes(request.getNotes())
                .build();
    }

    public void updateEntity(InboundReceipts entity, UpdateInboundReceiptsRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getReceiptDate() != null) {
            entity.setReceiptDate(request.getReceiptDate());
        }
        if (request.getDeliveryNoteNumber() != null) {
            entity.setDeliveryNoteNumber(request.getDeliveryNoteNumber());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    public InboundReceiptsResponse toResponse(
            InboundReceipts entity,
            PurchaseOrders purchaseOrders,
            Warehouses warehouses,
            List<InboundReceiptLinesResponse> lines
    ) {
        if (entity == null) {
            return null;
        }

        InboundReceiptsResponse response = toResponse(entity, lines);
        if (purchaseOrders != null) {
            response.setPurchaseOrderNumber(purchaseOrders.getPurchaseOrderNumber());
        }
        if (warehouses != null) {
            response.setWarehouseName(warehouses.getName());
        }
        return response;
    }

    public InboundReceiptsResponse toResponse(
            InboundReceipts entity,
            List<InboundReceiptLinesResponse> lines
    ) {
        if (entity == null) {
            return null;
        }

        return InboundReceiptsResponse.builder()
                .id(entity.getId())
                .receiptNumber(entity.getReceiptNumber())
                .purchaseOrderId(entity.getPurchaseOrderId())
                .warehouseId(entity.getWarehouseId())
                .receiptDate(entity.getReceiptDate())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .deliveryNoteNumber(entity.getDeliveryNoteNumber())
                .notes(entity.getNotes())
                .confirmedAt(entity.getConfirmedAt())
                .confirmedBy(entity.getConfirmedBy())
                .lines(lines == null ? List.of() : lines)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public InboundReceiptsResponse toSummaryResponse(InboundReceipts entity) {
        return toResponse(entity, List.of());
    }
}
