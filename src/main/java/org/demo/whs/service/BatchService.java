package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Batch.ChangeBatchStatusRequest;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.QuarantineBatchRequest;
import org.demo.whs.entity.dto.request.Batch.ReleaseBatchRequest;
import org.demo.whs.entity.dto.request.Batch.SearchBatchRequest;
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

    /**
     * Create a new batch.
     *
     * @param request the request containing the details of the batch to be created
     * @return the response containing the details of the created batch
     */
    BatchResponse createBatch(CreateBatchRequest request);

    /**
     * Get the details of a batch by its ID.
     *
     * @param id the ID of the batch to be retrieved
     * @return the response containing the details of the batch with the specified ID
     */
    BatchResponse getBatchById(String id);

    /**
     * Get a paginated list of batches based on the provided search criteria.
     *
     * @param request the search criteria for retrieving batches
     * @param page    the page number to retrieve
     * @param size    the number of records per page
     * @return a paginated response containing the list of batches matching the search criteria
     */
    PageResponse<BatchResponse> getAllBatches(SearchBatchRequest request, Integer page, Integer size);

    /**
     * Change the status of a batch.
     *
     * @param id      the ID of the batch to be updated
     * @param request the request containing the new status of the batch
     * @return the response containing the details of the updated batch
     */
    BatchResponse changeBatchStatus(String id, ChangeBatchStatusRequest request);

    /**
     * Update the details of an existing batch.
     *
     * @param id      the ID of the batch to be updated
     * @param request the request containing the updated details of the batch
     * @return the response containing the details of the updated batch
     */
    BatchResponse updateBatch(String id, UpdateBatchRequest request);

    /**
     * Quarantine a batch.
     *
     * @param id      the ID of the batch to be quarantined
     * @param request the request containing the details for quarantining the batch
     * @return the response containing the details of the quarantined batch
     */
    BatchResponse quarantineBatch(String id, QuarantineBatchRequest request);

    /**
     * Release a batch from quarantine.
     *
     * @param id      the ID of the batch to be released
     * @param request the request containing the details for releasing the batch
     * @return the response containing the details of the released batch
     */
    BatchResponse releaseBatch(String id, ReleaseBatchRequest request);

    /**
     * Get the traceability information of a batch by its ID.
     *
     * @param id the ID of the batch to be traced
     * @return the response containing the traceability information of the batch with the specified ID
     */
    BatchTraceabilityResponse getBatchTraceability(String id);

    /**
     * Get a list of batches that are expiring within a specified number of days.
     *
     * @param thresholdDays the number of days to check for expiring batches
     * @param warehouseId   the ID of the warehouse to filter by
     * @return a list of batches that are expiring within the specified number of days
     */
    List<BatchExpiringResponse> getExpiringBatches(Integer thresholdDays, String warehouseId);

    /**
     * Get a list of batch recommendations based on the FIFO (First-In-First-Out) principle for a specific product and warehouse.
     *
     * @param productId   the ID of the product to get recommendations for
     * @param warehouseId the ID of the warehouse to filter by
     * @param limit       the maximum number of recommendations to return
     * @return a list of batch recommendations based on the FIFO principle
     */
    List<BatchFifoRecommendationResponse> getFifoRecommendations(String productId, String warehouseId, Integer limit);

    /**
     * Get a list of batches for a specific product in a specific warehouse.
     *
     * @param productId   the ID of the product to filter by
     * @param warehouseId the ID of the warehouse to filter by
     * @return a list of batches for the specified product and warehouse
     */
    List<BatchByProductResponse> getBatchesByProduct(String productId, String warehouseId);
}
