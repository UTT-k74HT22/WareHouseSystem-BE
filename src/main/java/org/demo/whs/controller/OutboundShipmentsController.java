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
import org.demo.whs.entity.enums.OutboundShipmentsStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.OutboundShipmentsService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for managing outbound shipment-related operations.
 */
@RequestMapping("/api/v1/outbound-shipments")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Outbound Shipments", description = "APIs for managing outbound shipments")
public class OutboundShipmentsController {

    private final OutboundShipmentsService outboundShipmentsService;
    @PostMapping
    @Operation(summary = "Create a new outbound shipment draft")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_CREATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> create(@Valid @RequestBody OutboundShipmentsRequest request) {
        OutboundShipmentsResponse response = outboundShipmentsService.create(request);
        return new ResponseEntity<>(BaseResponse.success(response, "Tạo phiếu xuất thành công"), HttpStatus.CREATED);
    }
    /**
     * Lấy danh sách Outbound Shipment có phân trang và filter.
     * Nghiệp vụ:
     * - Hỗ trợ filter theo nhiều tiêu chí (status, date, ...)
     * - Phục vụ màn hình quản lý shipment
     * Kết quả:
     * - Trả về danh sách shipment theo pageable
     */
    @GetMapping
    @Operation(summary = "Get all outbound shipments with filtering and pagination")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_READ')")
    public ResponseEntity<BaseResponse<PageResponse<OutboundShipmentsResponse>>> getAll(
            @RequestParam(required = false) String shipmentNumber,
            @RequestParam(required = false) String salesOrderId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) OutboundShipmentsStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shipmentDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shipmentDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        List<String> allowedSortFields = List.of(
                "createdAt", "updatedAt", "shipmentNumber", "shipmentDate", "status");
        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
        if (page == null || page < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (size == null || size <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }
        if (size > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }
        OutboundShipmentsFilterRequest filter = new OutboundShipmentsFilterRequest();
        filter.setShipmentNumber(shipmentNumber);
        filter.setSalesOrderId(salesOrderId);
        filter.setWarehouseId(warehouseId);
        filter.setStatus(status);
        filter.setShipmentDateFrom(shipmentDateFrom);
        filter.setShipmentDateTo(shipmentDateTo);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<OutboundShipmentsResponse> response = outboundShipmentsService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy danh sách phiếu xuất thành công"));
    }
    /**
     * Lấy chi tiết một Outbound Shipment theo ID.
     * Nghiệp vụ:
     * - Dùng để xem thông tin shipment cụ thể
     * - Bao gồm trạng thái và thông tin liên quan
     * Validate:
     * - Shipment phải tồn tại
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_READ')")
    public ResponseEntity<BaseResponse<java.util.Map<String, Long>>> getStats() {
        return ResponseEntity.ok(BaseResponse.success(outboundShipmentsService.getStats()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an outbound shipment by its ID")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_READ')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> getById(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Lấy thông tin phiếu xuất thành công"));
    }
    /**
     * Cập nhật thông tin Outbound Shipment.
     * Nghiệp vụ:
     * - Chỉ cho phép update khi shipment ở trạng thái DRAFT
     * - Không cho sửa khi đã PICKING trở đi
     * Validate:
     * - Shipment tồn tại
     * - Status phải là DRAFT
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an outbound shipment (DRAFT status only)")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> update(@PathVariable String id, @Valid @RequestBody UpdateOutboundShipmentsRequest request) {
        OutboundShipmentsResponse response = outboundShipmentsService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Cập nhật phiếu xuất thành công"));
    }

    /**
     * Chuyển trạng thái shipment từ DRAFT -> PICKING.
     * Nghiệp vụ:
     * - Bắt đầu quá trình lấy hàng trong kho
     * - Nhân viên kho sẽ tiến hành pick hàng theo các line
     * Validate:
     * - Shipment phải ở trạng thái DRAFT
     * - Phải có ít nhất 1 shipment line
     * Kết quả:
     * - Status = PICKING
     */
    @PutMapping("/{id}/start-picking")
    @Operation(summary = "Transition: DRAFT -> PICKING")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> startPicking(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.startPicking(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Phiếu xuất đã chuyển sang trạng thái đang lấy hàng"));
    }
    /**
     * Chuyển trạng thái shipment từ PICKING -> PACKED.
     * Nghiệp vụ:
     * - Xác nhận đã lấy hàng xong và đóng gói
     * - Hàng sẵn sàng để giao
     * Validate:
     * - Shipment phải ở trạng thái PICKING
     * Kết quả:
     * - Status = PACKED
     */
    @PutMapping("/{id}/mark-as-packed")
    @Operation(summary = "Transition: PICKING -> PACKED")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> markAsPacked(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.markAsPacked(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Phiếu xuất đã chuyển sang trạng thái đã đóng gói"));
    }
    /**
     * Chuyển shipment sang khu vực chờ xuất (PACKED -> STAGING).
     * Nghiệp vụ:
     * - Đây là bước commit cuối cùng của nghiệp vụ kho
     * - Thực hiện trừ tồn kho (consume reserved)
     * - Ghi nhận stock movement (OUTBOUND)
     * - Cập nhật số lượng đã giao cho Sales Order Line
     * - Cập nhật trạng thái Sales Order (COMPLETED / PARTIALLY_SHIPPED)
     * Validate:
     * - Shipment phải ở trạng thái PACKED
     * - Phải có ít nhất 1 shipment line
     * - Không được vượt quá số lượng đặt hàng
     * Kết quả:
     * - Status = SHIPPED
     * - Tồn kho bị trừ thực tế
     */
    @PutMapping("/{id}/ship")
    @Operation(summary = "Transition: PACKED -> STAGING")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> ship(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.ship(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Phiếu xuất đã chuyển sang khu vực tập kết thành công"));
    }

    @PutMapping("/{id}/confirm-dispatch")
    @Operation(summary = "Transition: STAGING -> SHIPPED")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> confirmDispatch(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.confirmDispatch(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Xuất hàng và cập nhật tồn kho thành công"));
    }
    /**
     * Hủy Outbound Shipment.
     * Nghiệp vụ:
     * - Cho phép hủy shipment trước khi hoàn tất (chưa SHIPPED)
     * - Nếu đang PICKING hoặc PACKED:
     *   → phải release (unreserve) tồn kho đã giữ
     * Validate:
     * - Không cho phép hủy nếu đã SHIPPED
     * - Không cho phép hủy nếu đã phát sinh stock movement
     * Kết quả:
     * - Status = CANCELLED
     * - Tồn kho được trả lại (nếu có reserve)
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "Transition: Any status except SHIPPED -> CANCELLED")
    @PreAuthorize("hasAuthority('PERM_OUTBOUND_SHIPMENT_UPDATE')")
    public ResponseEntity<BaseResponse<OutboundShipmentsResponse>> cancel(@PathVariable String id) {
        OutboundShipmentsResponse response = outboundShipmentsService.cancel(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Hủy phiếu xuất thành công"));
    }
}
