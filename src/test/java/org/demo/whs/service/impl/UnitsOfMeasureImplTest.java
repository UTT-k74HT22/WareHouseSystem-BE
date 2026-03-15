package org.demo.whs.service.impl;

import org.demo.whs.entity.UnitsOfMeasure;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UnitsOfMeasureRequest;
import org.demo.whs.entity.dto.request.UnitsOfMeasure.UpdateUnitsOfMeasureRequest;
import org.demo.whs.entity.dto.response.UnitsOfMeasure.UnitsOfMeasureResponse;
import org.demo.whs.entity.enums.UnitsOfMeasureType;
import org.demo.whs.exception.BadRequestException;
import org.demo.whs.exception.ErrorCode;
import org.demo.whs.mapper.UnitsOfMeasureMapper;
import org.demo.whs.repository.ProductRepository;
import org.demo.whs.repository.UnitsOfMeasureRepository;
import org.demo.whs.utils.IdentifierGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UnitsOfMeasureImpl service.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnitsOfMeasureImpl Unit Tests")
class UnitsOfMeasureImplTest {

    @Mock
    private UnitsOfMeasureRepository unitsOfMeasureRepository;

    @Mock
    private UnitsOfMeasureMapper unitsOfMeasureMapper;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private IdentifierGenerator identifierGenerator = new IdentifierGenerator();

    @InjectMocks
    private UnitsOfMeasureImpl unitsOfMeasureService;

    @Nested
    @DisplayName("Create Unit of Measure Tests")
    class CreateTests {

        @Test
        @DisplayName("Should create unit of measure successfully with BE-generated code")
        void create_Success() {
            UnitsOfMeasureRequest request = mock(UnitsOfMeasureRequest.class);
            when(request.getCode()).thenReturn(null);
            when(request.getName()).thenReturn("Kilogram");

            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id("uom-001")
                    .name("Kilogram")
                    .description("Weight measurement")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(unitsOfMeasureRepository.existsUnitsOfMeasureByCode(anyString())).thenReturn(false);
            when(unitsOfMeasureMapper.buildRequest(request)).thenReturn(entity);
            when(unitsOfMeasureRepository.save(any(UnitsOfMeasure.class))).thenReturn(entity);
            when(unitsOfMeasureMapper.toResponse(any(UnitsOfMeasure.class))).thenAnswer(invocation -> {
                UnitsOfMeasure persisted = invocation.getArgument(0);
                return UnitsOfMeasureResponse.builder()
                        .id(persisted.getId())
                        .code(persisted.getCode())
                        .name(persisted.getName())
                        .description(persisted.getDescription())
                        .type(persisted.getType())
                        .build();
            });

            UnitsOfMeasureResponse result = unitsOfMeasureService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("uom-001");
            assertThat(result.getCode()).startsWith("UOM-");
            assertThat(result.getCode()).hasSizeLessThanOrEqualTo(10);
            assertThat(result.getName()).isEqualTo("Kilogram");
            assertThat(result.getType()).isEqualTo(UnitsOfMeasureType.WEIGHT);
            assertThat(entity.getCode()).isEqualTo(result.getCode());

            verify(unitsOfMeasureMapper).buildRequest(request);
            verify(unitsOfMeasureRepository).save(any(UnitsOfMeasure.class));
            verify(unitsOfMeasureMapper).toResponse(any(UnitsOfMeasure.class));
        }

