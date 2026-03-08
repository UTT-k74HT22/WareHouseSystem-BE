package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
}