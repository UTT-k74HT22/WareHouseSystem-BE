package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.BatchService;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
/**
 * Service interface for batch operations.
 */
public interface BatchService {

    BatchResponse createBatch (CreateBatchRequest request);

    BatchResponse getBatchById(String id);

    PageResponse<BatchResponse> getAllBatches(Integer page, Integer size);

    BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request);

    BatchResponse updateBatch(String id, UpdateBatchRequest request);

    BatchResponse quarantineBatch(String id);

    BatchResponse releaseBatch(String id);
}
