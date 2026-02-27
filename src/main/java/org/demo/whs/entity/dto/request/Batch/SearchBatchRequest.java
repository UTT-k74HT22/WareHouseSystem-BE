package org.demo.whs.entity.dto.request.Batch;

import lombok.Getter;
import lombok.Setter;
import org.demo.whs.entity.enums.BatchStatus;

/**
 * Request DTO for searching batch with multiple filters.
 * All fields are optional.
 */
@Getter
@Setter
public class SearchBatchRequest {

    private String keyword;

    private BatchStatus status;

    private int page = 1;

    private int size = 10;
}