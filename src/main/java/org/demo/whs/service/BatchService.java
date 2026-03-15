package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.PageResponse;

/**
 * Service interface for batch operations.
 */
public interface BatchService {

    BatchResponse createBatch(CreateBatchRequest request);

    BatchResponse getBatchById(String id);

    PageResponse<BatchResponse> getAllBatches(Integer page, Integer size);

    BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request);

    BatchResponse updateBatch(String id, UpdateBatchRequest request);

    BatchResponse quarantineBatch(String id, QuarantineBatchRequest request);

    BatchResponse releaseBatch(String id, ReleaseBatchRequest request);
}
