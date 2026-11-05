package com.assqp.platform.dto.dashboard;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private Long projectId;
    private long totalTests;
    private long passedTests;
    private long failedTests;
    private Double testCoverage;
    private long totalVulnerabilities;
    private long criticalVulnerabilities;
    private long highVulnerabilities;
    private long openDefects;
    private Double qualityScore;
    private Double securityScore;
    private Double riskScore;
    private List<String> recentAnalyses;
    private Map<String, Long> riskDistribution;
    private List<Map<String, Object>> testExecutionTrends;
    private List<String> mostRiskyComponents;

    // --- Real JaCoCo coverage (Phase 3) - additive, null/false when no
    // report has been imported yet for this project. Does not replace
    // `testCoverage` above, which remains the existing execution-ratio figure.
    private Boolean realCoverageAvailable;
    private Double jacocoLineCoveragePercent;
    private Double jacocoBranchCoveragePercent;
    private Double jacocoOverallCoveragePercent;
    private java.time.LocalDateTime coverageImportedAt;

    // --- Real SonarQube analysis (Phase 4) - additive, null/false when not
    // configured or nothing has been imported yet. The rule-based quality
    // score/analysis above is entirely unaffected either way.
    private Boolean sonarQubeAvailable;
    private Integer sonarBugs;
    private Integer sonarVulnerabilities;
    private Integer sonarCodeSmells;
    private Long sonarTechnicalDebtMinutes;
    private Double sonarMaintainabilityRating;
    private Double sonarReliabilityRating;
    private Double sonarSecurityRating;
    private Double sonarCoveragePercent;
    private Double sonarDuplicatedLinesDensity;
    private java.time.LocalDateTime sonarAnalyzedAt;

    // --- Git code churn (Phase 9) - additive, null/false when the
    // configured local repo isn't readable or hasn't been analyzed yet
    // for this project. Nothing above is affected either way.
    private Boolean codeChurnAvailable;
    private List<String> mostChurnedComponents;
}
