package org.demo.whs.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UpdateUnitsOfMeasureRequest;
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
        BaseResponse<List<UnitsOfMeasureResponse>> response = BaseResponse.success(data);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a unit of measure by its ID.
     *
     * @param id the unique identifier of the unit of measure
     * @return ResponseEntity containing the unit of measure response DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UnitsOfMeasureResponse>> findById(@PathVariable String id) {
        log.info("Received request to retrieve unit of measure with id: {}", id);
        UnitsOfMeasureResponse data = unitsOfMeasureService.findById(id);
        BaseResponse<UnitsOfMeasureResponse> response = BaseResponse.success(data);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing unit of measure.
     *
     * @param id      the unique identifier of the unit of measure to update
     * @param request the request DTO containing updated unit of measure details
     * @return ResponseEntity containing the updated unit of measure response DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UnitsOfMeasureResponse>> update(
            @PathVariable String id,
            @RequestBody @Valid UpdateUnitsOfMeasureRequest request) {
        log.info("Received request to update unit of measure with id: {}", id);
        UnitsOfMeasureResponse data = unitsOfMeasureService.update(id, request);
        BaseResponse<UnitsOfMeasureResponse> response = BaseResponse.success(data);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to delete a unit of measure (hard delete with validation).
     * Only allows deletion if the UOM is not referenced by any products.
     *
     * @param id the unique identifier of the unit of measure to delete
     * @return ResponseEntity with success message
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Received request to delete unit of measure with id: {}", id);
        unitsOfMeasureService.delete(id);
        BaseResponse<Void> response = BaseResponse.success(null);
        return ResponseEntity.ok(response);
    }
}
