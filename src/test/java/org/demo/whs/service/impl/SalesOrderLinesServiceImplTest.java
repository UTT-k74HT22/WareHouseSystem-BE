package org.demo.whs.service.impl;

import org.demo.whs.entity.Products;
import org.demo.whs.entity.SalesOrderLines;
import org.demo.whs.entity.SalesOrders;
import org.demo.whs.entity.dto.request.SalesOrderLines.CreateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.request.SalesOrderLines.UpdateSalesOrderLinesRequest;
import org.demo.whs.entity.dto.response.SalesOrderLines.SalesOrderLinesResponse;
import org.demo.whs.entity.enums.ProductStatus;
import org.demo.whs.entity.enums.SalesOrdersStatus;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.SalesOrderLinesMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.SalesOrderLinesRepository;
import org.demo.whs.repository.SalesOrdersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesOrderLinesServiceImpl Unit Tests")
class SalesOrderLinesServiceImplTest {

    @Mock
    private SalesOrderLinesRepository salesOrderLinesRepository;
    @Mock
    private SalesOrdersRepository salesOrdersRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private SalesOrderLinesMapper salesOrderLinesMapper;

    @InjectMocks
    private SalesOrderLinesServiceImpl service;

    @Test
    @DisplayName("should_CreateLine_When_SalesOrderIsDraft")
    void should_CreateLine_When_SalesOrderIsDraft() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("100.00"))
                .notes("Line note")
                .build();

        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);
        Products product = buildProduct("prod-1", ProductStatus.ACTIVE);
        SalesOrderLines savedLine = buildLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));
        when(salesOrderLinesRepository.findMaxLineNumber("so-1")).thenReturn(Optional.of(0));
        when(salesOrderLinesRepository.save(any(SalesOrderLines.class))).thenReturn(savedLine);
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of(savedLine));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenReturn(so);
        when(salesOrderLinesMapper.toResponse(savedLine)).thenReturn(
                SalesOrderLinesResponse.builder()
                        .id("line-1")
                        .salesOrderId("so-1")
                        .productId("prod-1")
                        .lineNumber(1)
                        .quantityOrdered(new BigDecimal("5.00"))
                        .unitPrice(new BigDecimal("100.00"))
                        .lineTotal(new BigDecimal("500.00"))
                        .build()
        );

        SalesOrderLinesResponse response = service.create(request);

        assertThat(response.getId()).isEqualTo("line-1");
        assertThat(response.getLineNumber()).isEqualTo(1);
        assertThat(response.getLineTotal()).isEqualByComparingTo("500.00");
        verify(salesOrderLinesRepository).save(any(SalesOrderLines.class));
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_SalesOrderNotFound")
    void should_ThrowNotFound_When_SalesOrderNotFound() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-missing")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        when(salesOrdersRepository.findByIdForUpdate("so-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sales order not found");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_SalesOrderIsNotDraft")
    void should_ThrowBadRequest_When_SalesOrderIsNotDraft() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        SalesOrders confirmed = buildSalesOrder("so-1", SalesOrdersStatus.CONFIRMED);
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft sales orders can add lines");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_ProductNotActive")
    void should_ThrowBadRequest_When_ProductNotActive() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("100.00"))
                .build();

        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);
        Products inactiveProduct = buildProduct("prod-1", ProductStatus.INACTIVE);

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_QuantityIsZero")
    void should_ThrowBadRequest_When_QuantityIsZero() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-1")
                .productId("prod-1")
                .quantityOrdered(BigDecimal.ZERO)
                .unitPrice(new BigDecimal("100.00"))
                .build();

        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);
        Products product = buildProduct("prod-1", ProductStatus.ACTIVE);

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("greater than zero");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UnitPriceIsNegative")
    void should_ThrowBadRequest_When_UnitPriceIsNegative() {
        CreateSalesOrderLinesRequest request = CreateSalesOrderLinesRequest.builder()
                .salesOrderId("so-1")
                .productId("prod-1")
                .quantityOrdered(new BigDecimal("5.00"))
                .unitPrice(new BigDecimal("-10.00"))
                .build();

        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);
        Products product = buildProduct("prod-1", ProductStatus.ACTIVE);

        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(productRepository.findById("prod-1")).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be negative");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_UpdateLine_When_RequestIsValid")
    void should_UpdateLine_When_RequestIsValid() {
        UpdateSalesOrderLinesRequest request = UpdateSalesOrderLinesRequest.builder()
                .quantityOrdered(new BigDecimal("10.00"))
                .unitPrice(new BigDecimal("120.00"))
                .notes("Updated note")
                .build();

        SalesOrderLines line = buildLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");
        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);

        when(salesOrderLinesRepository.findById("line-1")).thenReturn(Optional.of(line));
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));
        when(salesOrderLinesRepository.save(any(SalesOrderLines.class))).thenReturn(line);
        when(salesOrderLinesRepository.findBySalesOrderId("so-1")).thenReturn(List.of(line));
        when(salesOrdersRepository.save(any(SalesOrders.class))).thenReturn(so);
        when(salesOrderLinesMapper.toResponse(line)).thenReturn(
                SalesOrderLinesResponse.builder()
                        .id("line-1")
                        .quantityOrdered(new BigDecimal("10.00"))
                        .unitPrice(new BigDecimal("120.00"))
                        .lineTotal(new BigDecimal("1200.00"))
                        .notes("Updated note")
                        .build()
        );

        SalesOrderLinesResponse response = service.update("line-1", request);

        ArgumentCaptor<SalesOrderLines> captor = ArgumentCaptor.forClass(SalesOrderLines.class);
        verify(salesOrderLinesRepository).save(captor.capture());

        SalesOrderLines saved = captor.getValue();
        assertThat(saved.getQuantityOrdered()).isEqualByComparingTo("10.00");
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("120.00");
        assertThat(saved.getLineTotal()).isEqualByComparingTo("1200.00");
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateLineWithShippedQuantity")
    void should_ThrowBadRequest_When_UpdateLineWithShippedQuantity() {
        SalesOrderLines lineWithShipped = buildLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");
        lineWithShipped.setQuantityShipped(new BigDecimal("2.00"));
        SalesOrders so = buildSalesOrder("so-1", SalesOrdersStatus.DRAFT);

        UpdateSalesOrderLinesRequest request = UpdateSalesOrderLinesRequest.builder()
                .quantityOrdered(new BigDecimal("10.00"))
                .build();

        when(salesOrderLinesRepository.findById("line-1")).thenReturn(Optional.of(lineWithShipped));
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(so));

        assertThatThrownBy(() -> service.update("line-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot update line with shipped quantity");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowBadRequest_When_UpdateNonDraftSalesOrderLine")
    void should_ThrowBadRequest_When_UpdateNonDraftSalesOrderLine() {
        SalesOrderLines line = buildLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");
        SalesOrders confirmed = buildSalesOrder("so-1", SalesOrdersStatus.CONFIRMED);

        UpdateSalesOrderLinesRequest request = UpdateSalesOrderLinesRequest.builder()
                .quantityOrdered(new BigDecimal("10.00"))
                .build();

        when(salesOrderLinesRepository.findById("line-1")).thenReturn(Optional.of(line));
        when(salesOrdersRepository.findByIdForUpdate("so-1")).thenReturn(Optional.of(confirmed));

        assertThatThrownBy(() -> service.update("line-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only draft sales orders can update lines");

        verify(salesOrderLinesRepository, never()).save(any());
    }

    @Test
    @DisplayName("should_ThrowNotFound_When_UpdateLineNotFound")
    void should_ThrowNotFound_When_UpdateLineNotFound() {
        UpdateSalesOrderLinesRequest request = UpdateSalesOrderLinesRequest.builder()
                .quantityOrdered(new BigDecimal("10.00"))
                .build();

        when(salesOrderLinesRepository.findById("line-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("line-missing", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Sales order line not found");
    }

    @Test
    @DisplayName("should_ReturnLines_When_GetBySalesOrder")
    void should_ReturnLines_When_GetBySalesOrder() {
        SalesOrderLines line1 = buildLine("line-1", "so-1", 1, "5.00", "100.00", "500.00");
        SalesOrderLines line2 = buildLine("line-2", "so-1", 2, "3.00", "50.00", "150.00");

        when(salesOrderLinesRepository.findBySalesOrderIdOrderByLineNumberAsc("so-1"))
                .thenReturn(List.of(line1, line2));
        when(salesOrderLinesMapper.toResponse(line1)).thenReturn(
                SalesOrderLinesResponse.builder().id("line-1").lineNumber(1)
                        .quantityOrdered(new BigDecimal("5.00")).unitPrice(new BigDecimal("100.00"))
                        .lineTotal(new BigDecimal("500.00")).build()
        );
        when(salesOrderLinesMapper.toResponse(line2)).thenReturn(
                SalesOrderLinesResponse.builder().id("line-2").lineNumber(2)
                        .quantityOrdered(new BigDecimal("3.00")).unitPrice(new BigDecimal("50.00"))
                        .lineTotal(new BigDecimal("150.00")).build()
        );

        List<SalesOrderLinesResponse> responses = service.getBySalesOrder("so-1");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("line-1");
        assertThat(responses.get(1).getId()).isEqualTo("line-2");
    }

    private SalesOrders buildSalesOrder(String id, SalesOrdersStatus status) {
        SalesOrders entity = SalesOrders.builder()
                .soNumber("SO-001")
                .customerId("bp-1")
                .warehouseId("wh-1")
                .status(status)
                .subTotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();
        entity.setId(id);
        return entity;
    }

    private SalesOrderLines buildLine(String id, String soId, Integer lineNumber,
                                      String qtyOrdered, String unitPrice, String lineTotal) {
        SalesOrderLines line = SalesOrderLines.builder()
                .salesOrderId(soId)
                .productId("prod-1")
                .lineNumber(lineNumber)
                .quantityOrdered(new BigDecimal(qtyOrdered))
                .quantityShipped(BigDecimal.ZERO)
                .unitPrice(new BigDecimal(unitPrice))
                .lineTotal(new BigDecimal(lineTotal))
                .build();
        line.setId(id);
        return line;
    }

    private Products buildProduct(String id, ProductStatus status) {
        Products p = Products.builder()
                .name("Product A").status(status).sku("SKU-001")
                .categoryId("cat-1").uomId("uom-1").build();
        p.setId(id);
        return p;
    }
}
