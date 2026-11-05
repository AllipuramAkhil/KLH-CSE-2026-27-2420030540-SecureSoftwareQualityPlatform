package com.assqp.platform.controller;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;
import com.assqp.platform.dto.trivy.TrivyScanResultResponse;
import com.assqp.platform.dto.trivy.TrivyStatusResponse;
import com.assqp.platform.service.TrivyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Trivy", description = "Real Trivy security scanning (filesystem/dependency, container image, secrets) - additive to the built-in rule-based security scanner")
public class TrivyController {

    private final TrivyService trivyService;

    @GetMapping("/api/trivy/status")
    @Operation(summary = "Check whether the Trivy CLI is available on this backend")
    public ResponseEntity<TrivyStatusResponse> getStatus() {
        return ResponseEntity.ok(trivyService.getStatus());
    }

    @PostMapping("/api/projects/{projectId}/trivy/scan/filesystem")
    @Operation(summary = "Run `trivy fs` locally against the configured target path and import findings. " +
            "Returns 503 if the Trivy CLI isn't available on this backend - use /import-file instead.")
    public ResponseEntity<TrivyScanResultResponse> scanFilesystem(
            @PathVariable Long projectId,
            @RequestParam(value = "targetPath", required = false) String targetPath) {
        return ResponseEntity.ok(trivyService.triggerFilesystemScan(projectId, targetPath));
    }

    @PostMapping("/api/projects/{projectId}/trivy/scan/image")
    @Operation(summary = "Run `trivy image <imageRef>` locally and import findings. " +
            "Returns 503 if the Trivy CLI isn't available on this backend - use /import-file instead.")
    public ResponseEntity<TrivyScanResultResponse> scanImage(
            @PathVariable Long projectId,
            @RequestParam("imageRef") String imageRef) {
        return ResponseEntity.ok(trivyService.triggerImageScan(projectId, imageRef));
    }

    @PostMapping(value = "/api/projects/{projectId}/trivy/import-file", consumes = "multipart/form-data")
    @Operation(summary = "Import a Trivy JSON report uploaded directly, e.g. downloaded from the " +
            "GitHub Actions pipeline's trivy-filesystem-report or trivy-image-report artifact. " +
            "Always works, regardless of whether the CLI is installed on this backend.")
    public ResponseEntity<TrivyScanResultResponse> importFromFile(
            @PathVariable Long projectId,
            @RequestParam("report") MultipartFile report) {
        return ResponseEntity.ok(trivyService.importFromFile(projectId, report));
    }

    @GetMapping("/api/projects/{projectId}/trivy")
    @Operation(summary = "Get all Trivy-sourced findings for a project. " +
            "For the combined view (rule-based + Trivy), use GET /api/projects/{id}/security instead.")
    public ResponseEntity<List<SecurityVulnerabilityResponse>> getFindings(@PathVariable Long projectId) {
        return ResponseEntity.ok(trivyService.getFindings(projectId));
    }
}
