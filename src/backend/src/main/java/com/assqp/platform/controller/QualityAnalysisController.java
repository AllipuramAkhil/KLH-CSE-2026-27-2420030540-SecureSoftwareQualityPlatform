package com.assqp.platform.controller;

import com.assqp.platform.dto.analysis.QualityAnalysisResponse;
import com.assqp.platform.service.QualityAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/quality")
@RequiredArgsConstructor
@Tag(name = "Quality", description = "Automated software quality analysis")
public class QualityAnalysisController {

    private final QualityAnalysisService qualityAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "Run rule-based quality analysis on the project's uploaded source files")
    public ResponseEntity<QualityAnalysisResponse> analyze(@PathVariable Long projectId) {
        return ResponseEntity.ok(qualityAnalysisService.analyze(projectId));
    }

    @GetMapping
    @Operation(summary = "Get the full quality analysis history for a project")
    public ResponseEntity<List<QualityAnalysisResponse>> getHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(qualityAnalysisService.getHistory(projectId));
    }

    @GetMapping("/latest")
    @Operation(summary = "Get the most recent quality analysis. Returns 404 if none has run yet.")
    public ResponseEntity<QualityAnalysisResponse> getLatest(@PathVariable Long projectId) {
        return ResponseEntity.ok(qualityAnalysisService.getLatest(projectId));
    }

    @GetMapping("/{analysisId}")
    @Operation(summary = "Get one quality analysis run by id")
    public ResponseEntity<QualityAnalysisResponse> getById(@PathVariable Long projectId, @PathVariable Long analysisId) {
        return ResponseEntity.ok(qualityAnalysisService.getById(analysisId));
    }
}
