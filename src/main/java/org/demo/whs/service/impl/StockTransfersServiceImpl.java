package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.StockTransfers;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.entity.enums.StockTransfersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.mapper.StockTransfersMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.StockTransfersRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.StockTransfersService;
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
public class StockTransfersServiceImpl implements StockTransfersService {

    private final StockTransfersRepository stockTransfersRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementsRepository stockMovementsRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final BatchRepository batchRepository;
    private final AccountRepository accountRepository;
    private final StockTransfersMapper stockTransfersMapper;
    private final StockMovementsMapper stockMovementsMapper;

    @Override
    @Transactional
    public StockTransfersResponse createTransfer(StockTransfersRequest request) {
        validateTransferRequest(request);

        String actorId = getCurrentActorId();
        StockTransfers transfer = stockTransfersMapper.toEntity(request, generateTransferNumber(), actorId);
        StockTransfers savedTransfer = stockTransfersRepository.save(transfer);
        return stockTransfersMapper.toResponse(savedTransfer);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransfersResponse getById(String id) {
        StockTransfers transfer = stockTransfersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock transfer not found", ErrorCode.STF_001));
        return stockTransfersMapper.toResponse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StockTransfersResponse> getAll(Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size);
        Page<StockTransfers> transferPage = stockTransfersRepository.findAll(pageable);

        List<StockTransfersResponse> content = transferPage.getContent().stream()
                .map(stockTransfersMapper::toResponse)
                .toList();
        return PageResponse.from(transferPage, content);
    }

    @Override
    @Transactional
    public StockTransfersResponse complete(String id) {
        String actorId = getCurrentActorId();
        StockTransfers transfer = stockTransfersRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock transfer not found", ErrorCode.STF_001));

        if (transfer.getStatus() != StockTransfersStatus.DRAFT) {
            throw new BadRequestException("Only draft transfer can be completed", ErrorCode.STF_002);
        }

        Inventory sourceInventory = inventoryRepository.findByDimensionForUpdate(
                transfer.getProductId(),
                transfer.getWarehouseId(),
                transfer.getFromLocationId(),
                transfer.getBatchId()
        ).orElseThrow(() -> new NotFoundException("Source inventory not found", ErrorCode.INV_001));

        Inventory destinationInventory = inventoryRepository.findByDimensionForUpdate(
                transfer.getProductId(),
                transfer.getWarehouseId(),
                transfer.getToLocationId(),
                transfer.getBatchId()
        ).orElseGet(() -> createDestinationInventory(transfer, actorId));

        BigDecimal quantity = transfer.getQuantity();
        BigDecimal availableSource = sourceInventory.getOnHandQuantity().subtract(sourceInventory.getReservedQuantity());
        if (availableSource.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock at source location", ErrorCode.INV_004);
        }

        BigDecimal sourceBefore = sourceInventory.getOnHandQuantity();
        BigDecimal sourceAfter = sourceBefore.subtract(quantity);
        BigDecimal destinationBefore = destinationInventory.getOnHandQuantity();
        BigDecimal destinationAfter = destinationBefore.add(quantity);

        sourceInventory.setOnHandQuantity(sourceAfter);
        sourceInventory.setLastMovementAt(LocalDateTime.now());
        sourceInventory.setUpdatedBy(actorId);

        destinationInventory.setOnHandQuantity(destinationAfter);
        destinationInventory.setLastMovementAt(LocalDateTime.now());
        destinationInventory.setUpdatedBy(actorId);

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destinationInventory);

        transfer.setStatus(StockTransfersStatus.COMPLETED);
        transfer.setCompletedAt(LocalDateTime.now());
        transfer.setUpdatedBy(actorId);
        StockTransfers savedTransfer = stockTransfersRepository.save(transfer);

        StockMovements transferOutMovement = stockMovementsMapper.toEntity(
                StockMovementsType.TRANSFER_OUT,
                transfer.getProductId(),
                transfer.getWarehouseId(),
                transfer.getFromLocationId(),
                transfer.getBatchId(),
                quantity.negate(),
                sourceBefore,
                sourceAfter,
                ReferenceType.STOCK_TRANSFER,
                savedTransfer.getId(),
                savedTransfer.getTransferNumber(),
                transfer.getNotes(),
                actorId
        );
        StockMovements transferInMovement = stockMovementsMapper.toEntity(
                StockMovementsType.TRANSFER_IN,
                transfer.getProductId(),
                transfer.getWarehouseId(),
                transfer.getToLocationId(),
                transfer.getBatchId(),
                quantity,
                destinationBefore,
                destinationAfter,
                ReferenceType.STOCK_TRANSFER,
                savedTransfer.getId(),
                savedTransfer.getTransferNumber(),
                transfer.getNotes(),
                actorId
        );
        stockMovementsRepository.save(transferOutMovement);
        stockMovementsRepository.save(transferInMovement);

        return stockTransfersMapper.toResponse(savedTransfer);
    }

    @Override
    @Transactional
    public StockTransfersResponse cancel(String id) {
        String actorId = getCurrentActorId();
        StockTransfers transfer = stockTransfersRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock transfer not found", ErrorCode.STF_001));

        if (transfer.getStatus() != StockTransfersStatus.DRAFT) {
            throw new BadRequestException("Only draft transfer can be cancelled", ErrorCode.STF_002);
        }

        transfer.setStatus(StockTransfersStatus.CANCELLED);
        transfer.setUpdatedBy(actorId);

        StockTransfers savedTransfer = stockTransfersRepository.save(transfer);
        return stockTransfersMapper.toResponse(savedTransfer);
    }

    private void validateTransferRequest(StockTransfersRequest request) {
        if (request.getFromLocationId().equals(request.getToLocationId())) {
            throw new BadRequestException("Source and destination locations must be different", ErrorCode.STF_002);
        }

        if (!productRepository.existsById(request.getProductId())) {
            throw new BadRequestException("Product not found", ErrorCode.PROD_001);
        }

        Locations fromLocation = locationRepository.findById(request.getFromLocationId())
                .orElseThrow(() -> new BadRequestException("Source location not found", ErrorCode.LOC_001));
        Locations toLocation = locationRepository.findById(request.getToLocationId())
                .orElseThrow(() -> new BadRequestException("Destination location not found", ErrorCode.LOC_001));

        if (!fromLocation.getWarehouseId().equals(request.getWarehouseId())
                || !toLocation.getWarehouseId().equals(request.getWarehouseId())) {
            throw new BadRequestException("Transfer locations must belong to the provided warehouse", ErrorCode.STF_002);
        }

        if (request.getBatchId() != null && !request.getBatchId().isBlank()) {
            Batch batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new BadRequestException("Batch not found", ErrorCode.BATCH_001));
            if (!batch.getProductId().equals(request.getProductId())) {
                throw new BadRequestException("Batch does not belong to the provided product", ErrorCode.STF_002);
            }
        }
    }

    private Inventory createDestinationInventory(StockTransfers transfer, String actorId) {
        Inventory inventory = Inventory.builder()
                .productId(transfer.getProductId())
                .warehouseId(transfer.getWarehouseId())
                .locationId(transfer.getToLocationId())
                .batchId(transfer.getBatchId())
                .onHandQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .version(0)
                .build();
        inventory.setCreatedBy(actorId);
        inventory.setUpdatedBy(actorId);
        return inventory;
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

    private String generateTransferNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        for (int i = 0; i < 10; i++) {
            String candidate = "TRF-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!stockTransfersRepository.existsByTransferNumber(candidate)) {
                return candidate;
            }
        }
        throw new BadRequestException("Unable to generate unique transfer number", ErrorCode.STF_002);
    }

}
