package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.Batch;
import org.demo.whs.entity.Employee;
import org.demo.whs.entity.Inventory;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.StockTransfers;
import org.demo.whs.entity.dto.request.StockTransfers.StockTransfersRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockTransfers.StockTransfersResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.LocationType;
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
import org.demo.whs.repository.EmployeeRepository;
import org.demo.whs.repository.InventoryRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.StockMovementsRepository;
import org.demo.whs.repository.StockTransfersRepository;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.StockTransfersService;
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
    private final EmployeeRepository employeeRepository;
    private final StockTransfersMapper stockTransfersMapper;
    private final StockMovementsMapper stockMovementsMapper;
    private final IdentifierGenerator identifierGenerator;

    @Override
    @Transactional
    public StockTransfersResponse createTransfer(StockTransfersRequest request) {
        log.info("StockTransfersServiceImpl createTransfer request={}", request);

        validateTransferRequest(request);

        String actorId = getCurrentActorId();
        String transferNumber = identifierGenerator.generate("TRF", 50, stockTransfersRepository::existsByTransferNumber);
        StockTransfers transfer = stockTransfersMapper.toEntity(request, transferNumber, actorId);
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
        log.info("Attempting to complete stock transfer with ID: {}", id);

        //Step 1: Check current user and permissions
        String actorId = getCurrentActorId();
        LocalDateTime now = LocalDateTime.now();

        //Step 2: Load transfer và kiểm tra trạng thái (chỉ cho phép complete nếu đang ở trạng thái DRAFT)
        StockTransfers transfer = stockTransfersRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Stock transfer not found", ErrorCode.STF_001));

        if (transfer.getStatus() != StockTransfersStatus.DRAFT) {
            throw new BadRequestException("Only draft transfer can be completed", ErrorCode.STF_002);
        }

        //Step 3: Validate quantity
        BigDecimal quantity = transfer.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Quantity must be greater than 0", ErrorCode.STF_003);
        }

        //Step 4: Lock inventory theo thứ tự cố định để tránh deadlock
        String fromLocationId = transfer.getFromLocationId();
        String toLocationId = transfer.getToLocationId();
        boolean lockFromFirst = buildInventoryLockKey(transfer, fromLocationId)
                .compareTo(buildInventoryLockKey(transfer, toLocationId)) <= 0;

        String firstLocationId = lockFromFirst ? fromLocationId : toLocationId;
        String secondLocationId = lockFromFirst ? toLocationId : fromLocationId;

        Inventory firstInv = findInventoryForUpdate(transfer, firstLocationId).orElse(null);
        Inventory secondInv = findInventoryForUpdate(transfer, secondLocationId).orElse(null);

        Inventory sourceInventory = lockFromFirst ? firstInv : secondInv; //nguồn tồn kho
        Inventory destinationInventory = lockFromFirst ? secondInv : firstInv; // kho đích

        if (sourceInventory == null) {
            throw new NotFoundException("Source inventory not found", ErrorCode.INV_001);
        }

        // Step 5: Nếu kho đích chưa có tồn kho thì tạo mới (với onHand=0) để đảm bảo tính nhất quán
        if (destinationInventory == null) {
            destinationInventory = createOrReloadDestinationInventory(transfer, actorId);
        }

        // Step 6: Kiểm tra số lượng hàng có sẵn tại nguồn (hàng có sẵn đã được đặt trước).
        BigDecimal onHand = defaultZero(sourceInventory.getOnHandQuantity()); // tồn kho thực tế
        BigDecimal available = defaultZero(sourceInventory.getAvailableQuantity()); // loại trừ cả reserved và quarantine

        if (available.compareTo(quantity) < 0) {
            throw new BadRequestException("Insufficient available stock in source inventory", ErrorCode.INV_004);
        }

        // Step 7: Tính sự chênh lệch tồn kho và cập nhật cả 2 bên (nguồn trừ đi, đích cộng vào)
        BigDecimal sourceBefore = onHand; // số lượng hàng thực tế tại nguồn trước khi chuyển
        BigDecimal sourceAfter = sourceBefore.subtract(quantity); // số lượng hàng thực tế tại nguồn sau khi chuyển
        // Kiểm tra lại số lượng hàng có sẵn sau khi trừ đi lượng chuyển để đảm bảo không bị âm do các giao dịch khác đã cập nhật trước đó
        BigDecimal unavailableAfterTransfer = defaultZero(sourceInventory.getReservedQuantity())
                .add(defaultZero(sourceInventory.getQuarantineQuantity()));
        if (sourceAfter.compareTo(unavailableAfterTransfer) < 0) {
            throw new BadRequestException("Insufficient available stock in source inventory after re-checking", ErrorCode.INV_004);
        }
        BigDecimal destinationBefore = defaultZero(destinationInventory.getOnHandQuantity()); // số lượng hàng thực tế tại đích trước khi chuyển
        BigDecimal destinationAfter = destinationBefore.add(quantity); // số lượng hàng thực tế tại đích sau khi chuyển

        // Step 8: Cập nhật tồn kho và tạo bản ghi chuyển kho
        sourceInventory.setOnHandQuantity(sourceAfter);
        sourceInventory.setLastMovementAt(now);
        sourceInventory.setUpdatedBy(actorId);

        destinationInventory.setOnHandQuantity(destinationAfter);
        destinationInventory.setLastMovementAt(now);
        destinationInventory.setUpdatedBy(actorId);

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destinationInventory);

        // Step 9: Tạo bản ghi lịch sử chuyển kho
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
                transfer.getId(),
                transfer.getTransferNumber(),
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
                transfer.getId(),
                transfer.getTransferNumber(),
                transfer.getNotes(),
                actorId
        );
        stockMovementsRepository.save(transferOutMovement);
        stockMovementsRepository.save(transferInMovement);

        //Step 10: Update transfer status
        transfer.setStatus(StockTransfersStatus.COMPLETED);
        transfer.setCompletedAt(now);
        transfer.setUpdatedBy(actorId);

        StockTransfers savedTransfer = stockTransfersRepository.save(transfer);

        //Step 11: Return response
        return stockTransfersMapper.toResponse(savedTransfer);
    }

    @Override
    @Transactional
    public StockTransfersResponse cancel(String id) {
        log.info("Attempting to cancel stock transfer with ID: {}", id);

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
        validateWarehouseOwnership(request.getWarehouseId());

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

        validateLocationForTransfer(fromLocation, "Source");
        validateLocationForTransfer(toLocation, "Destination");

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
            validateBatchForTransfer(batch);
        }
    }

    private void validateLocationForTransfer(Locations location, String locationType) {
        if (location.getStatus() != LocationStatus.ACTIVE) {
            throw new BadRequestException(
                    locationType + " location is not active for stock transfer",
                    ErrorCode.LOC_007
            );
        }

        if (!isValidLocationTypeForTransfer(location.getType())) {
            throw new BadRequestException(
                    locationType + " location type is not valid for stock transfer",
                    ErrorCode.LOC_008
            );
        }
    }

    private boolean isValidLocationTypeForTransfer(LocationType type) {
        return type == LocationType.STORAGE
                || type == LocationType.PICKING
                || type == LocationType.STAGING;
    }

    private void validateBatchForTransfer(Batch batch) {
        if (batch.getStatus() != BatchStatus.AVAILABLE) {
            throw new BadRequestException(
                    "Batch is not available for stock transfer",
                    ErrorCode.BATCH_012
            );
        }

        if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException(
                    "Batch has expired",
                    ErrorCode.BATCH_020
            );
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

    private java.util.Optional<Inventory> findInventoryForUpdate(StockTransfers transfer, String locationId) {
        return inventoryRepository.findByDimensionForUpdate(
                transfer.getProductId(),
                transfer.getWarehouseId(),
                locationId,
                transfer.getBatchId()
        );
    }

    private Inventory createOrReloadDestinationInventory(StockTransfers transfer, String actorId) {
        try {
            Inventory newInv = createDestinationInventory(transfer, actorId);
            return inventoryRepository.saveAndFlush(newInv);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Destination inventory already created concurrently for transferId={}", transfer.getId());
            return findInventoryForUpdate(transfer, transfer.getToLocationId())
                    .orElseThrow(() -> new NotFoundException(
                            "Destination inventory not found after concurrent creation",
                            ErrorCode.INV_001
                    ));
        }
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

    private BigDecimal defaultZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String buildInventoryLockKey(StockTransfers transfer, String locationId) {
        return String.join("|",
                normalizeKeyPart(transfer.getProductId()),
                normalizeKeyPart(transfer.getWarehouseId()),
                normalizeKeyPart(locationId),
                normalizeKeyPart(transfer.getBatchId())
        );
    }

    private String normalizeKeyPart(String value) {
        return value == null ? "" : value;
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