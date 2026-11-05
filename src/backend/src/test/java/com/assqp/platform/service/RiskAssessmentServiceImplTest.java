package com.assqp.platform.service;

import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.dto.risk.RiskAssessmentResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.TestCase;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.enums.TestType;
import com.assqp.platform.integration.ml.MlPredictionClient;
import com.assqp.platform.repository.MlPredictionLogRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.TestCaseRepository;
import com.assqp.platform.risk.HybridRiskCalculator;
import com.assqp.platform.risk.RiskFeatureBuilder;
import com.assqp.platform.service.impl.RiskAssessmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceImplTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private SecurityVulnerabilityRepository vulnerabilityRepository;
    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private FeedbackProcessor feedbackProcessor;
    // Phase 8/10 dependencies - not exercised by these tests since
    // mlPredictionClient.isAvailable() is stubbed false below, matching
    // the pre-Phase-8 rule-based-only behavior these tests were written
    // for. Still required: RiskAssessmentServiceImpl's constructor needs
    // all of them, and @InjectMocks silently passes null for any
    // constructor param with no matching @Mock, which would NPE the
    // instant analyze() calls mlPredictionClient.isAvailable().
    @Mock
    private MlPredictionClient mlPredictionClient;
    @Mock
    private RiskFeatureBuilder riskFeatureBuilder;
    @Mock
    private HybridRiskCalculator hybridRiskCalculator;
    @Mock
    private MlPredictionLogRepository mlPredictionLogRepository;

    @InjectMocks
    private RiskAssessmentServiceImpl riskAssessmentService;

    @Test
    void analyze_criticalVulnerabilityWithNoTests_yieldsHighRiskScore() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(mlPredictionClient.isAvailable()).thenReturn(false);

        SecurityVulnerability critical = SecurityVulnerability.builder()
                .affectedFile("PaymentService.java").severity(Severity.CRITICAL).build();
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(critical));
        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of());
        when(riskAssessmentRepository.save(any())).thenAnswer(inv -> {
            var r = inv.getArgument(0, com.assqp.platform.entity.RiskAssessment.class);
            r.setId(1L);
            return r;
        });

        List<RiskAssessmentResponse> results = riskAssessmentService.analyze(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getComponentName()).isEqualTo("PaymentService.java");
        assertThat(results.get(0).getRiskScore()).isGreaterThan(0);
        assertThat(results.get(0).getRiskLevel()).isNotNull();
        assertThat(results.get(0).getPredictionMethod()).isEqualTo("RULE_BASED");
    }

    @Test
    void analyze_wellTestedLowSeverityComponent_yieldsLowerRiskThanUntested() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(mlPredictionClient.isAvailable()).thenReturn(false);

        SecurityVulnerability lowSeverity = SecurityVulnerability.builder()
                .affectedFile("Util.java").severity(Severity.LOW).build();
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(lowSeverity));

        TestCase relatedTest = TestCase.builder().testType(TestType.UNIT)
                .relatedComponent("Util.java").historicalFailureRate(0.0).build();
        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of(relatedTest));
        when(riskAssessmentRepository.save(any())).thenAnswer(inv -> {
            var r = inv.getArgument(0, com.assqp.platform.entity.RiskAssessment.class);
            r.setId(2L);
            return r;
        });

        List<RiskAssessmentResponse> results = riskAssessmentService.analyze(1L);

        assertThat(results.get(0).getRiskLevel()).isEqualTo("LOW");
    }

    // --- Bug-fix coverage: RiskAssessment rows must represent CURRENT
    // state, not accumulated history. Before this fix, analyze() never
    // deleted prior rows, so getByProject()/getSummary() (and the
    // frontend, which reads every returned row as "the" current
    // assessment) would silently blend stale rows from earlier runs with
    // the new ones after any repeat analysis. ---

    @Test
    void analyze_deletesExistingRowsForProjectBeforeSavingNewOnes() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(mlPredictionClient.isAvailable()).thenReturn(false);

        SecurityVulnerability vuln = SecurityVulnerability.builder()
                .affectedFile("Util.java").severity(Severity.LOW).build();
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(vuln));
        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of());
        when(riskAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        riskAssessmentService.analyze(1L);

        // The delete must happen, and specifically BEFORE any save - not
        // just at some point during the method.
        InOrder inOrder = inOrder(riskAssessmentRepository);
        inOrder.verify(riskAssessmentRepository).deleteByProjectId(1L);
        inOrder.verify(riskAssessmentRepository, atLeastOnce()).save(any());
    }

    @Test
    void analyze_calledTwice_refreshesRowsEachTimeInsteadOfAccumulating() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(mlPredictionClient.isAvailable()).thenReturn(false);

        SecurityVulnerability vuln = SecurityVulnerability.builder()
                .affectedFile("Util.java").severity(Severity.LOW).build();
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(vuln));
        when(testCaseRepository.findByProjectId(1L)).thenReturn(List.of());
        when(riskAssessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        riskAssessmentService.analyze(1L);
        riskAssessmentService.analyze(1L);

        // One component -> one row per call, but critically: a delete for
        // the project must precede EVERY analyze() call, not just the
        // first - otherwise the second run's inserts would land on top of
        // the first run's rows instead of replacing them.
        verify(riskAssessmentRepository, times(2)).deleteByProjectId(1L);
        verify(riskAssessmentRepository, times(2)).save(any());
    }

    @Test
    void getByProject_readsOnlyCurrentRowsFromRepository() {
        // getByProject() has no filtering logic of its own - it trusts the
        // repository to return "the" current set for the project, which is
        // exactly why analyze() deleting stale rows first matters: this
        // method has no way to distinguish current vs. historical rows.
        var current = com.assqp.platform.entity.RiskAssessment.builder()
                .id(5L).project(Project.builder().id(1L).build())
                .componentName("Util.java").riskScore(10.0)
                .riskLevel(com.assqp.platform.enums.RiskLevel.LOW)
                .build();
        when(riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(1L)).thenReturn(List.of(current));

        List<RiskAssessmentResponse> results = riskAssessmentService.getByProject(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(5L);
    }
}
