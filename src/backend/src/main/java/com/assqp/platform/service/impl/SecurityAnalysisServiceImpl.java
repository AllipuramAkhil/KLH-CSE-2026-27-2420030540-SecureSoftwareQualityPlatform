package com.assqp.platform.service.impl;

import com.assqp.platform.analysis.SecurityScanner;
import com.assqp.platform.analysis.VulnerabilityFinding;
import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.entity.SourceCodeFile;
import com.assqp.platform.enums.Severity;
import com.assqp.platform.enums.VulnerabilitySource;
import com.assqp.platform.enums.VulnerabilityStatus;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.repository.SourceCodeFileRepository;
import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.service.SecurityAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SecurityAnalysisServiceImpl implements SecurityAnalysisService {

    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final SourceCodeFileRepository sourceCodeFileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final SecurityScanner securityScanner;
    private final FeedbackProcessor feedbackProcessor;

    @Override
    @Transactional
    public List<SecurityVulnerabilityResponse> analyze(Long projectId) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        List<SourceCodeFile> files = sourceCodeFileRepository.findByProjectId(projectId);

        // Bug fix (Phase 12 continued audit): rule-based findings represent
        // the CURRENT state of the codebase's static-analysis issues, not a
        // history log - getByProject()/getByProjectFiltered() return every
        // row for the project with no "latest run" filtering, exactly the
        // same pattern that caused RiskAssessment to silently accumulate
        // duplicates across repeated runs. Scoped to RULE_BASED only so
        // Trivy/ZAP rows (which have their own correct dedup-on-import
        // lifecycle) are never touched by a rule-based re-scan.
        vulnerabilityRepository.deleteByProjectIdAndSource(projectId, VulnerabilitySource.RULE_BASED);

        List<SecurityVulnerability> saved = files.stream()
                .flatMap(f -> scanFile(f).stream())
                .map(finding -> toEntity(project, finding))
                .map(vulnerabilityRepository::save)
                .toList();

        // Score against the FULL current set (rule-based + any existing
        // Trivy/ZAP findings), matching how TrivyServiceImpl/ZapServiceImpl
        // already compute it - not just this run's freshly-inserted subset,
        // which would under-count whenever other-sourced findings exist.
        List<SecurityVulnerability> allFindings = vulnerabilityRepository.findByProjectId(projectId);
        double securityScore = computeSecurityScore(allFindings);
        project.setSecurityScore(securityScore);
        projectRepository.save(project);

        feedbackProcessor.recordCycle(project, "SECURITY_ANALYSIS",
                "Rule-based security analysis found " + saved.size() + " findings ("
                        + allFindings.size() + " total across all sources) - combined security score now " + securityScore,
                project.getQualityScore(), securityScore, project.getRiskScore());

        return saved.stream().map(this::toResponse).toList();
    }

    @Override
    public List<SecurityVulnerabilityResponse> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return vulnerabilityRepository.findByProjectId(projectId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<SecurityVulnerabilityResponse> getByProjectFiltered(Long projectId, String severity, String status) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return vulnerabilityRepository.findByProjectId(projectId).stream()
                .filter(v -> severity == null || v.getSeverity().name().equalsIgnoreCase(severity))
                .filter(v -> status == null || v.getStatus().name().equalsIgnoreCase(status))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<SecurityVulnerabilityResponse> getLatest(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return vulnerabilityRepository.findByProjectId(projectId).stream()
                .sorted((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()))
                .limit(20)
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SecurityVulnerabilityResponse getById(Long id) {
        SecurityVulnerability vulnerability = vulnerabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vulnerability not found with id: " + id));
        projectAccessGuard.checkAccess(vulnerability.getProject()); // ownership check
        return toResponse(vulnerability);
    }

    @Override
    @Transactional
    public SecurityVulnerabilityResponse updateStatus(Long id, String status) {
        SecurityVulnerability vulnerability = vulnerabilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vulnerability not found with id: " + id));
        projectAccessGuard.checkAccess(vulnerability.getProject()); // ownership check
        try {
            vulnerability.setStatus(VulnerabilityStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid vulnerability status: " + status);
        }
        vulnerability = vulnerabilityRepository.save(vulnerability);
        return toResponse(vulnerability);
    }

    private List<VulnerabilityFinding> scanFile(SourceCodeFile file) {
        try {
            String content = Files.readString(Path.of(file.getStoragePath()));
            return securityScanner.scan(file.getFileName(), content);
        } catch (IOException ex) {
            return List.of();
        }
    }

    private double computeSecurityScore(List<SecurityVulnerability> findings) {
        double penalty = 0;
        for (SecurityVulnerability v : findings) {
            penalty += switch (v.getSeverity()) {
                case CRITICAL -> 15;
                case HIGH -> 8;
                case MEDIUM -> 4;
                case LOW -> 1.5;
                case INFO -> 0.5;
            };
        }
        return Math.max(0, Math.min(100, 100 - penalty));
    }

    private SecurityVulnerability toEntity(Project project, VulnerabilityFinding finding) {
        return SecurityVulnerability.builder()
                .project(project)
                .title(finding.getTitle())
                .category(finding.getCategory())
                .cwe(finding.getCwe())
                .owaspCategory(finding.getOwaspCategory())
                .affectedFile(finding.getAffectedFile())
                .lineNumber(finding.getLineNumber())
                .description(finding.getDescription())
                .impact(finding.getImpact())
                .recommendation(finding.getRecommendation())
                .severity(finding.getSeverity())
                .status(VulnerabilityStatus.OPEN)
                .build();
    }

    private SecurityVulnerabilityResponse toResponse(SecurityVulnerability v) {
        return SecurityVulnerabilityResponse.builder()
                .id(v.getId())
                .projectId(v.getProject().getId())
                .title(v.getTitle())
                .category(v.getCategory())
                .cwe(v.getCwe())
                .owaspCategory(v.getOwaspCategory())
                .affectedFile(v.getAffectedFile())
                .lineNumber(v.getLineNumber())
                .description(v.getDescription())
                .impact(v.getImpact())
                .recommendation(v.getRecommendation())
                .severity(v.getSeverity().name())
                .status(v.getStatus().name())
                .detectedAt(v.getDetectedAt())
                .source(v.getSource() != null ? v.getSource().name() : null)
                .externalId(v.getExternalId())
                .packageName(v.getPackageName())
                .installedVersion(v.getInstalledVersion())
                .fixedVersion(v.getFixedVersion())
                .build();
    }
}
