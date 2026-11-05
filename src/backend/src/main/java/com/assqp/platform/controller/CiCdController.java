package com.assqp.platform.controller;

import com.assqp.platform.dto.cicd.CiCdPipelineResponse;
import com.assqp.platform.dto.cicd.CiCdWebhookRequest;
import com.assqp.platform.service.CiCdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CI/CD entry point. This controller only accepts and forwards pipeline
 * metadata to CiCdService; all triggered quality/security/risk analysis
 * runs inside the service layer, not here.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "CI/CD", description = "Continuous integration/deployment pipeline integration")
public class CiCdController {

    private final CiCdService ciCdService;

    @PostMapping("/api/cicd/webhook")
    @Operation(summary = "Receive a CI/CD pipeline event and trigger automated quality/security/risk analysis")
    public ResponseEntity<CiCdPipelineResponse> webhook(@Valid @RequestBody CiCdWebhookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ciCdService.handleWebhook(request));
    }

    @GetMapping("/api/cicd/{id}")
    @Operation(summary = "Get one pipeline run by id")
    public ResponseEntity<CiCdPipelineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ciCdService.getById(id));
    }

    @GetMapping("/api/projects/{projectId}/cicd")
    @Operation(summary = "Get all pipeline runs for a project, most recent first")
    public ResponseEntity<List<CiCdPipelineResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(ciCdService.getByProject(projectId));
    }

    @PostMapping("/api/projects/{projectId}/cicd/trigger")
    @Operation(summary = "Manually trigger a pipeline run (quality + security + risk analysis) outside of a webhook")
    public ResponseEntity<CiCdPipelineResponse> trigger(@PathVariable Long projectId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ciCdService.trigger(projectId));
    }
}
