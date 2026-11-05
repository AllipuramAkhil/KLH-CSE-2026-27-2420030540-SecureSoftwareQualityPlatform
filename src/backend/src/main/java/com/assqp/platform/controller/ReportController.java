package com.assqp.platform.controller;

import com.assqp.platform.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Centralized quality/security/risk/testing reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/projects/{projectId}/reports")
    @Operation(summary = "Generate a new centralized report combining quality, security, risk, and testing data")
    public ResponseEntity<Map<String, Object>> generate(@PathVariable Long projectId,
                                                          @RequestBody(required = false) Map<String, String> body) {
        String title = body != null ? body.get("title") : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(reportService.generate(projectId, title));
    }

    @GetMapping("/api/projects/{projectId}/reports")
    @Operation(summary = "Get all generated reports for a project, most recent first")
    public ResponseEntity<List<Map<String, Object>>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(reportService.getByProject(projectId));
    }

    @GetMapping("/api/reports/{id}")
    @Operation(summary = "Get one report by id")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getById(id));
    }

    @DeleteMapping("/api/reports/{id}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reportService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
