package org.demo.whs.repository;

import org.demo.whs.entity.BackgroundJobStepLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing BackgroundJobStepLog entities.
 */
public interface BackgroundJobStepLogRepository extends JpaRepository<BackgroundJobStepLog, String> {
}
