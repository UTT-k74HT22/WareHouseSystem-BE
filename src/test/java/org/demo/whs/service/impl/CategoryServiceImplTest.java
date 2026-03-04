package org.demo.whs.service.impl;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.mapper.CategoryMapper;
import org.demo.whs.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("should_CreateCategory_When_RequestIsValid")
    void should_CreateCategory_When_RequestIsValid() {
        CreateCategoryRequest request = buildCreateRequest("ELEC", "Electronics", CategoryStatus.ACTIVE);
        Category category = Category.builder()
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();
        Category savedCategory = Category.builder()
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();
        savedCategory.setId("cat-1");
        CategoryResponse expectedResponse = CategoryResponse.builder()
                .id("cat-1")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.existsByCodeIgnoreCase("ELEC")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCase("Electronics")).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(savedCategory);
        when(categoryMapper.toResponse(savedCategory)).thenReturn(expectedResponse);

        CategoryResponse actual = categoryService.createCategory(request);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo("cat-1");
        assertThat(actual.getCode()).isEqualTo("ELEC");
        verify(categoryRepository).existsByCodeIgnoreCase("ELEC");
        verify(categoryRepository).existsByNameIgnoreCase("Electronics");
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("should_ThrowConflictException_When_CodeAlreadyExists")
    void should_ThrowConflictException_When_CodeAlreadyExists() {
        CreateCategoryRequest request = buildCreateRequest("ELEC", "Electronics", CategoryStatus.ACTIVE);

        when(categoryRepository.existsByCodeIgnoreCase("ELEC")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CAT_002");
    }

    private CreateCategoryRequest buildCreateRequest(String code, String name, CategoryStatus status) {
        CreateCategoryRequest request = new CreateCategoryRequest();
        try {
            setField(request, "code", code);
            setField(request, "name", name);
            setField(request, "status", status);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to setup test request", ex);
        }
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
