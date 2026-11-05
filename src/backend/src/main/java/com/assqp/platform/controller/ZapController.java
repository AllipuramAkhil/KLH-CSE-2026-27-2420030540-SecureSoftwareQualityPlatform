package com.assqp.platform.controller;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.zap.ZapImportResultResponse;
import com.assqp.platform.dto.zap.ZapScanStatusResponse;
import com.assqp.platform.dto.zap.ZapScanTriggerResponse;
import com.assqp.platform.dto.zap.ZapStatusResponse;
import com.assqp.platform.service.ZapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "OWASP ZAP", description = "Dynamic Application Security Testing (baseline: spider + passive scan only) - " +
        "additive to the built-in rule-based security scanner and Trivy. Live scans only run against " +
        "pre-approved targets (ZAP_ALLOWED_TARGETS); anything else is rejected before it reaches ZAP.")
public class ZapController {

    private final ZapService zapService;

    @GetMapping("/api/zap/status")
    @Operation(summary = "Check whether a ZAP daemon is configured, and which targets are approved for scanning")
    public ResponseEntity<ZapStatusResponse> getStatus() {
        return ResponseEntity.ok(zapService.getStatus());
    }

    @PostMapping("/api/projects/{projectId}/zap/scan/baseline")
    @Operation(summary = "Start a baseline scan (spider crawl + ZAP's automatic passive scan, no active attacks) " +
            "against targetUrl. Rejected with 400 if targetUrl isn't on the configured allow-list, or 503 if no " +
            "ZAP daemon is configured - use /import-file instead in that case.")
    public ResponseEntity<ZapScanTriggerResponse> triggerBaselineScan(
            @PathVariable Long projectId,
            @RequestParam("targetUrl") String targetUrl) {
        return ResponseEntity.ok(zapService.triggerBaselineScan(projectId, targetUrl));
    }

    @GetMapping("/api/zap/scan/{scanId}/status")
    @Operation(summary = "Check spider crawl progress (0-100%) for a scan started via /scan/baseline")
    public ResponseEntity<ZapScanStatusResponse> getScanStatus(@PathVariable String scanId) {
        return ResponseEntity.ok(zapService.getScanStatus(scanId));
    }

    @PostMapping("/api/projects/{projectId}/zap/scan/results")
    @Operation(summary = "Pull ZAP's currently-accumulated alerts for targetUrl and import new ones. Safe to call " +
            "while a scan is still in progress - passive-scan alerts accumulate continuously as the spider crawls.")
    public ResponseEntity<ZapImportResultResponse> collectResults(
            @PathVariable Long projectId,
            @RequestParam("targetUrl") String targetUrl) {
        return ResponseEntity.ok(zapService.collectResults(projectId, targetUrl));
    }

    @PostMapping(value = "/api/projects/{projectId}/zap/import-file", consumes = "multipart/form-data")
    @Operation(summary = "Import a ZAP JSON report uploaded directly (live API 'alerts' shape, or a " +
            "zap-baseline.py -J report, e.g. from the GitHub Actions pipeline). Always works, regardless of " +
            "whether a live ZAP daemon is configured on this backend.")
    public ResponseEntity<ZapImportResultResponse> importFromFile(
            @PathVariable Long projectId,
            @RequestParam("report") MultipartFile report) {
        return ResponseEntity.ok(zapService.importFromFile(projectId, report));
    }

    @GetMapping("/api/projects/{projectId}/zap")
    @Operation(summary = "Get all ZAP-sourced findings for a project. For the combined view " +
            "(rule-based + Trivy + ZAP), use GET /api/projects/{id}/security instead.")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> getFindings(@PathVariable Long projectId) {
        return ResponseEntity.ok(zapService.getFindings(projectId));
    }
}
