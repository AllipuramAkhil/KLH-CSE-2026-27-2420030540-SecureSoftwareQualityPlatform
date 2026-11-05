package com.assqp.platform.controller;

import com.assqp.platform.dto.ai.AiAnalysisRequest;
import com.assqp.platform.dto.ai.AiAnalysisResponse;
import com.assqp.platform.dto.ai.AiGenerateTestsRequest;
import com.assqp.platform.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * All AI-driven endpoints. This controller never calls an external AI
 * provider directly - it only calls AiAnalysisService, which in turn uses
 * the AI engines (TestCaseGenerator, DefectPredictionEngine,
 * VulnerabilityClassificationEngine, RiskPredictionEngine). Those engines
 * automatically fall back to rule-based analysis if the AI provider is
 * unavailable, so these endpoints remain functional even without an
 * AI_API_KEY configured.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "AI-driven analysis with rule-based fallback")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "Run AI-assisted analysis over the project's latest quality/security results")
    public ResponseEntity<AiAnalysisResponse> analyze(@PathVariable Long projectId, @RequestBody AiAnalysisRequest request) {
        return ResponseEntity.ok(aiAnalysisService.analyze(projectId, request));
    }

    @PostMapping("/generate-tests")
    @Operation(summary = "Generate test case titles for a component or source snippet")
    public ResponseEntity<List<String>> generateTests(@PathVariable Long projectId, @RequestBody AiGenerateTestsRequest request) {
        return ResponseEntity.ok(aiAnalysisService.generateTests(projectId, request));
    }

    @PostMapping("/recommendations")
    @Operation(summary = "Get AI-driven risk-based recommendations for the project")
    public ResponseEntity<AiAnalysisResponse> recommendations(@PathVariable Long projectId) {
        return ResponseEntity.ok(aiAnalysisService.recommendations(projectId));
    }

    @PostMapping("/explain-defect")
    @Operation(summary = "Explain the root cause and fix for a specific quality analysis result")
    public ResponseEntity<AiAnalysisResponse> explainDefect(@PathVariable Long projectId, @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(aiAnalysisService.explainDefect(projectId, body.get("qualityAnalysisId")));
    }

    @PostMapping("/explain-vulnerability")
    @Operation(summary = "Explain the exploitability and remediation for a specific vulnerability")
    public ResponseEntity<AiAnalysisResponse> explainVulnerability(@PathVariable Long projectId, @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(aiAnalysisService.explainVulnerability(projectId, body.get("vulnerabilityId")));
    }
}
