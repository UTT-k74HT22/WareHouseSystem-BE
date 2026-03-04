package org.demo.whs.service.impl;

import org.demo.whs.entity.Category;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.CategoryMapper;
import org.demo.whs.repository.CategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

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

    @Test
    @DisplayName("should_GetCategories_When_StatusFilterProvided")
    void should_GetCategories_When_StatusFilterProvided() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Category category = Category.builder()
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();
        category.setId("cat-1");

        Page<Category> page = new PageImpl<>(List.of(category), pageable, 1);
        CategoryResponse mapped = CategoryResponse.builder()
                .id("cat-1")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.findAllByStatus(CategoryStatus.ACTIVE, pageable)).thenReturn(page);
        when(categoryMapper.toResponseList(page.getContent())).thenReturn(List.of(mapped));

        PageResponse<CategoryResponse> actual = categoryService.getCategories(CategoryStatus.ACTIVE, pageable);

        assertThat(actual).isNotNull();
        assertThat(actual.getContent()).hasSize(1);
        assertThat(actual.getContent().get(0).getId()).isEqualTo("cat-1");
        verify(categoryRepository).findAllByStatus(CategoryStatus.ACTIVE, pageable);
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageSizeExceedsLimit")
    void should_ThrowBadRequest_When_PageSizeExceedsLimit() {
        Pageable pageable = PageRequest.of(0, 101);

        assertThatThrownBy(() -> categoryService.getCategories(null, pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
    }

    @Test
    @DisplayName("should_GetCategoryById_When_CategoryExists")
    void should_GetCategoryById_When_CategoryExists() {
        Category category = Category.builder()
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();
        category.setId("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        CategoryResponse mapped = CategoryResponse.builder()
                .id("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(mapped);

        CategoryResponse actual = categoryService.getCategoryById("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    }

    @Test
    @DisplayName("should_ThrowNotFoundException_When_CategoryDoesNotExist")
    void should_ThrowNotFoundException_When_CategoryDoesNotExist() {
        when(categoryRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById("7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .isInstanceOf(NotFoundException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CAT_001");
    }

    @Test
    @DisplayName("should_UpdateCategory_When_RequestIsValid")
    void should_UpdateCategory_When_RequestIsValid() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        try {
            setField(request, "code", "ELEC-NEW");
            setField(request, "name", "Electronics New");
            setField(request, "description", "Updated desc");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        Category existing = Category.builder()
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();
        existing.setId("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        Category updated = Category.builder()
                .code("ELEC-NEW")
                .name("Electronics New")
                .description("Updated desc")
                .status(CategoryStatus.ACTIVE)
                .build();
        updated.setId("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        CategoryResponse mapped = CategoryResponse.builder()
                .id("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .code("ELEC-NEW")
                .name("Electronics New")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCodeIgnoreCaseAndIdNot("ELEC-NEW", "7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Electronics New", "7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(false);
        when(categoryRepository.save(existing)).thenReturn(updated);
        when(categoryMapper.toResponse(updated)).thenReturn(mapped);

        CategoryResponse actual = categoryService.updateCategory("7c9e6679-7425-40de-944b-e07fc1f90ae7", request);

        assertThat(actual).isNotNull();
        assertThat(actual.getCode()).isEqualTo("ELEC-NEW");
        verify(categoryMapper).updateEntity(existing, request);
    }

    @Test
    @DisplayName("should_ThrowConflictException_When_UpdatingToDuplicatedCode")
    void should_ThrowConflictException_When_UpdatingToDuplicatedCode() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        try {
            setField(request, "code", "ELEC");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }

        Category existing = Category.builder()
                .code("OLD")
                .name("Old Name")
                .status(CategoryStatus.ACTIVE)
                .build();
        existing.setId("7c9e6679-7425-40de-944b-e07fc1f90ae7");

        when(categoryRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByCodeIgnoreCaseAndIdNot("ELEC", "7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory("7c9e6679-7425-40de-944b-e07fc1f90ae7", request))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", "CAT_002");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdatePayloadHasNoFields")
    void should_ThrowBadRequest_When_UpdatePayloadHasNoFields() {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        Category existing = Category.builder()
                .code("OLD")
                .name("Old Name")
                .status(CategoryStatus.ACTIVE)
                .build();
        existing.setId("7c9e6679-7425-40de-944b-e07fc1f90ae7");
        when(categoryRepository.findById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> categoryService.updateCategory("7c9e6679-7425-40de-944b-e07fc1f90ae7", request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
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
