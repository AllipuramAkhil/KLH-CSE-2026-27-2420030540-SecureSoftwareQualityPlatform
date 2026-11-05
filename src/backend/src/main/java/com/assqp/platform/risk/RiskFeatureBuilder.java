package com.assqp.platform.risk;

import com.assqp.platform.entity.CodeChurnMetrics;
import com.assqp.platform.entity.QualityAnalysis;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.integration.ml.MlRiskRequest;
import com.assqp.platform.repository.CodeChurnMetricsRepository;
import com.assqp.platform.repository.QualityAnalysisRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the fixed 10-feature vector the ML service expects (see
 * ml-service/README.md "Feature reference") out of data ASSQP already
 * has, for one risk component at a time.
 *
 * Some features are genuinely component-specific (vulnerability count/
 * severity, historical defects, historical failure rate, and - as of
 * Phase 9 - code churn, all scoped to the vulnerabilities/tests/git
 * history tied to this component). Others (complexity, coverage, code
 * smells, duplication, technical debt) aren't tracked per-file anywhere
 * in this codebase yet, only at the whole-project level via
 * QualityAnalysis - so every component in a given project currently
 * shares those four values. That's a real limitation, not a bug:
 * per-file quality metrics would need deeper static-analysis
 * integration than exists today.
 */
@Component
@RequiredArgsConstructor
public class RiskFeatureBuilder {

    private static final double DEFAULT_CODE_CHURN = 0.0;

    private final QualityAnalysisRepository qualityAnalysisRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final CodeChurnMetricsRepository codeChurnMetricsRepository;

    public MlRiskRequest buildFeatures(Long projectId, String componentName,
                                        List<SecurityVulnerability> componentVulns,
                                        List<TestCase> componentTests) {
        QualityAnalysis latestQuality = qualityAnalysisRepository
                .findTopByProjectIdOrderByAnalyzedAtDesc(projectId)
                .orElse(null);

        Set<Long> componentTestIds = componentTests.stream().map(TestCase::getId).collect(Collectors.toSet());
        long historicalDefects = testExecutionRepository.findByTestCaseProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .filter(e -> e.getTestCase() != null && componentTestIds.contains(e.getTestCase().getId()))
                .filter(e -> e.getStatus() == ExecutionStatus.FAILED)
                .count();

        double maxSeverity = componentVulns.stream()
                .mapToDouble(v -> severityToNumeric(v.getSeverity()))
                .max().orElse(0.0);

        double avgFailureRate = componentTests.stream()
                .mapToDouble(t -> t.getHistoricalFailureRate() == null ? 0.0 : t.getHistoricalFailureRate())
                .average().orElse(0.0);

        // Phase 9: real git churn for this exact component (file path),
        // if it's been analyzed - see churn.GitChurnAnalyzer. Falls back
        // to 0 (never analyzed yet, or git history unavailable) exactly
        // as before Phase 9 landed.
        double codeChurn = codeChurnMetricsRepository.findByProjectIdAndComponentName(projectId, componentName)
                .map(CodeChurnMetrics::getRecentModificationScore)
                .orElse(DEFAULT_CODE_CHURN);

        return MlRiskRequest.builder()
                .codeComplexity(latestQuality != null && latestQuality.getCyclomaticComplexity() != null
                        ? latestQuality.getCyclomaticComplexity() : 0.0)
                .codeChurn(codeChurn)
                .historicalDefects((double) historicalDefects)
                .vulnerabilityCount((double) componentVulns.size())
                .maxVulnerabilitySeverity(maxSeverity)
                .testCoverage(latestQuality != null && latestQuality.getTestCoveragePercent() != null
                        ? latestQuality.getTestCoveragePercent() : 0.0)
                .codeSmells(latestQuality != null && latestQuality.getCodeSmells() != null
                        ? latestQuality.getCodeSmells().doubleValue() : 0.0)
                .duplication(latestQuality != null && latestQuality.getCodeDuplicationPercent() != null
                        ? latestQuality.getCodeDuplicationPercent() : 0.0)
                .historicalFailureRate(avgFailureRate)
                .technicalDebt(latestQuality != null && latestQuality.getTechnicalDebtHours() != null
                        ? latestQuality.getTechnicalDebtHours() : 0.0)
                .build();
    }

    /** Maps this platform's 5-level Severity onto the 0-10 scale the ML model was trained on. */
    private double severityToNumeric(com.assqp.platform.enums.Severity severity) {
        return switch (severity) {
            case CRITICAL -> 10.0;
            case HIGH -> 7.5;
            case MEDIUM -> 5.0;
            case LOW -> 2.5;
            case INFO -> 1.0;
        };
    }
}
