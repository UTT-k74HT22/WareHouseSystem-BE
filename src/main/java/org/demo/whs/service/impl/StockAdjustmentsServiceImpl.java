package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.StockAdjustments;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.StockAdjustments.ApproveStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.RejectStockAdjustmentRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.SearchStockAdjustmentsRequest;
import org.demo.whs.entity.dto.request.StockAdjustments.StockAdjustmentsRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockAdjustments.StockAdjustmentsResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockAdjustmentsStatus;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockAdjustmentsMapper;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.StockAdjustmentsRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.StockAdjustmentsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAdjustmentsServiceImpl implements StockAdjustmentsService {

    private final StockAdjustmentsRepository stockAdjustmentsRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final AccountRepository accountRepository;
    private final StockAdjustmentsMapper stockAdjustmentsMapper;
    private final StockMovementsMapper stockMovementsMapper;

    /**
     * Creates a new stock adjustment request.
     *
     * @param request the stock adjustment request details
     * @return the created stock adjustment response
     */
    @Override
    @Transactional
    public StockAdjustmentsResponse createAdjustment(StockAdjustmentsRequest request) {
        Inventory inventory = getInventoryForUpdate(request.getInventoryId());
        BigDecimal quantityBefore = inventory.getOnHandQuantity();
        BigDecimal quantityAfter = request.getQuantityAfter();
        BigDecimal adjustmentQuantity = quantityAfter.subtract(quantityBefore);

        validateAdjustmentRequest(inventory, quantityAfter, adjustmentQuantity);

        String actorId = getCurrentActorId();
        boolean requiresApproval = Boolean.TRUE.equals(request.getRequiresApproval());
        StockAdjustmentsStatus targetStatus = requiresApproval
                ? StockAdjustmentsStatus.PENDING_APPROVAL
                : StockAdjustmentsStatus.APPROVED;

        StockAdjustments adjustment = stockAdjustmentsMapper.toEntity(
                request,
                inventory,
                generateAdjustmentNumber(),
                actorId,
                requiresApproval,
                targetStatus
        );
        StockAdjustments savedAdjustment = stockAdjustmentsRepository.save(adjustment);

        if (!requiresApproval) {
            applyInventoryAfterQuantity(inventory, quantityAfter);
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

        return stockAdjustmentsMapper.toResponse(savedAdjustment);
    }

    /**
     * Retrieves a stock adjustment by its ID.
     *
     * @param id the ID of the stock adjustment
     * @return the stock adjustment response
     */
    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentsResponse getById(String id) {
        StockAdjustments adjustment = stockAdjustmentsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));
        return stockAdjustmentsMapper.toResponse(adjustment);
    }

    /**
     * Retrieves a paginated list of all stock adjustments.
     *
     * @param page the page number for pagination
     * @param size the page size for pagination
     * @return a paginated response containing the list of stock adjustments
     */
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

    /**
     * Searches for stock adjustments based on the provided criteria.
     *
     * @param request the search criteria for stock adjustments
     * @param page    the page number for pagination
     * @param size    the page size for pagination
     * @return a paginated response containing the search results
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockAdjustmentsResponse> search(SearchStockAdjustmentsRequest request, Integer page, Integer size) {
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

    /**
     * Approves a stock adjustment request.
     *
     * @param id         the ID of the stock adjustment to approve
     * @param request    the approval details
     * @return the updated stock adjustment response after approval
     */
    @Override
    @Transactional
    public StockAdjustmentsResponse approve(String id, ApproveStockAdjustmentRequest request) {
        String actorId = getCurrentActorId();
        StockAdjustments adjustment = stockAdjustmentsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));

        if (adjustment.getStatus() != StockAdjustmentsStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment is not in pending status", ErrorCode.STA_002);
        }

        Inventory inventory = getInventoryForUpdate(adjustment.getInventoryId());

        BigDecimal quantityBefore = inventory.getOnHandQuantity();
        BigDecimal quantityAfter = adjustment.getQuantityAfter();
        BigDecimal quantityChange = quantityAfter.subtract(quantityBefore);

        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0
                || quantityAfter.compareTo(inventory.getReservedQuantity()) < 0
                || quantityChange.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Inventory state changed and adjustment is no longer valid", ErrorCode.STA_001);
        }

        applyInventoryAfterQuantity(inventory, quantityAfter);
        inventoryRepository.save(inventory);

        adjustment.setStatus(StockAdjustmentsStatus.APPROVED);
        adjustment.setApprovedBy(actorId);
        adjustment.setApprovedAt(LocalDateTime.now());
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

    /**
     * @param id the ID of the stock adjustment to reject
     * @param request the rejection details
     * @return the updated stock adjustment response after rejection
     */
    @Override
    @Transactional
    public StockAdjustmentsResponse reject(String id, RejectStockAdjustmentRequest request) {
        String actorId = getCurrentActorId();
        StockAdjustments adjustment = stockAdjustmentsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock adjustment not found", ErrorCode.STA_404));

        if (adjustment.getStatus() != StockAdjustmentsStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Adjustment is not in pending status", ErrorCode.STA_002);
        }

        adjustment.setStatus(StockAdjustmentsStatus.REJECTED);
        adjustment.setRejectionReason(request.getRejectionReason());
        adjustment.setApprovedBy(actorId);
        adjustment.setApprovedAt(LocalDateTime.now());
        adjustment.setUpdatedBy(actorId);

        StockAdjustments savedAdjustment = stockAdjustmentsRepository.save(adjustment);
        return stockAdjustmentsMapper.toResponse(savedAdjustment);
    }

    private Inventory getInventoryForUpdate(String inventoryId) {
        return inventoryRepository.findByIdForUpdate(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventory not found", ErrorCode.INV_001));
    }

    private void validateAdjustmentRequest(Inventory inventory, BigDecimal quantityAfter, BigDecimal adjustmentQuantity) {
        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Quantity after must be non-negative", ErrorCode.STA_001);
        }
        if (quantityAfter.compareTo(inventory.getReservedQuantity()) < 0) {
            throw new BadRequestException("Quantity after cannot be lower than reserved quantity", ErrorCode.STA_001);
        }
        if (adjustmentQuantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("Adjustment quantity cannot be zero", ErrorCode.STA_001);
        }
    }

    private void applyInventoryAfterQuantity(Inventory inventory, BigDecimal quantityAfter) {
        if (quantityAfter.compareTo(inventory.getReservedQuantity()) < 0) {
            throw new BadRequestException("Quantity after cannot be lower than reserved quantity", ErrorCode.STA_001);
        }
        inventory.setOnHandQuantity(quantityAfter);
        inventory.setUpdatedBy(getCurrentActorId());
        inventory.setLastMovementAt(LocalDateTime.now());
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
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Unauthenticated request", ErrorCode.AUTH_002);
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User account not found", ErrorCode.AUTH_002));
        return account.getId();
    }

    private String generateAdjustmentNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for (int i = 0; i < 10; i++) {
            String candidate = "ADJ-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!stockAdjustmentsRepository.existsByAdjustmentNumber(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Unable to generate unique adjustment number", ErrorCode.STA_001);
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
}
