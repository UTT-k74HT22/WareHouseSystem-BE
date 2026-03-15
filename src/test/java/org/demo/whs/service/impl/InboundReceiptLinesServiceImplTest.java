package org.demo.whs.service.impl;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.InboundReceiptLines;
import org.demo.whs.entity.InboundReceipts;
import org.demo.whs.entity.Locations;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLineUpdateRequest;
import org.demo.whs.entity.dto.request.InboundReceiptLines.InboundReceiptLinesRequest;
import org.demo.whs.entity.dto.response.InboundReceiptLines.InboundReceiptLinesResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.demo.whs.entity.enums.InboundReceiptsStatus;
import org.demo.whs.entity.enums.LocationStatus;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.QualityStatus;
import org.demo.whs.exception.BaseException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.InboundReceiptLinesMapper;
import org.demo.whs.repository.BatchRepository;
import org.demo.whs.repository.InboundReceiptLinesRepository;
import org.demo.whs.repository.InboundReceiptsRepository;
import org.demo.whs.repository.LocationRepository;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundReceiptLinesServiceImplTest {

    @Mock
    private InboundReceiptLinesRepository inboundReceiptLinesRepository;
    @Mock
    private InboundReceiptsRepository inboundReceiptsRepository;
    @Mock
    private PurchaseOrderLinesRepository purchaseOrderLinesRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private InboundReceiptLinesMapper inboundReceiptLinesMapper;

    @InjectMocks
    private InboundReceiptLinesServiceImpl inboundReceiptLinesService;

    @Test
    @DisplayName("should_CreateLine_When_NonBatchProductAndQualityDefaultsToPass")
    void should_CreateLine_When_NonBatchProductAndQualityDefaultsToPass() {
        InboundReceiptLinesRequest request = createRequest(
                "receipt-current",
                "pol-1",
                "loc-1",
                null,
                new BigDecimal("20.00"),
                null,
                "  "
        );
        Products product = product(ProductStatus.ACTIVE, false);
        InboundReceiptLines mappedLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                1,
                new BigDecimal("20.00"),
                QualityStatus.PASS,
                null
        );
        InboundReceiptLinesResponse expectedResponse = InboundReceiptLinesResponse.builder()
                .id("line-1")
                .qualityStatus(QualityStatus.PASS.name())
                .lineNumber(1)
                .build();

        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("10.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", null, QualityStatus.PASS, null
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", null))
                .thenReturn(new BigDecimal("20.00"));
        when(inboundReceiptLinesRepository.findTopByInboundReceiptIdOrderByLineNumberDesc("receipt-current"))
                .thenReturn(Optional.empty());
        when(inboundReceiptLinesMapper.getInboundReceiptLines(request, product, 1)).thenReturn(mappedLine);
        when(inboundReceiptLinesRepository.save(mappedLine)).thenReturn(mappedLine);
        when(inboundReceiptLinesMapper.toResponse(mappedLine, "SKU-1", "Product 1", null, "LOC-1", "Location loc-1"))
                .thenReturn(expectedResponse);

        InboundReceiptLinesResponse response = inboundReceiptLinesService.create(request);

        assertNotNull(response);
        assertEquals(QualityStatus.PASS.name(), response.getQualityStatus());
        assertEquals(1, response.getLineNumber());
        assertNull(request.getBatchId());
        assertNull(request.getNotes());
    }

    @Test
    @DisplayName("should_CreateLine_When_SplittingSamePurchaseOrderLineAcrossDifferentLocations")
    void should_CreateLine_When_SplittingSamePurchaseOrderLineAcrossDifferentLocations() {
        InboundReceiptLinesRequest request = createRequest(
                "receipt-current",
                "pol-1",
                "loc-2",
                null,
                new BigDecimal("10.00"),
                QualityStatus.PASS,
                "split by location"
        );
        Products product = product(ProductStatus.ACTIVE, false);
        InboundReceiptLines lastLine = line(
                "line-9",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                2,
                new BigDecimal("15.00"),
                QualityStatus.PASS,
                "first split"
        );
        InboundReceiptLines newLine = line(
                "line-10",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-2",
                null,
                3,
                new BigDecimal("10.00"),
                QualityStatus.PASS,
                "split by location"
        );

        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("40.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(location("loc-2", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-2", null, QualityStatus.PASS, null
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", null))
                .thenReturn(new BigDecimal("20.00"));
        when(inboundReceiptLinesRepository.findTopByInboundReceiptIdOrderByLineNumberDesc("receipt-current"))
                .thenReturn(Optional.of(lastLine));
        when(inboundReceiptLinesMapper.getInboundReceiptLines(request, product, 3)).thenReturn(newLine);
        when(inboundReceiptLinesRepository.save(newLine)).thenReturn(newLine);
        when(inboundReceiptLinesMapper.toResponse(newLine, "SKU-1", "Product 1", null, "LOC-2", "Location loc-2"))
                .thenReturn(InboundReceiptLinesResponse.builder().id("line-10").lineNumber(3).build());

        InboundReceiptLinesResponse response = inboundReceiptLinesService.create(request);

        assertNotNull(response);
        assertEquals(3, response.getLineNumber());
    }

    @Test
    @DisplayName("should_CreateLine_When_BatchTrackedQuarantineRequestIsValid")
    void should_CreateLine_When_BatchTrackedQuarantineRequestIsValid() {
        InboundReceiptLinesRequest request = createRequest(
                "receipt-current",
                "pol-1",
                "loc-1",
                "batch-1",
                new BigDecimal("5.00"),
                QualityStatus.QUARANTINE,
                "damaged carton"
        );
        Products product = product(ProductStatus.ACTIVE, true);
        InboundReceiptLines newLine = line(
                "line-q1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                "batch-1",
                1,
                new BigDecimal("5.00"),
                QualityStatus.QUARANTINE,
                "damaged carton"
        );

        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-1", BatchStatus.AVAILABLE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", "batch-1", QualityStatus.QUARANTINE, null
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", null))
                .thenReturn(BigDecimal.ZERO);
        when(inboundReceiptLinesRepository.findTopByInboundReceiptIdOrderByLineNumberDesc("receipt-current"))
                .thenReturn(Optional.empty());
        when(inboundReceiptLinesMapper.getInboundReceiptLines(request, product, 1)).thenReturn(newLine);
        when(inboundReceiptLinesRepository.save(newLine)).thenReturn(newLine);
        when(inboundReceiptLinesMapper.toResponse(newLine, "SKU-1", "Product 1", "BATCH-001", "LOC-1", "Location loc-1"))
                .thenReturn(InboundReceiptLinesResponse.builder().id("line-q1").qualityStatus(QualityStatus.QUARANTINE.name()).build());

        InboundReceiptLinesResponse response = inboundReceiptLinesService.create(request);

        assertNotNull(response);
        assertEquals(QualityStatus.QUARANTINE.name(), response.getQualityStatus());
    }

    @Test
    @DisplayName("should_RejectCreate_When_ReceiptIsNotDraft")
    void should_RejectCreate_When_ReceiptIsNotDraft() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current"))
                .thenReturn(Optional.of(receipt("receipt-current", "po-1", "wh-1", InboundReceiptsStatus.CONFIRMED)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_003.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_PurchaseOrderLineBelongsToDifferentPurchaseOrder")
    void should_RejectCreate_When_PurchaseOrderLineBelongsToDifferentPurchaseOrder() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-other", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_004.getCode(), exception.getErrorCode());
        verifyNoInteractions(productRepository, locationRepository, batchRepository);
    }

    @Test
    @DisplayName("should_RejectCreate_When_QuantityReceivedIsNotPositive")
    void should_RejectCreate_When_QuantityReceivedIsNotPositive() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, BigDecimal.ZERO, QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_012.getCode(), exception.getErrorCode());
        verifyNoInteractions(locationRepository, batchRepository);
    }

    @Test
    @DisplayName("should_RejectCreate_When_RequestedQuantityExceedsRemainingWithinSameReceipt")
    void should_RejectCreate_When_RequestedQuantityExceedsRemainingWithinSameReceipt() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("40.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", null, QualityStatus.PASS, null
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", null))
                .thenReturn(new BigDecimal("45.00"));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("20.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_012.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).save(any(InboundReceiptLines.class));
    }

    @Test
    @DisplayName("should_RejectCreate_When_NonBatchProductContainsBatchId")
    void should_RejectCreate_When_NonBatchProductContainsBatchId() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", "batch-1", new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_009.getCode(), exception.getErrorCode());
        verifyNoInteractions(batchRepository);
    }

    @Test
    @DisplayName("should_RejectCreate_When_BatchTrackedProductMissingBatchId")
    void should_RejectCreate_When_BatchTrackedProductMissingBatchId() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, true)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_008.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_QuarantineStatusMissingNotes")
    void should_RejectCreate_When_QuarantineStatusMissingNotes() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.QUARANTINE, " ")
        ));

        assertEquals(ErrorCode.IRL_014.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).existsByDuplicateDimension(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("should_RejectCreate_When_DuplicateDimensionExistsWithNullBatch")
    void should_RejectCreate_When_DuplicateDimensionExistsWithNullBatch() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", null, QualityStatus.PASS, null
        )).thenReturn(true);

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_013.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).save(any(InboundReceiptLines.class));
    }

    @Test
    @DisplayName("should_RejectCreate_When_LocationDoesNotBelongToReceiptWarehouse")
    void should_RejectCreate_When_LocationDoesNotBelongToReceiptWarehouse() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-2", LocationStatus.ACTIVE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_006.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_LocationIsMaintenance")
    void should_RejectCreate_When_LocationIsMaintenance() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.MAINTENANCE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_007.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_ProductIsInactive")
    void should_RejectCreate_When_ProductIsInactive() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.INACTIVE, false)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", null, new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_005.getCode(), exception.getErrorCode());
        verifyNoInteractions(locationRepository, batchRepository);
    }

    @Test
    @DisplayName("should_RejectCreate_When_BatchDoesNotBelongToProduct")
    void should_RejectCreate_When_BatchDoesNotBelongToProduct() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, true)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-2", BatchStatus.AVAILABLE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", "batch-1", new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_009.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_PassLineUsesNonAvailableBatch")
    void should_RejectCreate_When_PassLineUsesNonAvailableBatch() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, true)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-1", BatchStatus.QUARANTINE)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", "batch-1", new BigDecimal("5.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_010.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_RejectCreate_When_QuarantineLineUsesRecalledBatch")
    void should_RejectCreate_When_QuarantineLineUsesRecalledBatch() {
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("50.00"), BigDecimal.ZERO)));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, true)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-1", BatchStatus.RECALLED)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.create(
                createRequest("receipt-current", "pol-1", "loc-1", "batch-1", new BigDecimal("5.00"), QualityStatus.QUARANTINE, "quality hold")
        ));

        assertEquals(ErrorCode.IRL_011.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_UpdateLine_When_QuantityIsWithinRemainingAndOptionalFieldsAreOmitted")
    void should_UpdateLine_When_QuantityIsWithinRemainingAndOptionalFieldsAreOmitted() {
        InboundReceiptLineUpdateRequest request = updateRequest("loc-1", null, new BigDecimal("12.00"), null, null);
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                "batch-1",
                1,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                "keep me"
        );
        Products product = product(ProductStatus.ACTIVE, true);

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("40.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-1", BatchStatus.AVAILABLE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", "batch-1", QualityStatus.PASS, "line-1"
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", "line-1"))
                .thenReturn(new BigDecimal("10.00"));
        when(inboundReceiptLinesRepository.save(currentLine)).thenReturn(currentLine);
        when(inboundReceiptLinesMapper.toResponse(currentLine, "SKU-1", "Product 1", "BATCH-001", "LOC-1", "Location loc-1"))
                .thenReturn(InboundReceiptLinesResponse.builder().id("line-1").quantityReceived(new BigDecimal("12.00")).build());

        InboundReceiptLinesResponse response = inboundReceiptLinesService.update("line-1", request);

        assertNotNull(response);
        assertEquals(new BigDecimal("12.00"), currentLine.getQuantityReceived());
        assertEquals("batch-1", currentLine.getBatchId());
        assertEquals(QualityStatus.PASS, currentLine.getQualityStatus());
        assertEquals("keep me", currentLine.getNotes());
    }

    @Test
    @DisplayName("should_RejectUpdate_When_ResolvedDimensionDuplicatesAnotherLine")
    void should_RejectUpdate_When_ResolvedDimensionDuplicatesAnotherLine() {
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                1,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                null
        );
        InboundReceiptLineUpdateRequest request = updateRequest("loc-2", null, new BigDecimal("8.00"), QualityStatus.PASS, null);

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("40.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(location("loc-2", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-2", null, QualityStatus.PASS, "line-1"
        )).thenReturn(true);

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.update("line-1", request));

        assertEquals(ErrorCode.IRL_013.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).save(any(InboundReceiptLines.class));
    }

    @Test
    @DisplayName("should_RejectUpdate_When_QuantityExceedsRemainingAfterExcludingCurrentLine")
    void should_RejectUpdate_When_QuantityExceedsRemainingAfterExcludingCurrentLine() {
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                1,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                null
        );
        InboundReceiptLineUpdateRequest request = updateRequest("loc-1", null, new BigDecimal("30.00"), QualityStatus.PASS, null);

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));
        when(purchaseOrderLinesRepository.findByIdForUpdate("pol-1"))
                .thenReturn(Optional.of(purchaseOrderLine("po-1", "prod-1", new BigDecimal("100.00"), new BigDecimal("40.00"))));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product(ProductStatus.ACTIVE, false)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesRepository.existsByDuplicateDimension(
                "receipt-current", "pol-1", "loc-1", null, QualityStatus.PASS, "line-1"
        )).thenReturn(false);
        when(inboundReceiptLinesRepository.sumQuantityByReceiptAndPurchaseOrderLine("receipt-current", "pol-1", "line-1"))
                .thenReturn(new BigDecimal("35.00"));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.update("line-1", request));

        assertEquals(ErrorCode.IRL_012.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).save(any(InboundReceiptLines.class));
    }

    @Test
    @DisplayName("should_RejectUpdate_When_ReceiptIsNotDraft")
    void should_RejectUpdate_When_ReceiptIsNotDraft() {
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                1,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                null
        );

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current"))
                .thenReturn(Optional.of(receipt("receipt-current", "po-1", "wh-1", InboundReceiptsStatus.CONFIRMED)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.update(
                "line-1",
                updateRequest("loc-1", null, new BigDecimal("8.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_003.getCode(), exception.getErrorCode());
    }

    @Test
    @DisplayName("should_DeleteLine_When_ReceiptIsDraft")
    void should_DeleteLine_When_ReceiptIsDraft() {
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                4,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                null
        );

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current")).thenReturn(Optional.of(draftReceipt()));

        inboundReceiptLinesService.delete("line-1");

        verify(inboundReceiptLinesRepository).delete(currentLine);
        assertEquals(4, currentLine.getLineNumber());
    }

    @Test
    @DisplayName("should_RejectDelete_When_ReceiptIsNotDraft")
    void should_RejectDelete_When_ReceiptIsNotDraft() {
        InboundReceiptLines currentLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                null,
                4,
                new BigDecimal("8.00"),
                QualityStatus.PASS,
                null
        );

        when(inboundReceiptLinesRepository.findByIdForUpdate("line-1")).thenReturn(Optional.of(currentLine));
        when(inboundReceiptsRepository.findByIdForUpdate("receipt-current"))
                .thenReturn(Optional.of(receipt("receipt-current", "po-1", "wh-1", InboundReceiptsStatus.CONFIRMED)));

        BaseException exception = assertThrows(BaseException.class, () -> inboundReceiptLinesService.delete("line-1"));

        assertEquals(ErrorCode.IRL_003.getCode(), exception.getErrorCode());
        verify(inboundReceiptLinesRepository, never()).delete(any(InboundReceiptLines.class));
    }

    @Test
    @DisplayName("should_FindLinesByInboundReceiptId_When_LinesExist")
    void should_FindLinesByInboundReceiptId_When_LinesExist() {
        InboundReceiptLines firstLine = line(
                "line-1",
                "receipt-current",
                "pol-1",
                "prod-1",
                "loc-1",
                "batch-1",
                1,
                new BigDecimal("5.00"),
                QualityStatus.PASS,
                "ok"
        );
        InboundReceiptLines secondLine = line(
                "line-2",
                "receipt-current",
                "pol-2",
                "prod-2",
                "loc-2",
                null,
                2,
                new BigDecimal("3.00"),
                QualityStatus.QUARANTINE,
                "hold"
        );

        when(inboundReceiptLinesRepository.findByInboundReceiptIdOrderByLineNumberAsc("receipt-current"))
                .thenReturn(List.of(firstLine, secondLine));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product("prod-1", "SKU-1", "Product 1", ProductStatus.ACTIVE, true)));
        when(productRepository.findById("prod-2")).thenReturn(Optional.of(product("prod-2", "SKU-2", "Product 2", ProductStatus.ACTIVE, false)));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(batch("batch-1", "prod-1", BatchStatus.AVAILABLE)));
        when(locationRepository.findById("loc-1")).thenReturn(Optional.of(location("loc-1", "wh-1", LocationStatus.ACTIVE)));
        when(locationRepository.findById("loc-2")).thenReturn(Optional.of(location("loc-2", "wh-1", LocationStatus.ACTIVE)));
        when(inboundReceiptLinesMapper.toResponse(firstLine, "SKU-1", "Product 1", "BATCH-001", "LOC-1", "Location loc-1"))
                .thenReturn(InboundReceiptLinesResponse.builder().id("line-1").lineNumber(1).build());
        when(inboundReceiptLinesMapper.toResponse(secondLine, "SKU-2", "Product 2", null, "LOC-2", "Location loc-2"))
                .thenReturn(InboundReceiptLinesResponse.builder().id("line-2").lineNumber(2).build());

        List<InboundReceiptLinesResponse> responses = inboundReceiptLinesService.findByInboundReceiptId("receipt-current");

        assertEquals(2, responses.size());
        assertEquals("line-1", responses.get(0).getId());
        assertEquals("line-2", responses.get(1).getId());
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_LineDoesNotExistOnUpdate")
    void should_ThrowNotFound_When_LineDoesNotExistOnUpdate() {
        when(inboundReceiptLinesRepository.findByIdForUpdate("missing")).thenReturn(Optional.empty());

        BaseException exception = assertThrows(NotFoundException.class, () -> inboundReceiptLinesService.update(
                "missing",
                updateRequest("loc-1", null, new BigDecimal("1.00"), QualityStatus.PASS, null)
        ));

        assertEquals(ErrorCode.IRL_001.getCode(), exception.getErrorCode());
    }

    private InboundReceipts draftReceipt() {
        return receipt("receipt-current", "po-1", "wh-1", InboundReceiptsStatus.DRAFT);
    }

    private InboundReceipts receipt(String id, String purchaseOrderId, String warehouseId, InboundReceiptsStatus status) {
        InboundReceipts receipt = InboundReceipts.builder()
                .purchaseOrderId(purchaseOrderId)
                .warehouseId(warehouseId)
                .status(status)
                .build();
        receipt.setId(id);
        return receipt;
    }

    private PurchaseOrderLines purchaseOrderLine(
            String purchaseOrderId,
            String productId,
            BigDecimal quantityOrdered,
            BigDecimal quantityReceived
    ) {
        PurchaseOrderLines poLine = PurchaseOrderLines.builder()
                .purchaseOrderId(purchaseOrderId)
                .productId(productId)
                .quantityOrdered(quantityOrdered)
                .quantityReceived(quantityReceived)
                .build();
        poLine.setId("pol-1");
        return poLine;
    }

    private Products product(ProductStatus status, boolean requiresBatchTracking) {
        return product("prod-1", "SKU-1", "Product 1", status, requiresBatchTracking);
    }

    private Products product(String id, String sku, String name, ProductStatus status, boolean requiresBatchTracking) {
        Products product = Products.builder()
                .sku(sku)
                .name(name)
                .status(status)
                .requiresBatchTracking(requiresBatchTracking)
                .build();
        product.setId(id);
        return product;
    }

    private Locations location(String id, String warehouseId, LocationStatus status) {
        Locations location = Locations.builder()
                .warehouseId(warehouseId)
                .code(id.toUpperCase())
                .name("Location " + id)
                .status(status)
                .build();
        location.setId(id);
        return location;
    }

    private Batch batch(String batchId, String productId, BatchStatus status) {
        Batch batch = Batch.builder()
                .batchNumber("BATCH-001")
                .productId(productId)
                .manufacturingDate(LocalDate.now().minusDays(10))
                .status(status)
                .build();
        batch.setId(batchId);
        return batch;
    }

    private InboundReceiptLines line(
            String id,
            String inboundReceiptId,
            String purchaseOrderLineId,
            String productId,
            String locationId,
            String batchId,
            int lineNumber,
            BigDecimal quantityReceived,
            QualityStatus qualityStatus,
            String notes
    ) {
        InboundReceiptLines line = InboundReceiptLines.builder()
                .inboundReceiptId(inboundReceiptId)
                .purchaseOrderLineId(purchaseOrderLineId)
                .productId(productId)
                .locationId(locationId)
                .batchId(batchId)
                .lineNumber(lineNumber)
                .quantityReceived(quantityReceived)
                .qualityStatus(qualityStatus)
                .notes(notes)
                .build();
        line.setId(id);
        return line;
    }

    private InboundReceiptLinesRequest createRequest(
            String inboundReceiptId,
            String purchaseOrderLineId,
            String locationId,
            String batchId,
            BigDecimal quantityReceived,
            QualityStatus qualityStatus,
            String notes
    ) {
        return InboundReceiptLinesRequest.builder()
                .inboundReceiptId(inboundReceiptId)
                .purchaseOrderLineId(purchaseOrderLineId)
                .locationId(locationId)
                .batchId(batchId)
                .quantityReceived(quantityReceived)
                .qualityStatus(qualityStatus)
                .notes(notes)
                .build();
    }

    private InboundReceiptLineUpdateRequest updateRequest(
            String locationId,
            String batchId,
            BigDecimal quantityReceived,
            QualityStatus qualityStatus,
            String notes
    ) {
        return InboundReceiptLineUpdateRequest.builder()
                .locationId(locationId)
                .batchId(batchId)
                .quantityReceived(quantityReceived)
                .qualityStatus(qualityStatus)
                .notes(notes)
                .build();
    }
}
