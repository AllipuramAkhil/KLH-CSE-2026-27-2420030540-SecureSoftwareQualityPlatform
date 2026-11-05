package com.assqp.platform.service.impl;

import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.trivy.TrivyScanResultResponse;
import com.assqp.platform.dto.trivy.TrivyStatusResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.enums.VulnerabilitySource;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.integration.trivy.TrivyClient;
import com.assqp.platform.integration.trivy.TrivyFinding;
import com.assqp.platform.integration.trivy.TrivyReportParser;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.service.TrivyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real Trivy integration (Phase 5). Findings are stored in the SAME
 * security_vulnerabilities table as RuleBasedSecurityScanner output -
 * distinguished by source=TRIVY - so the existing
 * /api/projects/{id}/security* endpoints automatically return a combined
 * view. RuleBasedSecurityScanner itself is completely untouched.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrivyServiceImpl implements TrivyService {

    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final TrivyClient trivyClient;
    private final TrivyReportParser trivyReportParser;
    private final FeedbackProcessor feedbackProcessor;

    @Value("${trivy.binary-path:trivy}")
    private String binaryPath;

    @Override
    public TrivyStatusResponse getStatus() {
        return TrivyStatusResponse.builder()
                .cliAvailable(trivyClient.isAvailable())
                .binaryPath(binaryPath)
                .build();
    }

    @Override
    @Transactional
    public TrivyScanResultResponse triggerFilesystemScan(Long projectId, String targetPathOverride) {
        Project project = getProject(projectId);
        String rawJson = trivyClient.scanFilesystem(targetPathOverride);
        List<TrivyFinding> findings = trivyReportParser.parse(rawJson);
        return persist(project, findings);
    }

    @Override
    @Transactional
    public TrivyScanResultResponse triggerImageScan(Long projectId, String imageRef) {
        Project project = getProject(projectId);
        String rawJson = trivyClient.scanImage(imageRef);
        List<TrivyFinding> findings = trivyReportParser.parse(rawJson);
        return persist(project, findings);
    }

    @Override
    @Transactional
    public TrivyScanResultResponse importFromFile(Long projectId, MultipartFile file) {
        Project project = getProject(projectId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded Trivy report file must not be empty");
        }
        try {
            List<TrivyFinding> findings = trivyReportParser.parse(file.getInputStream());
            return persist(project, findings);
        } catch (IOException ex) {
            throw new BadRequestException("Could not read uploaded Trivy report: " + ex.getMessage());
        }
    }

    @Override
    public List<SecurityVulnerabilityResponse> getFindings(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return vulnerabilityRepository.findByProjectIdAndSource(projectId, VulnerabilitySource.TRIVY)
                .stream()
                .sorted((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()))
                .map(this::toResponse)
                .toList();
    }

    private TrivyScanResultResponse persist(Project project, List<TrivyFinding> findings) {
        List<SecurityVulnerability> newlyStored = new ArrayList<>();
        int duplicates = 0;

        for (TrivyFinding finding : findings) {
            String target = finding.getTarget();
            boolean alreadyExists = vulnerabilityRepository
                    .existsByProjectIdAndSourceAndExternalIdAndPackageNameAndAffectedFile(
                            project.getId(), VulnerabilitySource.TRIVY,
                            finding.getVulnerabilityId(), finding.getPackageName(), target);
            if (alreadyExists) {
                duplicates++;
                continue;
            }

            SecurityVulnerability entity = SecurityVulnerability.builder()
                    .project(project)
                    .title(finding.getTitle())
                    .category(finding.isSecretFinding() ? "Hardcoded Secret" : "Dependency Vulnerability")
                    .affectedFile(target)
                    .description(finding.getDescription())
                    .recommendation(finding.getFixedVersion() != null
                            ? "Upgrade " + finding.getPackageName() + " to " + finding.getFixedVersion() + " or later"
                            : (finding.isSecretFinding() ? "Rotate the exposed secret and remove it from source control"
                            : "No fixed version published yet - monitor for updates"))
                    .severity(finding.getSeverity())
                    .source(VulnerabilitySource.TRIVY)
                    .externalId(finding.getVulnerabilityId())
                    .packageName(finding.getPackageName())
                    .installedVersion(finding.getInstalledVersion())
                    .fixedVersion(finding.getFixedVersion())
                    .build();

            newlyStored.add(vulnerabilityRepository.save(entity));
        }

        // Recompute the project's security score from the FULL current set of
        // findings (rule-based + Trivy + any future ZAP/Sonar-sourced ones),
        // not just this batch - this is what "combine" means for the score,
        // not only for the findings list.
        List<SecurityVulnerability> allFindings = vulnerabilityRepository.findByProjectId(project.getId());
        double securityScore = computeSecurityScore(allFindings);
        project.setSecurityScore(securityScore);
        projectRepository.save(project);

        feedbackProcessor.recordCycle(project, "TRIVY_SCAN",
                "Trivy scan imported " + newlyStored.size() + " new finding(s), " +
                        duplicates + " already known - combined security score now " + securityScore,
                project.getQualityScore(), securityScore, project.getRiskScore());

        log.info("Trivy import for project {}: {} parsed, {} new, {} duplicates skipped",
                project.getId(), findings.size(), newlyStored.size(), duplicates);

        return TrivyScanResultResponse.builder()
                .totalFindingsParsed(findings.size())
                .newFindingsStored(newlyStored.size())
                .duplicatesSkipped(duplicates)
                .newFindings(newlyStored.stream().map(this::toResponse).toList())
                .build();
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

    private Project getProject(Long projectId) {
        return projectAccessGuard.getAccessibleProject(projectId);
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
