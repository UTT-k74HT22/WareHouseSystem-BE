package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.WareHouse.CreateWarehouseRequest;
import org.demo.whs.entity.dto.response.WareHouse.WareHouseResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.mapper.WareHouseMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.UserProfileRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.utils.IdentifierGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WareHouseServiceImplTest {

    @InjectMocks
    private WareHouseServiceImpl wareHouseService;

    @Mock
    private WareHouseRepository wareHouseRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private WareHouseMapper wareHouseMapper;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @Test
    void deleteWarehouse_shouldThrowException_whenWarehouseNotFound() {

        when(wareHouseRepository.findById("WH1"))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> wareHouseService.deleteWarehouse("WH1"));
    }

    @Test
    void deleteWarehouse_shouldThrowException_whenWarehouseInactive() {

        Warehouses warehouse = new Warehouses();
        warehouse.setId("WH1");
        warehouse.setStatus(WareHouseStatus.INACTIVE);

        when(wareHouseRepository.findById("WH1"))
                .thenReturn(Optional.of(warehouse));

        assertThrows(BadRequestException.class,
                () -> wareHouseService.deleteWarehouse("WH1"));
    }

    @Test
    void deleteWarehouse_shouldThrowException_whenActiveLocationsExist() {

        Warehouses warehouse = new Warehouses();
        warehouse.setId("WH1");
        warehouse.setStatus(WareHouseStatus.ACTIVE);

        when(wareHouseRepository.findById("WH1"))
                .thenReturn(Optional.of(warehouse));

        when(locationRepository.countByWarehouseIdAndStatusNot(
                "WH1", LocationStatus.INACTIVE))
                .thenReturn(2L);

        assertThrows(BadRequestException.class,
                () -> wareHouseService.deleteWarehouse("WH1"));
    }

    @Test
    void deleteWarehouse_shouldThrowException_whenInventoryExists() {

        Warehouses warehouse = new Warehouses();
        warehouse.setId("WH1");
        warehouse.setStatus(WareHouseStatus.ACTIVE);

        when(wareHouseRepository.findById("WH1"))
                .thenReturn(Optional.of(warehouse));

        when(locationRepository.countByWarehouseIdAndStatusNot(
                "WH1", LocationStatus.INACTIVE))
                .thenReturn(0L);

        when(inventoryRepository.existsActiveInventoryByWarehouseId("WH1"))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> wareHouseService.deleteWarehouse("WH1"));
    }

    @Test
    void deleteWarehouse_shouldSoftDeleteWarehouse() {

        Warehouses warehouse = new Warehouses();
        warehouse.setId("WH1");
        warehouse.setStatus(WareHouseStatus.ACTIVE);

        when(wareHouseRepository.findById("WH1"))
                .thenReturn(Optional.of(warehouse));

        when(locationRepository.countByWarehouseIdAndStatusNot(
                "WH1", LocationStatus.INACTIVE))
                .thenReturn(0L);

        when(inventoryRepository.existsActiveInventoryByWarehouseId("WH1"))
                .thenReturn(false);

        Account account = new Account();
        account.setId("USER1");

        when(accountRepository.findByUsername("admin"))
                .thenReturn(Optional.of(account));

        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {

            utilities.when(SecurityUtils::getCurrentUsername)
                    .thenReturn("admin");

            wareHouseService.deleteWarehouse("WH1");

            assertEquals(WareHouseStatus.INACTIVE, warehouse.getStatus());

            verify(wareHouseRepository).save(warehouse);
        }
    }

    @Test
    void createWH_shouldGenerateCode_whenRequestCodeMissing() {
        CreateWarehouseRequest request = mock(CreateWarehouseRequest.class);
        when(request.getName()).thenReturn("Main Warehouse");

        Warehouses warehouse = new Warehouses();
        warehouse.setManagerId("M1");

        Account account = new Account();
        account.setId("USER1");

        when(wareHouseMapper.toEntity(request)).thenReturn(warehouse);
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));
        when(userProfileRepository.getAccountsByIds(List.of("M1"))).thenReturn(List.of());
        when(wareHouseMapper.toResponse(warehouse, null)).thenAnswer(invocation -> WareHouseResponse.builder()
                .code(warehouse.getCode())
                .name("Main Warehouse")
                .build());

        try (MockedStatic<SecurityUtils> utilities = mockStatic(SecurityUtils.class)) {
            utilities.when(SecurityUtils::getCurrentUsername).thenReturn("admin");

            WareHouseResponse response = wareHouseService.createWH(request);

            assertNotNull(response);
            assertTrue(response.getCode().startsWith("WH-"));
            assertTrue(response.getCode().length() <= 20);
            verify(wareHouseRepository).save(warehouse);
        }
    }

    @Test
    void createWH_shouldReject_whenRequestProvidesCode() {
        CreateWarehouseRequest request = mock(CreateWarehouseRequest.class);
        when(request.getCode()).thenReturn("WH-001");

        assertThrows(BadRequestException.class, () -> wareHouseService.createWH(request));

        verify(wareHouseRepository, never()).save(any(Warehouses.class));
    }
}
