package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.OutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.request.OutboundShipmentLines.UpdateOutboundShipmentLinesRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.OutboundShipmentLines.OutboundShipmentLinesResponse;
import org.demo.whs.service.OutboundShipmentLinesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing outbound shipment lines-related operations.
 */
@RequestMapping("/api/v1/outbound-shipment-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Outbound Shipment Lines", description = "APIs for managing outbound shipment lines")
@PreAuthorize("isAuthenticated()")
public class OutboundShipmentLinesController {

    private final OutboundShipmentLinesService outboundShipmentLinesService;
    /**
     * Tạo mới một Outbound Shipment Line (chi tiết xuất kho).
     *
     * Nghiệp vụ:
     * - Thêm sản phẩm cần xuất vào shipment
     * - Xác định cụ thể: product, location, batch, quantity
     * Kết quả:
     * - Tạo mới shipment line
     * - Gán lineNumber tự tăng
     */
    @PostMapping
    @Operation(summary = "Create a new outbound shipment line")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentLinesResponse>> create(@Valid @RequestBody OutboundShipmentLinesRequest request) {
        OutboundShipmentLinesResponse response = outboundShipmentLinesService.create(request);
        return new ResponseEntity<>(BaseResponse.success(response, "Outbound shipment line created successfully"), HttpStatus.CREATED);
    }
    /**
     * Lấy danh sách tất cả Shipment Line theo Shipment ID.
     *
     * Nghiệp vụ:
     * - Dùng để hiển thị danh sách hàng cần xuất trong shipment
     * - Bao gồm thông tin product, location, batch
     * Kết quả:
     * - Trả về danh sách các line thuộc shipment
     */
    @GetMapping("/shipment/{shipmentId}")
    @Operation(summary = "Get all lines for a specific outbound shipment")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<List<OutboundShipmentLinesResponse>>> getByShipmentId(@PathVariable String shipmentId) {
        List<OutboundShipmentLinesResponse> response = outboundShipmentLinesService.getByShipmentId(shipmentId);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipment lines retrieved successfully"));
    }
    /**
     * Lấy chi tiết một Outbound Shipment Line theo ID.
     * Nghiệp vụ:
     * - Dùng để xem thông tin cụ thể của một line
     * - Bao gồm product, location, batch, quantity

     * Kết quả:
     * - Trả về thông tin chi tiết của shipment line
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an outbound shipment line by its ID")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentLinesResponse>> getById(@PathVariable String id) {
        OutboundShipmentLinesResponse response = outboundShipmentLinesService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipment line retrieved successfully"));
    }
    /**
     * Cập nhật Outbound Shipment Line.
     * Nghiệp vụ:
     * - Cho phép chỉnh sửa quantity hoặc location trước khi bắt đầu xử lý kho
     * Kết quả:
     * - Line được cập nhật thông tin mới
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an outbound shipment line (DRAFT status only)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<OutboundShipmentLinesResponse>> update(@PathVariable String id, @Valid @RequestBody UpdateOutboundShipmentLinesRequest request) {
        OutboundShipmentLinesResponse response = outboundShipmentLinesService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Outbound shipment line updated successfully"));
    }
    /**
     * Cập nhật Outbound Shipment Line.
     * Nghiệp vụ:
     * - Cho phép chỉnh sửa quantity hoặc location trước khi bắt đầu xử lý kho
     * Kết quả:
     * - Line được cập nhật thông tin mới
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Remove an outbound shipment line (DRAFT status only)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public ResponseEntity<BaseResponse<Void>> remove(@PathVariable String id) {
        outboundShipmentLinesService.remove(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Outbound shipment line removed successfully"));
    }
}
