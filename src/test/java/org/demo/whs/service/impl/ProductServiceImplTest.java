package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Category;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.request.Product.UpdateProductRequest;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.ProductMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UnitsOfMeasureRepository unitsOfMeasureRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private ProductServiceImpl productService;

    private Account currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new Account();
        currentUser.setId("acc-1");
        currentUser.setUsername("admin");
    }

    @Test
    void createProduct_shouldGenerateSku_When_RequestSkuIsMissing() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Demo Product");
        request.setCategoryId("cat-1");
        request.setUomId("uom-1");

        Category category = new Category();
        category.setId("cat-1");
        category.setStatus(CategoryStatus.ACTIVE);

        UnitsOfMeasure uom = new UnitsOfMeasure();
        uom.setId("uom-1");

        Products product = new Products();
        product.setName("Demo Product");

        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(unitsOfMeasureRepository.findById("uom-1")).thenReturn(Optional.of(uom));
        when(productMapper.toEntity(request)).thenReturn(product);
        when(productRepository.save(product)).thenAnswer(invocation -> {
            Products persisted = invocation.getArgument(0);
            persisted.setId("prod-1");
            return persisted;
        });
        when(productMapper.toResponse(product, category, uom)).thenAnswer(invocation ->
                ProductResponse.builder()
                        .id(product.getId())
                        .sku(product.getSku())
                        .name(product.getName())
                        .build()
        );

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            ProductResponse response = productService.createProduct(request);

            assertThat(response.getSku()).startsWith("SKU-");
            assertThat(response.getSku()).hasSizeLessThanOrEqualTo(50);
            assertThat(product.getSku()).isEqualTo(response.getSku());
        }
    }

    @Test
    void createProduct_shouldReject_When_RequestProvidesSku() {
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU-001");
        request.setName("Demo Product");
        request.setCategoryId("cat-1");
        request.setUomId("uom-1");

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
    }

    @Test
    void updateProduct_shouldThrowException_When_DisableBatchTrackingWithInventory() {
        // Arrange
        String productId = "prod-1";

        UpdateProductRequest request = new UpdateProductRequest();
        request.setRequiresBatchTracking(false);

        Products product = new Products();
        product.setId(productId);
        product.setRequiresBatchTracking(true);
        product.setSku("SKU-001");
        product.setCategoryId("cat-1");
        product.setUomId("uom-1");
        product.setMinStockLevel(BigDecimal.TEN);
        product.setMaxStockLevel(new BigDecimal("100"));
        product.setReOrderPoint(new BigDecimal("20"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.existsBatchInventoryByProductId(productId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROD_006.getCode());
    }

    @Test
    void updateProduct_shouldSucceed_When_DisableBatchTrackingWithoutInventory() {
        // Arrange
        String productId = "prod-1";

        UpdateProductRequest request = new UpdateProductRequest();
        request.setRequiresBatchTracking(false);

        Products product = new Products();
        product.setId(productId);
        product.setRequiresBatchTracking(true);
        product.setSku("SKU-001");
        product.setCategoryId("cat-1");
        product.setUomId("uom-1");
        product.setMinStockLevel(BigDecimal.TEN);
        product.setMaxStockLevel(new BigDecimal("100"));
        product.setReOrderPoint(new BigDecimal("20"));

        Category category = new Category();
        category.setId("cat-1");
        category.setStatus(CategoryStatus.ACTIVE);

        UnitsOfMeasure uom = new UnitsOfMeasure();
        uom.setId("uom-1");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(inventoryRepository.existsBatchInventoryByProductId(productId)).thenReturn(false);
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(category));
        when(unitsOfMeasureRepository.findById("uom-1")).thenReturn(Optional.of(uom));
        doAnswer(invocation -> {
            Products p = invocation.getArgument(0);
            UpdateProductRequest req = invocation.getArgument(1);
            if (req.getRequiresBatchTracking() != null) {
                p.setRequiresBatchTracking(req.getRequiresBatchTracking());
            }
            return null;
        }).when(productMapper).updateEntity(any(), any());
        when(productRepository.save(product)).thenAnswer(invocation -> {
            Products saved = invocation.getArgument(0);
            return saved;
        });
        when(productMapper.toResponse(product, category, uom)).thenAnswer(invocation -> {
            Products p = invocation.getArgument(0);
            return ProductResponse.builder()
                    .id(productId)
                    .requiresBatchTracking(p.getRequiresBatchTracking())
                    .build();
        });

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            // Act
            ProductResponse response = productService.updateProduct(productId, request);

            // Assert
            assertThat(response.getRequiresBatchTracking()).isFalse();
        }
    }

    @Test
    void deleteProduct_shouldSucceed() {
        // Arrange
        String productId = "prod-1";

        Products product = new Products();
        product.setId(productId);
        product.setStatus(ProductStatus.ACTIVE);
        product.setSku("SKU-001");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            // Act
            productService.deleteProduct(productId);

            // Assert
            assertThat(product.getStatus()).isEqualTo(ProductStatus.DISCONTINUED);
        }
    }

    @Test
    void updateProduct_shouldValidateStockLevels_When_UpdateMinMax() {
        // Arrange
        String productId = "prod-1";

        UpdateProductRequest request = new UpdateProductRequest();
        request.setMinStockLevel(new BigDecimal("100"));
        request.setMaxStockLevel(new BigDecimal("50")); // Max < Min

        Products product = new Products();
        product.setId(productId);
        product.setSku("SKU-001");
        product.setCategoryId("cat-1");
        product.setUomId("uom-1");
        product.setMinStockLevel(BigDecimal.TEN);
        product.setMaxStockLevel(new BigDecimal("100"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROD_007.getCode());
    }

    @Test
    void updateProduct_shouldValidateReorderPoint_When_BelowMinStock() {
        // Arrange
        String productId = "prod-1";

        UpdateProductRequest request = new UpdateProductRequest();
        request.setReorderPoint(new BigDecimal("5")); // Below min

        Products product = new Products();
        product.setId(productId);
        product.setSku("SKU-001");
        product.setCategoryId("cat-1");
        product.setUomId("uom-1");
        product.setMinStockLevel(BigDecimal.TEN);
        product.setMaxStockLevel(new BigDecimal("100"));
        product.setReOrderPoint(new BigDecimal("20"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROD_008.getCode());
    }
}
