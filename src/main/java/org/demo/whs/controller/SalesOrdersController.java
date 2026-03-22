package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersFilterRequest;
import org.demo.whs.entity.dto.request.SalesOrders.SalesOrdersRequest;
import org.demo.whs.entity.dto.request.SalesOrders.UpdateSalesOrdersRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.SalesOrders.SalesOrdersResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.SalesOrdersService;
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
 * Controller for managing sales orders.
 */
@RequestMapping("/api/v1/sales-orders")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Sales Orders", description = "APIs for managing sales orders")
@PreAuthorize("isAuthenticated()")
public class SalesOrdersController {

    private final SalesOrdersService salesOrdersService;

    /**
     * Tạo mới đơn bán hàng ở trạng thái nháp (DRAFT).
     *
     * Mục đích:
     * - Ghi nhận nhu cầu đặt hàng
     * - Chưa ảnh hưởng đến tồn kho
     */
    @Operation(summary = "Create sales order draft")
    @PostMapping
    public ResponseEntity<BaseResponse<SalesOrdersResponse>> create(@RequestBody @Valid SalesOrdersRequest request) {
        log.info("Create sales order draft");
        SalesOrdersResponse response = salesOrdersService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Lấy danh sách đơn bán hàng có phân trang, lọc và sắp xếp.
     *
     * Mục đích:
     * - Tra cứu đơn theo nhiều tiêu chí (khách hàng, trạng thái, ngày...)
     * - Hỗ trợ phân trang để tối ưu hiệu năng
     */
    @Operation(summary = "Get paginated sales orders")
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<SalesOrdersResponse>>> getAll(
            @RequestParam(required = false) String soNumber,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDeliveryDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedDeliveryDateTo,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        List<String> allowedSortFields = List.of(
                "createdAt",
                "updatedAt",
                "soNumber",
                "orderDate",
                "requestedDeliveryDate",
                "status"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        SalesOrdersFilterRequest filter = SalesOrdersFilterRequest.builder()
                .soNumber(soNumber)
                .customerId(customerId)
                .warehouseId(warehouseId)
                .status(status)
                .orderDateFrom(orderDateFrom)
                .orderDateTo(orderDateTo)
                .requestedDeliveryDateFrom(requestedDeliveryDateFrom)
                .requestedDeliveryDateTo(requestedDeliveryDateTo)
                .build();

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        PageResponse<SalesOrdersResponse> response = salesOrdersService.getAll(filter, pageable);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy chi tiết một đơn bán hàng theo ID.
     *
     * Mục đích:
     * - Xem thông tin đơn và các dòng sản phẩm
     */
    @Operation(summary = "Get sales order by id")
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<SalesOrdersResponse>> getById(@PathVariable String id) {
        log.info("Get sales order by id: {}", id);
        SalesOrdersResponse response = salesOrdersService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Cập nhật đơn bán hàng ở trạng thái nháp (DRAFT).
     *
     * Mục đích:
     * - Chỉnh sửa thông tin đơn trước khi xác nhận
     *
     * Lưu ý:
     * - Chỉ cho phép cập nhật khi đơn chưa confirm
     */
    @Operation(summary = "Update sales order draft")
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<SalesOrdersResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateSalesOrdersRequest request) {
        log.info("Update sales order draft with id: {}", id);
        SalesOrdersResponse response = salesOrdersService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Xác nhận đơn bán hàng.
     *
     * Mục đích:
     * - Reserve (giữ) tồn kho cho đơn
     * - Ngăn đơn khác sử dụng số lượng này
     *
     * Lưu ý:
     * - Nếu không đủ tồn kho → sẽ báo lỗi
     */
    @Operation(summary = "Confirm sales order")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BaseResponse<SalesOrdersResponse>> confirm(@PathVariable String id) {
        log.info("Confirming sales order with id: {}", id);
        SalesOrdersResponse response = salesOrdersService.confirm(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Hủy đơn bán hàng.
     *
     * Mục đích:
     * - Unreserve (trả lại) tồn kho đã giữ
     * - Dừng xử lý đơn
     *
     * Lưu ý:
     * - Không cho hủy nếu đã có shipment đang xử lý
     */
    @Operation(summary = "Cancel sales order")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BaseResponse<SalesOrdersResponse>> cancel(@PathVariable String id) {
        log.info("Cancelling sales order with id: {}", id);
        SalesOrdersResponse response = salesOrdersService.cancel(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
