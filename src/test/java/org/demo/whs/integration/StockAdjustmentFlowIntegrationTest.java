package org.demo.whs.integration;

import com.jayway.jsonpath.JsonPath;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Stock Adjustment Flow Integration Tests")
class StockAdjustmentFlowIntegrationTest extends BaseIntegrationTest {

    private Warehouses warehouse;
    private Locations storageLocation;
    private Products product;
    private Inventory inventory;

    private String adminUser;
    private String managerUser;
    private String staffUser;

    private static final String[] ADJUSTMENT_ALL_PERMS = {
            "PERM_STOCK_ADJUSTMENT_CREATE",
            "PERM_STOCK_ADJUSTMENT_READ",
            "PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE"
    };

    private static final String[] ADJUSTMENT_STAFF_PERMS = {
            "PERM_STOCK_ADJUSTMENT_CREATE",
            "PERM_STOCK_ADJUSTMENT_READ"
    };

    @BeforeEach
    void setUpMasterData() {
        String suffix = UUID.randomUUID().toString().substring(0, 4);
        adminUser = "adm_" + suffix;
        managerUser = "mgr_" + suffix;
        staffUser = "stf_" + suffix;

        warehouse = dataFactory.createWarehouse("WH-ADJ-" + suffix, "Adjustment Warehouse " + suffix);
        storageLocation = dataFactory.createLocation(warehouse.getId(), "LOC-STR-" + suffix, "Storage " + suffix, LocationType.STORAGE);

        Category category = dataFactory.createCategory("CAT-" + suffix, "Category " + suffix);
        UnitsOfMeasure uom = dataFactory.createUom("U-" + suffix, "Piece", UnitsOfMeasureType.COUNT);
        product = dataFactory.createProduct("SKU-ADJ-" + suffix, "Adjustment Product " + suffix, category.getId(), uom.getId());

        // Setup Accounts and Employees for ADMIN, MANAGER, and STAFF
        Account adminAcc = dataFactory.createAccountWithRole(adminUser, "ADMIN");
        dataFactory.createEmployee(adminAcc.getId(), warehouse.getId(), "EMP-ADM-" + suffix);

        Account managerAcc = dataFactory.createAccountWithRole(managerUser, "MANAGER");
        dataFactory.createEmployee(managerAcc.getId(), warehouse.getId(), "EMP-MGR-" + suffix);

        Account staffAcc = dataFactory.createAccountWithRole(staffUser, "STAFF");
        dataFactory.createEmployee(staffAcc.getId(), warehouse.getId(), "EMP-STF-" + suffix);
    }

    @Test
    @DisplayName("ADJ-01: Auto-Approval - Admin adjusts inventory -> immediately approved, stock increases, movement recorded")
    void should_AutoApproveAndIncreaseStock_When_AdminCreatesStockAdjustment() throws Exception {
        // Initial inventory: 50 items in storage
        inventory = dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("50.00"), BigDecimal.ZERO);

        // Admin increases stock from 50.00 to 65.00 (+15.00 items)
        StockAdjustmentsRequest request = StockAdjustmentsRequest.builder()
                .inventoryId(inventory.getId())
                .quantityAfter(new BigDecimal("65.00"))
                .reason(ReasonType.COUNT_ERROR)
                .notes("Admin routine cycle count discovery")
                .build();

