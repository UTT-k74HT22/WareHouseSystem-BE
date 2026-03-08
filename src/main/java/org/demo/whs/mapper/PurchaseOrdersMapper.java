package org.demo.whs.mapper;

import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.dto.request.PurchaseOrders.PurchaseOrdersRequest;
import org.demo.whs.entity.dto.request.PurchaseOrders.UpdatePurchaseOrdersRequest;
import org.demo.whs.entity.dto.response.PurchaseOrders.PurchaseOrdersResponse;
import org.demo.whs.entity.enums.CurrencyType;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper class for Purchase Orders.
 */
@Component
public class PurchaseOrdersMapper {

    public PurchaseOrders toEntity(PurchaseOrdersRequest request) {
        if (request == null) {
            return null;
        }

        return PurchaseOrders.builder()
                .supplierId(request.getSupplierId())
                .warehouseId(request.getWarehouseId())
                .orderDate(request.getOrderDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .status(PurchaseOrdersStatus.DRAFT)
                .subTotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .currency(parseCurrency(request.getCurrency()))
                .paymentTerms(request.getPaymentTerms())
                .notes(request.getNotes())
                .build();
    }

    public PurchaseOrdersResponse toResponse(PurchaseOrders entity) {
        if (entity == null) {
            return null;
        }

        return PurchaseOrdersResponse.builder()
                .id(entity.getId())
                .purchaseOrderNumber(entity.getPurchaseOrderNumber())
                .supplierId(entity.getSupplierId())
                .warehouseId(entity.getWarehouseId())
                .orderDate(entity.getOrderDate())
                .expectedDeliveryDate(entity.getExpectedDeliveryDate())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .subTotal(defaultZero(entity.getSubTotal()))
                .taxAmount(defaultZero(entity.getTaxAmount()))
                .totalAmount(defaultZero(entity.getTotalAmount()))
                .currency(entity.getCurrency() == null ? null : entity.getCurrency().name())
                .paymentTerms(entity.getPaymentTerms())
                .notes(entity.getNotes())
                .confirmedAt(entity.getConfirmedAt())
                .confirmedBy(entity.getConfirmedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntity(PurchaseOrders entity, UpdatePurchaseOrdersRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getSupplierId() != null) {
            entity.setSupplierId(request.getSupplierId());
        }
        if (request.getWarehouseId() != null) {
            entity.setWarehouseId(request.getWarehouseId());
        }
        if (request.getOrderDate() != null) {
            entity.setOrderDate(request.getOrderDate());
        }
        if (request.getExpectedDeliveryDate() != null) {
            entity.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        }
        if (request.getCurrency() != null) {
            entity.setCurrency(parseCurrency(request.getCurrency()));
        }
        if (request.getPaymentTerms() != null) {
            entity.setPaymentTerms(request.getPaymentTerms());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
    }

    private CurrencyType parseCurrency(String currency) {
        return currency == null ? null : CurrencyType.valueOf(currency.toUpperCase());
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
