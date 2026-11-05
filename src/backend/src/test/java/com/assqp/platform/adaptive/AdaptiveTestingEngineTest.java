package com.assqp.platform.adaptive;

import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.entity.TestExecution;
import com.assqp.platform.enums.ExecutionStatus;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.enums.TestType;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.repository.TestExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies that the AdaptiveTestingEngine produces a deterministic priority
 * ordering that accounts for historical failure rate, component risk, and
 * associated vulnerability severity - the core differentiator of ASSQP.
 */
@ExtendWith(MockitoExtension.class)
class AdaptiveTestingEngineTest {

    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private TestExecutionRepository testExecutionRepository;
    @Mock
    private SecurityVulnerabilityRepository securityVulnerabilityRepository;
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    private final TestPriorityCalculator priorityCalculator = new TestPriorityCalculator();
    private RiskBasedPrioritizer riskBasedPrioritizer;
    private final TestSelectionEngine testSelectionEngine = new TestSelectionEngine();

    private AdaptiveTestingEngine engine;

    private void initRiskPrioritizer() {
        this.riskBasedPrioritizer = new RiskBasedPrioritizer(riskAssessmentRepository);
    }

    @Test
    void prioritize_ordersHighRiskTestsFirst_deterministically() {
        initRiskPrioritizer();
        org.mockito.Mockito.lenient().when(riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(1L)).thenReturn(List.of());

        Project project = Project.builder().id(1L).build();

        TestCase lowRiskTest = TestCase.builder()
                .id(1L).title("Stable getter test").testType(TestType.UNIT)
                .project(project).relatedComponent("UtilComponent")
                .historicalFailureRate(0.0).lastExecution(LocalDateTime.now())
                .updatedAt(LocalDateTime.now().minusDays(90))
                .build();

        TestCase highRiskTest = TestCase.builder()
                .id(2L).title("Payment flow test").testType(TestType.SECURITY)
                .project(project).relatedComponent("PaymentController")
                .historicalFailureRate(0.85).updatedAt(LocalDateTime.now())
                .build();

        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of(lowRiskTest, highRiskTest));

        TestExecution failedExecution = TestExecution.builder().status(ExecutionStatus.FAILED).build();
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(failedExecution));
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        SecurityVulnerability criticalVuln = SecurityVulnerability.builder()
                .affectedFile("PaymentController").severity(Severity.CRITICAL).build();
        when(securityVulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(criticalVuln));

        engine = new AdaptiveTestingEngine(testCaseRepository, testExecutionRepository,
                securityVulnerabilityRepository, priorityCalculator, riskBasedPrioritizer, testSelectionEngine);

        List<AdaptiveTestRecommendation> result = engine.prioritize(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getTestCaseId()).isEqualTo(2L);
        assertThat(result.get(0).getRiskLevel()).isIn("HIGH", "CRITICAL");
        assertThat(result.get(0).getPriorityScore()).isGreaterThan(result.get(1).getPriorityScore());
    }

    @Test
    void prioritize_sameInputs_producesSameOrderTwice() {
        initRiskPrioritizer();
        org.mockito.Mockito.lenient().when(riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(1L)).thenReturn(List.of());

        Project project = Project.builder().id(1L).build();
        TestCase t1 = TestCase.builder().id(1L).title("A").testType(TestType.UNIT).project(project)
                .historicalFailureRate(0.4).build();
        TestCase t2 = TestCase.builder().id(2L).title("B").testType(TestType.UNIT).project(project)
                .historicalFailureRate(0.1).build();

        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of(t1, t2));
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(testExecutionRepository.findByTestCaseIdOrderByCreatedAtDesc(2L)).thenReturn(List.of());
        when(securityVulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of());

        engine = new AdaptiveTestingEngine(testCaseRepository, testExecutionRepository,
                securityVulnerabilityRepository, priorityCalculator, riskBasedPrioritizer, testSelectionEngine);

        List<AdaptiveTestRecommendation> firstRun = engine.prioritize(1L);
        List<AdaptiveTestRecommendation> secondRun = engine.prioritize(1L);

        assertThat(firstRun.stream().map(AdaptiveTestRecommendation::getTestCaseId).toList())
                .isEqualTo(secondRun.stream().map(AdaptiveTestRecommendation::getTestCaseId).toList());
    }

}
