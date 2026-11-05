package com.assqp.platform.controller;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.analysis.VulnerabilityStatusUpdateRequest;
import com.assqp.platform.service.SecurityAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Security", description = "Automated vulnerability detection and classification")
public class SecurityAnalysisController {

    private final SecurityAnalysisService securityAnalysisService;

    @PostMapping("/api/projects/{projectId}/security/analyze")
    @Operation(summary = "Run rule-based security scanning on the project's uploaded source files")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> analyze(@PathVariable Long projectId) {
        return ResponseEntity.ok(securityAnalysisService.analyze(projectId));
    }

    @GetMapping("/api/projects/{projectId}/security")
    @Operation(summary = "Get all vulnerabilities for a project (all sources combined: rule-based, Trivy, ZAP)")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(securityAnalysisService.getByProject(projectId));
    }

    @GetMapping("/api/projects/{projectId}/security/latest")
    @Operation(summary = "Get vulnerabilities from the most recent rule-based scan only")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> getLatest(@PathVariable Long projectId) {
        return ResponseEntity.ok(securityAnalysisService.getLatest(projectId));
    }

    @GetMapping("/api/projects/{projectId}/security/vulnerabilities")
    @Operation(summary = "List vulnerabilities for a project, optionally filtered by severity and/or status")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> getVulnerabilities(
            @PathVariable Long projectId,
            @Parameter(description = "Filter by severity: CRITICAL, HIGH, MEDIUM, LOW, INFO")
            @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by status: OPEN, IN_PROGRESS, RESOLVED, FALSE_POSITIVE, ACCEPTED_RISK")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(securityAnalysisService.getByProjectFiltered(projectId, severity, status));
    }

    @GetMapping("/api/vulnerabilities/{id}")
    @Operation(summary = "Get one vulnerability by id")
    public ResponseEntity<SecurityVulnerabilityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(securityAnalysisService.getById(id));
    }

    @PatchMapping("/api/vulnerabilities/{id}/status")
    @Operation(summary = "Update a vulnerability's triage status (e.g. mark resolved, false positive, accepted risk)")
    public ResponseEntity<SecurityVulnerabilityResponse> updateStatus(@PathVariable Long id,
                                                                        @Valid @RequestBody VulnerabilityStatusUpdateRequest request) {
        return ResponseEntity.ok(securityAnalysisService.updateStatus(id, request.getStatus()));
    }
}
