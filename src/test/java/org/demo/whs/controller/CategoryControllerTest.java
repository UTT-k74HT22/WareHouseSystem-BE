package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.Category.CreateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryRequest;
import org.demo.whs.entity.dto.request.Category.UpdateCategoryStatusRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Category.CategoryResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.ConflictException;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.CategoryService;
import org.demo.whs.service.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("should_CreateCategory_When_RequestIsValid")
    void should_CreateCategory_When_RequestIsValid() throws Exception {
        CreateCategoryRequest request = new CreateCategoryRequest();
        setField(request, "code", "ELEC");
        setField(request, "name", "Electronics");
        setField(request, "status", CategoryStatus.ACTIVE);

        CategoryResponse response = CategoryResponse.builder()
                .id("cat-1")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryService.createCategory(any(CreateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("cat-1"))
                .andExpect(jsonPath("$.data.code").value("ELEC"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_CreatePayloadMissingRequiredFields")
    void should_ReturnBadRequest_When_CreatePayloadMissingRequiredFields() throws Exception {
        String invalidPayload = """
                {
                  "code": "",
                  "name": "",
                  "status": null
                }
                """;

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_GetCategories_When_RequestIsValid")
    void should_GetCategories_When_RequestIsValid() throws Exception {
        CategoryResponse category = CategoryResponse.builder()
                .id("cat-1")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        PageResponse<CategoryResponse> pageResponse = PageResponse.<CategoryResponse>builder()
                .content(List.of(category))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(categoryService.getCategories(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/categories")
                        .param("page", "0")
                        .param("size", "10")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("cat-1"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_StatusFilterIsInvalid")
    void should_ReturnBadRequest_When_StatusFilterIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/categories")
                        .param("status", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_GetCategoryById_When_CategoryExists")
    void should_GetCategoryById_When_CategoryExists() throws Exception {
        CategoryResponse response = CategoryResponse.builder()
                .id("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryService.getCategoryById("7c9e6679-7425-40de-944b-e07fc1f90ae7")).thenReturn(response);

        mockMvc.perform(get("/api/v1/categories/{id}", "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("7c9e6679-7425-40de-944b-e07fc1f90ae7"));
    }

    @Test
    @DisplayName("should_ReturnNotFound_When_CategoryDoesNotExist")
    void should_ReturnNotFound_When_CategoryDoesNotExist() throws Exception {
        when(categoryService.getCategoryById("7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .thenThrow(new NotFoundException("Category not found", ErrorCode.CAT_001));

        mockMvc.perform(get("/api/v1/categories/{id}", "7c9e6679-7425-40de-944b-e07fc1f90ae7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("CAT_001"))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_CategoryIdIsInvalid")
    void should_ReturnBadRequest_When_CategoryIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/categories/{id}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_UpdateCategory_When_RequestIsValid")
    void should_UpdateCategory_When_RequestIsValid() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        setField(request, "code", "ELEC-NEW");
        setField(request, "name", "Electronics New");
        setField(request, "description", "Updated desc");

        CategoryResponse response = CategoryResponse.builder()
                .id("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .code("ELEC-NEW")
                .name("Electronics New")
                .status(CategoryStatus.ACTIVE)
                .build();

        when(categoryService.updateCategory(any(), any(UpdateCategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/categories/{id}", "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("ELEC-NEW"));
    }

    @Test
    @DisplayName("should_ReturnConflict_When_UpdateCategoryDuplicated")
    void should_ReturnConflict_When_UpdateCategoryDuplicated() throws Exception {
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        setField(request, "code", "ELEC");

        when(categoryService.updateCategory(any(), any(UpdateCategoryRequest.class)))
                .thenThrow(new ConflictException(ErrorCode.CAT_002));

        mockMvc.perform(put("/api/v1/categories/{id}", "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("CAT_002"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_UpdatePayloadInvalid")
    void should_ReturnBadRequest_When_UpdatePayloadInvalid() throws Exception {
        String invalidPayload = """
                {
                  "code": "invalid code with spaces"
                }
                """;

        mockMvc.perform(put("/api/v1/categories/{id}", "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_UpdateCategoryStatus_When_RequestIsValid")
    void should_UpdateCategoryStatus_When_RequestIsValid() throws Exception {
        UpdateCategoryStatusRequest request = new UpdateCategoryStatusRequest();
        setField(request, "status", CategoryStatus.INACTIVE);

        CategoryResponse response = CategoryResponse.builder()
                .id("7c9e6679-7425-40de-944b-e07fc1f90ae7")
                .code("ELEC")
                .name("Electronics")
                .status(CategoryStatus.INACTIVE)
                .build();

        when(categoryService.updateCategoryStatus(any(), any(UpdateCategoryStatusRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/categories/{id}/status", "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_StatusPayloadIsInvalid")
    void should_ReturnBadRequest_When_StatusPayloadIsInvalid() throws Exception {
        String invalidPayload = """
                {
                  "status": null
                }
                """;

        mockMvc.perform(patch("/api/v1/categories/{id}/status", "7c9e6679-7425-40de-944b-e07fc1f90ae7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
