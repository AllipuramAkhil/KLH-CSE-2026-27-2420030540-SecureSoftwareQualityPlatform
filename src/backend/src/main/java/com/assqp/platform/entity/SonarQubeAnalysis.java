package com.assqp.platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A single imported SonarQube analysis snapshot for a project. Kept
 * separate from {@link QualityAnalysis} (the RuleBasedQualityAnalyzer's
 * output) - SonarQube is a real, optional, external integration; the
 * rule-based analyzer remains the always-available fallback/supplement,
 * per project requirements. Multiple rows per project give a history/trend.
 */
@Entity
@Table(name = "sonarqube_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SonarQubeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "sonar_project_key", nullable = false, length = 200)
    private String sonarProjectKey;

    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;

    /** SonarQube reports technical debt as "sqale_index", in minutes. */
    @Column(name = "technical_debt_minutes")
    private Long technicalDebtMinutes;

    private Double complexity;

    /** Sonar letter ratings 1.0-5.0 (A-E), stored as a number since Sonar's API returns them that way. */
    @Column(name = "maintainability_rating")
    private Double maintainabilityRating;

    @Column(name = "reliability_rating")
    private Double reliabilityRating;

    @Column(name = "security_rating")
    private Double securityRating;

    @Column(name = "coverage_percent")
    private Double coveragePercent;

    @Column(name = "duplicated_lines_density")
    private Double duplicatedLinesDensity;

    @Column(name = "lines_of_code")
    private Long linesOfCode;

    /** Raw JSON of the measures response, kept for troubleshooting/audit - not surfaced in the DTO by default. */
    @Lob
    @Column(name = "raw_measures_json")
    private String rawMeasuresJson;

    @Column(nullable = false, updatable = false)
    private LocalDateTime importedAt;

    @PrePersist
    protected void onCreate() {
        importedAt = LocalDateTime.now();
    }
}
