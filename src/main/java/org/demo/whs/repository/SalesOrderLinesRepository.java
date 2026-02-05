package org.demo.whs.repository;

import org.demo.whs.entity.SalesOrderLines;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for SalesOrderLines entity.
 */
@Repository
public interface SalesOrderLinesRepository extends JpaRepository<SalesOrderLines, String> {
}
