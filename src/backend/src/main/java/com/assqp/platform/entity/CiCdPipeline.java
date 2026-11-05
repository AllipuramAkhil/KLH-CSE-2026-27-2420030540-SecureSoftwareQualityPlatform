package com.assqp.platform.entity;

import com.assqp.platform.enums.PipelineStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cicd_pipelines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiCdPipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(length = 100)
    private String branch;

    @Column(name = "commit_hash", length = 100)
    private String commitHash;

    @Column(name = "build_number", length = 100)
    private String buildNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PipelineStatus status = PipelineStatus.QUEUED;

    @Column(name = "trigger_source", length = 100)
    private String triggerSource;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
