package com.assqp.platform.controller;

import com.assqp.platform.dto.sonarqube.SonarQubeAnalysisResponse;
import com.assqp.platform.dto.sonarqube.SonarQubeStatusResponse;
import com.assqp.platform.service.SonarQubeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "SonarQube", description = "Real SonarQube integration - additive to the built-in rule-based quality analyzer")
public class SonarQubeController {

    private final SonarQubeService sonarQubeService;

    @GetMapping("/api/sonarqube/status")
    @Operation(summary = "Check whether SonarQube is configured (SONAR_HOST_URL / SONAR_TOKEN)")
    public ResponseEntity<SonarQubeStatusResponse> getStatus() {
        return ResponseEntity.ok(sonarQubeService.getStatus());
    }

    @PostMapping("/api/projects/{projectId}/sonarqube/import")
    @Operation(summary = "Trigger a fetch of the latest SonarQube measures for this project and store them. " +
            "Returns 503 (not a crash) if SonarQube is unavailable or misconfigured.")
    public ResponseEntity<SonarQubeAnalysisResponse> importLatest(
            @PathVariable Long projectId,
            @RequestParam(value = "sonarProjectKey", required = false) String sonarProjectKey) {
        return ResponseEntity.ok(sonarQubeService.importLatest(projectId, sonarProjectKey));
    }

    @GetMapping("/api/projects/{projectId}/sonarqube/latest")
    @Operation(summary = "Get the most recently imported SonarQube analysis for a project")
    public ResponseEntity<SonarQubeAnalysisResponse> getLatest(@PathVariable Long projectId) {
        return ResponseEntity.ok(sonarQubeService.getLatest(projectId));
    }

    @GetMapping("/api/projects/{projectId}/sonarqube")
    @Operation(summary = "Get the full SonarQube import history for a project")
    public ResponseEntity<List<SonarQubeAnalysisResponse>> getHistory(@PathVariable Long projectId) {
        return ResponseEntity.ok(sonarQubeService.getHistory(projectId));
    }
}
