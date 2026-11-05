package com.assqp.platform.controller;

import com.assqp.platform.dto.ml.ModelInfoResponse;
import com.assqp.platform.dto.ml.ModelMetricsResponse;
import com.assqp.platform.dto.ml.PredictionHistoryEntry;
import com.assqp.platform.dto.ml.RetrainResponse;
import com.assqp.platform.dto.risk.DirectRiskPredictionRequest;
import com.assqp.platform.dto.risk.DirectRiskPredictionResponse;
import com.assqp.platform.entity.MlPredictionLog;
import com.assqp.platform.exception.MlServiceException;
import com.assqp.platform.integration.ml.MlPredictionClient;
import com.assqp.platform.integration.ml.MlRiskRequest;
import com.assqp.platform.integration.ml.MlRiskResponse;
import com.assqp.platform.repository.MlPredictionLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "ML Risk Prediction", description = "Direct access to the standalone ML risk-prediction service " +
        "(Phase 8) plus basic MLOps (Phase 10: model version/metrics, retraining, prediction history). For " +
        "hybrid (ML + rule-based) per-component risk in context, use POST /api/projects/{id}/risk/analyze instead.")
public class MlPredictionController {

    private final MlPredictionClient mlPredictionClient;
    private final MlPredictionLogRepository mlPredictionLogRepository;

    @GetMapping("/api/ml/status")
    @Operation(summary = "Check whether the ML risk-prediction service is configured")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(Map.of("available", mlPredictionClient.isAvailable()));
    }

    @PostMapping("/api/ml/predict-risk")
    @Operation(summary = "Send a raw 10-feature vector directly to the ML service and get its prediction back. " +
            "Returns 503 if the ML service isn't configured or unreachable - this endpoint does NOT fall back " +
            "to the rule-based engine (it has no project/component context to do so); use the project-scoped " +
            "risk analysis endpoint for a guaranteed result.")
    public ResponseEntity<DirectRiskPredictionResponse> predictRisk(@Valid @RequestBody DirectRiskPredictionRequest request) {
        if (!mlPredictionClient.isAvailable()) {
            throw new MlServiceException(
                    "ML risk prediction service is not configured (ML_SERVICE_URL missing).");
        }

        MlRiskRequest mlRequest = MlRiskRequest.builder()
                .codeComplexity(request.getCodeComplexity())
                .codeChurn(request.getCodeChurn())
                .historicalDefects(request.getHistoricalDefects())
                .vulnerabilityCount(request.getVulnerabilityCount())
                .maxVulnerabilitySeverity(request.getMaxVulnerabilitySeverity())
                .testCoverage(request.getTestCoverage())
                .codeSmells(request.getCodeSmells())
                .duplication(request.getDuplication())
                .historicalFailureRate(request.getHistoricalFailureRate())
                .technicalDebt(request.getTechnicalDebt())
                .build();

        MlRiskResponse response = mlPredictionClient.predictRisk(mlRequest);

        return ResponseEntity.ok(DirectRiskPredictionResponse.builder()
                .riskLevel(response.getRiskLevel())
                .confidence(response.getConfidence())
                .modelVersion(response.getModelVersion())
                .featureContributions(response.getFeatureContributions())
                .predictionMethod("ML")
                .build());
    }

    @GetMapping("/api/ml/model-info")
    @Operation(summary = "Model version, algorithm, training timestamp, dataset hash/row count, and feature/class " +
            "lists for the currently-loaded ML model. Proxies the ML service's own /model-info. 503 if unconfigured.")
    public ResponseEntity<ModelInfoResponse> getModelInfo() {
        return ResponseEntity.ok(mlPredictionClient.getModelInfo());
    }

    @GetMapping("/api/ml/metrics")
    @Operation(summary = "Accuracy/precision/recall/F1 for the currently-loaded model, from its last training run. " +
            "Proxies the ML service's own /metrics. 503 if unconfigured.")
    public ResponseEntity<ModelMetricsResponse> getMetrics() {
        return ResponseEntity.ok(mlPredictionClient.getMetrics());
    }

    @PostMapping("/api/ml/retrain")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Re-run training on the ML service's currently configured dataset and hot-reload the " +
            "result (admin only - this is a heavyweight operation, previously callable by any authenticated user). " +
            "Lightweight/synchronous - fine for this project's data scale, no job queue involved. " +
            "compareLogreg also reports LogisticRegression metrics for comparison (not served, informational only).")
    public ResponseEntity<RetrainResponse> retrain(
            @RequestParam(name = "compareLogreg", defaultValue = "false") boolean compareLogreg) {
        return ResponseEntity.ok(mlPredictionClient.retrain(compareLogreg));
    }

    @GetMapping("/api/ml/prediction-history")
    @Operation(summary = "Recent ML predictions actually made during project risk analysis (project/component-" +
            "aware, backed by this backend's own log - NOT the ML service's own context-free JSONL log). " +
            "Optionally filter to one project.")
    public ResponseEntity<List<PredictionHistoryEntry>> getPredictionHistory(
            @RequestParam(required = false) Long projectId,
            @RequestParam(defaultValue = "50") int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 500));
        List<MlPredictionLog> logs = projectId != null
                ? mlPredictionLogRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(0, boundedLimit))
                : mlPredictionLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, boundedLimit));

        return ResponseEntity.ok(logs.stream().map(this::toHistoryEntry).toList());
    }

    private PredictionHistoryEntry toHistoryEntry(MlPredictionLog log) {
        return PredictionHistoryEntry.builder()
                .id(log.getId())
                .projectId(log.getProject().getId())
                .componentName(log.getComponentName())
                .predictionMethod(log.getPredictionMethod().name())
                .ruleBasedRiskLevel(log.getRuleBasedRiskLevel())
                .mlRiskLevel(log.getMlRiskLevel())
                .finalRiskLevel(log.getFinalRiskLevel())
                .mlConfidence(log.getMlConfidence())
                .mlModelVersion(log.getMlModelVersion())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