        @Test
        @DisplayName("Should reject when client provides unit of measure code")
        void create_ShouldReject_When_RequestProvidesCode() {
            UnitsOfMeasureRequest request = mock(UnitsOfMeasureRequest.class);
            when(request.getCode()).thenReturn("KG");

            assertThatThrownBy(() -> unitsOfMeasureService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COM_001.getCode());

            verify(unitsOfMeasureRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should generate unit of measure code when code is missing")
        void create_GenerateCodeWhenMissing() {
            UnitsOfMeasureRequest request = mock(UnitsOfMeasureRequest.class);
            when(request.getCode()).thenReturn(null);
            when(request.getName()).thenReturn("Kilogram");

            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            when(unitsOfMeasureRepository.existsUnitsOfMeasureByCode(anyString())).thenReturn(false);
            when(unitsOfMeasureMapper.buildRequest(request)).thenReturn(entity);
            when(unitsOfMeasureRepository.save(any(UnitsOfMeasure.class))).thenReturn(entity);
            when(unitsOfMeasureMapper.toResponse(any(UnitsOfMeasure.class))).thenAnswer(invocation -> {
                UnitsOfMeasure persisted = invocation.getArgument(0);
                return UnitsOfMeasureResponse.builder()
                        .code(persisted.getCode())
                        .name(persisted.getName())
                        .type(persisted.getType())
                        .build();
            });

            UnitsOfMeasureResponse result = unitsOfMeasureService.create(request);

            assertThat(result.getCode()).startsWith("UOM-");
            assertThat(result.getCode()).hasSizeLessThanOrEqualTo(10);
            assertThat(entity.getCode()).isEqualTo(result.getCode());
        }

        @Test
        @DisplayName("Should throw BadRequestException when name is null")
        void create_NameIsNull() {
            UnitsOfMeasureRequest request = mock(UnitsOfMeasureRequest.class);
            when(request.getCode()).thenReturn(null);
            when(request.getName()).thenReturn(null);

            when(unitsOfMeasureRepository.existsUnitsOfMeasureByCode(anyString())).thenReturn(false);

            assertThatThrownBy(() -> unitsOfMeasureService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_003.getCode());

            verify(unitsOfMeasureRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when name is blank")
        void create_NameIsBlank() {
            UnitsOfMeasureRequest request = mock(UnitsOfMeasureRequest.class);
            when(request.getCode()).thenReturn(null);
            when(request.getName()).thenReturn("   ");

            when(unitsOfMeasureRepository.existsUnitsOfMeasureByCode(anyString())).thenReturn(false);

            assertThatThrownBy(() -> unitsOfMeasureService.create(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_003.getCode());

            verify(unitsOfMeasureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Find All Unit of Measure Tests")
    class FindAllTests {

        @Test
        @DisplayName("Should return all units of measure successfully")
        void findAll_Success() {
            // Given
            UnitsOfMeasure uom1 = UnitsOfMeasure.builder()
                    .id("uom-001")
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            UnitsOfMeasure uom2 = UnitsOfMeasure.builder()
                    .id("uom-002")
                    .code("M")
                    .name("Meter")
                    .type(UnitsOfMeasureType.LENGTH)
                    .build();

            List<UnitsOfMeasure> entities = Arrays.asList(uom1, uom2);

            UnitsOfMeasureResponse response1 = UnitsOfMeasureResponse.builder()
                    .id("uom-001")
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            UnitsOfMeasureResponse response2 = UnitsOfMeasureResponse.builder()
                    .id("uom-002")
                    .code("M")
                    .name("Meter")
                    .type(UnitsOfMeasureType.LENGTH)
                    .build();

            List<UnitsOfMeasureResponse> expectedResponses = Arrays.asList(response1, response2);

            when(unitsOfMeasureRepository.findAll()).thenReturn(entities);
            when(unitsOfMeasureMapper.toResponse((List<UnitsOfMeasure>) entities)).thenReturn(expectedResponses);

            // When
            List<UnitsOfMeasureResponse> result = unitsOfMeasureService.findAll();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCode()).isEqualTo("KG");
            assertThat(result.get(1).getCode()).isEqualTo("M");

            verify(unitsOfMeasureRepository).findAll();
            verify(unitsOfMeasureMapper).toResponse(entities);
        }

        @Test
        @DisplayName("Should return empty list when no units of measure exist")
        void findAll_EmptyList() {
            // Given
            when(unitsOfMeasureRepository.findAll()).thenReturn(List.of());
            when(unitsOfMeasureMapper.toResponse(List.of())).thenReturn(List.of());

            // When
            List<UnitsOfMeasureResponse> result = unitsOfMeasureService.findAll();

            // Then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(unitsOfMeasureRepository).findAll();
        }
    }

    @Nested
    @DisplayName("Find By ID Tests")
    class FindByIdTests {

        @Test
        @DisplayName("Should return unit of measure by ID successfully")
        void findById_Success() {
            // Given
            String id = "uom-001";
            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            UnitsOfMeasureResponse expectedResponse = UnitsOfMeasureResponse.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));
            when(unitsOfMeasureMapper.toResponse(entity)).thenReturn(expectedResponse);

            // When
            UnitsOfMeasureResponse result = unitsOfMeasureService.findById(id);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getCode()).isEqualTo("KG");
            assertThat(result.getName()).isEqualTo("Kilogram");

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureMapper).toResponse(entity);
        }

        @Test
        @DisplayName("Should throw BadRequestException when unit of measure not found")
        void findById_NotFound() {
            // Given
            String id = "non-existent-id";
            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.findById(id))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_001.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureMapper, never()).toResponse(any(UnitsOfMeasure.class));
        }
    }

