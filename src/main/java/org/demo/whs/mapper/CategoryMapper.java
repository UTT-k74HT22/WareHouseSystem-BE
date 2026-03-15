package org.demo.whs.mapper;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryRequest;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    /**
     * Converts a CreateCategoryRequest DTO to a Category entity.
     *
     * @param request the CreateCategoryRequest containing category details
     * @return a Category entity populated with data from the request
     */
    public Category toEntity(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

    /**
     * Converts a Category entity to a CategoryResponse DTO.
     *
     * @param entity the Category entity to convert
     * @return a CategoryResponse DTO populated with data from the entity
     */
    public CategoryResponse toResponse(Category entity) {
        if (entity == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    /**
     * Converts a list of Category entities to a list of CategoryResponse DTOs.
     *
     * @param entities the list of Category entities to convert
     * @return a list of CategoryResponse DTOs populated with data from the entities
     */
    public List<CategoryResponse> toResponseList(List<Category> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing Category entity with data from UpdateCategoryRequest.
     * Only updates fields that are not null in the request.
     *
     * @param entity  the existing Category entity to update
     * @param request the update request containing new values
     */
    public void updateEntity(Category entity, UpdateCategoryRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}
