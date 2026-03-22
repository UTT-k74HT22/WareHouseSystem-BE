package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsFilterRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.OutboundShipmentsRequest;
import org.demo.whs.entity.dto.request.OutboundShipments.UpdateOutboundShipmentsRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.OutboundShipments.OutboundShipmentsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.OutboundShipmentsService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing outbound shipment-related operations.
 */
@RequestMapping("/api/v1/outbound-shipments")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Outbound Shipments", description = "APIs for managing outbound shipments")
@PreAuthorize("isAuthenticated()")
public class OutboundShipmentsController {

    private final OutboundShipmentsService outboundShipmentsService;

    // ==================== CRUD ====================

    @PostMapping
    @Operation(summary = "Create a new outbound shipment draft")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> create(@Valid @RequestBody OutboundShipmentsRequest request) {
        OutboundShipmentsResponse response = outboundShipmentsService.create(request);
        return new ResponseEntity<>(BaseResponse.success(response, "Outbound shipment created successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all outbound shipments with filtering and pagination")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<PageResponse<OutboundShipmentsResponse>>> getAll(OutboundShipmentsFilterRequest filter, Pageable pageable) {
        PageResponse<OutboundShipmentsResponse> response = outboundShipmentsService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipments retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an outbound shipment by its ID")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> getById(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipment retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an outbound shipment (DRAFT status only)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> update(@PathVariable String id, @Valid @RequestBody UpdateOutboundShipmentsRequest request) {
        OutboundShipmentsResponse response = outboundShipmentsService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipment updated successfully"));
    }

    // ==================== WORKFLOW ====================

    @PutMapping("/{id}/start-picking")
    @Operation(summary = "Transition: DRAFT -> PICKING")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> startPicking(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.startPicking(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Shipment status changed to PICKING"));
    }

    @PutMapping("/{id}/mark-as-packed")
    @Operation(summary = "Transition: PICKING -> PACKED")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> markAsPacked(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.markAsPacked(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Shipment status changed to PACKED"));
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "Transition: PACKED -> SHIPPED")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> ship(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.ship(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Shipment confirmed and inventory decreased"));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Transition: Any status except SHIPPED -> CANCELLED")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> cancel(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.cancel(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Shipment cancelled successfully"));
    }
}
