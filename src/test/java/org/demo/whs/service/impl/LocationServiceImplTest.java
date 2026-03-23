package org.demo.whs.service.impl;

import org.demo.whs.entity.Account;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.Location.ChangeLocationStatusRequest;
import org.demo.whs.entity.dto.request.Location.CreateLocationRequest;
import org.demo.whs.entity.dto.response.Location.LocationResponse;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.WareHouseStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.LocationMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WareHouseRepository wareHouseRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationMapper locationMapper;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private LocationServiceImpl locationService;

    private Account currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new Account();
        currentUser.setId("acc-1");
        currentUser.setUsername("admin");
    }

    @Test
    void createLocation_shouldGenerateCode_When_RequestCodeIsMissing() {
        CreateLocationRequest request = mock(CreateLocationRequest.class);
        when(request.getWarehouseId()).thenReturn("wh-1");

        Warehouses warehouse = new Warehouses();
        warehouse.setId("wh-1");
        warehouse.setStatus(WareHouseStatus.ACTIVE);

        Locations location = new Locations();
        location.setWarehouseId("wh-1");
        location.setName("A1");

        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
        when(locationMapper.toEntity(request)).thenReturn(location);
        when(locationRepository.save(location)).thenAnswer(invocation -> {
            Locations persisted = invocation.getArgument(0);
            persisted.setId("loc-1");
            return persisted;
        });
        when(locationMapper.toResponseWithWarehouse(location, warehouse)).thenAnswer(invocation ->
                LocationResponse.builder()
                        .id(location.getId())
                        .code(location.getCode())
                        .warehouseId(location.getWarehouseId())
                        .name(location.getName())
                        .build()
        );

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            LocationResponse response = locationService.createLocation(request);

            assertThat(response.getCode()).startsWith("LOC-");
            assertThat(response.getCode()).hasSizeLessThanOrEqualTo(50);
            assertThat(location.getCode()).isEqualTo(response.getCode());
        }
    }

    @Test
    void createLocation_shouldReject_When_RequestProvidesCode() {
        CreateLocationRequest request = mock(CreateLocationRequest.class);
        when(request.getWarehouseId()).thenReturn("wh-1");
        when(request.getCode()).thenReturn("LOC-001");

        Warehouses warehouse = new Warehouses();
        warehouse.setId("wh-1");
        warehouse.setStatus(WareHouseStatus.ACTIVE);

        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));

        assertThatThrownBy(() -> locationService.createLocation(request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "COM_001");
    }

    @Test
    void changeLocationStatus_shouldThrowException_When_LocationHasActiveInventory() {
        // Arrange
        String locationId = "loc-1";
        LocationStatus newStatus = LocationStatus.INACTIVE;

        ChangeLocationStatusRequest request = mock(ChangeLocationStatusRequest.class);
        when(request.getStatus()).thenReturn(newStatus);

        Locations location = new Locations();
        location.setId(locationId);
        location.setStatus(LocationStatus.ACTIVE);
        location.setWarehouseId("wh-1");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsActiveInventoryByLocationId(locationId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> locationService.changeLocationStatus(locationId, request))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOC_006.getCode());
    }

    @Test
    void changeLocationStatus_shouldSucceed_When_NoActiveInventory() {
        // Arrange
        String locationId = "loc-1";
        LocationStatus newStatus = LocationStatus.INACTIVE;

        ChangeLocationStatusRequest request = mock(ChangeLocationStatusRequest.class);
        when(request.getStatus()).thenReturn(newStatus);

        Locations location = new Locations();
        location.setId(locationId);
        location.setStatus(LocationStatus.ACTIVE);
        location.setWarehouseId("wh-1");

        Warehouses warehouse = new Warehouses();
        warehouse.setId("wh-1");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsActiveInventoryByLocationId(locationId)).thenReturn(false);
        when(locationRepository.save(location)).thenReturn(location);
        when(wareHouseRepository.findById("wh-1")).thenReturn(Optional.of(warehouse));
        when(locationMapper.toResponseWithWarehouse(location, warehouse)).thenReturn(
                LocationResponse.builder()
                        .id(locationId)
                        .status(newStatus)
                        .build()
        );

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            // Act
            LocationResponse response = locationService.changeLocationStatus(locationId, request);

            // Assert
            assertThat(response.getStatus()).isEqualTo(newStatus);
            assertThat(location.getStatus()).isEqualTo(newStatus);
        }
    }

    @Test
    void deleteLocation_shouldThrowException_When_LocationHasActiveInventory() {
        // Arrange
        String locationId = "loc-1";

        Locations location = new Locations();
        location.setId(locationId);
        location.setStatus(LocationStatus.ACTIVE);
        location.setWarehouseId("wh-1");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsActiveInventoryByLocationId(locationId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> locationService.deleteLocation(locationId))
                .isInstanceOf(BadRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOC_006.getCode());
    }

    @Test
    void deleteLocation_shouldSucceed_When_NoActiveInventory() {
        // Arrange
        String locationId = "loc-1";

        Locations location = new Locations();
        location.setId(locationId);
        location.setStatus(LocationStatus.ACTIVE);
        location.setWarehouseId("wh-1");

        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(inventoryRepository.existsActiveInventoryByLocationId(locationId)).thenReturn(false);
        when(locationRepository.save(location)).thenReturn(location);

        try (var mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUsername).thenReturn("admin");
            when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(currentUser));

            // Act
            locationService.deleteLocation(locationId);

            // Assert
            assertThat(location.getStatus()).isEqualTo(LocationStatus.INACTIVE);
        }
    }
}