    @Nested
    @DisplayName("Update Unit of Measure Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should update unit of measure successfully")
        void update_Success() {
            // Given
            String id = "uom-001";
            UpdateUnitsOfMeasureRequest request = mock(UpdateUnitsOfMeasureRequest.class);
            when(request.getName()).thenReturn("Kilogram Updated");

            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .description("Original description")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            UnitsOfMeasureResponse expectedResponse = UnitsOfMeasureResponse.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram Updated")
                    .description("Updated description")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));
            doNothing().when(unitsOfMeasureMapper).updateEntity(any(UnitsOfMeasure.class), eq(request));
            when(unitsOfMeasureRepository.save(any(UnitsOfMeasure.class))).thenReturn(entity);
            when(unitsOfMeasureMapper.toResponse(any(UnitsOfMeasure.class))).thenReturn(expectedResponse);

            // When
            UnitsOfMeasureResponse result = unitsOfMeasureService.update(id, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getName()).isEqualTo("Kilogram Updated");

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureMapper).updateEntity(any(UnitsOfMeasure.class), eq(request));
            verify(unitsOfMeasureRepository).save(any(UnitsOfMeasure.class));
            verify(unitsOfMeasureMapper).toResponse(any(UnitsOfMeasure.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when unit of measure not found")
        void update_NotFound() {
            // Given
            String id = "non-existent-id";
            UpdateUnitsOfMeasureRequest request = mock(UpdateUnitsOfMeasureRequest.class);

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.update(id, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_001.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when update name is null")
        void update_NameIsNull() {
            // Given
            String id = "uom-001";
            UpdateUnitsOfMeasureRequest request = mock(UpdateUnitsOfMeasureRequest.class);
            when(request.getName()).thenReturn(null);

            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.update(id, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_003.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when update name is blank")
        void update_NameIsBlank() {
            // Given
            String id = "uom-001";
            UpdateUnitsOfMeasureRequest request = mock(UpdateUnitsOfMeasureRequest.class);
            when(request.getName()).thenReturn("   ");

            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.update(id, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_003.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Unit of Measure Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete unit of measure successfully")
        void delete_Success() {
            // Given
            String id = "uom-001";
            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));
            when(productRepository.countByUomId(id)).thenReturn(0L);
            doNothing().when(unitsOfMeasureRepository).delete(entity);

            // When
            unitsOfMeasureService.delete(id);

            // Then
            verify(unitsOfMeasureRepository).findById(id);
            verify(productRepository).countByUomId(id);
            verify(unitsOfMeasureRepository).delete(entity);
        }

        @Test
        @DisplayName("Should throw BadRequestException when unit of measure is referenced by products")
        void delete_InUseByProducts() {
            // Given
            String id = "uom-001";
            UnitsOfMeasure entity = UnitsOfMeasure.builder()
                    .id(id)
                    .code("KG")
                    .name("Kilogram")
                    .type(UnitsOfMeasureType.WEIGHT)
                    .build();

            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.of(entity));
            when(productRepository.countByUomId(id)).thenReturn(2L);

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.delete(id))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_004.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(productRepository).countByUomId(id);
            verify(unitsOfMeasureRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when unit of measure not found")
        void delete_NotFound() {
            // Given
            String id = "non-existent-id";
            when(unitsOfMeasureRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> unitsOfMeasureService.delete(id))
                    .isInstanceOf(BadRequestException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UOM_001.getCode());

            verify(unitsOfMeasureRepository).findById(id);
            verify(unitsOfMeasureRepository, never()).delete(any());
        }
    }
}
