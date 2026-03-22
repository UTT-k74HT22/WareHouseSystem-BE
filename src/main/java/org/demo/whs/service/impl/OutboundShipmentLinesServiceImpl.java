package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.OutboundShipmentLinesMapper;
import org.demo.whs.repository.*;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.OutboundShipmentLinesService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the OutboundShipmentLinesService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OutboundShipmentLinesServiceImpl implements OutboundShipmentLinesService {

    private final OutboundShipmentLinesRepository outboundShipmentLinesRepository;
    private final OutboundShipmentsRepository outboundShipmentsRepository;
    private final SalesOrderLinesRepository salesOrderLinesRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;
    private final AccountRepository accountRepository;
    private final OutboundShipmentLinesMapper outboundShipmentLinesMapper;

    @Override
    @Transactional
    public OutboundShipmentLinesResponse create(OutboundShipmentLinesRequest request) {
        log.info("Create outbound shipment line, request={}", request);

        // 1. Validate Shipment
        OutboundShipments shipment = outboundShipmentsRepository.findById(request.getOutboundShipmentId())
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT shipments can have lines added", ErrorCode.COM_001);
        }

        // 2. Validate Sales Order Line
        SalesOrderLines soLine = salesOrderLinesRepository.findById(request.getSalesOrderLineId())
                .orElseThrow(() -> new NotFoundException("Sales order line not found", ErrorCode.COM_001));

        if (!soLine.getSalesOrderId().equals(shipment.getSalesOrderId())) {
            throw new BadRequestException("Sales order line does not belong to the shipment's sales order", ErrorCode.COM_001);
        }

        BigDecimal remainingQuantity = soLine.getQuantityOrdered().subtract(soLine.getQuantityShipped());
        if (request.getQuantityShipped().compareTo(remainingQuantity) > 0) {
            throw new BadRequestException("Shipped quantity exceeds remaining quantity for this order line", ErrorCode.COM_001);
        }

        // 3. Validate Product
        Products product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found", ErrorCode.COM_001));

        // 4. Validate Location
        Locations location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new NotFoundException("Location not found", ErrorCode.COM_001));

        if (!location.getWarehouseId().equals(shipment.getWarehouseId())) {
            throw new BadRequestException("Location does not belong to the shipment's warehouse", ErrorCode.COM_001);
        }

        // 5. Validate Batch (if provided)
        if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
            batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new NotFoundException("Batch not found", ErrorCode.COM_001));
        }

        // 6. Create Entity
        OutboundShipmentLines line = outboundShipmentLinesMapper.toEntity(request);
        
        Integer maxLineNumber = outboundShipmentLinesRepository.findMaxLineNumber(shipment.getId());
        line.setLineNumber(maxLineNumber == null ? 1 : maxLineNumber + 1);

        applyAuditFields(line, getCurrentActorId(), true);

        OutboundShipmentLines savedLine = outboundShipmentLinesRepository.save(line);
        
        OutboundShipmentLinesResponse response = outboundShipmentLinesMapper.toResponse(savedLine);
        enrichResponse(response, product, location, line.getBatchId());
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboundShipmentLinesResponse> getByShipmentId(String shipmentId) {
        log.info("Get outbound shipment lines by shipmentId={}", shipmentId);
        return outboundShipmentLinesRepository.findByOutboundShipmentId(shipmentId).stream()
                .map(line -> {
                    OutboundShipmentLinesResponse response = outboundShipmentLinesMapper.toResponse(line);
                    Products product = productRepository.findById(line.getProductId()).orElse(null);
                    Locations location = locationRepository.findById(line.getLocationId()).orElse(null);
                    enrichResponse(response, product, location, line.getBatchId());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OutboundShipmentLinesResponse getById(String id) {
        log.info("Get outbound shipment line by id={}", id);
        OutboundShipmentLines line = findById(id);
        OutboundShipmentLinesResponse response = outboundShipmentLinesMapper.toResponse(line);
        
        Products product = productRepository.findById(line.getProductId()).orElse(null);
        Locations location = locationRepository.findById(line.getLocationId()).orElse(null);
        enrichResponse(response, product, location, line.getBatchId());
        
        return response;
    }

    @Override
    @Transactional
    public OutboundShipmentLinesResponse update(String id, UpdateOutboundShipmentLinesRequest request) {
        log.info("Update outbound shipment line, id={}, request={}", id, request);
        OutboundShipmentLines line = findById(id);
        
        OutboundShipments shipment = outboundShipmentsRepository.findById(line.getOutboundShipmentId())
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT shipments can have lines updated", ErrorCode.COM_001);
        }

        if (request.getQuantityShipped() != null) {
            SalesOrderLines soLine = salesOrderLinesRepository.findById(line.getSalesOrderLineId())
                    .orElseThrow(() -> new NotFoundException("Sales order line not found", ErrorCode.COM_001));
            
            BigDecimal remainingQuantity = soLine.getQuantityOrdered().subtract(soLine.getQuantityShipped());
            if (request.getQuantityShipped().compareTo(remainingQuantity) > 0) {
                throw new BadRequestException("Shipped quantity exceeds remaining quantity for this order line", ErrorCode.COM_001);
            }
        }

        if (request.getLocationId() != null) {
            Locations location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new NotFoundException("Location not found", ErrorCode.COM_001));

            if (!location.getWarehouseId().equals(shipment.getWarehouseId())) {
                throw new BadRequestException("Location does not belong to the shipment's warehouse", ErrorCode.COM_001);
            }
        }

        outboundShipmentLinesMapper.updateEntity(line, request);
        applyAuditFields(line, getCurrentActorId(), false);

        OutboundShipmentLines updatedLine = outboundShipmentLinesRepository.save(line);
        
        OutboundShipmentLinesResponse response = outboundShipmentLinesMapper.toResponse(updatedLine);
        Products product = productRepository.findById(line.getProductId()).orElse(null);
        Locations location = locationRepository.findById(line.getLocationId()).orElse(null);
        enrichResponse(response, product, location, line.getBatchId());
        
        return response;
    }

    @Override
    @Transactional
    public void remove(String id) {
        log.info("Remove outbound shipment line, id={}", id);
        OutboundShipmentLines line = findById(id);
        
        OutboundShipments shipment = outboundShipmentsRepository.findById(line.getOutboundShipmentId())
                .orElseThrow(() -> new NotFoundException("Outbound shipment not found", ErrorCode.COM_001));

        if (shipment.getStatus() != OutboundShipmentsStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT shipments can have lines removed", ErrorCode.COM_001);
        }

        outboundShipmentLinesRepository.delete(line);
    }

    private OutboundShipmentLines findById(String id) {
        return outboundShipmentLinesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Outbound shipment line not found", ErrorCode.COM_001));
    }

    private String getCurrentActorId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));

        return account.getId();
    }

    private void applyAuditFields(OutboundShipmentLines line, String actorId, boolean isCreate) {
        if (isCreate) {
            line.setCreatedBy(actorId);
        }
        line.setUpdatedBy(actorId);
    }

    private void enrichResponse(OutboundShipmentLinesResponse response, Products product, Locations location, String batchId) {
        if (product != null) {
            response.setSku(product.getSku());
            response.setProductName(product.getName());
        }
        if (location != null) {
            response.setLocationName(location.getName());
        }
        if (batchId != null && !batchId.isBlank()) {
            batchRepository.findById(batchId).ifPresent(batch -> response.setBatchNumber(batch.getBatchNumber()));
        }
    }
}
