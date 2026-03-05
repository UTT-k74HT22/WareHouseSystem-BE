package org.demo.whs.service.impl;

import org.demo.whs.entity.dto.request.Inventory.InventoryFilterRequest;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.mapper.InventoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryServiceImpl Unit Tests")
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WareHouseRepository wareHouseRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private BatchRepository batchRepository;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageNumberIsNegative")
    void should_ThrowBadRequest_When_PageNumberIsNegative() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(-1);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_006");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageSizeIsZero")
    void should_ThrowBadRequest_When_PageSizeIsZero() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_007");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_PageSizeExceedsLimit")
    void should_ThrowBadRequest_When_PageSizeExceedsLimit() {
        Pageable pageable = mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(101);

        assertThatThrownBy(() -> inventoryService.getInventories(InventoryFilterRequest.builder().build(), pageable))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_008");
    }
}
