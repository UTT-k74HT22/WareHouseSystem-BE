package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Category;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryStatusRequest;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.CategoryMapper;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.service.CategoryService;
import org.demo.whs.utils.IdentifierGenerator;
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
    private final IdentifierGenerator identifierGenerator;

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String normalizedCode = identifierGenerator.generateSystemManaged(
                request.getCode(),
                "Category code",
                "CAT",
                20,
                categoryRepository::existsByCodeIgnoreCase
        );
        String normalizedName = request.getName().trim();

        boolean duplicatedName = categoryRepository.existsByNameIgnoreCase(normalizedName);
        if (duplicatedName) {
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
        Page<Category> categoryPage = status == null
                ? categoryRepository.findAll(pageable)
                : categoryRepository.findAllByStatus(status, pageable);

        List<CategoryResponse> responses = categoryMapper.toResponseList(categoryPage.getContent());
        return PageResponse.from(categoryPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(String id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found", ErrorCode.CAT_001));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(String id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found", ErrorCode.CAT_001));

        identifierGenerator.assertSystemManagedFieldNotProvided(request.getCode(), "Category code");

        if (!hasAnyUpdatableField(request)) {
            throw new BadRequestException(ErrorCode.COM_001);
        }

        String normalizedName = normalize(request.getName());
        if (normalizedName != null && categoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw new ConflictException(ErrorCode.CAT_002);
        }

        categoryMapper.updateEntity(category, request);
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategoryStatus(String id, UpdateCategoryStatusRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found", ErrorCode.CAT_001));

        category.setStatus(request.getStatus());
        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    private boolean hasAnyUpdatableField(UpdateCategoryRequest request) {
        return normalize(request.getName()) != null
                || request.getDescription() != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
