package org.demo.whs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.ReportService;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Endpoints for report exporting")
public class ReportController {

    private final ReportService reportService;

    @GetMapping(value = "/current-stock/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Export current stock report as PDF")
    @PreAuthorize("hasAuthority('PERM_REPORT_CURRENT_STOCK_READ')")
    public ResponseEntity<byte[]> exportCurrentStockPdf(InventoryFilterRequest filter) {
        GeneratedExportFile generatedFile = reportService.exportCurrentStockPdf(
                filter,
                SecurityUtils.getCurrentUsername()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(generatedFile.getFileName()).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(generatedFile.getContent());
    }
}
