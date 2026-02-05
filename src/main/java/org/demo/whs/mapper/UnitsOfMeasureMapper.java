package org.demo.whs.mapper;

import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UpdateUnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.springframework.stereotype.Component;

import java.util.List;

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
     * @param unitsOfMeasure the UnitsOfMeasure entity
     * @return the corresponding UnitsOfMeasureResponse DTO
     */
    public UnitsOfMeasureResponse toResponse(UnitsOfMeasure unitsOfMeasure) {
        if (unitsOfMeasure == null) {
            return null;
        }

        return UnitsOfMeasureResponse.builder()
                .id(unitsOfMeasure.getId())
                .code(unitsOfMeasure.getCode())
                .name(unitsOfMeasure.getName())
                .description(unitsOfMeasure.getDescription())
                .type(unitsOfMeasure.getType())
                .build();
    }

    /**
     * Converts a list of UnitsOfMeasure entities to a list of UnitsOfMeasureResponse DTOs.
     *
     * @param unitsOfMeasures the list of UnitsOfMeasure entities
     * @return the corresponding list of UnitsOfMeasureResponse DTOs
     */
    public List<UnitsOfMeasureResponse> toResponse(List<UnitsOfMeasure> unitsOfMeasures) {
        if (unitsOfMeasures == null) {
            return null;
        }

        return unitsOfMeasures.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Updates an existing UnitsOfMeasure entity with data from UpdateUnitsOfMeasureRequest.
     *
     * @param entity  the existing UnitsOfMeasure entity to update
     * @param request the UpdateUnitsOfMeasureRequest DTO containing new data
     */
    public void updateEntity(UnitsOfMeasure entity, UpdateUnitsOfMeasureRequest request) {
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
    }
}
