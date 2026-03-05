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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT st FROM StockTransfers st WHERE st.id = :id")
    Optional<StockTransfers> findByIdForUpdate(@Param("id") String id);
}
