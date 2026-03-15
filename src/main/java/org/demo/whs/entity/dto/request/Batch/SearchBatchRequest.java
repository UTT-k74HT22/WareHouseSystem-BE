package org.demo.whs.entity.dto.request.Batch;

import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BatchStatus;

import java.time.LocalDate;

/**
 * Request DTO for searching batches with business filters.
 * All fields are optional and map to GET /api/v1/batches query parameters.
 */
@Getter
@Setter
public class SearchBatchRequest {

    private String keyword;

    private String productId;

    private String warehouseId;

    private BatchStatus status;

    private LocalDate manufacturingDateFrom;

    private LocalDate manufacturingDateTo;

    private LocalDate expiryDateFrom;

    private LocalDate expiryDateTo;
}
