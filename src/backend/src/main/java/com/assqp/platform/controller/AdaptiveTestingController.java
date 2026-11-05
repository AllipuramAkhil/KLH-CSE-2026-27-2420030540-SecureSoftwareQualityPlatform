package com.assqp.platform.controller;

import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import com.assqp.platform.entity.AnalysisHistory;
import com.assqp.platform.service.AdaptiveTestingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core adaptive testing endpoints. Delegates all scoring logic to
 * AdaptiveTestingEngine (which internally composes RiskBasedPrioritizer,
 * TestSelectionEngine, and TestPriorityCalculator) and records feedback
 * cycles via FeedbackProcessor - no scoring logic lives in this controller.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/adaptive-testing")
@RequiredArgsConstructor
@Tag(name = "Adaptive Testing", description = "AI-driven adaptive test prioritization")
public class AdaptiveTestingController {

    private final AdaptiveTestingService adaptiveTestingService;

    @PostMapping("/prioritize")
    @Operation(summary = "Compute a prioritized, recommended test execution order for the project")
    public ResponseEntity<Map<String, Object>> prioritize(@PathVariable Long projectId) {
        return ResponseEntity.ok(buildResponse(projectId, adaptiveTestingService.prioritize(projectId)));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get the current adaptive test prioritization recommendations")
    public ResponseEntity<Map<String, Object>> recommendations(@PathVariable Long projectId) {
        return ResponseEntity.ok(buildResponse(projectId, adaptiveTestingService.getRecommendations(projectId)));
    }

    @PostMapping("/feedback")
    @Operation(summary = "Submit feedback from a completed testing cycle to inform future prioritization")
    public ResponseEntity<Void> submitFeedback(@PathVariable Long projectId, @RequestBody Map<String, String> body) {
        adaptiveTestingService.submitFeedback(projectId, body.getOrDefault("summary", "Manual feedback submitted"));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/history")
    @Operation(summary = "Get the historical feedback/analysis cycles used to inform adaptive decisions")
    public ResponseEntity<List<Map<String, Object>>> history(@PathVariable Long projectId) {
        List<Map<String, Object>> history = adaptiveTestingService.getHistory(projectId).stream()
                .map(this::toHistoryMap)
                .toList();
        return ResponseEntity.ok(history);
    }

    private Map<String, Object> buildResponse(Long projectId, List<AdaptiveTestRecommendation> recommendations) {
        List<Map<String, Object>> recommendedTests = new java.util.ArrayList<>();
        int order = 1;
        for (AdaptiveTestRecommendation rec : recommendations) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("testCaseId", rec.getTestCaseId());
            entry.put("title", rec.getTitle());
            entry.put("priorityScore", rec.getPriorityScore());
            entry.put("riskLevel", rec.getRiskLevel());
            entry.put("recommendedOrder", order++);
            entry.put("reason", rec.getReason());
            recommendedTests.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("projectId", projectId);
        response.put("totalTests", recommendations.size());
        response.put("recommendedTests", recommendedTests);
        return response;
    }

    private Map<String, Object> toHistoryMap(AnalysisHistory h) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", h.getId());
        map.put("cycleType", h.getCycleType());
        map.put("summary", h.getSummary());
        map.put("qualityScore", h.getQualityScore());
        map.put("securityScore", h.getSecurityScore());
        map.put("riskScore", h.getRiskScore());
        map.put("recordedAt", h.getRecordedAt());
        return map;
    }
}
