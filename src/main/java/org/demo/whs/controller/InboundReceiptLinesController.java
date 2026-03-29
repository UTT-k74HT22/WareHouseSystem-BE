package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLineUpdateRequest;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.service.InboundReceiptLinesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RequestMapping("/api/v1/inbound-receipt-lines")
@RestController
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "Inbound Receipt Lines", description = "APIs for managing inbound receipt lines")
public class InboundReceiptLinesController {

    private final InboundReceiptLinesService inboundReceiptLinesService;

    /**
     * Create a new line for an inbound receipt in DRAFT status.
     *
     * @param request the request body containing line details
     * @return the created line details
     */
    @PostMapping
    @Operation(summary = "Create inbound receipt line", description = "Create a new line for an inbound receipt in DRAFT status")
    @PreAuthorize("hasAuthority('PERM_INBOUND_RECEIPT_LINE_CREATE')")
    public ResponseEntity<BaseResponse<InboundReceiptLinesResponse>> create(@Valid @RequestBody InboundReceiptLinesRequest request) {
        log.info("Creating inbound receipt line for receipt: {}", request.getInboundReceiptId());
        InboundReceiptLinesResponse response = inboundReceiptLinesService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Inbound receipt line created successfully"));
    }

    /**
     * Update an existing line of an inbound receipt in DRAFT status.
     *
     * @param id      the ID of the line to update
     * @param request the request body containing updated line details
     * @return the updated line details
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update inbound receipt line", description = "Update an existing line of an inbound receipt in DRAFT status")
    @PreAuthorize("hasAuthority('PERM_INBOUND_RECEIPT_LINE_UPDATE')")
    public ResponseEntity<BaseResponse<InboundReceiptLinesResponse>> update(@PathVariable String id, @Valid @RequestBody InboundReceiptLineUpdateRequest request) {
        log.info("Updating inbound receipt line: {}", id);
        InboundReceiptLinesResponse response = inboundReceiptLinesService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Inbound receipt line updated successfully"));
    }

    /**
     * Delete a line from an inbound receipt in DRAFT status.
     *
     * @param id the ID of the line to delete
     * @return a success message
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete inbound receipt line", description = "Delete a line from an inbound receipt in DRAFT status")
    @PreAuthorize("hasAuthority('PERM_INBOUND_RECEIPT_LINE_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting inbound receipt line: {}", id);
        inboundReceiptLinesService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Inbound receipt line deleted successfully"));
    }

    /**
     * Get all lines for a specific inbound receipt.
     *
     * @param inboundReceiptId the ID of the inbound receipt
     * @return a list of lines associated with the specified inbound receipt
     */
    @GetMapping
    @Operation(summary = "Get lines by inbound receipt", description = "Get all lines for a specific inbound receipt")
    @PreAuthorize("hasAuthority('PERM_INBOUND_RECEIPT_LINE_READ')")
    public ResponseEntity<BaseResponse<List<InboundReceiptLinesResponse>>> findByInboundReceiptId(@RequestParam String inboundReceiptId) {
        log.info("Getting lines for inbound receipt: {}", inboundReceiptId);
        List<InboundReceiptLinesResponse> responses = inboundReceiptLinesService.findByInboundReceiptId(inboundReceiptId);
        return ResponseEntity.ok(BaseResponse.success(responses));
    }
}
