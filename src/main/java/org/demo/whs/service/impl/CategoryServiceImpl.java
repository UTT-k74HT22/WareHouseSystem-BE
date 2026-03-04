package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Category;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.CategoryMapper;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedCode = request.getCode().trim();
        String normalizedName = request.getName().trim();

        boolean duplicatedCode = categoryRepository.existsByCodeIgnoreCase(normalizedCode);
        boolean duplicatedName = categoryRepository.existsByNameIgnoreCase(normalizedName);
        if (duplicatedCode || duplicatedName) {
            log.warn("Category code or name already exists, code={}, name={}", normalizedCode, normalizedName);
            throw new ConflictException(ErrorCode.CAT_002);
        }

        Category category = categoryMapper.toEntity(request);
        category.setCode(normalizedCode);
        category.setName(normalizedName);

        Category savedCategory = categoryRepository.save(category);
        log.info("Category created successfully, id={}, code={}", savedCategory.getId(), savedCategory.getCode());

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getCategories(CategoryStatus status, Pageable pageable) {
        validatePageable(pageable);

        Page<Category> categoryPage = status == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findAllByStatus(status, pageable);

        List<CategoryResponse> responses = categoryMapper.toResponseList(categoryPage.getContent());
        return PageResponse.from(categoryPage, responses);
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0 || pageable.getPageSize() <= 0 || pageable.getPageSize() > 100) {
            throw new BadRequestException(ErrorCode.COM_001);
        }
    }
}
