package org.demo.whs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.GlobalExceptionHandle;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(GlobalExceptionHandle.class)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private RateLimitService rateLimitService;

    @Test
    @DisplayName("should_CreateProduct_When_RequestIsValid")
    void should_CreateProduct_When_RequestIsValid() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        setField(request, "name", "Test Product");
        setField(request, "categoryId", "cat-1");
        setField(request, "uomId", "uom-1");

        ProductResponse response = ProductResponse.builder()
                .id("prod-1")
                .sku("SKU-1234")
                .name("Test Product")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("prod-1"))
                .andExpect(jsonPath("$.data.sku").value("SKU-1234"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_CreatePayloadMissingRequiredFields")
    void should_ReturnBadRequest_When_CreatePayloadMissingRequiredFields() throws Exception {
        String invalidPayload = """
                {
                  "name": "",
                  "categoryId": null
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error_code").value("COM_001"));
    }

    @Test
    @DisplayName("should_UpdateProduct_When_RequestIsValid")
    void should_UpdateProduct_When_RequestIsValid() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest();
        setField(request, "name", "Updated Product");

        ProductResponse response = ProductResponse.builder()
                .id("prod-1")
                .sku("SKU-1234")
                .name("Updated Product")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productService.updateProduct(any(), any(UpdateProductRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/products/{id}", "prod-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Product"));
    }

    @Test
    @DisplayName("should_ReturnNotFound_When_UpdateProductDoesNotExist")
    void should_ReturnNotFound_When_UpdateProductDoesNotExist() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest();
        setField(request, "name", "Updated Product");

        when(productService.updateProduct(any(), any(UpdateProductRequest.class)))
                .thenThrow(new NotFoundException("Product not found", ErrorCode.PROD_001));

        mockMvc.perform(put("/api/v1/products/{id}", "invalid-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PROD_001"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_DisableBatchTrackingWithInventory")
    void should_ReturnBadRequest_When_DisableBatchTrackingWithInventory() throws Exception {
        UpdateProductRequest request = new UpdateProductRequest();
        setField(request, "requiresBatchTracking", false);

        when(productService.updateProduct(any(), any(UpdateProductRequest.class)))
                .thenThrow(new BadRequestException(ErrorCode.PROD_006));

        mockMvc.perform(put("/api/v1/products/{id}", "prod-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("PROD_006"));
    }

    @Test
    @DisplayName("should_GetProductById_When_ProductExists")
    void should_GetProductById_When_ProductExists() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id("prod-1")
                .sku("SKU-1234")
                .name("Test Product")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productService.getProductById("prod-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("prod-1"))
                .andExpect(jsonPath("$.data.sku").value("SKU-1234"));
    }

    @Test
    @DisplayName("should_ReturnNotFound_When_ProductDoesNotExist")
    void should_ReturnNotFound_When_ProductDoesNotExist() throws Exception {
        when(productService.getProductById("invalid-id"))
                .thenThrow(new NotFoundException("Product not found", ErrorCode.PROD_001));

        mockMvc.perform(get("/api/v1/products/{id}", "invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PROD_001"));
    }

    @Test
    @DisplayName("should_GetProductBySku_When_ProductExists")
    void should_GetProductBySku_When_ProductExists() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id("prod-1")
                .sku("SKU-1234")
                .name("Test Product")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productService.getProductBySku("SKU-1234")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/sku/{sku}", "SKU-1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value("SKU-1234"));
    }

    @Test
    @DisplayName("should_GetAllProducts_When_RequestIsValid")
    void should_GetAllProducts_When_RequestIsValid() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id("prod-1")
                .sku("SKU-1234")
                .name("Test Product")
                .status(ProductStatus.ACTIVE)
                .build();

        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .content(List.of(product))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .isFirst(true)
                .isLast(true)
                .build();

        when(productService.getAllProducts(any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("prod-1"));
    }

    @Test
    @DisplayName("should_DeleteProduct_When_ProductExists")
    void should_DeleteProduct_When_ProductExists() throws Exception {
        doNothing().when(productService).deleteProduct("prod-1");

        mockMvc.perform(delete("/api/v1/products/{id}", "prod-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("should_ReturnNotFound_When_DeleteProductDoesNotExist")
    void should_ReturnNotFound_When_DeleteProductDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Product not found", ErrorCode.PROD_001))
                .when(productService).deleteProduct("invalid-id");

        mockMvc.perform(delete("/api/v1/products/{id}", "invalid-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("PROD_001"));
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
