package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.SalesOrderLines.CreateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.UpdateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.demo.whs.service.SalesOrderLinesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing sales order lines.
 */
@RequestMapping("/api/v1/sales-order-lines")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Sales Order Lines", description = "APIs for managing sales order lines")
public class SalesOrderLinesController {

    private final SalesOrderLinesService salesOrderLinesService;

    /**
     * Thêm mới dòng hàng vào đơn bán (Sales Order).
     *
     * Mục đích:
     * - Bổ sung sản phẩm vào đơn ở trạng thái DRAFT
     * - Tự động tính lineTotal và cập nhật tổng tiền đơn
     */
    @Operation(summary = "Add line to sales order")
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_SALES_ORDER_LINE_CREATE')")
    public ResponseEntity<BaseResponse<SalesOrderLinesResponse>> create(@RequestBody @Valid CreateSalesOrderLinesRequest request) {
        log.info("Add line to sales order");
        SalesOrderLinesResponse response = salesOrderLinesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(response));
    }

    /**
     * Cập nhật thông tin dòng hàng trong đơn bán.
     *
     * Mục đích:
     * - Cho phép chỉnh sửa số lượng, giá, ghi chú
     * - Chỉ áp dụng khi đơn ở trạng thái DRAFT và chưa phát sinh giao hàng
     */
    @Operation(summary = "Update sales order line")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_SALES_ORDER_LINE_UPDATE')")
    public ResponseEntity<BaseResponse<SalesOrderLinesResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateSalesOrderLinesRequest request) {
        log.info("Update sales order line with id: {}", id);
        SalesOrderLinesResponse response = salesOrderLinesService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Lấy danh sách các dòng hàng theo Sales Order ID.
     *
     * Mục đích:
     * - Xem chi tiết các sản phẩm trong một đơn bán
     * - Phục vụ hiển thị hoặc xử lý nghiệp vụ tiếp theo (confirm, shipment)
     */
    @Operation(summary = "Get lines by sales order id")
    @GetMapping("/by-so/{soId}")
    @PreAuthorize("hasAuthority('PERM_SALES_ORDER_LINE_READ')")
    public ResponseEntity<BaseResponse<List<SalesOrderLinesResponse>>> getBySalesOrder(@PathVariable String soId) {
        log.info("Get lines for sales order: {}", soId);
        List<SalesOrderLinesResponse> response = salesOrderLinesService.getBySalesOrder(soId);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
