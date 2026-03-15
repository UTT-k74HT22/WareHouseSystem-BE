package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchByProductResponse;
import org.demo.whs.entity.dto.response.Batch.BatchExpiringResponse;
import org.demo.whs.entity.dto.response.Batch.BatchFifoRecommendationResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Batch.BatchTraceabilityResponse;
import org.demo.whs.entity.dto.response.PageResponse;

import java.util.List;

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

    BatchTraceabilityResponse getBatchTraceability(String id);

    List<BatchExpiringResponse> getExpiringBatches(Integer thresholdDays, String warehouseId);

    List<BatchFifoRecommendationResponse> getFifoRecommendations(String productId, String warehouseId, Integer limit);

    List<BatchByProductResponse> getBatchesByProduct(String productId, String warehouseId);
}
