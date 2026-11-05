package com.assqp.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Git-derived code churn metrics for one component/file (Phase 9), local
 * to a project. Upserted (one row per project+componentName) each time
 * CodeChurnServiceImpl.analyze() runs - re-analyzing refreshes existing
 * rows rather than accumulating duplicates.
 */
@Entity
@Table(name = "code_churn_metrics", uniqueConstraints = @UniqueConstraint(
        name = "uk_churn_project_component", columnNames = {"project_id", "component_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeChurnMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "component_name", nullable = false, length = 500)
    private String componentName;

    @Builder.Default
    private Integer commitCount = 0;

    @Builder.Default
    @Column(name = "lines_added")
    private Integer linesAdded = 0;

    @Builder.Default
    @Column(name = "lines_deleted")
    private Integer linesDeleted = 0;

    @Builder.Default
    @Column(name = "total_lines_changed")
    private Integer totalLinesChanged = 0;

    /** Commits per day over the configured lookback window. */
    @Builder.Default
    @Column(name = "change_frequency")
    private Double changeFrequency = 0.0;

    /** Recency-decay-weighted activity score - see churn.GitChurnAnalyzer for the formula. */
    @Builder.Default
    @Column(name = "recent_modification_score")
    private Double recentModificationScore = 0.0;

    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        computedAt = LocalDateTime.now();
    }
}
