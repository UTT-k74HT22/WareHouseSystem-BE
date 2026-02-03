package org.demo.whs.repository;

import org.demo.whs.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Batch entity operations.
 */
@Repository
public interface BatchRepository extends JpaRepository<Batch, String> {
}
