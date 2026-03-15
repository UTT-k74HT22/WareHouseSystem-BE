package org.demo.whs.repository;

import org.demo.whs.entity.StockMovements;
import org.demo.whs.entity.enums.ReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for StockMovements entity.
 */
@Repository
public interface StockMovementsRepository extends JpaRepository<StockMovements, String> {

    Page<StockMovements> findByReferenceTypeAndReferenceId(
            ReferenceType referenceType,
            String referenceId,
            Pageable pageable
    );

    boolean existsByReferenceTypeAndReferenceId(ReferenceType referenceType, String referenceId);

    boolean existsByReferenceTypeAndReferenceNumber(ReferenceType referenceType, String referenceNumber);

    List<StockMovements> findByBatchIdOrderByMovementDateDesc(String batchId);
}
