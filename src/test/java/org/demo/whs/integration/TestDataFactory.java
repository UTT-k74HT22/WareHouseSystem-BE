package org.demo.whs.integration;

import lombok.RequiredArgsConstructor;
import org.demo.whs.entity.*;
import org.demo.whs.entity.enums.*;
import org.demo.whs.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * TestDataFactory: Utility to seed Master Data and transactional prerequisites into test H2 database.
 */
@Component
@RequiredArgsConstructor
public class TestDataFactory {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AccountHasRoleRepository accountHasRoleRepository;
    private final EmployeeRepository employeeRepository;
    private final WareHouseRepository wareHouseRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final UnitsOfMeasureRepository unitsOfMeasureRepository;
    private final ProductRepository productRepository;
    private final BusinessPartnersRepository businessPartnersRepository;
    private final BatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create or retrieve an Account and assign a Role to it.
     */
    public Account createAccountWithRole(String username, String roleName) {
        return accountRepository.findByUsername(username).orElseGet(() -> {
            Role role = roleRepository.findByName(roleName).orElseGet(() ->
                    roleRepository.save(Role.builder()
                            .code("ROLE_" + roleName.toUpperCase())
                            .name(roleName)
                            .description(roleName + " role")
                            .isDefault(false)
                            .build())
            );

            Account account = accountRepository.save(Account.builder()
                    .username(username)
                    .password(passwordEncoder.encode("Password@123"))
                    .status(AccountStatus.ACTIVE)
                    .build());

            AccountRoleId accountRoleId = AccountRoleId.builder()
                    .accountId(account.getId())
                    .roleId(role.getId())
                    .build();

            accountHasRoleRepository.save(new AccountHasRole(accountRoleId));
            return account;
        });
    }

    /**
     * Create an employee record linked to an account and warehouse.
     */
    public Employee createEmployee(String accountId, String warehouseId, String employeeCode) {
        return employeeRepository.findByAccountId(accountId).orElseGet(() ->
                employeeRepository.save(Employee.builder()
                        .accountId(accountId)
                        .employeeCode(employeeCode)
                        .warehouseId(warehouseId)
                        .department("Warehouse")
                        .position("Manager")
                        .status(EmployeeStatus.ACTIVE)
                        .hireDate(LocalDate.now())
                        .build())
        );
    }

    /**
     * Create a Warehouse.
     */
    public Warehouses createWarehouse(String code, String name) {
        return wareHouseRepository.save(Warehouses.builder()
                .code(code)
                .name(name)
                .address("123 Industrial Park")
                .city("Hanoi")
                .country("Vietnam")
                .type(WareHouseType.MAIN)
                .status(WareHouseStatus.ACTIVE)
                .capacity(new BigDecimal("100000.00"))
                .build());
    }

    /**
     * Create standard locations for a warehouse (STORAGE, PICKING, PACKING, STAGING).
     */
    public Locations createLocation(String warehouseId, String code, String name, LocationType type) {
        return locationRepository.save(Locations.builder()
                .warehouseId(warehouseId)
                .code(code)
                .name(name)
                .type(type)
                .status(LocationStatus.ACTIVE)
                .capacity(new BigDecimal("10000.00"))
                .usedCapacity(BigDecimal.ZERO)
                .zone("ZONE-A")
                .build());
    }

    /**
     * Create product Category.
     */
    public Category createCategory(String code, String name) {
        return categoryRepository.save(Category.builder()
                .code(code)
                .name(name)
                .description("Category " + name)
                .status(CategoryStatus.ACTIVE)
                .build());
    }

    /**
     * Create Unit of Measure.
     */
    public UnitsOfMeasure createUom(String code, String name, UnitsOfMeasureType type) {
        return unitsOfMeasureRepository.save(UnitsOfMeasure.builder()
                .code(code)
                .name(name)
                .description("UOM " + name)
                .type(type)
                .build());
    }

    /**
     * Create Product.
     */
    public Products createProduct(String sku, String name, String categoryId, String uomId) {
        return productRepository.save(Products.builder()
                .sku(sku)
                .name(name)
                .categoryId(categoryId)
                .uomId(uomId)
                .status(ProductStatus.ACTIVE)
                .costPrice(new BigDecimal("50000.00"))
                .sellingPrice(new BigDecimal("80000.00"))
                .minStockLevel(new BigDecimal("10.00"))
                .maxStockLevel(new BigDecimal("1000.00"))
                .reOrderPoint(new BigDecimal("20.00"))
                .requiresBatchTracking(false)
                .build());
    }

    /**
     * Create Business Partner (Vendor / Supplier or Customer).
     */
    public BusinessPartners createPartner(String code, String name, BusinessPartnerType type) {
        return businessPartnersRepository.save(BusinessPartners.builder()
                .code(code)
                .name(name)
                .type(type)
                .email(code.toLowerCase() + "@example.com")
                .phone("0987654321")
                .address("456 Logistics Road")
                .city("Hanoi")
                .country("Vietnam")
                .status(BusinessPartnerStatus.ACTIVE)
                .creditLimit(new BigDecimal("100000000.00"))
                .build());
    }

    /**
     * Create a Product Batch.
     */
    public Batch createBatch(String productId, String batchNumber) {
        return batchRepository.save(Batch.builder()
                .productId(productId)
                .batchNumber(batchNumber)
                .manufacturingDate(LocalDate.now().minusMonths(1))
                .expiryDate(LocalDate.now().plusYears(1))
                .status(BatchStatus.AVAILABLE)
                .build());
    }

    /**
     * Seed initial inventory.
     */
    public Inventory seedInventory(String productId, String warehouseId, String locationId, String batchId,
                                   BigDecimal onHand, BigDecimal reserved) {
        return inventoryRepository.save(Inventory.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .locationId(locationId)
                .batchId(batchId)
                .onHandQuantity(onHand != null ? onHand : BigDecimal.ZERO)
                .quarantineQuantity(BigDecimal.ZERO)
                .reservedQuantity(reserved != null ? reserved : BigDecimal.ZERO)
                .version(0)
                .build());
    }
}
