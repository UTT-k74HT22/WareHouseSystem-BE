package org.demo.whs.repository;

import org.demo.whs.entity.EmailLog;
import org.demo.whs.entity.enums.EmailStatus;
import org.demo.whs.entity.enums.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EmailLogRepository: Repository for email log operations
 */
@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, String> {

    /**
     * Find email logs by recipient
     */
    Page<EmailLog> findByRecipient(String recipient, Pageable pageable);

    /**
     * Find email logs by status
     */
    Page<EmailLog> findByStatus(EmailStatus status, Pageable pageable);

    /**
     * Find email logs by email type
     */
    Page<EmailLog> findByEmailType(EmailType emailType, Pageable pageable);

    /**
     * Find pending or retry emails that need to be sent
     */
    @Query("SELECT e FROM EmailLog e WHERE e.status IN :statuses AND " +
           "(e.scheduledAt IS NULL OR e.scheduledAt <= :now) " +
           "ORDER BY e.priority ASC, e.createdAt ASC")
    List<EmailLog> findPendingEmails(
            @Param("statuses") List<EmailStatus> statuses,
            @Param("now") LocalDateTime now
    );

    /**
     * Find failed emails that can be retried
     */
    @Query("SELECT e FROM EmailLog e WHERE e.status = :status AND " +
           "e.retryCount < e.maxRetry " +
           "ORDER BY e.priority ASC, e.createdAt ASC")
    List<EmailLog> findFailedEmailsForRetry(@Param("status") EmailStatus status);

    /**
     * Count emails by status
     */
    long countByStatus(EmailStatus status);

    /**
     * Find emails by recipient and date range
     */
    @Query("SELECT e FROM EmailLog e WHERE e.recipient = :recipient AND " +
           "e.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY e.createdAt DESC")
    List<EmailLog> findByRecipientAndDateRange(
            @Param("recipient") String recipient,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find emails that failed multiple times
     */
    @Query("SELECT e FROM EmailLog e WHERE e.status = 'FAILED' AND " +
           "e.retryCount >= e.maxRetry")
    List<EmailLog> findPermanentlyFailedEmails();

    /**
     * Delete old email logs (for cleanup)
     */
    void deleteByCreatedAtBefore(LocalDateTime date);
}
