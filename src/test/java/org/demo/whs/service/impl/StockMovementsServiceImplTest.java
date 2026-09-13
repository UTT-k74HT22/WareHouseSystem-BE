package org.demo.whs.service.impl;

import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.dto.request.Inventory.InventoryDecreaseRequest;
import org.demo.whs.entity.dto.request.Inventory.InventoryIncreaseRequest;
import org.demo.whs.entity.dto.response.PageResponse;
import org.demo.whs.entity.dto.response.StockMovements.StockMovementsResponse;
import org.demo.whs.entity.enums.ReferenceType;
import org.demo.whs.entity.enums.StockMovementsType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.exception.NotFoundException;
import org.demo.whs.mapper.StockMovementsMapper;
import org.demo.whs.repository.StockMovementsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockMovementsServiceImpl Unit Tests")
class StockMovementsServiceImplTest {

    @Mock
    private StockMovementsRepository stockMovementsRepository;

    @Mock
    private StockMovementsMapper stockMovementsMapper;

    @InjectMocks
    private StockMovementsServiceImpl stockMovementsService;

    @Nested
    @DisplayName("getById tests")
    class GetByIdTests {

        @Test
        @DisplayName("should_ReturnStockMovementsResponse_When_MovementExists")
        void should_ReturnStockMovementsResponse_When_MovementExists() {
            // Arrange
            String movementId = "sm-001";
            StockMovements movement = StockMovements.builder()
                    .movementType(StockMovementsType.INBOUND)
                    .productId("prod-1")
                    .quantityChange(BigDecimal.valueOf(100))
                    .build();
            movement.setId(movementId);

            StockMovementsResponse expectedResponse = StockMovementsResponse.builder()
                    .id(movementId)
                    .movementType(StockMovementsType.INBOUND)
                    .productId("prod-1")
                    .quantityChange(BigDecimal.valueOf(100))
                    .build();

            when(stockMovementsRepository.findById(movementId)).thenReturn(Optional.of(movement));
            when(stockMovementsMapper.toResponse(movement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.getById(movementId);

            // Assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(movementId);
            assertThat(actual.getMovementType()).isEqualTo(StockMovementsType.INBOUND);
            verify(stockMovementsRepository).findById(movementId);
            verify(stockMovementsMapper).toResponse(movement);
        }

        @Test
        @DisplayName("should_ThrowNotFoundException_When_MovementDoesNotExist")
        void should_ThrowNotFoundException_When_MovementDoesNotExist() {
            // Arrange
            String nonExistentId = "sm-999";
            when(stockMovementsRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> stockMovementsService.getById(nonExistentId))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Stock movement not found")
                    .matches(e -> ((NotFoundException) e).getErrorCode().equals(ErrorCode.COM_004.getCode()));

            verify(stockMovementsRepository).findById(nonExistentId);
            verifyNoInteractions(stockMovementsMapper);
        }
    }

    @Nested
    @DisplayName("getAll tests")
    class GetAllTests {

        @Test
        @DisplayName("should_ReturnPageResponse_When_ValidPagination")
        void should_ReturnPageResponse_When_ValidPagination() {
            // Arrange
            StockMovements movement = StockMovements.builder().movementType(StockMovementsType.INBOUND).build();
            StockMovementsResponse response = StockMovementsResponse.builder().movementType(StockMovementsType.INBOUND).build();
            Page<StockMovements> page = new PageImpl<>(List.of(movement));

            when(stockMovementsRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(stockMovementsMapper.toResponse(movement)).thenReturn(response);

            // Act
            PageResponse<StockMovementsResponse> result = stockMovementsService.getAll(0, 10);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0)).isEqualTo(response);
            verify(stockMovementsRepository).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_PageNegative")
        void should_ThrowBadRequestException_When_PageNegative() {
            assertThatThrownBy(() -> stockMovementsService.getAll(-1, 10))
                    .isInstanceOf(BadRequestException.class)
                    .matches(e -> ((BadRequestException) e).getErrorCode().equals(ErrorCode.COM_001.getCode()));
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_SizeZeroOrNegative")
        void should_ThrowBadRequestException_When_SizeZeroOrNegative() {
            assertThatThrownBy(() -> stockMovementsService.getAll(0, 0))
                    .isInstanceOf(BadRequestException.class);
            assertThatThrownBy(() -> stockMovementsService.getAll(0, -5))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("should_ThrowBadRequestException_When_SizeExceedsMax")
        void should_ThrowBadRequestException_When_SizeExceedsMax() {
            assertThatThrownBy(() -> stockMovementsService.getAll(0, 201))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("getByReference tests")
    class GetByReferenceTests {

        @Test
        @DisplayName("should_ReturnPageResponse_When_ValidReference")
        void should_ReturnPageResponse_When_ValidReference() {
            // Arrange
            ReferenceType refType = ReferenceType.INBOUND_RECEIPT;
            String refId = "inb-100";
            StockMovements movement = StockMovements.builder()
                    .referenceType(refType)
                    .referenceId(refId)
                    .build();
            StockMovementsResponse response = StockMovementsResponse.builder()
                    .referenceType(refType)
                    .referenceId(refId)
                    .build();
            Page<StockMovements> page = new PageImpl<>(List.of(movement));

            when(stockMovementsRepository.findByReferenceTypeAndReferenceId(eq(refType), eq(refId), any(Pageable.class)))
                    .thenReturn(page);
            when(stockMovementsMapper.toResponse(movement)).thenReturn(response);

            // Act
            PageResponse<StockMovementsResponse> result = stockMovementsService.getByReference(refType, refId, 0, 20);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getReferenceId()).isEqualTo(refId);
        }
    }

    @Nested
    @DisplayName("recordMovement tests")
    class RecordMovementTests {

        @Test
        @DisplayName("should_SaveMovementAndReturnResponse_When_Called")
        void should_SaveMovementAndReturnResponse_When_Called() {
            // Arrange
            StockMovements movement = StockMovements.builder()
                    .productId("p-1")
                    .movementType(StockMovementsType.INBOUND)
                    .build();
            StockMovements saved = StockMovements.builder()
                    .productId("p-1")
                    .movementType(StockMovementsType.INBOUND)
                    .build();
            saved.setId("sm-123");

            StockMovementsResponse response = StockMovementsResponse.builder()
                    .id("sm-123")
                    .build();

            when(stockMovementsRepository.save(movement)).thenReturn(saved);
            when(stockMovementsMapper.toResponse(saved)).thenReturn(response);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordMovement(movement);

            // Assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo("sm-123");
            verify(stockMovementsRepository).save(movement);
            verify(stockMovementsMapper).toResponse(saved);
        }
    }

    @Nested
    @DisplayName("recordIncrease tests")
    class RecordIncreaseTests {

        @Test
        @DisplayName("should_RecordInboundMovement_When_ReferenceTypeIsNotAdjustment")
        void should_RecordInboundMovement_When_ReferenceTypeIsNotAdjustment() {
            // Arrange
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .quantity(BigDecimal.valueOf(50))
                    .referenceType(ReferenceType.PURCHASE_ORDER)
                    .referenceId("po-1")
                    .build();

            StockMovements movement = StockMovements.builder().build();
            StockMovements savedMovement = StockMovements.builder().build();
            savedMovement.setId("sm-inc-1");
            StockMovementsResponse expectedResponse = StockMovementsResponse.builder().id("sm-inc-1").build();

            when(stockMovementsMapper.toEntity(
                    eq(StockMovementsType.INBOUND),
                    eq(request),
                    eq(BigDecimal.ZERO),
                    eq(BigDecimal.valueOf(50)),
                    any()
            )).thenReturn(movement);

            when(stockMovementsRepository.save(movement)).thenReturn(savedMovement);
            when(stockMovementsMapper.toResponse(savedMovement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordIncrease(
                    request, BigDecimal.ZERO, BigDecimal.valueOf(50)
            );

            // Assert
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo("sm-inc-1");
            verify(stockMovementsMapper).toEntity(
                    eq(StockMovementsType.INBOUND),
                    eq(request),
                    eq(BigDecimal.ZERO),
                    eq(BigDecimal.valueOf(50)),
                    any()
            );
        }

        @Test
        @DisplayName("should_RecordAdjustmentIncreaseMovement_When_ReferenceTypeIsStockAdjustment")
        void should_RecordAdjustmentIncreaseMovement_When_ReferenceTypeIsStockAdjustment() {
            // Arrange
            InventoryIncreaseRequest request = InventoryIncreaseRequest.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .quantity(BigDecimal.valueOf(10))
                    .referenceType(ReferenceType.STOCK_ADJUSTMENT)
                    .referenceId("adj-1")
                    .build();

            StockMovements movement = StockMovements.builder().build();
            StockMovements savedMovement = StockMovements.builder().build();
            StockMovementsResponse expectedResponse = StockMovementsResponse.builder().id("sm-adj-inc").build();

            when(stockMovementsMapper.toEntity(
                    eq(StockMovementsType.ADJUSTMENT_INCREASE),
                    eq(request),
                    eq(BigDecimal.valueOf(20)),
                    eq(BigDecimal.valueOf(30)),
                    any()
            )).thenReturn(movement);

            when(stockMovementsRepository.save(movement)).thenReturn(savedMovement);
            when(stockMovementsMapper.toResponse(savedMovement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordIncrease(
                    request, BigDecimal.valueOf(20), BigDecimal.valueOf(30)
            );

            // Assert
            assertThat(actual).isNotNull();
            verify(stockMovementsMapper).toEntity(
                    eq(StockMovementsType.ADJUSTMENT_INCREASE),
                    eq(request),
                    eq(BigDecimal.valueOf(20)),
                    eq(BigDecimal.valueOf(30)),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("recordDecrease tests")
    class RecordDecreaseTests {

        @Test
        @DisplayName("should_RecordAdjustmentDecreaseMovement_When_ReferenceTypeIsStockAdjustment")
        void should_RecordAdjustmentDecreaseMovement_When_ReferenceTypeIsStockAdjustment() {
            // Arrange
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .quantity(BigDecimal.valueOf(5))
                    .referenceType(ReferenceType.STOCK_ADJUSTMENT)
                    .referenceId("adj-2")
                    .build();

            StockMovements movement = StockMovements.builder().build();
            StockMovements savedMovement = StockMovements.builder().build();
            StockMovementsResponse expectedResponse = StockMovementsResponse.builder().id("sm-adj-dec").build();

            when(stockMovementsMapper.toEntity(
                    eq(StockMovementsType.ADJUSTMENT_DECREASE),
                    eq(request),
                    eq(BigDecimal.valueOf(50)),
                    eq(BigDecimal.valueOf(45)),
                    any()
            )).thenReturn(movement);

            when(stockMovementsRepository.save(movement)).thenReturn(savedMovement);
            when(stockMovementsMapper.toResponse(savedMovement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordDecrease(
                    request, BigDecimal.valueOf(50), BigDecimal.valueOf(45)
            );

            // Assert
            assertThat(actual).isNotNull();
            verify(stockMovementsMapper).toEntity(
                    eq(StockMovementsType.ADJUSTMENT_DECREASE),
                    eq(request),
                    eq(BigDecimal.valueOf(50)),
                    eq(BigDecimal.valueOf(45)),
                    any()
            );
        }

        @Test
        @DisplayName("should_RecordOutboundMovement_When_ReferenceTypeIsOutboundShipment")
        void should_RecordOutboundMovement_When_ReferenceTypeIsOutboundShipment() {
            // Arrange
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .quantity(BigDecimal.valueOf(20))
                    .referenceType(ReferenceType.OUTBOUND_SHIPMENT)
                    .referenceId("ship-1")
                    .build();

            StockMovements movement = StockMovements.builder().build();
            StockMovements savedMovement = StockMovements.builder().build();
            StockMovementsResponse expectedResponse = StockMovementsResponse.builder().id("sm-out-1").build();

            when(stockMovementsMapper.toEntity(
                    eq(StockMovementsType.OUTBOUND),
                    eq(request),
                    eq(BigDecimal.valueOf(50)),
                    eq(BigDecimal.valueOf(30)),
                    any()
            )).thenReturn(movement);

            when(stockMovementsRepository.save(movement)).thenReturn(savedMovement);
            when(stockMovementsMapper.toResponse(savedMovement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordDecrease(
                    request, BigDecimal.valueOf(50), BigDecimal.valueOf(30)
            );

            // Assert
            assertThat(actual).isNotNull();
            verify(stockMovementsMapper).toEntity(
                    eq(StockMovementsType.OUTBOUND),
                    eq(request),
                    eq(BigDecimal.valueOf(50)),
                    eq(BigDecimal.valueOf(30)),
                    any()
            );
        }

        @Test
        @DisplayName("should_RecordOutboundMovement_When_ReferenceTypeIsOther")
        void should_RecordOutboundMovement_When_ReferenceTypeIsOther() {
            // Arrange
            InventoryDecreaseRequest request = InventoryDecreaseRequest.builder()
                    .productId("prod-1")
                    .warehouseId("wh-1")
                    .locationId("loc-1")
                    .quantity(BigDecimal.valueOf(15))
                    .referenceType(ReferenceType.SALES_ORDER)
                    .referenceId("so-1")
                    .build();

            StockMovements movement = StockMovements.builder().build();
            StockMovements savedMovement = StockMovements.builder().build();
            StockMovementsResponse expectedResponse = StockMovementsResponse.builder().id("sm-so-1").build();

            when(stockMovementsMapper.toEntity(
                    eq(StockMovementsType.OUTBOUND),
                    eq(request),
                    eq(BigDecimal.valueOf(30)),
                    eq(BigDecimal.valueOf(15)),
                    any()
            )).thenReturn(movement);

            when(stockMovementsRepository.save(movement)).thenReturn(savedMovement);
            when(stockMovementsMapper.toResponse(savedMovement)).thenReturn(expectedResponse);

            // Act
            StockMovementsResponse actual = stockMovementsService.recordDecrease(
                    request, BigDecimal.valueOf(30), BigDecimal.valueOf(15)
            );

            // Assert
            assertThat(actual).isNotNull();
            verify(stockMovementsMapper).toEntity(
                    eq(StockMovementsType.OUTBOUND),
                    eq(request),
                    eq(BigDecimal.valueOf(30)),
                    eq(BigDecimal.valueOf(15)),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("existsByReference tests")
    class ExistsByReferenceTests {

        @Test
        @DisplayName("should_ReturnFalse_When_ReferenceIdIsNull")
        void should_ReturnFalse_When_ReferenceIdIsNull() {
            boolean exists = stockMovementsService.existsByReference(ReferenceType.INBOUND_RECEIPT, null);
            assertThat(exists).isFalse();
            verifyNoInteractions(stockMovementsRepository);
        }

        @Test
        @DisplayName("should_ReturnFalse_When_ReferenceIdIsBlank")
        void should_ReturnFalse_When_ReferenceIdIsBlank() {
            boolean exists = stockMovementsService.existsByReference(ReferenceType.INBOUND_RECEIPT, "   ");
            assertThat(exists).isFalse();
            verifyNoInteractions(stockMovementsRepository);
        }

        @Test
        @DisplayName("should_ReturnRepositoryResult_When_ReferenceIdIsValid")
        void should_ReturnRepositoryResult_When_ReferenceIdIsValid() {
            when(stockMovementsRepository.existsByReferenceTypeAndReferenceId(ReferenceType.INBOUND_RECEIPT, "inb-1"))
                    .thenReturn(true);

            boolean exists = stockMovementsService.existsByReference(ReferenceType.INBOUND_RECEIPT, "inb-1");
            assertThat(exists).isTrue();
            verify(stockMovementsRepository).existsByReferenceTypeAndReferenceId(ReferenceType.INBOUND_RECEIPT, "inb-1");
        }
    }

    @Nested
    @DisplayName("existsByReferenceNumber tests")
    class ExistsByReferenceNumberTests {

        @Test
        @DisplayName("should_ReturnFalse_When_ReferenceNumberIsNull")
        void should_ReturnFalse_When_ReferenceNumberIsNull() {
            boolean exists = stockMovementsService.existsByReferenceNumber(ReferenceType.INBOUND_RECEIPT, null);
            assertThat(exists).isFalse();
            verifyNoInteractions(stockMovementsRepository);
        }

        @Test
        @DisplayName("should_ReturnFalse_When_ReferenceNumberIsBlank")
        void should_ReturnFalse_When_ReferenceNumberIsBlank() {
            boolean exists = stockMovementsService.existsByReferenceNumber(ReferenceType.INBOUND_RECEIPT, "");
            assertThat(exists).isFalse();
            verifyNoInteractions(stockMovementsRepository);
        }

        @Test
        @DisplayName("should_ReturnRepositoryResult_When_ReferenceNumberIsValid")
        void should_ReturnRepositoryResult_When_ReferenceNumberIsValid() {
            when(stockMovementsRepository.existsByReferenceTypeAndReferenceNumber(ReferenceType.INBOUND_RECEIPT, "REF-001"))
                    .thenReturn(true);

            boolean exists = stockMovementsService.existsByReferenceNumber(ReferenceType.INBOUND_RECEIPT, "REF-001");
            assertThat(exists).isTrue();
            verify(stockMovementsRepository).existsByReferenceTypeAndReferenceNumber(ReferenceType.INBOUND_RECEIPT, "REF-001");
        }
    }
}
