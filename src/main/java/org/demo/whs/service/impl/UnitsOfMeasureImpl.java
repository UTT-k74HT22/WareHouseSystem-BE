package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.UnitsOfMeasureMapper;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.service.UnitsOfMeasureService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of the UnitsOfMeasureService interface.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UnitsOfMeasureImpl implements UnitsOfMeasureService {

    private final UnitsOfMeasureRepository unitsOfMeasureRepository;
    private final UnitsOfMeasureMapper unitsOfMeasureMapper;

    /**
     * Creates a new unit of measure.
     *
     * @param request the request DTO containing unit of measure details
     * @return the response DTO of the created unit of measure
     */
    @Override
    public UnitsOfMeasureResponse create(UnitsOfMeasureRequest request) {
        log.info(("Creating unit of measure with code: {}"), request.getCode());

        //Step validate
        validateRequest(request);

        //Step build and save
        UnitsOfMeasure unitsOfMeasure = unitsOfMeasureMapper.buildRequest(request);
        unitsOfMeasureRepository.save(unitsOfMeasure);
        log.info("Unit of measure with code {} created successfully", request.getCode());
        return unitsOfMeasureMapper.toResponse(unitsOfMeasure);

    }

    /**
     * Retrieves all units of measure.
     *
     * @return a list of response DTOs for all units of measure
     */
    @Override
    @Cacheable(cacheNames = "uom_list", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<UnitsOfMeasureResponse> findAll() {
        log.info("Retrieving all units of measure");
        List<UnitsOfMeasure> list = unitsOfMeasureRepository.findAll();
        return unitsOfMeasureMapper.toResponse(list);
    }


    private void validateRequest(UnitsOfMeasureRequest request) {
        if (request.getCode() != null && unitsOfMeasureRepository.existsUnitsOfMeasureByCode(request.getCode())) {
            log.error("Unit of measure with code {} already exists", request.getCode());
            throw new BadRequestException(ErrorCode.UOM_002);
        }

        if (request.getName() == null || request.getName().isBlank()) {
            log.error("Unit of measure name is required");
            throw new BadRequestException(ErrorCode.UOM_003);
        }
    }
}
