package com.assqp.platform.controller;

import com.assqp.platform.dto.churn.CodeChurnAnalysisResponse;
import com.assqp.platform.dto.churn.CodeChurnResponse;
import com.assqp.platform.dto.churn.GitChurnStatusResponse;
import com.assqp.platform.service.CodeChurnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Code Churn", description = "Git-based code churn metrics (Phase 9) - reads LOCAL repository " +
        "history via JGit, no GitHub API or network access required. Feeds into risk assessment (Phase 8's " +
        "code_churn ML feature) and the dashboard.")
public class CodeChurnController {

    private final CodeChurnService codeChurnService;

    @GetMapping("/api/code-churn/status")
    @Operation(summary = "Check whether the configured local git repository is readable")
    public ResponseEntity<GitChurnStatusResponse> getStatus() {
        return ResponseEntity.ok(codeChurnService.getStatus());
    }

    @PostMapping("/api/projects/{projectId}/code-churn/analyze")
    @Operation(summary = "Analyze local git history and upsert per-file churn metrics for this project. " +
            "Always returns 200 (gitAvailable=false with an empty result set if the repo isn't readable) - " +
            "per the graceful-degradation requirement, this never fails the request.")
    public ResponseEntity<CodeChurnAnalysisResponse> analyze(@PathVariable Long projectId) {
        return ResponseEntity.ok(codeChurnService.analyze(projectId));
    }

    @GetMapping("/api/projects/{projectId}/code-churn")
    @Operation(summary = "Get the most recently computed churn metrics for this project, highest-churn first")
    public ResponseEntity<List<CodeChurnResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(codeChurnService.getByProject(projectId));
    }
}
