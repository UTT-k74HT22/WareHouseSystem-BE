package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for category-related operations.
 */
public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);

    PageResponse<CategoryResponse> getCategories(CategoryStatus status, Pageable pageable);

    CategoryResponse getCategoryById(String id);
}
