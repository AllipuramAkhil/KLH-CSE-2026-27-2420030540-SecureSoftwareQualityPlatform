package com.assqp.platform.service.impl;

import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.dto.dashboard.DashboardResponse;
import com.assqp.platform.entity.CoverageReport;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.RiskAssessment;
import com.assqp.platform.entity.SonarQubeAnalysis;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.enums.RiskLevel;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.*;
import com.assqp.platform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AnalysisHistoryRepository analysisHistoryRepository;
    private final com.assqp.platform.repository.CoverageReportRepository coverageReportRepository;
    private final com.assqp.platform.repository.SonarQubeAnalysisRepository sonarQubeAnalysisRepository;
    private final com.assqp.platform.repository.CodeChurnMetricsRepository codeChurnMetricsRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public DashboardResponse getDashboard(Long projectId) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        List<TestExecution> executions = testExecutionRepository.findByTestCaseProjectIdOrderByCreatedAtDesc(projectId);
        long totalTests = testCaseRepository.findByProjectId(projectId).size();
        long passed = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.PASSED).count();
        long failed = executions.stream().filter(e -> e.getStatus() == ExecutionStatus.FAILED || e.getStatus() == ExecutionStatus.ERROR).count();
        double coverage = totalTests == 0 ? 0 : Math.min(100.0, (executions.size() * 100.0) / Math.max(1, totalTests));

        long totalVulns = vulnerabilityRepository.findByProjectId(projectId).size();
        long critical = vulnerabilityRepository.countByProjectIdAndSeverity(projectId, Severity.CRITICAL);
        long high = vulnerabilityRepository.countByProjectIdAndSeverity(projectId, Severity.HIGH);

        List<RiskAssessment> risks = riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId);
        Map<String, Long> riskDistribution = new HashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            riskDistribution.put(level.name(), risks.stream().filter(r -> r.getRiskLevel() == level).count());
        }

        List<String> mostRisky = risks.stream()
                .sorted((a, b) -> Double.compare(b.getRiskScore(), a.getRiskScore()))
                .limit(5)
                .map(RiskAssessment::getComponentName)
                .toList();

        List<String> recentAnalyses = analysisHistoryRepository.findByProjectIdOrderByRecordedAtDesc(projectId)
                .stream().limit(5).map(h -> h.getCycleType() + ": " + h.getSummary()).toList();

        // Real JaCoCo coverage, if any has been imported (Phase 3). Left
        // null/false rather than throwing so the dashboard still renders
        // fully for projects that haven't imported a coverage report yet.
        Optional<CoverageReport> latestCoverage = coverageReportRepository.findTopByProjectIdOrderByImportedAtDesc(projectId);

        // Real SonarQube analysis, if any has been imported (Phase 4). Same
        // graceful-absence pattern as coverage above - null/false, never a
        // failure, when SonarQube isn't configured or nothing's been imported yet.
        Optional<SonarQubeAnalysis> latestSonar = sonarQubeAnalysisRepository.findTopByProjectIdOrderByImportedAtDesc(projectId);

        // Git code churn (Phase 9) - empty list (not null/error) when the
        // repo isn't readable or hasn't been analyzed for this project yet.
        List<com.assqp.platform.entity.CodeChurnMetrics> churnMetrics =
                codeChurnMetricsRepository.findByProjectIdOrderByTotalLinesChangedDesc(projectId);
        List<String> mostChurned = churnMetrics.stream()
                .limit(5)
                .map(com.assqp.platform.entity.CodeChurnMetrics::getComponentName)
                .toList();

        List<Map<String, Object>> trends = executions.stream()
                .collect(Collectors.groupingBy(e -> e.getCreatedAt().format(FORMATTER), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", e.getKey());
                    point.put("executions", e.getValue());
                    return point;
                }).toList();

        return DashboardResponse.builder()
                .projectId(projectId)
                .totalTests(totalTests)
                .passedTests(passed)
                .failedTests(failed)
                .testCoverage(Math.round(coverage * 100.0) / 100.0)
                .totalVulnerabilities(totalVulns)
                .criticalVulnerabilities(critical)
                .highVulnerabilities(high)
                .openDefects(failed)
                .qualityScore(project.getQualityScore())
                .securityScore(project.getSecurityScore())
                .riskScore(project.getRiskScore())
                .recentAnalyses(recentAnalyses)
                .riskDistribution(riskDistribution)
                .testExecutionTrends(trends)
                .mostRiskyComponents(mostRisky)
                .realCoverageAvailable(latestCoverage.isPresent())
                .jacocoLineCoveragePercent(latestCoverage.map(CoverageReport::getLineCoveragePercent).orElse(null))
                .jacocoBranchCoveragePercent(latestCoverage.map(CoverageReport::getBranchCoveragePercent).orElse(null))
                .jacocoOverallCoveragePercent(latestCoverage.map(CoverageReport::getOverallCoveragePercent).orElse(null))
                .coverageImportedAt(latestCoverage.map(CoverageReport::getImportedAt).orElse(null))
                .sonarQubeAvailable(latestSonar.isPresent())
                .sonarBugs(latestSonar.map(SonarQubeAnalysis::getBugs).orElse(null))
                .sonarVulnerabilities(latestSonar.map(SonarQubeAnalysis::getVulnerabilities).orElse(null))
                .sonarCodeSmells(latestSonar.map(SonarQubeAnalysis::getCodeSmells).orElse(null))
                .sonarTechnicalDebtMinutes(latestSonar.map(SonarQubeAnalysis::getTechnicalDebtMinutes).orElse(null))
                .sonarMaintainabilityRating(latestSonar.map(SonarQubeAnalysis::getMaintainabilityRating).orElse(null))
                .sonarReliabilityRating(latestSonar.map(SonarQubeAnalysis::getReliabilityRating).orElse(null))
                .sonarSecurityRating(latestSonar.map(SonarQubeAnalysis::getSecurityRating).orElse(null))
                .sonarCoveragePercent(latestSonar.map(SonarQubeAnalysis::getCoveragePercent).orElse(null))
                .sonarDuplicatedLinesDensity(latestSonar.map(SonarQubeAnalysis::getDuplicatedLinesDensity).orElse(null))
                .sonarAnalyzedAt(latestSonar.map(SonarQubeAnalysis::getImportedAt).orElse(null))
                .codeChurnAvailable(!churnMetrics.isEmpty())
                .mostChurnedComponents(mostChurned)
                .build();
    }
}
