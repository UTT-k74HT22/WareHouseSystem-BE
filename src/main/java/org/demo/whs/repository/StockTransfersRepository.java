package org.demo.whs.repository;

import org.demo.whs.entity.StockTransfers;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Repository interface for managing stock transfers data.
 */
@Repository
public interface StockTransfersRepository extends JpaRepository<StockTransfers, String> {

    boolean existsByTransferNumber(String transferNumber);

    /**
     * Retrieves a stock transfer by its ID with a pessimistic write lock to prevent concurrent modifications.
     *
     * @param id the ID of the stock transfer to retrieve
     * @return an Optional containing the stock transfer if found, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTransfers st WHERE st.id = :id")
    Optional<StockTransfers> findByIdForUpdate(@Param("id") String id);
}
