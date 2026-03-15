package org.demo.whs.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UpdateUnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.UnitsOfMeasureMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.service.UnitsOfMeasureService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
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
    private final ProductRepository productRepository;
    private final UnitsOfMeasureMapper unitsOfMeasureMapper;
    private final IdentifierGenerator identifierGenerator;

    /**
     * Creates a new unit of measure.
     *
     * @param request the request DTO containing unit of measure details
     * @return the response DTO of the created unit of measure
     */
    @Override
    @CacheEvict(cacheNames = "uom_list", key = "'all'")
    public UnitsOfMeasureResponse create(UnitsOfMeasureRequest request) {
        String code = identifierGenerator.generateSystemManaged(
                request.getCode(),
                "Unit of measure code",
                "UOM",
                10,
                unitsOfMeasureRepository::existsUnitsOfMeasureByCode
        );
        log.info("Creating unit of measure with code: {}", code);

        validateRequest(request, code);

        UnitsOfMeasure unitsOfMeasure = unitsOfMeasureMapper.buildRequest(request);
        unitsOfMeasure.setCode(code);
        unitsOfMeasureRepository.save(unitsOfMeasure);
        log.info("Unit of measure with code {} created successfully", code);
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

    /**
     * Retrieves a unit of measure by its unique identifier.
     *
     * @param id the unique identifier of the unit of measure
     * @return the response DTO of the unit of measure
     */
    @Override
    @Cacheable(cacheNames = "uom", key = "#id", unless = "#result == null")
    public UnitsOfMeasureResponse findById(String id) {
        log.info("Retrieving unit of measure with id: {}", id);
        UnitsOfMeasure unitsOfMeasure = unitsOfMeasureRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Unit of measure with id {} not found", id);
                    return new BadRequestException(ErrorCode.UOM_001);
                });
        return unitsOfMeasureMapper.toResponse(unitsOfMeasure);
    }

    /**
     * Updates an existing unit of measure.
     *
     * @param id      the unique identifier of the unit of measure to update
     * @param request the request DTO containing updated unit of measure details
     * @return the response DTO of the updated unit of measure
     */
    @Override
    @CacheEvict(cacheNames = {"uom", "uom_list"}, allEntries = true)
    public UnitsOfMeasureResponse update(String id, UpdateUnitsOfMeasureRequest request) {
        log.info("Updating unit of measure with id: {}", id);

        UnitsOfMeasure unitsOfMeasure = unitsOfMeasureRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Unit of measure with id {} not found", id);
                    return new BadRequestException(ErrorCode.UOM_001);
                });

        // Validate request
        validateUpdateRequest(request);

        // Update entity
        unitsOfMeasureMapper.updateEntity(unitsOfMeasure, request);
        unitsOfMeasure.setUpdatedAt(LocalDateTime.now());

        unitsOfMeasureRepository.save(unitsOfMeasure);
        log.info("Unit of measure with id {} updated successfully", id);

        return unitsOfMeasureMapper.toResponse(unitsOfMeasure);
    }

    /**
     * Deletes a unit of measure (hard delete with validation).
     * Only allows deletion if the UOM is not referenced by any products.
     *
     * @param id the unique identifier of the unit of measure to delete
     */
    @Override
    @CacheEvict(cacheNames = {"uom", "uom_list"}, allEntries = true)
    public void delete(String id) {
        log.info("Deleting unit of measure with id: {}", id);

        UnitsOfMeasure unitsOfMeasure = unitsOfMeasureRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Unit of measure with id {} not found", id);
                    return new BadRequestException(ErrorCode.UOM_001);
                });

        long referencedProducts = productRepository.countByUomId(id);
        if (referencedProducts > 0) {
            log.warn("Cannot delete unit of measure id {} because it is referenced by {} product(s)", id, referencedProducts);
            throw new BadRequestException(ErrorCode.UOM_004);
        }

        unitsOfMeasureRepository.delete(unitsOfMeasure);

        log.info("Unit of measure with id {} deleted successfully (hard delete)", id);
    }

    private void validateRequest(UnitsOfMeasureRequest request, String code) {
        if (unitsOfMeasureRepository.existsUnitsOfMeasureByCode(code)) {
            log.error("Unit of measure with code {} already exists", code);
            throw new BadRequestException(ErrorCode.UOM_002);
        }

        if (request.getName() == null || request.getName().isBlank()) {
            log.error("Unit of measure name is required");
            throw new BadRequestException(ErrorCode.UOM_003);
        }
    }

    private void validateUpdateRequest(UpdateUnitsOfMeasureRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            log.error("Unit of measure name is required");
            throw new BadRequestException(ErrorCode.UOM_003);
        }
    }
}
