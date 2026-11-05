package com.assqp.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quality_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    private Integer linesOfCode;
    private Integer numberOfMethods;
    private Double cyclomaticComplexity;
    private Double codeDuplicationPercent;
    private Double maintainabilityIndex;
    private Integer codeSmells;
    private Integer potentialBugs;
    private Double testCoveragePercent;
    private Double technicalDebtHours;

    @Column(nullable = false)
    private Double overallQualityScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}
