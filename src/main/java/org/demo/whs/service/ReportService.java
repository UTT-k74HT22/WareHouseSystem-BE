package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.utils.strategy.export.GeneratedExportFile;

public interface ReportService {

    /**
     * Generates a PDF report of the current stock based on the provided filter criteria.
     *
     * @param filter The criteria to filter the inventory data for the report.
     * @param requestedBy The username or identifier of the user requesting the report, used for auditing purposes.
     * @return A GeneratedExportFile containing the PDF report and metadata about the generated file.
     */
    GeneratedExportFile exportCurrentStockPdf(InventoryFilterRequest filter, String requestedBy);
}
