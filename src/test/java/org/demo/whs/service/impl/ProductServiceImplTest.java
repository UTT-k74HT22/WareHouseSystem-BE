package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Category;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.Product.CreateProductRequest;
import org.demo.whs.entity.dto.response.Product.ProductResponse;
import org.demo.whs.entity.enums.CategoryStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.ProductMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.CategoryRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ProductMapper productMapper;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private ProductServiceImpl productService;

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

        Account account = new Account();
        account.setId("acc-1");
        account.setUsername("admin");

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
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));

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
}
