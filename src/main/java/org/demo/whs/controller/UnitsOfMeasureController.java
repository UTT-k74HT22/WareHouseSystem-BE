package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.BaseEntity;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.BaseResponse;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.demo.whs.service.UnitsOfMeasureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/v1/units-of-measure")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UnitsOfMeasureController {

    private final UnitsOfMeasureService unitsOfMeasureService;

    /**
     * Endpoint to create a new unit of measure.
     *
     * @param request the request DTO containing unit of measure details
     * @return ResponseEntity containing the created unit of measure response DTO
     */
    @PostMapping
    public ResponseEntity<BaseResponse<UnitsOfMeasureResponse>> create(@RequestBody @Valid UnitsOfMeasureRequest request) {
        log.info("Received request to create unit of measure with code: {}", request.getCode());
        UnitsOfMeasureResponse data = unitsOfMeasureService.create(request);
        BaseResponse<UnitsOfMeasureResponse> response = BaseResponse.success(data);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve all units of measure.
     *
     * @return ResponseEntity containing a list of all units of measure response DTOs
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<UnitsOfMeasureResponse>>> findAll() {
        log.info("Received request to retrieve all units of measure");
        List<UnitsOfMeasureResponse> data = unitsOfMeasureService.findAll();
        BaseResponse<java.util.List<UnitsOfMeasureResponse>> response = BaseResponse.success(data);
        return ResponseEntity.ok(response);
    }
}
