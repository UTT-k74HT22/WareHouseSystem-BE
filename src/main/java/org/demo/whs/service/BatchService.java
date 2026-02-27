package org.demo.whs.service;

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
}
