package com.assqp.platform.service;

import com.assqp.platform.analysis.RuleBasedSecurityScanner;
import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.SourceCodeFile;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.enums.VulnerabilityStatus;
import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.SourceCodeFileRepository;
import com.assqp.platform.service.impl.SecurityAnalysisServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAnalysisServiceImplTest {

    @Mock
    private SecurityVulnerabilityRepository vulnerabilityRepository;
    @Mock
    private SourceCodeFileRepository sourceCodeFileRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private FeedbackProcessor feedbackProcessor;
    // The service's actual dependency is the SecurityScanner interface -
    // @InjectMocks only wires @Mock/@Spy fields, so without this the
    // service's securityScanner stays null and analyze() NPEs the moment
    // it calls securityScanner.scan(...). The plain `scanner` field below
    // is used directly by ruleBasedScanner_detectsMultipleSeverities,
    // which deliberately tests the real scanner in isolation rather than
    // through the service.
    @Mock
    private com.assqp.platform.analysis.SecurityScanner securityScanner;

    private final RuleBasedSecurityScanner scanner = new RuleBasedSecurityScanner();

    @InjectMocks
    private SecurityAnalysisServiceImpl securityAnalysisService;

    @Test
    void updateStatus_validStatus_updatesVulnerability() {
        SecurityVulnerability vuln = SecurityVulnerability.builder()
                .id(1L).title("Hardcoded Secret").severity(Severity.CRITICAL)
                .status(VulnerabilityStatus.OPEN)
                .project(Project.builder().id(1L).build())
                .build();

        when(vulnerabilityRepository.findById(1L)).thenReturn(Optional.of(vuln));
        when(vulnerabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecurityVulnerabilityResponse response = securityAnalysisService.updateStatus(1L, "RESOLVED");

        assertThat(response.getStatus()).isEqualTo("RESOLVED");
    }

    @Test
    void updateStatus_invalidStatus_throwsBadRequest() {
        SecurityVulnerability vuln = SecurityVulnerability.builder()
                .id(1L).status(VulnerabilityStatus.OPEN)
                .project(Project.builder().id(1L).build())
                .build();
        when(vulnerabilityRepository.findById(1L)).thenReturn(Optional.of(vuln));

        assertThatThrownBy(() -> securityAnalysisService.updateStatus(1L, "NOT_A_REAL_STATUS"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(vulnerabilityRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityAnalysisService.getById(404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ruleBasedScanner_detectsMultipleSeverities() {
        String code = "String pw = \"secret123\"; Runtime.getRuntime().exec(cmd);";
        var findings = scanner.scan("Sample.java", code);
        assertThat(findings).extracting("severity").contains(Severity.CRITICAL);
    }

    // --- Bug-fix coverage (Phase 12 continued audit): the same
    // accumulate-instead-of-refresh bug found in RiskAssessment also
    // existed here - analyze() never cleared prior RULE_BASED rows before
    // inserting new ones, so getByProject()/getByProjectFiltered() (no
    // "latest run" filtering) would return duplicates of every real
    // finding after a second scan. ---

    @Test
    void analyze_deletesExistingRuleBasedRowsBeforeSavingNewOnes() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        SourceCodeFile file = SourceCodeFile.builder().fileName("Sample.java").storagePath("unused").build();
        when(sourceCodeFileRepository.findByProjectId(1L)).thenReturn(List.of(file));
        when(securityScanner.scan(any(), any())).thenReturn(List.of(
                com.assqp.platform.analysis.VulnerabilityFinding.builder()
                        .title("Hardcoded Secret").severity(Severity.CRITICAL).affectedFile("Sample.java").build()));
        when(vulnerabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of());

        securityAnalysisService.analyze(1L);

        InOrder inOrder = inOrder(vulnerabilityRepository);
        inOrder.verify(vulnerabilityRepository)
                .deleteByProjectIdAndSource(1L, com.assqp.platform.enums.VulnerabilitySource.RULE_BASED);
        inOrder.verify(vulnerabilityRepository, atLeastOnce()).save(any());
    }

    @Test
    void analyze_calledTwice_refreshesRuleBasedRowsEachTime() {
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        SourceCodeFile file = SourceCodeFile.builder().fileName("Sample.java").storagePath("unused").build();
        when(sourceCodeFileRepository.findByProjectId(1L)).thenReturn(List.of(file));
        when(securityScanner.scan(any(), any())).thenReturn(List.of(
                com.assqp.platform.analysis.VulnerabilityFinding.builder()
                        .title("Hardcoded Secret").severity(Severity.CRITICAL).affectedFile("Sample.java").build()));
        when(vulnerabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of());

        securityAnalysisService.analyze(1L);
        securityAnalysisService.analyze(1L);

        // A delete must precede EVERY analyze() call, not just the first -
        // otherwise the second run's findings would be inserted on top of
        // the first run's instead of replacing them.
        verify(vulnerabilityRepository, times(2))
                .deleteByProjectIdAndSource(1L, com.assqp.platform.enums.VulnerabilitySource.RULE_BASED);
    }

    @Test
    void analyze_scoresAgainstFullCombinedSetNotJustThisRunsFindings() {
        // Mirrors how TrivyServiceImpl/ZapServiceImpl already compute the
        // score, so whichever scan type runs last doesn't under-count
        // findings from the other sources.
        Project project = Project.builder().id(1L).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        SourceCodeFile file = SourceCodeFile.builder().fileName("Sample.java").storagePath("unused").build();
        when(sourceCodeFileRepository.findByProjectId(1L)).thenReturn(List.of(file));
        when(securityScanner.scan(any(), any())).thenReturn(List.of());
        when(vulnerabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SecurityVulnerability existingTrivyFinding = SecurityVulnerability.builder()
                .severity(Severity.CRITICAL).source(com.assqp.platform.enums.VulnerabilitySource.TRIVY).build();
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of(existingTrivyFinding));

        securityAnalysisService.analyze(1L);

        // 100 - 15 (CRITICAL penalty) = 85, from the Trivy finding alone
        // even though this run found zero rule-based issues of its own.
        assertThat(project.getSecurityScore()).isEqualTo(85.0);
    }
}
