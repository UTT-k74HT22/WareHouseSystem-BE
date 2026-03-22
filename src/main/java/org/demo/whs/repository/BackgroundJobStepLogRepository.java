package org.demo.whs.repository;

import org.demo.whs.entity.BackgroundJobStepLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for managing BackgroundJobStepLog entities.
 */
public interface BackgroundJobStepLogRepository extends JpaRepository<BackgroundJobStepLog, String> {

    /**
     * Finds a list of BackgroundJobStepLog entities by the associated job ID, ordered by creation date in ascending order.
     *
     * @param jobId the ID of the associated BackgroundJob
     * @return a list of BackgroundJobStepLog entities
     */
    List<BackgroundJobStepLog> findByJobIdOrderByCreatedAtAsc(String jobId);
}
