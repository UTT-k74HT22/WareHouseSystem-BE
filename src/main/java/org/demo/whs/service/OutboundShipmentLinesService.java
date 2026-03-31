package org.demo.whs.service;

import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;

import java.util.List;

/**
 * Service interface for managing OutboundShipmentLines.
 */
public interface OutboundShipmentLinesService {
    
    // CREATE
    OutboundShipmentLinesResponse create(OutboundShipmentLinesRequest request);
    
    // READ
    List<OutboundShipmentLinesResponse> getByShipmentId(String shipmentId);
    OutboundShipmentLinesResponse getById(String id);
    
    // UPDATE (ONLY DRAFT SHIPMENT)
    OutboundShipmentLinesResponse update(String id, UpdateOutboundShipmentLinesRequest request);
    
    // REMOVE (soft logic - not hard delete)
    void remove(String id);
}
