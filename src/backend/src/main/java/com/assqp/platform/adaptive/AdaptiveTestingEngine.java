package com.assqp.platform.adaptive;

import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Core adaptive testing orchestrator. Combines historical execution data,
 * component risk, and related vulnerability severity into a prioritized,
 * recommended test execution order. This is the primary differentiator
 * of the ASSQP platform described in the project proposal.
 */
@Component
@RequiredArgsConstructor
public class AdaptiveTestingEngine {

    private final TestCaseRepository testCaseRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final SecurityVulnerabilityRepository securityVulnerabilityRepository;
    private final TestPriorityCalculator priorityCalculator;
    private final RiskBasedPrioritizer riskBasedPrioritizer;
    private final TestSelectionEngine testSelectionEngine;

    public List<AdaptiveTestRecommendation> prioritize(Long projectId) {
        List<TestCase> testCases = testCaseRepository.findByProjectId(projectId);
        List<SecurityVulnerability> vulnerabilities = securityVulnerabilityRepository.findByProjectId(projectId);

        List<AdaptiveTestRecommendation> recommendations = testCases.stream().map(testCase -> {
            List<TestExecution> recentExecutions = testExecutionRepository
                    .findByTestCaseIdOrderByCreatedAtDesc(testCase.getId());

            double componentRisk = riskBasedPrioritizer.resolveComponentRisk(projectId, testCase.getRelatedComponent());

            Severity highestSeverity = vulnerabilities.stream()
                    .filter(v -> testCase.getRelatedComponent() != null
                            && testCase.getRelatedComponent().equalsIgnoreCase(v.getAffectedFile()))
                    .map(SecurityVulnerability::getSeverity)
                    .min(Comparator.comparingInt(this::severityRank))
                    .orElse(null);

            boolean recentlyModified = testCase.getUpdatedAt() != null
                    && testCase.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(14));

            double score = priorityCalculator.calculateScore(
                    testCase, recentExecutions, componentRisk, highestSeverity, recentlyModified);

            String riskLevel = priorityCalculator.classifyRiskLevel(score);
            String reason = priorityCalculator.buildReason(testCase, componentRisk, highestSeverity, recentlyModified);

            return AdaptiveTestRecommendation.builder()
                    .testCaseId(testCase.getId())
                    .title(testCase.getTitle())
                    .priorityScore(score)
                    .riskLevel(riskLevel)
                    .reason(reason)
                    .build();
        }).toList();

        return testSelectionEngine.orderByPriority(recommendations);
    }

    private int severityRank(Severity severity) {
        return switch (severity) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
            case INFO -> 4;
        };
    }
}
