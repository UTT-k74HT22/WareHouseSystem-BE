package org.demo.whs.mapper;

import org.demo.whs.entity.SalesOrderLines;
import org.demo.whs.entity.dto.request.SalesOrderLines.SalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper class for Sales Order Lines.
 */
@Component
public class SalesOrderLinesMapper {

    public SalesOrderLines toEntity(SalesOrderLinesRequest request) {
        if (request == null) {
            return null;
        }

        return SalesOrderLines.builder()
                .productId(request.getProductId())
                .quantityOrdered(request.getQuantityOrdered())
                .quantityShipped(BigDecimal.ZERO)
                .unitPrice(request.getUnitPrice())
                .lineTotal(request.getQuantityOrdered().multiply(request.getUnitPrice()))
                .notes(request.getNotes())
                .build();
    }

    public SalesOrderLinesResponse toResponse(SalesOrderLines entity) {
        if (entity == null) {
            return null;
        }

        return SalesOrderLinesResponse.builder()
                .id(entity.getId())
                .salesOrderId(entity.getSalesOrderId())
                .productId(entity.getProductId())
                .lineNumber(entity.getLineNumber())
                .quantityOrdered(entity.getQuantityOrdered())
                .quantityShipped(entity.getQuantityShipped())
                .unitPrice(entity.getUnitPrice())
                .lineTotal(entity.getLineTotal())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
