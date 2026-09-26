package org.demo.whs.repository;

import org.demo.whs.entity.BackgroundJob;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.enums.BackgroundJobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for managing BackgroundJob entities.
 */
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, String> {

    /**
     * Finds a page of BackgroundJob entities requested by a specific user, ordered by creation date in descending order.
     *
     * @param requestedBy the username of the requester
     * @param pageable    the pagination information
     * @return a page of BackgroundJob entities
     */
    Page<BackgroundJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);

    /**
     * Finds a BackgroundJob entity by its ID and the username of the requester.
     *
     * @param id          the ID of the BackgroundJob
     * @param requestedBy the username of the requester
     * @return an Optional containing the found BackgroundJob, or empty if not found
     */
    Optional<BackgroundJob> findByIdAndRequestedBy(String id, String requestedBy);

    @Query("SELECT j FROM BackgroundJob j WHERE j.requestedBy = :requestedBy"
            + " AND (:jobTypes IS NULL OR j.jobType IN :jobTypes)"
            + " AND (:statuses IS NULL OR j.status IN :statuses)"
            + " AND (:businessType IS NULL OR LOWER(j.businessType) LIKE LOWER(CONCAT('%', :businessType, '%')))"
            + " AND (:jobCode IS NULL OR LOWER(j.jobCode) LIKE LOWER(CONCAT('%', :jobCode, '%')))"
            + " AND (:createdFrom IS NULL OR j.createdAt >= :createdFrom)"
            + " AND (:createdTo IS NULL OR j.createdAt <= :createdTo)")
    Page<BackgroundJob> searchMyJobs(
            @Param("requestedBy") String requestedBy,
            @Param("jobTypes") Set<BackgroundJobType> jobTypes,
            @Param("statuses") Set<BackgroundJobStatus> statuses,
            @Param("businessType") String businessType,
            @Param("jobCode") String jobCode,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);
}
