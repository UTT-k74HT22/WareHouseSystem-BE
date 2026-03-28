package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.*;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.*;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockAdjustmentsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.*;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.StockAdjustmentsService;
import org.demo.whs.service.LocationService;
import org.demo.whs.utils.IdentifierGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static java.math.BigDecimal.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAdjustmentsServiceImpl implements StockAdjustmentsService {

    private static final String STOCK_ADJUSTMENT_APPROVAL_PERMISSION = "PERM_STOCK_ADJUSTMENT_APPROVAL_UPDATE";

    private final StockAdjustmentsRepository stockAdjustmentsRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;
    private final StockAdjustmentsMapper stockAdjustmentsMapper;
    private final StockMovementsMapper stockMovementsMapper;
    private final RoleRepository roleRepository;
    private final IdentifierGenerator identifierGenerator;
    private final LocationService locationService;

    @Override
    @Transactional
    public StockAdjustmentsResponse createAdjustment(StockAdjustmentsRequest request) {
        log.info("Attempting to create stock adjustment. inventoryId={}, reason=",
                request == null ? null : request.getInventoryId(),
                request == null ? null : request.getReason());

        //Step 1: Check current user (ROLE_NAME)
        String actorId = getCurrentActorId();
        List<String> roles = roleRepository.findRoleNamesByAccountId(actorId);

        //Step 2: Retrieve inventory with pessimistic lock to ensure data integrity during adjustment
        Inventory inventory = getInventoryForUpdate(request.getInventoryId());

        boolean isAdmin = roles.stream()
                .anyMatch("ADMIN"::equalsIgnoreCase);

        if (!isAdmin) {
            validateWarehouseOwnership(inventory.getWarehouseId());
        }

        BigDecimal quantityBefore = inventory.getOnHandQuantity();
        BigDecimal quantityAfter = request.getQuantityAfter();
        BigDecimal adjustmentQuantity = quantityAfter.subtract(quantityBefore);

        //Step 3: Validate quantity after check
        validateAdjustmentRequest(inventory, quantityAfter, adjustmentQuantity);

        //Step 4: Check requiresApproval and set status
        boolean requiresApproval = requiresApproval(roles, request.getReason(), adjustmentQuantity);
        StockAdjustmentsStatus status = requiresApproval ? StockAdjustmentsStatus.PENDING_APPROVAL : StockAdjustmentsStatus.APPROVED;

        //Step 5: Create and save stock adjustment record
        LocalDateTime now = LocalDateTime.now();
        StockAdjustments stockAdjustments = stockAdjustmentsMapper.toEntity(
                request,
                inventory,
                identifierGenerator.generate("ADJ", 50, stockAdjustmentsRepository::existsByAdjustmentNumber),
                actorId,
                requiresApproval,
                status
        );

        // IMPORTANT: satisfy chk_adjustment_status_metadata (DB strict)
        if (status == StockAdjustmentsStatus.APPROVED) {
            stockAdjustments.setApprovedBy(actorId);
            stockAdjustments.setApprovedAt(now);
            stockAdjustments.setRejectionReason(null);
        } else {
            stockAdjustments.setApprovedBy(null);
            stockAdjustments.setApprovedAt(null);
            stockAdjustments.setRejectionReason(null);
        }

        StockAdjustments savedAdjustment;
        try {
            savedAdjustment = stockAdjustmentsRepository.save(stockAdjustments);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating stock adjustment: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to create stock adjustment due to data integrity violation", ErrorCode.STA_001);
        }

        //Step 6: If no approval required, apply inventory changes and create stock movement record
        if (!requiresApproval) {
            log.info("Stock adjustment does not require approval, applying inventory changes immediately for inventory ID: {}", inventory.getId());

            applyInventoryAfterQuantity(inventory, quantityAfter, actorId, now);
            inventoryRepository.save(inventory);

            StockMovements movement = stockMovementsMapper.toEntity(
                    adjustmentQuantity.signum() > 0 ? StockMovementsType.ADJUSTMENT_INCREASE : StockMovementsType.ADJUSTMENT_DECREASE,
                    inventory.getProductId(),
                    inventory.getWarehouseId(),
                    inventory.getLocationId(),
                    inventory.getBatchId(),
                    adjustmentQuantity,
                    quantityBefore,
                    quantityAfter,
                    ReferenceType.STOCK_ADJUSTMENT,
                    savedAdjustment.getId(),
                    savedAdjustment.getAdjustmentNumber(),
                    request.getNotes(),
                    actorId
            );
            stockMovementsRepository.save(movement);
        }

        //Step 7: Return response
        return stockAdjustmentsMapper.toResponse(savedAdjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentsResponse getById(String id) {
        StockAdjustments adjustment = stockAdjustmentsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));
        return stockAdjustmentsMapper.toResponse(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentsResponse> getAll(Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size);
        Page<StockAdjustments> adjustmentPage = stockAdjustmentsRepository.findAll(pageable);

        List<StockAdjustmentsResponse> content = adjustmentPage.getContent().stream()
                .map(stockAdjustmentsMapper::toResponse)
                .toList();
        return PageResponse.from(adjustmentPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentsResponse> search(SearchStockAdjustmentsRequest request, Integer page, Integer size) {
        if (request == null) {
            request = new SearchStockAdjustmentsRequest();
        }
        validateSearchRequest(request);
        Pageable pageable = buildPageable(page, size);
        Page<StockAdjustments> adjustmentPage = stockAdjustmentsRepository.search(
                request.getStatus(),
                request.getProductId(),
                request.getWarehouseId(),
                request.getInventoryId(),
                request.getAdjustmentNumber(),
                request.getCreatedFrom(),
                request.getCreatedTo(),
                pageable
        );

        List<StockAdjustmentsResponse> content = adjustmentPage.getContent().stream()
                .map(stockAdjustmentsMapper::toResponse)
                .toList();
        return PageResponse.from(adjustmentPage, content);
    }

    @Override
    @Transactional
    public StockAdjustmentsResponse approve(String id, ApproveStockAdjustmentRequest request) {
        log.info("Attempting to approve stock adjustment with ID: {} and approval note present: {}",
                id, request != null && request.getApprovalNote() != null && !request.getApprovalNote().isBlank());

        //Step 1: Check current user (ROLE_NAME)
        String actorId = getCurrentActorId();
        List<String> roles = roleRepository.findRoleNamesByAccountId(actorId);
        assertCanApproveReject(roles);

        //Step 2: Retrieve stock adjustment with pessimistic lock to ensure data integrity during update
        StockAdjustments adjustment = stockAdjustmentsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));

        //Step 3: Validate that adjustment is in pending status before allowing approval
        if (adjustment.getStatus() != StockAdjustmentsStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment is not in pending status", ErrorCode.STA_002);
        }

        //Step 4: Retrieve inventory with pessimistic lock to ensure data integrity during adjustment approval
        Inventory inventory = getInventoryForUpdate(adjustment.getInventoryId());
        validateWarehouseOwnership(inventory.getWarehouseId());

        // IMPORTANT: Use adjustment fields for audit correctness
        BigDecimal quantityBefore = adjustment.getQuantityBefore();
        BigDecimal quantityAfter = adjustment.getQuantityAfter();
        BigDecimal quantityChange = adjustment.getAdjustmentQuantity();

        //check: nếu inventory đã bị thay đổi từ lúc tạo adjustment → không approve
        if (inventory.getOnHandQuantity().compareTo(quantityBefore) != 0) {
            throw new BadRequestException(
                    "Inventory on-hand quantity has changed since adjustment was created. Please review and recreate the adjustment.",
                    ErrorCode.STA_001
            );
        }

        //Step 5: Validate that the adjustment is still valid at approval time
        if (quantityAfter.compareTo(ZERO) < 0
                || quantityAfter.compareTo(inventory.getReservedQuantity()) < 0
                || quantityChange.compareTo(ZERO) == 0) {
            throw new BadRequestException("Inventory state changed and adjustment is no longer valid", ErrorCode.STA_001);
        }

        //Step 6: Apply the inventory changes and update the stock adjustment record with approval details
        LocalDateTime now = LocalDateTime.now();
        applyInventoryAfterQuantity(inventory, quantityAfter, actorId, now);
        inventoryRepository.save(inventory);
        //Step 7: Update stock adjustment record with approval details and save
        adjustment.setStatus(StockAdjustmentsStatus.APPROVED);
        adjustment.setApprovedBy(actorId);
        adjustment.setApprovedAt(now);
        adjustment.setRejectionReason(null);
        adjustment.setUpdatedBy(actorId);
        if (request != null && request.getApprovalNote() != null && !request.getApprovalNote().isBlank()) {
            adjustment.setNotes(appendNote(adjustment.getNotes(), "APPROVAL_NOTE", request.getApprovalNote()));
        }
        StockAdjustments savedAdjustment = stockAdjustmentsRepository.save(adjustment);

        StockMovements movement = stockMovementsMapper.toEntity(
                quantityChange.signum() > 0 ? StockMovementsType.ADJUSTMENT_INCREASE : StockMovementsType.ADJUSTMENT_DECREASE,
                inventory.getProductId(),
                inventory.getWarehouseId(),
                inventory.getLocationId(),
                inventory.getBatchId(),
                quantityChange,
                quantityBefore,
                quantityAfter,
                ReferenceType.STOCK_ADJUSTMENT,
                savedAdjustment.getId(),
                savedAdjustment.getAdjustmentNumber(),
                request == null ? null : request.getApprovalNote(),
                actorId
        );
        stockMovementsRepository.save(movement);

        return stockAdjustmentsMapper.toResponse(savedAdjustment);
    }

    @Override
    @Transactional
    public StockAdjustmentsResponse reject(String id, RejectStockAdjustmentRequest request) {
        log.info("Attempting to reject stock adjustment with ID: {} and rejection reason present: {}",
                id, request != null && request.getRejectionReason() != null && !request.getRejectionReason().isBlank());

        //Step 1: Check current user (ROLE_NAME)
        String actorId = getCurrentActorId();
        List<String> roles = roleRepository.findRoleNamesByAccountId(actorId);
        assertCanApproveReject(roles);

        //Step 2: Retrieve stock adjustment with pessimistic lock to ensure data integrity during update
        StockAdjustments adjustment = stockAdjustmentsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));

        //Step 3: Validate that adjustment is in pending status before allowing rejection
        if (adjustment.getStatus() != StockAdjustmentsStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment is not in pending status", ErrorCode.STA_002);
        }

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new BadRequestException("Rejection reason is required", ErrorCode.STA_003);
        }

        LocalDateTime now = LocalDateTime.now();
        adjustment.setStatus(StockAdjustmentsStatus.REJECTED);
        adjustment.setRejectionReason(request.getRejectionReason());
        adjustment.setApprovedBy(actorId);
        adjustment.setApprovedAt(now);
        adjustment.setUpdatedBy(actorId);

        adjustment.setNotes(appendNote(adjustment.getNotes(), "REJECTION_REASON", request.getRejectionReason()));

        StockAdjustments savedAdjustment = stockAdjustmentsRepository.save(adjustment);
        return stockAdjustmentsMapper.toResponse(savedAdjustment);
    }

    private Inventory getInventoryForUpdate(String inventoryId) {
        return inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found", ErrorCode.INV_001));
    }

    private void validateAdjustmentRequest(Inventory inventory, BigDecimal quantityAfter, BigDecimal adjustmentQuantity) {
        validateQuantityFormat(quantityAfter);

        if (quantityAfter.compareTo(ZERO) < 0) {
            throw new BadRequestException("Quantity after must be non-negative", ErrorCode.STA_001);
        }
        if (quantityAfter.compareTo(inventory.getReservedQuantity()) < 0) {
            throw new BadRequestException("Quantity after cannot be lower than reserved quantity", ErrorCode.STA_001);
        }
        if (adjustmentQuantity.compareTo(ZERO) == 0) {
            throw new BadRequestException("Adjustment quantity cannot be zero", ErrorCode.STA_001);
        }
    }

    private void validateQuantityFormat(BigDecimal quantityAfter) {
        if (quantityAfter == null) {
            throw new BadRequestException("Quantity after is required", ErrorCode.STA_001);
        }
        if (quantityAfter.scale() > 2) {
            throw new BadRequestException("Quantity after must not exceed 2 decimal places", ErrorCode.STA_001);
        }
        String plain = quantityAfter.abs().toPlainString();
        int dotIdx = plain.indexOf('.');
        String integerPart = dotIdx >= 0 ? plain.substring(0, dotIdx) : plain;
        if (integerPart.length() > 13) {
            throw new BadRequestException("Quantity after exceeds maximum supported value", ErrorCode.STA_001);
        }
    }

    private void applyInventoryAfterQuantity(Inventory inventory, BigDecimal quantityAfter, String actorId, LocalDateTime now) {
        if (quantityAfter.compareTo(inventory.getReservedQuantity()) < 0) {
            throw new BadRequestException("Quantity after cannot be lower than reserved quantity", ErrorCode.STA_001);
        }
        inventory.setOnHandQuantity(quantityAfter);
        inventory.setUpdatedBy(actorId);
        inventory.setLastMovementAt(now);
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int targetPage = page == null ? 0 : page;
        int targetSize = size == null ? 10 : size;
        if (targetPage < 0 || targetSize <= 0 || targetSize > 100) {
            throw new BadRequestException("Invalid pagination parameters", ErrorCode.COM_001);
        }

        return PageRequest.of(targetPage, targetSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private String getCurrentActorId() {
        String accountId = SecurityUtils.getCurrentAccountId();
        if (accountId != null && !accountId.isBlank()) {
            return accountId;
        }

        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));
        return account.getId();
    }

    private String appendNote(String existing, String key, String value) {
        if (value == null || value.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return key + ": " + value.trim();
        }
        return existing + System.lineSeparator() + key + ": " + value.trim();
    }

    private boolean requiresApproval(List<String> actorRoles, ReasonType reasonType, BigDecimal adjustmentQuantity) {
        if (hasRole(actorRoles, RoleType.ADMIN)) {
            return false;
        }

        boolean sensitiveReason = reasonType == ReasonType.THEFT || reasonType == ReasonType.SYSTEM_ERROR;
        boolean largeDelta = adjustmentQuantity.abs().compareTo(new BigDecimal("5.00")) >= 0;

        if (hasRole(actorRoles, RoleType.MANAGER)) {
            return sensitiveReason || largeDelta;
        }

        return true;
    }

    private void validateSearchRequest(SearchStockAdjustmentsRequest request) {
        if (request == null) {
            return;
        }
        LocalDateTime createdFrom = request.getCreatedFrom();
        LocalDateTime createdTo = request.getCreatedTo();
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BadRequestException("createdFrom must be before or equal to createdTo", ErrorCode.COM_001);
        }
    }

    private void assertCanApproveReject(List<String> roles) {
        if (SecurityUtils.hasAuthority(STOCK_ADJUSTMENT_APPROVAL_PERMISSION)) {
            return;
        }

        if (roles == null || roles.isEmpty()) {
            throw new BadRequestException("User role not found", ErrorCode.AUTH_002);
        }

        if (!hasRole(roles, RoleType.ADMIN)) {
            throw new BadRequestException("You do not have permission to approve/reject adjustments", ErrorCode.AUTH_002);
        }
    }

    private boolean hasRole(List<String> roleNames, RoleType target) {
        if (roleNames == null || roleNames.isEmpty()) return false;
        return roleNames.contains(target.name());
    }

    private void validateWarehouseOwnership(String warehouseId) {
        String accountId = getCurrentActorId();

        Employee employee = employeeRepository.findByAccountId(accountId)
                .orElseThrow(() -> new BadRequestException("Employee not found for current user", ErrorCode.AUTH_003));
        if (!employee.getWarehouseId().equals(warehouseId)) {
            throw new BadRequestException("You do not have permission to access this warehouse", ErrorCode.AUTH_003);
        }
    }
}
