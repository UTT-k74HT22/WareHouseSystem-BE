package org.demo.whs.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.demo.whs.entity.enums.BackgroundJobStatus;

@Entity
@Table(name = "background_job_step_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BackgroundJobStepLog extends BaseEntity {

    @Column(name = "job_id", nullable = false, columnDefinition = "char(36)")
    private String jobId;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BackgroundJobStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent = 0;
}
