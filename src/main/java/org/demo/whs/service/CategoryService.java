package org.demo.whs.service;

import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;

/**
 * Service interface for category-related operations.
 */
public interface CategoryService {

    CategoryResponse createCategory(CreateCategoryRequest request);
}
