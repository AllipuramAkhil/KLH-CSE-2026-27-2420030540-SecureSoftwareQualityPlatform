package com.assqp.platform.adaptive;

import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.enums.Severity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes a normalized 0.0 - 1.0 priority score for a test case, based on:
 * historical failure rate, code change recency, defect density, related
 * vulnerability severity, component risk, previous execution result, and
 * test coverage / staleness.
 */
@Component
public class TestPriorityCalculator {

    public double calculateScore(TestCase testCase,
                                  List<TestExecution> recentExecutions,
                                  double componentRiskScore,
                                  Severity highestRelatedVulnerabilitySeverity,
                                  boolean recentlyModified) {

        double failureRateScore = normalize(testCase.getHistoricalFailureRate() == null ? 0 : testCase.getHistoricalFailureRate(), 0, 1);

        double lastResultScore = lastExecutionScore(recentExecutions);

        double staleScore = stalenessScore(testCase.getLastExecution());

        double vulnerabilityScore = severityToScore(highestRelatedVulnerabilitySeverity);

        double riskScore = normalize(componentRiskScore, 0, 100);

        double changeScore = recentlyModified ? 1.0 : 0.2;

        // Weighted combination - weights sum to 1.0
        double score = (failureRateScore * 0.25)
                + (lastResultScore * 0.20)
                + (riskScore * 0.20)
                + (vulnerabilityScore * 0.15)
                + (changeScore * 0.12)
                + (staleScore * 0.08);

        return round(Math.max(0.0, Math.min(1.0, score)));
    }

    public String classifyRiskLevel(double score) {
        if (score >= 0.75) return "CRITICAL";
        if (score >= 0.5) return "HIGH";
        if (score >= 0.25) return "MEDIUM";
        return "LOW";
    }

    public String buildReason(TestCase testCase, double componentRiskScore,
                               Severity highestSeverity, boolean recentlyModified) {
        StringBuilder sb = new StringBuilder();
        if (testCase.getHistoricalFailureRate() != null && testCase.getHistoricalFailureRate() > 0.3) {
            sb.append("High historical failure rate; ");
        }
        if (recentlyModified) {
            sb.append("recent code changes in related component; ");
        }
        if (highestSeverity == Severity.CRITICAL || highestSeverity == Severity.HIGH) {
            sb.append("associated with ").append(highestSeverity).append(" severity vulnerability; ");
        }
        if (componentRiskScore > 60) {
            sb.append("component risk score is elevated (").append(componentRiskScore).append("); ");
        }
        if (sb.isEmpty()) {
            sb.append("Standard regression priority based on baseline coverage and stability.");
        }
        return sb.toString().trim();
    }

    private double lastExecutionScore(List<TestExecution> recentExecutions) {
        if (recentExecutions == null || recentExecutions.isEmpty()) {
            return 0.5; // unknown -> medium priority to establish a baseline
        }
        TestExecution latest = recentExecutions.get(0);
        if (latest.getStatus() == ExecutionStatus.FAILED || latest.getStatus() == ExecutionStatus.ERROR) {
            return 1.0;
        }
        if (latest.getStatus() == ExecutionStatus.BLOCKED) {
            return 0.7;
        }
        return 0.15;
    }

    private double stalenessScore(LocalDateTime lastExecution) {
        if (lastExecution == null) {
            return 1.0;
        }
        long daysSince = ChronoUnit.DAYS.between(lastExecution, LocalDateTime.now());
        if (daysSince > 30) return 1.0;
        if (daysSince > 14) return 0.6;
        if (daysSince > 7) return 0.3;
        return 0.1;
    }

    private double severityToScore(Severity severity) {
        if (severity == null) return 0.0;
        return switch (severity) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
            case INFO -> 0.1;
        };
    }

    private double normalize(double value, double min, double max) {
        if (max <= min) return 0;
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
