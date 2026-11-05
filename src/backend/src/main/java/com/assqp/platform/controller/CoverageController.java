package com.assqp.platform.controller;

import com.assqp.platform.dto.coverage.CoverageReportResponse;
import com.assqp.platform.dto.coverage.CoverageTrendPoint;
import com.assqp.platform.service.CoverageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/coverage")
@RequiredArgsConstructor
@Tag(name = "Coverage", description = "Real test coverage imported from JaCoCo reports")
public class CoverageController {

    private final CoverageService coverageService;

    @PostMapping("/import")
    @Operation(summary = "Import the JaCoCo XML report from the backend's configured local path " +
            "(coverage.jacoco.report-path), e.g. after running `mvn verify` locally or in a CI job " +
            "sharing this filesystem")
    public ResponseEntity<CoverageReportResponse> importFromDefaultPath(@PathVariable Long projectId) {
        return ResponseEntity.ok(coverageService.importFromDefaultPath(projectId));
    }

    @PostMapping(value = "/import-file", consumes = "multipart/form-data")
    @Operation(summary = "Import a JaCoCo XML report uploaded directly, e.g. as a GitHub Actions artifact")
    public ResponseEntity<CoverageReportResponse> importFromFile(
            @PathVariable Long projectId,
            @RequestParam("report") MultipartFile report) {
        return ResponseEntity.ok(coverageService.importFromFile(projectId, report));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recently imported coverage report for a project")
    public ResponseEntity<CoverageReportResponse> getLatest(@PathVariable Long projectId) {
        return ResponseEntity.ok(coverageService.getLatest(projectId));
    }

    @GetMapping
    @Operation(summary = "Get the full coverage import history for a project")
    public ResponseEntity<List<CoverageReportResponse>> getHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(coverageService.getHistory(projectId));
    }

    @GetMapping("/trend")
    @Operation(summary = "Get line/branch/overall coverage trend points over time")
    public ResponseEntity<List<CoverageTrendPoint>> getTrend(@PathVariable Long projectId) {
        return ResponseEntity.ok(coverageService.getTrend(projectId));
    }
}
