package org.demo.whs.repository;

import org.demo.whs.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Batch entity operations.
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {

    /**
     * Checks whether a batch exists by its batch number.
     *
     * Used to validate uniqueness when creating a new batch.
     *
     * @param batchNumber the batch number to check
     * @return true if batch exists, false otherwise
     */
    boolean existsByProductIdAndBatchNumber(String productId, String batchNumber);

    /**
     * Finds a batch by its batch number.
     *
     * Used for retrieving batch information or validating batch existence.
     *
     * @param batchNumber the batch number
     * @return Optional containing the batch if found, otherwise empty
     */
    Optional<Batch> findByBatchNumber(String batchNumber);
}
