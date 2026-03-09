package org.demo.whs.service.impl;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.demo.whs.entity.Products;
import org.demo.whs.entity.PurchaseOrderLines;
import org.demo.whs.entity.PurchaseOrders;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.PurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.request.PurchaseOrderLines.UpdatePurchaseOrderLinesRequest;
import org.demo.whs.entity.dto.response.PurchaseOrderLines.PurchaseOrderLinesResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.PurchaseOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.PurchaseOrderLinesMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.PurchaseOrderLinesRepository;
import org.demo.whs.repository.PurchaseOrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderLinesServiceImplTest {

    @Mock
    private PurchaseOrderLinesRepository purchaseOrderLinesRepository;

    @Mock
    private PurchaseOrdersRepository purchaseOrdersRepository;

    @Mock
    private ProductRepository productRepository;

    private PurchaseOrderLinesServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderLinesServiceImpl(
                purchaseOrderLinesRepository,
                purchaseOrdersRepository,
                productRepository,
                new PurchaseOrderLinesMapper()
        );

        lenient().when(purchaseOrderLinesRepository.save(any(PurchaseOrderLines.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class CreateTests {

        @Test
        void create_shouldSucceed_whenValidAndHasExistingLines() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.ACTIVE);
            PurchaseOrderLines lastLine = line("LINE-9", "PO-1", "PROD-X", 3, "1", "0", "1", "1.00");

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId("PO-1", "PROD-1")).thenReturn(false);
            when(purchaseOrderLinesRepository.findTopByPurchaseOrderIdOrderByLineNumberDesc("PO-1"))
                    .thenReturn(Optional.of(lastLine));

            PurchaseOrderLinesResponse response = service.create(request);

            assertNotNull(response);
            assertEquals("PO-1", response.getPurchaseOrderId());
            assertEquals("PROD-1", response.getProductId());
            assertEquals(4, response.getLineNumber());
            assertBigDecimalEquals("10", response.getQuantityOrdered());
            assertBigDecimalEquals("0", response.getQuantityReceived());
            assertBigDecimalEquals("3.10", response.getUnitPrice());
            assertBigDecimalEquals("31.00", response.getLineTotal());

            ArgumentCaptor<PurchaseOrderLines> captor = ArgumentCaptor.forClass(PurchaseOrderLines.class);
            verify(purchaseOrderLinesRepository).save(captor.capture());
            PurchaseOrderLines saved = captor.getValue();

            assertEquals("PO-1", saved.getPurchaseOrderId());
            assertEquals("PROD-1", saved.getProductId());
            assertEquals(4, saved.getLineNumber());
            assertBigDecimalEquals("0", saved.getQuantityReceived());
            assertBigDecimalEquals("31.00", saved.getLineTotal());
        }

        @Test
        void create_shouldStartLineNumberAtOne_whenNoExistingLine() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "5", "2.00");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.ACTIVE);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId("PO-1", "PROD-1")).thenReturn(false);
            when(purchaseOrderLinesRepository.findTopByPurchaseOrderIdOrderByLineNumberDesc("PO-1"))
                    .thenReturn(Optional.empty());

            PurchaseOrderLinesResponse response = service.create(request);

            assertEquals(1, response.getLineNumber());
            assertBigDecimalEquals("10.00", response.getLineTotal());
        }

        @Test
        void create_shouldThrowNotFound_whenPurchaseOrderNotFound() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(request));

            verifyNoInteractions(productRepository);
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void create_shouldThrowBadRequest_whenPurchaseOrderNotDraft() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.APPROVED);
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.create(request));

            verifyNoInteractions(productRepository);
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void create_shouldThrowNotFound_whenProductNotFound() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.create(request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void create_shouldThrowBadRequest_whenProductInactive() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.INACTIVE);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));

            assertThrows(BadRequestException.class, () -> service.create(request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void create_shouldThrowBadRequest_whenDuplicateProductExistsInSamePo() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.ACTIVE);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId("PO-1", "PROD-1")).thenReturn(true);

            assertThrows(BadRequestException.class, () -> service.create(request));

            verify(purchaseOrderLinesRepository, never()).findTopByPurchaseOrderIdOrderByLineNumberDesc(any());
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void create_shouldThrowBadRequest_whenQuantityOrderedInvalid() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "0", "3.10");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.ACTIVE);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId("PO-1", "PROD-1")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> service.create(request));
        }

        @Test
        void create_shouldThrowBadRequest_whenUnitPriceInvalid() {
            PurchaseOrderLinesRequest request = createRequest("PO-1", "PROD-1", "10", "-1");

            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products product = product("PROD-1", ProductStatus.ACTIVE);

            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductId("PO-1", "PROD-1")).thenReturn(false);

            assertThrows(BadRequestException.class, () -> service.create(request));
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void update_shouldSucceed_whenProductUnchanged() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setQuantityOrdered(new BigDecimal("12"));
            request.setUnitPrice(new BigDecimal("6.00"));
            request.setNotes("updated notes");

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            PurchaseOrderLinesResponse response = service.update("LINE-1", request);

            assertNotNull(response);
            assertEquals("LINE-1", response.getId());
            assertEquals("PROD-1", response.getProductId());
            assertBigDecimalEquals("12", response.getQuantityOrdered());
            assertBigDecimalEquals("6.00", response.getUnitPrice());
            assertBigDecimalEquals("72.00", response.getLineTotal());
            assertEquals("updated notes", response.getNotes());

            verifyNoInteractions(productRepository);
            verify(purchaseOrderLinesRepository, never())
                    .existsByPurchaseOrderIdAndProductIdAndIdNot(any(), any(), any());
            verify(purchaseOrderLinesRepository).save(existing);
        }

        @Test
        void update_shouldSucceed_whenProductChangesAndNotDuplicate() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setProductId("PROD-2");
            request.setQuantityOrdered(new BigDecimal("8"));
            request.setUnitPrice(new BigDecimal("4.50"));
            request.setNotes("changed product");

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products newProduct = product("PROD-2", ProductStatus.ACTIVE);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-2")).thenReturn(Optional.of(newProduct));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductIdAndIdNot("PO-1", "PROD-2", "LINE-1"))
                    .thenReturn(false);

            PurchaseOrderLinesResponse response = service.update("LINE-1", request);

            assertEquals("PROD-2", response.getProductId());
            assertBigDecimalEquals("8", response.getQuantityOrdered());
            assertBigDecimalEquals("4.50", response.getUnitPrice());
            assertBigDecimalEquals("36.00", response.getLineTotal());
            assertEquals("changed product", response.getNotes());

            verify(productRepository).findById("PROD-2");
            verify(purchaseOrderLinesRepository)
                    .existsByPurchaseOrderIdAndProductIdAndIdNot("PO-1", "PROD-2", "LINE-1");
            verify(purchaseOrderLinesRepository).save(existing);
        }

        @Test
        void update_shouldThrowNotFound_whenLineNotFound() {
            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.update("LINE-1", new UpdatePurchaseOrderLinesRequest()));

            verifyNoInteractions(purchaseOrdersRepository, productRepository);
        }

        @Test
        void update_shouldThrowNotFound_whenParentPurchaseOrderNotFound() {
            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.update("LINE-1", new UpdatePurchaseOrderLinesRequest()));

            verifyNoInteractions(productRepository);
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenParentPurchaseOrderNotDraft() {
            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.APPROVED);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", new UpdatePurchaseOrderLinesRequest()));

            verifyNoInteractions(productRepository);
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowNotFound_whenNewProductNotFound() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setProductId("PROD-2");

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-2")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never())
                    .existsByPurchaseOrderIdAndProductIdAndIdNot(any(), any(), any());
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenNewProductInactive() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setProductId("PROD-2");

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products newProduct = product("PROD-2", ProductStatus.INACTIVE);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-2")).thenReturn(Optional.of(newProduct));

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never())
                    .existsByPurchaseOrderIdAndProductIdAndIdNot(any(), any(), any());
            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenNewProductDuplicatesExistingLine() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setProductId("PROD-2");

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);
            Products newProduct = product("PROD-2", ProductStatus.ACTIVE);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));
            when(productRepository.findById("PROD-2")).thenReturn(Optional.of(newProduct));
            when(purchaseOrderLinesRepository.existsByPurchaseOrderIdAndProductIdAndIdNot("PO-1", "PROD-2", "LINE-1"))
                    .thenReturn(true);

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenQuantityOrderedInvalid() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setQuantityOrdered(BigDecimal.ZERO);

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenUnitPriceInvalid() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setUnitPrice(new BigDecimal("-1"));

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }

        @Test
        void update_shouldThrowBadRequest_whenQuantityReceivedExceedsQuantityOrdered() {
            UpdatePurchaseOrderLinesRequest request = new UpdatePurchaseOrderLinesRequest();
            request.setQuantityOrdered(new BigDecimal("5"));

            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "8", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.update("LINE-1", request));

            verify(purchaseOrderLinesRepository, never()).save(any());
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void delete_shouldSucceed_whenLineExistsAndParentPoDraft() {
            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.DRAFT);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertDoesNotThrow(() -> service.delete("LINE-1"));

            verify(purchaseOrderLinesRepository).delete(existing);
        }

        @Test
        void delete_shouldThrowNotFound_whenLineNotFound() {
            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.delete("LINE-1"));

            verifyNoInteractions(purchaseOrdersRepository);
            verify(purchaseOrderLinesRepository, never()).delete(any());
        }

        @Test
        void delete_shouldThrowNotFound_whenParentPoNotFound() {
            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.delete("LINE-1"));

            verify(purchaseOrderLinesRepository, never()).delete(any());
        }

        @Test
        void delete_shouldThrowBadRequest_whenParentPoNotDraft() {
            PurchaseOrderLines existing = line("LINE-1", "PO-1", "PROD-1", 1, "10", "2", "5.00", "50.00");
            PurchaseOrders po = purchaseOrder("PO-1", PurchaseOrdersStatus.APPROVED);

            when(purchaseOrderLinesRepository.findByIdForUpdate("LINE-1")).thenReturn(Optional.of(existing));
            when(purchaseOrdersRepository.findByIdForUpdate("PO-1")).thenReturn(Optional.of(po));

            assertThrows(BadRequestException.class, () -> service.delete("LINE-1"));

            verify(purchaseOrderLinesRepository, never()).delete(any());
        }
    }

    // =========================
    // Helpers
    // =========================

    private PurchaseOrderLinesRequest createRequest(String poId, String productId, String quantityOrdered, String unitPrice) {
        PurchaseOrderLinesRequest request = new PurchaseOrderLinesRequest();
        request.setPurchaseOrderId(poId);
        request.setProductId(productId);
        request.setQuantityOrdered(new BigDecimal(quantityOrdered));
        request.setUnitPrice(new BigDecimal(unitPrice));
        request.setNotes("note");
        return request;
    }

    private PurchaseOrders purchaseOrder(String id, PurchaseOrdersStatus status) {
        PurchaseOrders po = new PurchaseOrders();
        po.setId(id);
        po.setStatus(status);
        return po;
    }

    private Products product(String id, ProductStatus status) {
        Products product = new Products();
        product.setId(id);
        product.setStatus(status);
        return product;
    }

    private PurchaseOrderLines line(
            String id,
            String purchaseOrderId,
            String productId,
            Integer lineNumber,
            String quantityOrdered,
            String quantityReceived,
            String unitPrice,
            String lineTotal
    ) {
        PurchaseOrderLines line = new PurchaseOrderLines();
        line.setId(id);
        line.setPurchaseOrderId(purchaseOrderId);
        line.setProductId(productId);
        line.setLineNumber(lineNumber);
        line.setQuantityOrdered(new BigDecimal(quantityOrdered));
        line.setQuantityReceived(new BigDecimal(quantityReceived));
        line.setUnitPrice(new BigDecimal(unitPrice));
        line.setLineTotal(new BigDecimal(lineTotal));
        line.setNotes("original note");
        return line;
    }

    private void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, actual.compareTo(new BigDecimal(expected)));
    }
}