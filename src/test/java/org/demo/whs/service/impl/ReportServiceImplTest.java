package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.entity.enums.ReportExportFormat;
import org.demo.whs.entity.enums.ReportType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.utils.strategy.export.ExportPayload;
import org.demo.whs.utils.strategy.export.ExportStrategy;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;
import org.demo.whs.utils.strategy.report.ReportDataProvider;
import org.demo.whs.utils.strategy.report.ReportDataSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl Unit Tests")
class ReportServiceImplTest {

    @Mock
    private ReportDataProvider stockDataProvider;

    @Mock
    private ExportStrategy pdfExportStrategy;

    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        reportService = new ReportServiceImpl(
                List.of(stockDataProvider),
                List.of(pdfExportStrategy),
                objectMapper
        );
    }

    @Nested
    @DisplayName("exportCurrentStockPdf tests")
    class ExportCurrentStockPdfTests {

        @Test
        @DisplayName("should_ExportPdf_When_ProviderAndStrategyFound")
        void should_ExportPdf_When_ProviderAndStrategyFound() {
            // Arrange
            ReportDataSet dataSet = ReportDataSet.builder()
                    .headers(List.of("SKU", "Qty"))
                    .rows(List.of(Map.of("SKU", "SKU-001", "Qty", 100)))
                    .parameters(Map.of("generatedAt", "2026-09-14"))
                    .build();

            GeneratedExportFile file = GeneratedExportFile.builder()
                    .fileName("current_stock_test.pdf")
                    .content(new byte[]{1, 2, 3})
                    .build();

            when(stockDataProvider.supports(ReportType.CURRENT_STOCK)).thenReturn(true);
            when(pdfExportStrategy.supports(ReportExportFormat.PDF)).thenReturn(true);
            when(stockDataProvider.loadDataSet(isNull(), any())).thenReturn(dataSet);
            when(pdfExportStrategy.export(any(ExportPayload.class))).thenReturn(file);

            // Act
            GeneratedExportFile result = reportService.exportCurrentStockPdf(null, "acc-1");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFileName()).isEqualTo("current_stock_test.pdf");
            verify(stockDataProvider).loadDataSet(isNull(), any());
            verify(pdfExportStrategy).export(any(ExportPayload.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_NoProviderForReportType")
        void should_ThrowBadRequestException_When_NoProviderForReportType() {
            when(stockDataProvider.supports(ReportType.CURRENT_STOCK)).thenReturn(false);

            assertThatThrownBy(() -> reportService.exportCurrentStockPdf(null, "acc-1"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_007.getCode()));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_NoExportStrategyForFormat")
        void should_ThrowBadRequestException_When_NoExportStrategyForFormat() {
            // Provider found but export strategy not found
            ReportDataSet dataSet = ReportDataSet.builder()
                    .headers(List.of())
                    .rows(List.of())
                    .build();

            when(stockDataProvider.supports(ReportType.CURRENT_STOCK)).thenReturn(true);
            when(stockDataProvider.loadDataSet(any(), any())).thenReturn(dataSet);
            when(pdfExportStrategy.supports(ReportExportFormat.PDF)).thenReturn(false);

            assertThatThrownBy(() -> reportService.exportCurrentStockPdf(null, "acc-1"))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.JOB_006.getCode()));
        }

        @Test
        @DisplayName("should_UseDefaultRequestedBy_When_RequestedByIsNullOrBlank")
        void should_UseDefaultRequestedBy_When_RequestedByIsNullOrBlank() {
            ReportDataSet dataSet = ReportDataSet.builder().headers(List.of()).rows(List.of()).build();
            GeneratedExportFile file = GeneratedExportFile.builder()
                    .fileName("current_stock.pdf").content(new byte[0]).build();

            when(stockDataProvider.supports(ReportType.CURRENT_STOCK)).thenReturn(true);
            when(pdfExportStrategy.supports(ReportExportFormat.PDF)).thenReturn(true);
            when(stockDataProvider.loadDataSet(any(), any())).thenReturn(dataSet);
            when(pdfExportStrategy.export(any())).thenReturn(file);

            // null requestedBy -> should default to "system"
            GeneratedExportFile result = reportService.exportCurrentStockPdf(null, null);

            // Verify loadDataSet was invoked with payload containing "system" as generatedBy
            verify(stockDataProvider).loadDataSet(isNull(), argThat(payload ->
                    payload.getParameters() != null &&
                    "system".equals(payload.getParameters().get("generatedBy"))));
            verify(pdfExportStrategy).export(any());
        }

        @Test
        @DisplayName("should_ApplyFilterWhenProvided")
        void should_ApplyFilterWhenProvided() {
            InventoryFilterRequest filter = new InventoryFilterRequest();
            filter.setWarehouseId("wh-1");

            ReportDataSet dataSet = ReportDataSet.builder().headers(List.of()).rows(List.of()).build();
            GeneratedExportFile file = GeneratedExportFile.builder()
                    .fileName("stock.pdf").content(new byte[0]).build();

            when(stockDataProvider.supports(ReportType.CURRENT_STOCK)).thenReturn(true);
            when(pdfExportStrategy.supports(ReportExportFormat.PDF)).thenReturn(true);
            when(stockDataProvider.loadDataSet(any(), any())).thenReturn(dataSet);
            when(pdfExportStrategy.export(any())).thenReturn(file);

            GeneratedExportFile result = reportService.exportCurrentStockPdf(filter, "acc-2");

            assertThat(result).isNotNull();
        }
    }
}
