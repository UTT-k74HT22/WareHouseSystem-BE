package org.demo.whs.mapper;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.dto.request.Batch.CreateBatchRequest;
import org.demo.whs.entity.dto.request.Batch.UpdateBatchRequest;
import org.demo.whs.entity.dto.response.Batch.BatchResponse;
import org.demo.whs.entity.enums.BatchStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper class for Batch entity transformations.
 */
@Component
public class BatchMapper {
    /**
     * Convert CreateBatchRequest → Batch entity
     */
    public Batch createEntity(CreateBatchRequest request) {
        if (request == null) {
            return null;
        }

        return Batch.builder()
                .batchNumber(request.getBatchNumber())
                .productId(request.getProductId())
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .supplierBatchNumber(request.getSupplierBatchNumber())
                .notes(request.getNotes())
                .status(BatchStatus.AVAILABLE)
                .build();
    }
    /**
     * Update existing Batch entity from UpdateBatchRequest
     */
    public void updateEntity(UpdateBatchRequest request, Batch batch) {

        if (batch == null || request == null) {
            return;
        }

        if (request.getBatchNumber() != null) {
            batch.setBatchNumber(request.getBatchNumber());
        }

        if ( request.getManufacturingDate() != null) {
            batch.setManufacturingDate(request.getManufacturingDate());
        }

        if ( request.getExpiryDate() != null) {
            batch.setExpiryDate(request.getExpiryDate());
        }

        if ( request.getSupplierBatchNumber() != null) {
            batch.setSupplierBatchNumber(request.getSupplierBatchNumber());
        }

        if ( request.getNotes() != null) {
            batch.setNotes(request.getNotes());
        }
    }

    /**
     * Convert Batch → BatchResponse
     */
    public BatchResponse toResponse(Batch batch) {

        if (batch == null) {
            return null;
        }

        return BatchResponse.builder()
                .id(batch.getId())
                .batchNumber(batch.getBatchNumber())
                .productId(batch.getProductId())
                .manufacturingDate(batch.getManufacturingDate())
                .expiryDate(batch.getExpiryDate())
                .supplierBatchNumber(batch.getSupplierBatchNumber())
                .notes(batch.getNotes())
                .status(batch.getStatus())
                .createdBy(batch.getCreatedBy())
                .createdAt(batch.getCreatedAt())
                .updatedBy(batch.getUpdatedBy())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
