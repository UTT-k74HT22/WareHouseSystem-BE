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

    public Category toEntity(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }

        return Category.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }

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

    public List<CategoryResponse> toResponseList(List<Category> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntity(Category entity, UpdateCategoryRequest request) {
        if (entity == null || request == null) {
            return;
        }

        if (request.getCode() != null && !request.getCode().isBlank()) {
            entity.setCode(request.getCode());
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            entity.setName(request.getName());
        }

        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
    }
}
