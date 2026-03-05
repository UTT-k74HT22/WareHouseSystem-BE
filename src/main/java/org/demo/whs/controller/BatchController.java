package org.demo.whs.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.service.BatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/v1/batches")
@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<BaseResponse<BatchResponse>> createBatch(
            @RequestBody @Valid CreateBatchRequest request) {
        log.info("Create batch request: {}", request);
        BatchResponse response = batchService.createBatch(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Batch created successfully"));
    }


    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<BatchResponse>> updateBatch(
            @PathVariable String id,
            @RequestBody @Valid UpdateBatchRequest request) {

        log.info("Received update request for batch id={}, payload={}", id, request);

        BatchResponse response = batchService.updateBatch(id, request);

        log.info("Update completed for batch id={}", id);

        return ResponseEntity.ok(
                BaseResponse.success(response, "Batch updated successfully")
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<BatchResponse>> getBatchesById(
            @PathVariable String id) {
        log.info("Get batches with id: {}", id);

        BatchResponse response = batchService.getBatchById(id);
        BaseResponse<BatchResponse> baseResponse = BaseResponse.success(response, "Batch retrieved successfully");
        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<BatchResponse>>> getAllBatches(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Fetching all batches - page: {}, size: {}", page, size);

        PageResponse<BatchResponse> response = batchService.getAllBatches(page, size);
        BaseResponse<PageResponse<BatchResponse>> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }
}
