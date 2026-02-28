package org.demo.whs.service;

import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.BatchService;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;

import java.util.List;

/**
 * Service interface for batch operations.
 */
public interface BatchService {

    BatchResponse createBatch (CreateBatchRequest request);

    PageResponse<BatchResponse> getAllBatches(Integer size, Integer page);
}
