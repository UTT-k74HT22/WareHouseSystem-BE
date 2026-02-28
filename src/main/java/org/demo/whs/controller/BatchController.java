package org.demo.whs.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
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

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<BatchResponse>>> getAllBatches(
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "10") Integer size) {
        log.info("Fetching all batchs - page: {}, size: {}", page, size);

        PageResponse<BatchResponse> response = batchService.getAllBatches(page, size);
        BaseResponse<PageResponse<BatchResponse>> baseResponse = BaseResponse.success(response);

        return ResponseEntity.ok(baseResponse);
    }
}
