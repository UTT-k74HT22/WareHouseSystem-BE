package org.demo.whs.repository;

import org.demo.whs.entity.Batch;
import org.demo.whs.entity.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Batch entity operations.
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, String>, JpaSpecificationExecutor<Batch> {

    boolean existsByProductIdAndBatchNumber(String productId, String batchNumber);

    Optional<Batch> findByBatchNumber(String batchNumber);

    boolean existsByProductIdAndBatchNumberAndIdNot(String productId, String batchNumber, String id);

    @Query("SELECT b FROM Batch b WHERE b.productId = :productId AND b.id = :batchId")
    Optional<Object> findByProductIdAndBatchId(@Param("productId") String productId, @Param("batchId") String batchId);

    List<Batch> findByProductIdOrderByManufacturingDateAscExpiryDateAscCreatedAtAsc(String productId);

    @Query("""
           SELECT b
           FROM Batch b
           WHERE b.expiryDate IS NOT NULL
             AND b.expiryDate BETWEEN :startDate AND :endDate
             AND b.status IN :statuses
           ORDER BY b.expiryDate ASC, b.manufacturingDate ASC, b.createdAt ASC
           """)
    List<Batch> findExpiringBatches(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Collection<BatchStatus> statuses
    );
}
