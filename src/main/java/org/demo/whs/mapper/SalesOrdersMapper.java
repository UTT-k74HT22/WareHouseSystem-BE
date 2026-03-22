package org.demo.whs.mapper;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.entity.enums.CurrencyType;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Mapper class for Sales Orders.
 */
@Component
@RequiredArgsConstructor
public class SalesOrdersMapper {

    private final SalesOrderLinesMapper salesOrderLinesMapper;

    public SalesOrders toEntity(SalesOrdersRequest request) {
        if (request == null) {
            return null;
        }

        return SalesOrders.builder()
                .customerId(request.getCustomerId())
                .warehouseId(request.getWarehouseId())
                .orderDate(request.getOrderDate())
                .requestedDeliveryDate(request.getRequestedDeliveryDate())
                .status(SalesOrdersStatus.DRAFT)
                .subTotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .currency(parseCurrency(request.getCurrency()))
                .notes(request.getNotes())
                .build();
    }

    public SalesOrdersResponse toResponse(SalesOrders entity) {
        if (entity == null) {
            return null;
        }

        return SalesOrdersResponse.builder()
                .id(entity.getId())
                .soNumber(entity.getSoNumber())
                .customerId(entity.getCustomerId())
                .warehouseId(entity.getWarehouseId())
                .orderDate(entity.getOrderDate())
                .requestedDeliveryDate(entity.getRequestedDeliveryDate())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .subTotal(defaultZero(entity.getSubTotal()))
                .taxAmount(defaultZero(entity.getTaxAmount()))
                .totalAmount(defaultZero(entity.getTotalAmount()))
                .currency(entity.getCurrency() == null ? null : entity.getCurrency().name())
                .notes(entity.getNotes())
                .confirmedAt(entity.getConfirmedAt())
                .confirmedBy(entity.getConfirmedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .lines(new ArrayList<>())
                .build();
    }

    public void updateEntity(SalesOrders entity, UpdateSalesOrdersRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCustomerId() != null) {
            entity.setCustomerId(request.getCustomerId());
        }
        if (request.getWarehouseId() != null) {
            entity.setWarehouseId(request.getWarehouseId());
        }
        if (request.getOrderDate() != null) {
            entity.setOrderDate(request.getOrderDate());
        }
        if (request.getRequestedDeliveryDate() != null) {
            entity.setRequestedDeliveryDate(request.getRequestedDeliveryDate());
        }
        if (request.getCurrency() != null) {
            entity.setCurrency(parseCurrency(request.getCurrency()));
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
