package org.demo.whs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.whs.entity.Account;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.Warehouses;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsFilterRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.InboundReceiptsRequest;
import org.demo.whs.entity.dto.request.InboundReceipts.UpdateInboundReceiptsRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.dto.response.InboundReceipts.InboundReceiptsResponse;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.mapper.InboundReceiptsMapper;
import org.demo.whs.repository.AccountRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.demo.whs.repository.WareHouseRepository;
import org.demo.whs.repository.specification.InboundReceiptsSpecification;
import org.demo.whs.security.SecurityUtils;
import org.demo.whs.service.InboundReceiptsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the InboundReceiptsService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InboundReceiptsServiceImpl implements InboundReceiptsService {

    private final InboundReceiptsRepository inboundReceiptsRepository;
    private final InboundReceiptLinesRepository inboundReceiptLinesRepository;
    private final PurchaseOrdersRepository purchaseOrdersRepository;
    private final WareHouseRepository wareHouseRepository;
    private final AccountRepository accountRepository;
    private final InboundReceiptsMapper inboundReceiptsMapper;
    private final InboundReceiptLinesMapper inboundReceiptLinesMapper;

    @Override
    @Transactional
    public InboundReceiptsResponse create(InboundReceiptsRequest request) {
        log.info("Create inbound receipt draft requested, purchaseOrderId={}", request.getPurchaseOrderId());

        // Step 1: Validate Purchase Order
        PurchaseOrders purchaseOrders = purchaseOrdersRepository.findByIdForUpdate(request.getPurchaseOrderId())
                .orElseThrow(() -> new NotFoundException("Purchase order not found", ErrorCode.PO_001));

        if (purchaseOrders.getStatus() != PurchaseOrdersStatus.CONFIRMED
                && purchaseOrders.getStatus() != PurchaseOrdersStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException(
                    "Receipt can only be created from CONFIRMED or PARTIALLY_RECEIVED purchase orders",
                    ErrorCode.COM_001
            );
        }

        // Step 2: Fetch Warehouse info
        Warehouses warehouses = wareHouseRepository.findById(purchaseOrders.getWarehouseId())
                .orElseThrow(() -> new NotFoundException("Warehouse not found", ErrorCode.WHS_001));

        // Step 3: Create draft receipt
        String receiptNumber = generateInboundReceiptNumber();
        String actorId = getCurrentActorId();

        InboundReceipts receipt = inboundReceiptsMapper.toEntity(request);
        receipt.setReceiptNumber(receiptNumber);
        receipt.setWarehouseId(purchaseOrders.getWarehouseId());
        receipt.setCreatedBy(actorId);
        receipt.setUpdatedBy(actorId);

        receipt = inboundReceiptsRepository.save(receipt);

        log.info("Inbound receipt draft created successful, id={}, receiptNumber={}", receipt.getId(), receipt.getReceiptNumber());
        return inboundReceiptsMapper.toResponse(receipt, purchaseOrders, warehouses, Collections.emptyList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InboundReceiptsResponse> getAll(InboundReceiptsFilterRequest filter, Pageable pageable) {
        log.info("Get inbound receipts, filter={}, pageable={}", filter, pageable);
        validatePageable(pageable);
        normalizeAndValidateFilter(filter);

        Page<InboundReceipts> receiptPage = inboundReceiptsRepository.findAll(
                InboundReceiptsSpecification.withFilter(filter),
                pageable
        );

        List<InboundReceipts> receipts = receiptPage.getContent();
        if (receipts.isEmpty()) {
            return PageResponse.from(receiptPage, Collections.emptyList());
        }

        // Fetch related POs and Warehouses in bulk for mapping
        Set<String> poIds = receipts.stream().map(InboundReceipts::getPurchaseOrderId).collect(Collectors.toSet());
        Set<String> whIds = receipts.stream().map(InboundReceipts::getWarehouseId).collect(Collectors.toSet());

        Map<String, PurchaseOrders> poMap = purchaseOrdersRepository.findAllById(poIds).stream()
                .collect(Collectors.toMap(PurchaseOrders::getId, po -> po));
        Map<String, Warehouses> whMap = wareHouseRepository.findByIdIn(whIds).stream()
                .collect(Collectors.toMap(Warehouses::getId, wh -> wh));

        List<InboundReceiptsResponse> responses = receipts.stream()
                .map(receipt -> inboundReceiptsMapper.toResponse(
                        receipt,
                        poMap.get(receipt.getPurchaseOrderId()),
                        whMap.get(receipt.getWarehouseId()),
                        Collections.emptyList()
                ))
                .toList();

        return PageResponse.from(receiptPage, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public InboundReceiptsResponse getById(String id) {
        log.info("Get inbound receipt by id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        PurchaseOrders po = purchaseOrdersRepository.findById(receipt.getPurchaseOrderId()).orElse(null);
        Warehouses wh = wareHouseRepository.findById(receipt.getWarehouseId()).orElse(null);

        return inboundReceiptsMapper.toResponse(receipt, po, wh, getLineResponses(receipt.getId()));
    }

    @Override
    @Transactional
    public InboundReceiptsResponse update(String id, UpdateInboundReceiptsRequest request) {
        log.info("Update inbound receipt draft requested, id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        if (receipt.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException("Only draft receipts can be updated", ErrorCode.COM_001);
        }

        inboundReceiptsMapper.updateEntity(receipt, request);
        receipt.setUpdatedBy(getCurrentActorId());

        receipt = inboundReceiptsRepository.save(receipt);

        PurchaseOrders po = purchaseOrdersRepository.findById(receipt.getPurchaseOrderId()).orElse(null);
        Warehouses wh = wareHouseRepository.findById(receipt.getWarehouseId()).orElse(null);

        return inboundReceiptsMapper.toResponse(receipt, po, wh, getLineResponses(receipt.getId()));
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Delete inbound receipt draft requested, id={}", id);
        InboundReceipts receipt = inboundReceiptsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Inbound receipt not found", ErrorCode.COM_004));

        if (receipt.getStatus() != InboundReceiptsStatus.DRAFT) {
            throw new BadRequestException("Only draft receipts can be deleted", ErrorCode.COM_001);
        }

        long lineCount = inboundReceiptLinesRepository.countByInboundReceiptId(id);
        if (lineCount > 0) {
            throw new BadRequestException("Cannot delete receipt with lines", ErrorCode.COM_001);
        }

        inboundReceiptsRepository.delete(receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InboundReceiptsResponse> getByPurchaseOrderId(String purchaseOrderId) {
        log.info("Get inbound receipts by purchaseOrderId={}", purchaseOrderId);
        List<InboundReceipts> receipts = inboundReceiptsRepository.findByPurchaseOrderIdOrderByCreatedAtDesc(purchaseOrderId);
        if (receipts.isEmpty()) {
            return Collections.emptyList();
        }

        PurchaseOrders po = purchaseOrdersRepository.findById(purchaseOrderId).orElse(null);
        Set<String> whIds = receipts.stream().map(InboundReceipts::getWarehouseId).collect(Collectors.toSet());
        Map<String, Warehouses> whMap = wareHouseRepository.findByIdIn(whIds).stream()
                .collect(Collectors.toMap(Warehouses::getId, wh -> wh));

        return receipts.stream()
                .map(receipt -> inboundReceiptsMapper.toResponse(
                        receipt,
                        po,
                        whMap.get(receipt.getWarehouseId()),
                        Collections.emptyList()
                ))
                .toList();
    }

    private String generateInboundReceiptNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        while (true) {
            String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String receiptNumber = String.format("GR-%s-%s", datePart, suffix);
            if (!inboundReceiptsRepository.existsByReceiptNumber(receiptNumber)) {
                return receiptNumber;
            }
        }
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

    @Override
    @Transactional
    public InboundReceiptsResponse confirm(String id) {
        log.info("Confirm inbound receipt requested, id={}", id);

        return null;
    }

    private List<InboundReceiptLinesResponse> getLineResponses(String inboundReceiptId) {
        List<InboundReceiptLines> lines = inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc(inboundReceiptId);
        return inboundReceiptLinesMapper.toResponses(lines);
    }

    private void normalizeAndValidateFilter(InboundReceiptsFilterRequest filter) {
        if (filter == null) {
            return;
        }

        if (filter.getReceiptDateFrom() != null
                && filter.getReceiptDateTo() != null
                && filter.getReceiptDateFrom().isAfter(filter.getReceiptDateTo())) {
            throw new BadRequestException(
                    "receiptDateFrom must be less than or equal to receiptDateTo",
                    ErrorCode.COM_001
            );
        }

        if (StringUtils.hasText(filter.getStatus())) {
            try {
                filter.setStatus(InboundReceiptsStatus.valueOf(filter.getStatus().trim().toUpperCase()).name());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid inbound receipt status", ErrorCode.COM_001);
            }
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageNumber() < 0) {
            throw new BadRequestException(ErrorCode.COM_006);
        }
        if (pageable.getPageSize() <= 0) {
            throw new BadRequestException(ErrorCode.COM_007);
        }
        if (pageable.getPageSize() > 100) {
            throw new BadRequestException(ErrorCode.COM_008);
        }
    }
}
