package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.SalesOrderLines;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.dto.request.SalesOrderLines.CreateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.UpdateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.SalesOrderLinesMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.SalesOrderLinesRepository;
import org.demo.whs.repository.SalesOrdersRepository;
import org.demo.whs.service.SalesOrderLinesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the SalesOrderLinesService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderLinesServiceImpl implements SalesOrderLinesService {

    private final SalesOrderLinesRepository salesOrderLinesRepository;
    private final SalesOrdersRepository salesOrdersRepository;
    private final ProductRepository productRepository;
    private final SalesOrderLinesMapper salesOrderLinesMapper;

    @Override
    @Transactional
    public SalesOrderLinesResponse create(CreateSalesOrderLinesRequest request) {
        log.info("Creating sales order line, request={}", request);

        // Lock the parent Sales Order to prevent concurrent subtotal/line_number issues
        SalesOrders salesOrder = salesOrdersRepository.findByIdForUpdate(request.getSalesOrderId())
                .orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));
        
        validateDraftStatus(salesOrder, "add lines to");
        validateProduct(request.getProductId());

        // Defense-in-depth: Validate quantity
        if (request.getQuantityOrdered() == null || request.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity ordered must be greater than zero", ErrorCode.COM_001);
        }
        
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Unit price cannot be negative", ErrorCode.COM_001);
        }

        // Atomic line number generation
        Integer nextLineNumber = salesOrderLinesRepository.findMaxLineNumber(salesOrder.getId()).orElse(0) + 1;

        SalesOrderLines line = SalesOrderLines.builder()
                .salesOrderId(salesOrder.getId())
                .productId(request.getProductId())
                .lineNumber(nextLineNumber)
                .quantityOrdered(request.getQuantityOrdered())
                .quantityShipped(BigDecimal.ZERO)
                .unitPrice(request.getUnitPrice())
                .lineTotal(request.getUnitPrice().multiply(request.getQuantityOrdered())) // Recalculate on server
                .notes(request.getNotes())
                .build();

        SalesOrderLines savedLine = salesOrderLinesRepository.save(line);
        recalculateAndSaveSalesOrder(salesOrder);

        return salesOrderLinesMapper.toResponse(savedLine);
    }

    @Override
    @Transactional
    public SalesOrderLinesResponse update(String id, UpdateSalesOrderLinesRequest request) {
        log.info("Updating sales order line, id={}, request={}", id, request);

        SalesOrderLines line = salesOrderLinesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sales order line not found", ErrorCode.COM_001));

        // Lock the parent Sales Order
        SalesOrders salesOrder = salesOrdersRepository.findByIdForUpdate(line.getSalesOrderId())
                .orElseThrow(() -> new NotFoundException("Sales order parent not found", ErrorCode.COM_001));

        if (!salesOrder.getId().equals(line.getSalesOrderId())) {
            throw new BadRequestException("Sales order line does not belong to this sales order", ErrorCode.COM_001);
        }
        
        validateDraftStatus(salesOrder, "update lines of");

        if (line.getQuantityShipped().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("Cannot update line with shipped quantity", ErrorCode.COM_001);
        }

        if (request.getQuantityOrdered() != null) {
            if (request.getQuantityOrdered().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Quantity ordered must be greater than zero", ErrorCode.COM_001);
            }
            line.setQuantityOrdered(request.getQuantityOrdered());
        }
        
        if (request.getUnitPrice() != null) {
            if (request.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Unit price cannot be negative", ErrorCode.COM_001);
            }
            line.setUnitPrice(request.getUnitPrice());
        }
        
        if (request.getNotes() != null) {
            line.setNotes(request.getNotes());
        }

        // Recalculate total on server
        line.setLineTotal(line.getUnitPrice().multiply(line.getQuantityOrdered()));
        SalesOrderLines updatedLine = salesOrderLinesRepository.save(line);

        recalculateAndSaveSalesOrder(salesOrder);

        return salesOrderLinesMapper.toResponse(updatedLine);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesOrderLinesResponse> getBySalesOrder(String salesOrderId) {
        log.info("Getting lines for sales order, id={}", salesOrderId);
        
        // Validate PO exists
        salesOrdersRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));

        return salesOrderLinesRepository.findBySalesOrderIdOrderByLineNumberAsc(salesOrderId)
                .stream()
                .map(salesOrderLinesMapper::toResponse)
                .collect(Collectors.toList());
    }

    private SalesOrders validateAndGetSalesOrder(String salesOrderId) {
        return salesOrdersRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales order not found", ErrorCode.COM_001));
    }

    private void validateDraftStatus(SalesOrders salesOrder, String operation) {
        if (salesOrder.getStatus() != SalesOrdersStatus.DRAFT) {
            throw new BadRequestException(
                    String.format("Only draft sales orders can be %s", operation),
                    ErrorCode.COM_001
            );
        }
    }

    private void validateProduct(String productId) {
        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found", ErrorCode.COM_001));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Product is not active", ErrorCode.COM_001);
        }
    }

    private void recalculateAndSaveSalesOrder(SalesOrders salesOrder) {
        List<SalesOrderLines> lines = salesOrderLinesRepository.findBySalesOrderId(salesOrder.getId());
        BigDecimal subTotal = lines.stream()
                .map(SalesOrderLines::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        salesOrder.setSubTotal(subTotal);
        salesOrder.setTotalAmount(subTotal.add(salesOrder.getTaxAmount() != null ? salesOrder.getTaxAmount() : BigDecimal.ZERO));
        salesOrdersRepository.save(salesOrder);
    }
}
