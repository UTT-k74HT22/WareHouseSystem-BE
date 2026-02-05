package org.demo.whs.mapper;

import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between Units of Measure entities and DTOs.
 */
@Component
public class UnitsOfMeasureMapper {

    /**
     * Converts a UnitsOfMeasureRequest DTO to a UnitsOfMeasure entity.
     *
     * @param request the UnitsOfMeasureRequest DTO
     * @return the corresponding UnitsOfMeasure entity
     */
    public UnitsOfMeasure buildRequest(UnitsOfMeasureRequest request) {
        return UnitsOfMeasure.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .build();
    }

    /**
     * Converts a UnitsOfMeasure entity to a UnitsOfMeasureResponse DTO.
     *
     * @param UnitsOfMeasure the UnitsOfMeasure entity
     * @return the corresponding UnitsOfMeasureResponse DTO
     */
    public UnitsOfMeasureResponse toResponse(UnitsOfMeasure UnitsOfMeasure) {
        if (UnitsOfMeasure == null) {
            return null;
        }

        return UnitsOfMeasureResponse.builder()
                .id(UnitsOfMeasure.getId())
                .code(UnitsOfMeasure.getCode())
                .name(UnitsOfMeasure.getName())
                .description(UnitsOfMeasure.getDescription())
                .type(UnitsOfMeasure.getType())
                .build();
    }
}
