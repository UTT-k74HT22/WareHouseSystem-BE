package org.demo.whs.service;

import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;

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

}