        MvcResult result = performPost("/api/v1/stock-adjustments", request, adminUser, ADJUSTMENT_ALL_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.requires_approval").value(false))
                .andExpect(jsonPath("$.data.quantity_before").value(50.00))
                .andExpect(jsonPath("$.data.quantity_after").value(65.00))
                .andExpect(jsonPath("$.data.adjustment_quantity").value(15.00))
                .andReturn();

        String adjustmentId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");

        // Assert DB State: StockAdjustment record
        StockAdjustments savedAdjustment = stockAdjustmentsRepository.findById(adjustmentId).orElseThrow();
        assertThat(savedAdjustment.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
        assertThat(savedAdjustment.getApprovedAt()).isNotNull();
        assertThat(savedAdjustment.getApprovedBy()).isNotNull();

        // Assert DB State: Inventory updated immediately
        Inventory updatedInv = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(updatedInv.getOnHandQuantity()).isEqualByComparingTo("65.00");
        assertThat(updatedInv.getAvailableQuantity()).isEqualByComparingTo("65.00");

        // Assert DB State: Stock Movement recorded
        List<StockMovements> movements = stockMovementsRepository.findAll().stream()
                .filter(m -> adjustmentId.equals(m.getReferenceId()))
                .toList();
        assertThat(movements).hasSize(1);
        StockMovements movement = movements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_INCREASE);
        assertThat(movement.getQuantityBefore()).isEqualByComparingTo("50.00");
        assertThat(movement.getQuantityAfter()).isEqualByComparingTo("65.00");
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("ADJ-02: 2-Step Approval - Staff creates pending adjustment -> Manager approves -> stock decreases, movement recorded")
    void should_RequireApprovalThenApplyStockDecrease_When_StaffCreatesAndManagerApproves() throws Exception {
        // Initial inventory: 100 items in storage
        inventory = dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("100.00"), BigDecimal.ZERO);

        // Step 1: Staff creates adjustment from 100.00 to 85.00 (-15.00 items due to DAMAGED goods)
        StockAdjustmentsRequest request = StockAdjustmentsRequest.builder()
                .inventoryId(inventory.getId())
                .quantityAfter(new BigDecimal("85.00"))
                .reason(ReasonType.DAMAGE)
                .notes("Found damaged goods during warehouse inspection")
                .build();

        MvcResult createResult = performPost("/api/v1/stock-adjustments", request, staffUser, ADJUSTMENT_STAFF_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.data.requires_approval").value(true))
                .andExpect(jsonPath("$.data.quantity_before").value(100.00))
                .andExpect(jsonPath("$.data.quantity_after").value(85.00))
                .andExpect(jsonPath("$.data.adjustment_quantity").value(-15.00))
                .andReturn();

        String adjustmentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // Verify stock is NOT modified while in PENDING_APPROVAL
        Inventory invDuringPending = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(invDuringPending.getOnHandQuantity()).isEqualByComparingTo("100.00");

        // Verify NO stock movements exist yet
        List<StockMovements> pendingMovements = stockMovementsRepository.findAll().stream()
                .filter(m -> adjustmentId.equals(m.getReferenceId()))
                .toList();
        assertThat(pendingMovements).isEmpty();

        // Step 2: Manager reviews and approves the adjustment
        ApproveStockAdjustmentRequest approveRequest = ApproveStockAdjustmentRequest.builder()
                .approvalNote("Damaged goods verified and approved for write-off")
                .build();

        performPut("/api/v1/stock-adjustments/" + adjustmentId + "/approve", approveRequest, managerUser, ADJUSTMENT_ALL_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.quantity_after").value(85.00));

        // Assert DB State: StockAdjustment record updated
        StockAdjustments approvedAdjustment = stockAdjustmentsRepository.findById(adjustmentId).orElseThrow();
        assertThat(approvedAdjustment.getStatus()).isEqualTo(StockAdjustmentsStatus.APPROVED);
        assertThat(approvedAdjustment.getApprovedAt()).isNotNull();
        assertThat(approvedAdjustment.getNotes()).contains("APPROVAL_NOTE");

        // Assert DB State: Inventory decreased from 100.00 to 85.00
        Inventory finalInv = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(finalInv.getOnHandQuantity()).isEqualByComparingTo("85.00");
        assertThat(finalInv.getAvailableQuantity()).isEqualByComparingTo("85.00");

        // Assert DB State: Stock Movement recorded for ADJUSTMENT_DECREASE
        List<StockMovements> movements = stockMovementsRepository.findAll().stream()
                .filter(m -> adjustmentId.equals(m.getReferenceId()))
                .toList();
        assertThat(movements).hasSize(1);
        StockMovements movement = movements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(StockMovementsType.ADJUSTMENT_DECREASE);
        assertThat(movement.getQuantityBefore()).isEqualByComparingTo("100.00");
        assertThat(movement.getQuantityAfter()).isEqualByComparingTo("85.00");
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("-15.00");
    }

    @Test
    @DisplayName("ADJ-03: Rejection - Staff creates pending adjustment -> Manager rejects -> status REJECTED, stock unchanged, no movement")
    void should_RejectAdjustmentAndKeepStockUnchanged_When_ManagerRejects() throws Exception {
        // Initial inventory: 50 items in storage
        inventory = dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("50.00"), BigDecimal.ZERO);

        // Staff creates adjustment requesting to decrease from 50.00 to 40.00 (-10 items)
        StockAdjustmentsRequest request = StockAdjustmentsRequest.builder()
                .inventoryId(inventory.getId())
                .quantityAfter(new BigDecimal("40.00"))
                .reason(ReasonType.EXPIRED)
                .notes("Suspected expired lot")
                .build();

        MvcResult createResult = performPost("/api/v1/stock-adjustments", request, staffUser, ADJUSTMENT_STAFF_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn();

        String adjustmentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // Manager rejects with explanation
        RejectStockAdjustmentRequest rejectRequest = RejectStockAdjustmentRequest.builder()
                .rejectionReason("Quality inspection confirmed batch expiration date is still valid for 6 months")
                .build();

        performPut("/api/v1/stock-adjustments/" + adjustmentId + "/reject", rejectRequest, managerUser, ADJUSTMENT_ALL_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejection_reason").value("Quality inspection confirmed batch expiration date is still valid for 6 months"));

        // Assert DB State: StockAdjustment record is REJECTED
        StockAdjustments rejectedAdjustment = stockAdjustmentsRepository.findById(adjustmentId).orElseThrow();
        assertThat(rejectedAdjustment.getStatus()).isEqualTo(StockAdjustmentsStatus.REJECTED);
        assertThat(rejectedAdjustment.getRejectionReason()).isNotEmpty();

        // Assert DB State: Inventory MUST remain untouched at 50.00
        Inventory inv = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(inv.getOnHandQuantity()).isEqualByComparingTo("50.00");

        // Assert DB State: NO stock movements were recorded
        List<StockMovements> movements = stockMovementsRepository.findAll().stream()
                .filter(m -> adjustmentId.equals(m.getReferenceId()))
                .toList();
        assertThat(movements).isEmpty();
    }

    @Test
    @DisplayName("ADJ-04: Concurrency & Invariant Guard - Approval fails if inventory on-hand quantity changed while adjustment was pending")
    void should_ThrowBadRequest_When_OnHandChangedWhileAdjustmentPending() throws Exception {
        // Initial inventory: 100 items in storage
        inventory = dataFactory.seedInventory(product.getId(), warehouse.getId(), storageLocation.getId(), null,
                new BigDecimal("100.00"), BigDecimal.ZERO);

        // Staff creates adjustment based on on-hand = 100.00 (requesting after = 90.00)
        StockAdjustmentsRequest request = StockAdjustmentsRequest.builder()
                .inventoryId(inventory.getId())
                .quantityAfter(new BigDecimal("90.00"))
                .reason(ReasonType.DAMAGE)
                .notes("Found 10 damaged boxes")
                .build();

        MvcResult createResult = performPost("/api/v1/stock-adjustments", request, staffUser, ADJUSTMENT_STAFF_PERMS)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_APPROVAL"))
                .andReturn();

        String adjustmentId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // Simulate concurrent mutation: another transaction changes inventory on-hand from 100.00 to 120.00
        inventory.setOnHandQuantity(new BigDecimal("120.00"));
        inventoryRepository.saveAndFlush(inventory);

        // Manager attempts to approve the adjustment based on outdated quantity_before (100.00 != 120.00)
        ApproveStockAdjustmentRequest approveRequest = ApproveStockAdjustmentRequest.builder()
                .approvalNote("Attempting to approve stale adjustment")
                .build();

        performPut("/api/v1/stock-adjustments/" + adjustmentId + "/approve", approveRequest, managerUser, ADJUSTMENT_ALL_PERMS)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("STA_001"));

        // Verify status remains PENDING_APPROVAL
        StockAdjustments adjustment = stockAdjustmentsRepository.findById(adjustmentId).orElseThrow();
        assertThat(adjustment.getStatus()).isEqualTo(StockAdjustmentsStatus.PENDING_APPROVAL);

        // Verify inventory on-hand was not modified by the failed approval
        Inventory unchangedInv = inventoryRepository.findById(inventory.getId()).orElseThrow();
        assertThat(unchangedInv.getOnHandQuantity()).isEqualByComparingTo("120.00");
    }
}
