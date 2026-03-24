package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.demo.whs.entity.enums.BackgroundJobStatus;
import org.demo.whs.entity.enums.BackgroundJobType;
import java.time.LocalDateTime;

@Entity
@Table(name = "background_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BackgroundJob extends BaseEntity {

    @Column(name = "job_code", nullable = false, unique = true, length = 64)
    private String jobCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private BackgroundJobType jobType;

    @Column(name = "business_type", nullable = false, length = 100)
    private String businessType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BackgroundJobStatus status = BackgroundJobStatus.PENDING;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(name = "requested_by", nullable = false, length = 36, columnDefinition = "char(36)")
    private String requestedBy;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "request_hash", length = 128)
    private String requestHash;

    @Builder.Default
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;

    @Builder.Default
    @Column(name = "processed_rows", nullable = false)
    private Long processedRows = 0L;

    @Builder.Default
    @Column(name = "total_rows", nullable = false)
    private Long totalRows = 0L;

    @Column(name = "result_file_name", length = 255)
    private String resultFileName;

    @Column(name = "storage_object_key", length = 500)
    private String storageObjectKey;

    @Column(name = "result_mime_type", length = 100)
    private String resultMimeType;

    @Column(name = "result_file_size")
    private Long resultFileSize;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;
}
