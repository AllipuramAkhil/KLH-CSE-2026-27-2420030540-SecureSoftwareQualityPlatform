package com.assqp.platform.adaptive;

import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.enums.TestType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestPriorityCalculatorTest {

    private final TestPriorityCalculator calculator = new TestPriorityCalculator();

    @Test
    void calculateScore_highFailureRateAndCriticalVulnerability_yieldsCriticalPriority() {
        TestCase testCase = TestCase.builder()
                .title("Login flow test")
                .testType(TestType.SECURITY)
                .historicalFailureRate(0.9)
                .updatedAt(LocalDateTime.now())
                .build();

        TestExecution failedExecution = TestExecution.builder()
                .status(ExecutionStatus.FAILED)
                .build();

        double score = calculator.calculateScore(testCase, List.of(failedExecution), 80.0, Severity.CRITICAL, true);

        assertThat(score).isGreaterThan(0.7);
        assertThat(calculator.classifyRiskLevel(score)).isIn("HIGH", "CRITICAL");
    }

    @Test
    void calculateScore_stablePassingTest_yieldsLowPriority() {
        TestCase testCase = TestCase.builder()
                .title("Basic getter test")
                .testType(TestType.UNIT)
                .historicalFailureRate(0.0)
                .lastExecution(LocalDateTime.now())
                .updatedAt(LocalDateTime.now().minusDays(60))
                .build();

        TestExecution passedExecution = TestExecution.builder()
                .status(ExecutionStatus.PASSED)
                .build();

        double score = calculator.calculateScore(testCase, List.of(passedExecution), 5.0, null, false);

        assertThat(score).isLessThan(0.5);
    }

    @Test
    void calculateScore_returnsNormalizedRange() {
        TestCase testCase = TestCase.builder()
                .title("Edge case test")
                .testType(TestType.FUNCTIONAL)
                .build();

        double score = calculator.calculateScore(testCase, List.of(), 50.0, Severity.MEDIUM, false);

        assertThat(score).isBetween(0.0, 1.0);
    }
}
