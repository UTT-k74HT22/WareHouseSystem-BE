package org.demo.whs.service;

import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UpdateUnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import java.util.List;

/**
 * Service interface for units of measure-related operations.
 */
public interface UnitsOfMeasureService {

    /**
     * Creates a new unit of measure.
     *
     * @param request the request DTO containing unit of measure details
     * @return the response DTO of the created unit of measure
     */
    UnitsOfMeasureResponse create(UnitsOfMeasureRequest request);

    /**
     * Retrieves all units of measure.
     *
     * @return a list of response DTOs for all units of measure
     */
    List<UnitsOfMeasureResponse> findAll();

    /**
     * Retrieves a unit of measure by its unique identifier.
     *
     * @param id the unique identifier of the unit of measure
     * @return the response DTO of the unit of measure
     */
    UnitsOfMeasureResponse findById(String id);

    /**
     * Updates an existing unit of measure.
     *
     * @param id      the unique identifier of the unit of measure to update
     * @param request the request DTO containing updated unit of measure details
     * @return the response DTO of the updated unit of measure
     */
    UnitsOfMeasureResponse update(String id, UpdateUnitsOfMeasureRequest request);

    /**
     * Deletes a unit of measure (hard delete with validation).
     * Only allows deletion if the UOM is not referenced by any products.
     *
     * @param id the unique identifier of the unit of measure to delete
     */
    void delete(String id);
}
