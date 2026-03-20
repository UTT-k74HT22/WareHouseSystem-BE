package org.demo.whs.repository;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.demo.whs.entity.SalesOrders;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Sales Orders.
 */
@Repository
public interface SalesOrdersRepository extends JpaRepository<SalesOrders, String>, JpaSpecificationExecutor<SalesOrders> {
    boolean existsBySoNumber(String soNumber);

    /**
     * Lấy SalesOrder theo ID với khóa ghi (PESSIMISTIC_WRITE).
     *
     * - Khóa row ở DB (SELECT ... FOR UPDATE) để tránh update đồng thời.
     * - Các transaction khác sẽ phải chờ hoặc bị fail khi cố lock cùng row.
     *
     * QueryHint:
     * - jakarta.persistence.lock.timeout = 5000 (ms)
     * - Thời gian tối đa chờ lock là 5 giây, quá thời gian sẽ throw exception.
     *
     * Lưu ý:
     * - Phải chạy trong @Transactional thì lock mới có hiệu lực.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
            @QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000") // 5s
    })
    @Query("select so from SalesOrders so where so.id = :id")
    Optional<SalesOrders> findByIdForUpdate(@Param("id") String id);
}
