package com.assqp.platform.service.impl;

import com.assqp.platform.dto.risk.RiskAssessmentResponse;
import com.assqp.platform.dto.risk.RiskSummaryResponse;
import com.assqp.platform.entity.MlPredictionLog;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.RiskAssessment;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.enums.PredictionMethod;
import com.assqp.platform.enums.RiskLevel;
import com.assqp.platform.exception.MlServiceException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.integration.ml.MlPredictionClient;
import com.assqp.platform.integration.ml.MlRiskRequest;
import com.assqp.platform.integration.ml.MlRiskResponse;
import com.assqp.platform.repository.MlPredictionLogRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.risk.HybridRiskCalculator;
import com.assqp.platform.risk.RiskFeatureBuilder;
import com.assqp.platform.service.RiskAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes component-level risk using Risk Score = Probability x Impact,
 * where probability is derived from defect/vulnerability frequency and test
 * coverage, and impact is derived from vulnerability severity and component
 * criticality (approximated by how many test cases/vulnerabilities reference it).
 *
 * Phase 8: for each component, also calls the ML risk-prediction service
 * (if configured/reachable) via {@link RiskFeatureBuilder} +
 * {@link MlPredictionClient}, and combines the two into a final level via
 * {@link HybridRiskCalculator}. The rule-based score/level are ALWAYS
 * computed first and used as-is if the ML service is unavailable -
 * nothing about the pre-Phase-8 behavior changes in that case.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final FeedbackProcessor feedbackProcessor;
    private final MlPredictionClient mlPredictionClient;
    private final RiskFeatureBuilder riskFeatureBuilder;
    private final HybridRiskCalculator hybridRiskCalculator;
    private final MlPredictionLogRepository mlPredictionLogRepository;

    @Override
    @Transactional
    public List<RiskAssessmentResponse> analyze(Long projectId) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        List<SecurityVulnerability> vulnerabilities = vulnerabilityRepository.findByProjectId(projectId);
        List<TestCase> testCases = testCaseRepository.findByProjectId(projectId);

        Map<String, List<SecurityVulnerability>> byComponent = vulnerabilities.stream()
                .filter(v -> v.getAffectedFile() != null)
                .collect(Collectors.groupingBy(SecurityVulnerability::getAffectedFile));

        boolean mlAvailable = mlPredictionClient.isAvailable();

        List<RiskAssessment> assessments = byComponent.entrySet().stream().map(entry -> {
            String component = entry.getKey();
            List<SecurityVulnerability> componentVulns = entry.getValue();
            List<TestCase> componentTests = testCases.stream()
                    .filter(t -> component.equalsIgnoreCase(t.getRelatedComponent()))
                    .toList();

            double impact = componentVulns.stream().mapToDouble(this::severityWeight).max().orElse(0) * 20;
            long relatedTests = componentTests.size();
            double failureRate = componentTests.stream()
                    .mapToDouble(t -> t.getHistoricalFailureRate() == null ? 0 : t.getHistoricalFailureRate())
                    .average().orElse(0.3);

            double probability = Math.min(1.0, (componentVulns.size() * 0.15) + failureRate
                    + (relatedTests == 0 ? 0.2 : 0));

            double riskScore = Math.min(100, probability * impact);
            RiskLevel ruleBasedLevel = classify(riskScore);

            // --- ML prediction (Phase 8) - best-effort, never blocks analysis ---
            RiskLevel finalLevel = ruleBasedLevel;
            PredictionMethod method = PredictionMethod.RULE_BASED;
            String mlRiskLevel = null;
            Double mlConfidence = null;
            String mlModelVersion = null;

            if (mlAvailable) {
                try {
                    MlRiskRequest features = riskFeatureBuilder.buildFeatures(projectId, component, componentVulns, componentTests);
                    MlRiskResponse mlResponse = mlPredictionClient.predictRisk(features);
                    RiskLevel mlLevel = RiskLevel.valueOf(mlResponse.getRiskLevel());

                    finalLevel = hybridRiskCalculator.combine(ruleBasedLevel, mlLevel);
                    method = PredictionMethod.HYBRID;
                    mlRiskLevel = mlResponse.getRiskLevel();
                    mlConfidence = mlResponse.getConfidence();
                    mlModelVersion = mlResponse.getModelVersion();
                } catch (MlServiceException | IllegalArgumentException ex) {
                    // IllegalArgumentException covers an unrecognized riskLevel string
                    // from the ML service - treat exactly like it being unavailable.
                    log.warn("ML prediction unavailable for project {} component {}, using rule-based only: {}",
                            projectId, component, ex.getMessage());
                }
            }

            return RiskAssessment.builder()
                    .project(project)
                    .componentName(component)
                    .probability(Math.round(probability * 100.0) / 100.0)
                    .impact(Math.round(impact * 100.0) / 100.0)
                    .riskScore(Math.round(riskScore * 100.0) / 100.0)
                    .riskLevel(finalLevel)
                    .contributingFactors(componentVulns.size() + " vulnerabilities, "
                            + relatedTests + " related tests, failure rate " + Math.round(failureRate * 100) + "%")
                    .predictionMethod(method)
                    .mlRiskLevel(mlRiskLevel)
                    .mlConfidence(mlConfidence)
                    .mlModelVersion(mlModelVersion)
                    .build();
        }).toList();

        // Bug fix: RiskAssessment rows represent the project's CURRENT risk
        // state, not a history log (getByProject()/getSummary() and the
        // frontend both read every row for the project as "the" current
        // assessment, with no "latest per component" filtering). Without
        // this delete, every analyze() call appended a fresh set of rows
        // on top of the previous run's, so a project analyzed twice would
        // return duplicate/stale rows per component and getSummary()'s
        // average/distribution would silently blend old and new results.
        // Delete-then-insert in the same @Transactional so a failed
        // analysis can never leave the project with an empty risk state.
        riskAssessmentRepository.deleteByProjectId(projectId);

        List<RiskAssessment> saved = assessments.stream().map(riskAssessmentRepository::save).toList();

        // Phase 10: log one row per component the ML service actually
        // responded for - this is what GET /api/ml/prediction-history reads.
        saved.stream()
                .filter(a -> a.getPredictionMethod() == PredictionMethod.HYBRID)
                .forEach(a -> mlPredictionLogRepository.save(MlPredictionLog.builder()
                        .project(project)
                        .componentName(a.getComponentName())
                        .predictionMethod(a.getPredictionMethod())
                        .ruleBasedRiskLevel(classify(a.getRiskScore()).name())
                        .mlRiskLevel(a.getMlRiskLevel())
                        .finalRiskLevel(a.getRiskLevel().name())
                        .mlConfidence(a.getMlConfidence())
                        .mlModelVersion(a.getMlModelVersion())
                        .build()));

        double overallRisk = saved.stream().mapToDouble(RiskAssessment::getRiskScore).average().orElse(0);
        project.setRiskScore(Math.round(overallRisk * 100.0) / 100.0);
        projectRepository.save(project);

        long hybridCount = saved.stream().filter(a -> a.getPredictionMethod() == PredictionMethod.HYBRID).count();
        feedbackProcessor.recordCycle(project, "RISK_ASSESSMENT",
                "Risk assessment completed for " + saved.size() + " components ("
                        + hybridCount + " hybrid ML+rule-based, " + (saved.size() - hybridCount) + " rule-based only)",
                project.getQualityScore(), project.getSecurityScore(), project.getRiskScore());

        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    public List<RiskAssessmentResponse> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public RiskAssessmentResponse getById(Long id) {
        RiskAssessment assessment = riskAssessmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Risk assessment not found with id: " + id));
        projectAccessGuard.checkAccess(assessment.getProject()); // ownership check
        return toResponse(assessment);
    }

    @Override
    public RiskSummaryResponse getSummary(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        List<RiskAssessment> assessments = riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId);

        double overall = assessments.stream().mapToDouble(RiskAssessment::getRiskScore).average().orElse(0);
        Map<String, Long> distribution = new HashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            distribution.put(level.name(), assessments.stream().filter(a -> a.getRiskLevel() == level).count());
        }
        String topRisky = assessments.stream()
                .max((a, b) -> Double.compare(a.getRiskScore(), b.getRiskScore()))
                .map(RiskAssessment::getComponentName)
                .orElse(null);

        return RiskSummaryResponse.builder()
                .projectId(projectId)
                .overallRiskScore(Math.round(overall * 100.0) / 100.0)
                .overallRiskLevel(classify(overall).name())
                .riskDistribution(distribution)
                .topRiskyComponent(topRisky)
                .build();
    }

    private double severityWeight(SecurityVulnerability v) {
        return switch (v.getSeverity()) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
        };
    }

    private RiskLevel classify(double score) {
        if (score >= 75) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    private RiskAssessmentResponse toResponse(RiskAssessment r) {
        return RiskAssessmentResponse.builder()
                .id(r.getId())
                .projectId(r.getProject().getId())
                .componentName(r.getComponentName())
                .probability(r.getProbability())
                .impact(r.getImpact())
                .riskScore(r.getRiskScore())
                .riskLevel(r.getRiskLevel().name())
                .contributingFactors(r.getContributingFactors())
                .assessedAt(r.getAssessedAt())
                .predictionMethod(r.getPredictionMethod() != null ? r.getPredictionMethod().name() : null)
                .mlRiskLevel(r.getMlRiskLevel())
                .mlConfidence(r.getMlConfidence())
                .mlModelVersion(r.getMlModelVersion())
                .build();
    }
}
