package com.assqp.platform.service.impl;

import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.dto.sonarqube.SonarQubeAnalysisResponse;
import com.assqp.platform.dto.sonarqube.SonarQubeStatusResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SonarQubeAnalysis;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.integration.sonarqube.SonarQubeClient;
import com.assqp.platform.integration.sonarqube.SonarQubeMeasures;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.SonarQubeAnalysisRepository;
import com.assqp.platform.service.SonarQubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Real SonarQube integration (Phase 4). Additive and optional: the
 * existing RuleBasedQualityAnalyzer (see QualityAnalysisServiceImpl)
 * continues to run unconditionally and is unaffected by this service -
 * SonarQube data is surfaced alongside it, never in place of it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SonarQubeServiceImpl implements SonarQubeService {

    private final SonarQubeAnalysisRepository sonarQubeAnalysisRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final SonarQubeClient sonarQubeClient;
    private final FeedbackProcessor feedbackProcessor;

    @Value("${sonar.host.url:}")
    private String hostUrl;

    @Value("${sonar.project.key:}")
    private String defaultProjectKey;

    @Override
    public SonarQubeStatusResponse getStatus() {
        return SonarQubeStatusResponse.builder()
                .configured(sonarQubeClient.isAvailable())
                .hostUrl(blankToNull(hostUrl))
                .defaultProjectKey(blankToNull(defaultProjectKey))
                .build();
    }

    @Override
    @Transactional
    public SonarQubeAnalysisResponse importLatest(Long projectId, String sonarProjectKeyOverride) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        String projectKey = (sonarProjectKeyOverride != null && !sonarProjectKeyOverride.isBlank())
                ? sonarProjectKeyOverride : defaultProjectKey;

        // Let SonarQubeException propagate as-is (handled by GlobalExceptionHandler
        // -> 503 with a clear message) rather than swallowing it here - the
        // rule-based analyzer's own endpoints are entirely separate and keep working.
        SonarQubeMeasures measures = sonarQubeClient.fetchMeasures(projectKey);

        SonarQubeAnalysis analysis = SonarQubeAnalysis.builder()
                .project(project)
                .sonarProjectKey(projectKey)
                .bugs(measures.getBugs())
                .vulnerabilities(measures.getVulnerabilities())
                .codeSmells(measures.getCodeSmells())
                .technicalDebtMinutes(measures.getTechnicalDebtMinutes())
                .complexity(measures.getComplexity())
                .maintainabilityRating(measures.getMaintainabilityRating())
                .reliabilityRating(measures.getReliabilityRating())
                .securityRating(measures.getSecurityRating())
                .coveragePercent(measures.getCoveragePercent())
                .duplicatedLinesDensity(measures.getDuplicatedLinesDensity())
                .linesOfCode(measures.getLinesOfCode())
                .rawMeasuresJson(measures.getRawJson())
                .build();

        analysis = sonarQubeAnalysisRepository.save(analysis);

        feedbackProcessor.recordCycle(project, "SONARQUBE_IMPORT",
                "SonarQube analysis imported for " + projectKey + ": " +
                        measures.getBugs() + " bugs, " + measures.getVulnerabilities() + " vulnerabilities, " +
                        measures.getCodeSmells() + " code smells",
                project.getQualityScore(), project.getSecurityScore(), project.getRiskScore());

        log.info("Imported SonarQube analysis for project {} (Sonar key {})", projectId, projectKey);

        return toResponse(analysis);
    }

    @Override
    public SonarQubeAnalysisResponse getLatest(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return sonarQubeAnalysisRepository.findTopByProjectIdOrderByImportedAtDesc(projectId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No SonarQube analysis has been imported yet for project " + projectId));
    }

    @Override
    public List<SonarQubeAnalysisResponse> getHistory(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return sonarQubeAnalysisRepository.findByProjectIdOrderByImportedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private SonarQubeAnalysisResponse toResponse(SonarQubeAnalysis a) {
        return SonarQubeAnalysisResponse.builder()
                .id(a.getId())
                .projectId(a.getProject().getId())
                .sonarProjectKey(a.getSonarProjectKey())
                .bugs(a.getBugs())
                .vulnerabilities(a.getVulnerabilities())
                .codeSmells(a.getCodeSmells())
                .technicalDebtMinutes(a.getTechnicalDebtMinutes())
                .complexity(a.getComplexity())
                .maintainabilityRating(a.getMaintainabilityRating())
                .reliabilityRating(a.getReliabilityRating())
                .securityRating(a.getSecurityRating())
                .coveragePercent(a.getCoveragePercent())
                .duplicatedLinesDensity(a.getDuplicatedLinesDensity())
                .linesOfCode(a.getLinesOfCode())
                .importedAt(a.getImportedAt())
                .build();
    }
}
