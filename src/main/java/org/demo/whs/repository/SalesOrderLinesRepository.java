package org.demo.whs.repository;

import org.demo.whs.entity.SalesOrderLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Sales Order Lines.
 */
@Repository
public interface SalesOrderLinesRepository extends JpaRepository<SalesOrderLines, String> {

    /**
     * Get all lines by Sales Order ID
     */
    List<SalesOrderLines> findBySalesOrderId(String salesOrderId);

    /**
     * Get all lines ordered by lineNumber
     */
    List<SalesOrderLines> findBySalesOrderIdOrderByLineNumberAsc(String salesOrderId);

    /**
     * Get max line number for a Sales Order
     */
    @Query("""
        SELECT MAX(sol.lineNumber)
        FROM SalesOrderLines sol
        WHERE sol.salesOrderId = :salesOrderId
    """)
    Optional<Integer> findMaxLineNumber(@Param("salesOrderId") String salesOrderId);

    /**
     * Lock a specific line for update (pessimistic lock)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sol
        FROM SalesOrderLines sol
        WHERE sol.id = :id
    """)
    Optional<SalesOrderLines> findByIdForUpdate(@Param("id") String id);

    /**
     * Bulk fetch lines by Sales Order ID (useful for batch operations)
     */
    @Query("""
        SELECT sol
        FROM SalesOrderLines sol
        WHERE sol.salesOrderId = :salesOrderId
    """)
    List<SalesOrderLines> findAllBySalesOrderId(@Param("salesOrderId") String salesOrderId);

    /**
     * Sum total amount of all lines (optimized for performance)
     */
    @Query("""
        SELECT COALESCE(SUM(sol.lineTotal), 0)
        FROM SalesOrderLines sol
        WHERE sol.salesOrderId = :salesOrderId
    """)
    BigDecimal sumLineTotalBySalesOrderId(@Param("salesOrderId") String salesOrderId);
}