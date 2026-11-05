package com.assqp.platform.service.impl;

import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.zap.ZapImportResultResponse;
import com.assqp.platform.dto.zap.ZapScanStatusResponse;
import com.assqp.platform.dto.zap.ZapScanTriggerResponse;
import com.assqp.platform.dto.zap.ZapStatusResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.enums.VulnerabilitySource;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.integration.zap.ZapAlert;
import com.assqp.platform.integration.zap.ZapClient;
import com.assqp.platform.integration.zap.ZapReportParser;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.service.ZapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Real OWASP ZAP integration (Phase 6). Findings are stored in the SAME
 * security_vulnerabilities table as RuleBasedSecurityScanner/Trivy output -
 * distinguished by source=ZAP - so the existing /api/projects/{id}/security*
 * endpoints automatically return the combined view. Neither the rule-based
 * scanner nor TrivyServiceImpl is touched.
 *
 * Safety: triggerBaselineScan rejects any target that isn't on the
 * zap.allowed-targets allow-list BEFORE any request reaches ZAP or the
 * network. Only a spider (crawl) + ZAP's automatic passive scan run here -
 * never an active/attack scan.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZapServiceImpl implements ZapService {

    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final ZapClient zapClient;
    private final ZapReportParser zapReportParser;
    private final FeedbackProcessor feedbackProcessor;

    @Override
    public ZapStatusResponse getStatus() {
        return ZapStatusResponse.builder()
                .apiAvailable(zapClient.isAvailable())
                .allowedTargets(zapClient.getAllowedTargets())
                .build();
    }

    @Override
    public ZapScanTriggerResponse triggerBaselineScan(Long projectId, String targetUrl) {
        getProject(projectId); // 404s early if the project doesn't exist

        if (targetUrl == null || targetUrl.isBlank()) {
            throw new BadRequestException("targetUrl is required to trigger a ZAP scan");
        }
        if (!zapClient.isTargetAllowed(targetUrl)) {
            throw new BadRequestException(
                    "'" + targetUrl + "' is not on the configured ZAP scan allow-list " +
                            "(ZAP_ALLOWED_TARGETS). Only pre-approved targets can be scanned - " +
                            "add it to that env var if this target is intentional.");
        }

        String scanId = zapClient.startSpiderScan(targetUrl);
        log.info("Started ZAP spider scan {} for project {} target {}", scanId, projectId, targetUrl);

        return ZapScanTriggerResponse.builder()
                .scanId(scanId)
                .targetUrl(targetUrl)
                .status("STARTED")
                .build();
    }

    @Override
    public ZapScanStatusResponse getScanStatus(String scanId) {
        if (scanId == null || scanId.isBlank()) {
            throw new BadRequestException("scanId is required");
        }
        int percent = zapClient.getSpiderStatus(scanId);
        return ZapScanStatusResponse.builder()
                .scanId(scanId)
                .progressPercent(percent)
                .complete(percent >= 100)
                .build();
    }

    @Override
    @Transactional
    public ZapImportResultResponse collectResults(Long projectId, String targetUrl) {
        Project project = getProject(projectId);
        if (targetUrl == null || targetUrl.isBlank()) {
            throw new BadRequestException("targetUrl is required to collect ZAP results");
        }
        if (!zapClient.isTargetAllowed(targetUrl)) {
            throw new BadRequestException(
                    "'" + targetUrl + "' is not on the configured ZAP scan allow-list (ZAP_ALLOWED_TARGETS).");
        }

        String rawJson = zapClient.fetchAlertsJson(targetUrl);
        List<ZapAlert> alerts = zapReportParser.parse(rawJson);
        return persist(project, alerts);
    }

    @Override
    @Transactional
    public ZapImportResultResponse importFromFile(Long projectId, MultipartFile file) {
        Project project = getProject(projectId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded ZAP report file must not be empty");
        }
        try {
            List<ZapAlert> alerts = zapReportParser.parse(file.getInputStream());
            return persist(project, alerts);
        } catch (IOException ex) {
            throw new BadRequestException("Could not read uploaded ZAP report: " + ex.getMessage());
        }
    }

    @Override
    public List<SecurityVulnerabilityResponse> getFindings(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return vulnerabilityRepository.findByProjectIdAndSource(projectId, VulnerabilitySource.ZAP)
                .stream()
                .sorted((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()))
                .map(this::toResponse)
                .toList();
    }

    private ZapImportResultResponse persist(Project project, List<ZapAlert> alerts) {
        List<SecurityVulnerability> newlyStored = new ArrayList<>();
        int duplicates = 0;

        for (ZapAlert alert : alerts) {
            boolean alreadyExists = vulnerabilityRepository
                    .existsByProjectIdAndSourceAndExternalIdAndPackageNameAndAffectedFile(
                            project.getId(), VulnerabilitySource.ZAP, alert.getPluginId(), null, alert.getUrl());
            if (alreadyExists) {
                duplicates++;
                continue;
            }

            SecurityVulnerability entity = SecurityVulnerability.builder()
                    .project(project)
                    .title(alert.getName())
                    .category("Dynamic Application Security (DAST)")
                    .cwe(alert.getCweId())
                    .affectedFile(alert.getUrl())
                    .description(alert.getDescription())
                    .recommendation(alert.getSolution())
                    .severity(alert.getSeverity())
                    .source(VulnerabilitySource.ZAP)
                    .externalId(alert.getPluginId())
                    .confidence(alert.getConfidence())
                    .build();

            newlyStored.add(vulnerabilityRepository.save(entity));
        }

        // Recompute the project's security score from the FULL current set
        // (rule-based + Trivy + ZAP), same as TrivyServiceImpl does.
        List<SecurityVulnerability> allFindings = vulnerabilityRepository.findByProjectId(project.getId());
        double securityScore = computeSecurityScore(allFindings);
        project.setSecurityScore(securityScore);
        projectRepository.save(project);

        feedbackProcessor.recordCycle(project, "ZAP_SCAN",
                "ZAP scan imported " + newlyStored.size() + " new finding(s), " +
                        duplicates + " already known - combined security score now " + securityScore,
                project.getQualityScore(), securityScore, project.getRiskScore());

        log.info("ZAP import for project {}: {} parsed, {} new, {} duplicates skipped",
                project.getId(), alerts.size(), newlyStored.size(), duplicates);

        return ZapImportResultResponse.builder()
                .totalFindingsParsed(alerts.size())
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
                .confidence(v.getConfidence())
                .build();
    }
}
