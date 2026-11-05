package com.assqp.platform.controller;

import com.assqp.platform.dto.risk.RiskAssessmentResponse;
import com.assqp.platform.dto.risk.RiskSummaryResponse;
import com.assqp.platform.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}/risk")
@RequiredArgsConstructor
@Tag(name = "Risk", description = "Risk-based component analysis (Risk = Probability x Impact)")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    @PostMapping("/analyze")
    @Operation(summary = "Recompute risk scores for all of a project's components with known vulnerabilities/" +
            "tests. Replaces the project's existing risk assessment rows entirely (current-state snapshot, " +
            "not an accumulating history) - safe to call repeatedly.")
    public ResponseEntity<List<RiskAssessmentResponse>> analyze(@PathVariable Long projectId) {
        return ResponseEntity.ok(riskAssessmentService.analyze(projectId));
    }

    @GetMapping
    @Operation(summary = "Get the most recent risk assessment results for all of a project's components")
    public ResponseEntity<List<RiskAssessmentResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(riskAssessmentService.getByProject(projectId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one component's risk assessment by id")
    public ResponseEntity<RiskAssessmentResponse> getById(@PathVariable Long projectId, @PathVariable Long id) {
        return ResponseEntity.ok(riskAssessmentService.getById(id));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get an aggregate risk summary (overall score/level, distribution, top risky component)")
    public ResponseEntity<RiskSummaryResponse> getSummary(@PathVariable Long projectId) {
        return ResponseEntity.ok(riskAssessmentService.getSummary(projectId));
    }
}
