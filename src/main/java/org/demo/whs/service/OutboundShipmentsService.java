package org.demo.whs.service;

import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing OutboundShipments.
 */
public interface OutboundShipmentsService {

    // ==================== CRUD ====================
    
    /**
     * Create shipment (DRAFT)
     */
    OutboundShipmentsResponse create(OutboundShipmentsRequest request);

    /**
     * Query all shipments with filters
     */
    PageResponse<OutboundShipmentsResponse> getAll(OutboundShipmentsFilterRequest filter, Pageable pageable);

    /**
     * Get shipment by ID
     */
    OutboundShipmentsResponse getById(String id);

    /**
     * Update shipment (ONLY DRAFT)
     */
    OutboundShipmentsResponse update(String id, UpdateOutboundShipmentsRequest request);

    // ==================== WORKFLOW ====================

    /**
     * Transition: DRAFT -> PICKING
     */
    OutboundShipmentsResponse startPicking(String id);

    /**
     * Transition: PICKING -> PACKED
     */
    OutboundShipmentsResponse markAsPacked(String id);

    /**
     * Transition: PACKED -> STAGING
     * Move shipment into staging area without decreasing inventory yet
     */
    OutboundShipmentsResponse ship(String id);

    /**
     * Transition: STAGING -> SHIPPED
     * Confirm physical dispatch and decrease inventory
     */
    OutboundShipmentsResponse confirmDispatch(String id);

    /**
     * Transition: Any status except SHIPPED -> CANCELLED
     */
    OutboundShipmentsResponse cancel(String id);
}
