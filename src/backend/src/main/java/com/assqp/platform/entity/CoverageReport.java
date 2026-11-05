package com.assqp.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single imported code-coverage snapshot (currently sourced from JaCoCo,
 * see {@link com.assqp.platform.coverage.JacocoReportParser}). Kept as its
 * own history table - separate from {@link QualityAnalysis} - because
 * coverage is produced by an external build/test run (JaCoCo instruments
 * the actual Maven test execution), not by the on-demand source-file
 * analysis that QualityAnalysis represents. Multiple rows per project give
 * the coverage trend the frontend renders.
 */
@Entity
@Table(name = "coverage_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "line_coverage_percent", nullable = false)
    private Double lineCoveragePercent;

    @Column(name = "branch_coverage_percent", nullable = false)
    private Double branchCoveragePercent;

    @Column(name = "instruction_coverage_percent", nullable = false)
    private Double instructionCoveragePercent;

    @Column(name = "overall_coverage_percent", nullable = false)
    private Double overallCoveragePercent;

    @Column(name = "lines_covered")
    private Integer linesCovered;

    @Column(name = "lines_missed")
    private Integer linesMissed;

    @Column(name = "branches_covered")
    private Integer branchesCovered;

    @Column(name = "branches_missed")
    private Integer branchesMissed;

    @Column(name = "classes_covered")
    private Integer classesCovered;

    @Column(name = "classes_missed")
    private Integer classesMissed;

    /** e.g. "JACOCO". Kept as a plain string so future sources (e.g. a
     *  different coverage tool) don't require a schema migration. */
    @Column(name = "report_source", nullable = false, length = 30)
    @Builder.Default
    private String reportSource = "JACOCO";

    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
