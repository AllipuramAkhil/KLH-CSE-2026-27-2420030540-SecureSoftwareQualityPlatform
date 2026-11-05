package com.assqp.platform.controller;

import com.assqp.platform.dto.testing.TestExecutionRequest;
import com.assqp.platform.dto.testing.TestExecutionResponse;
import com.assqp.platform.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Testing", description = "Test execution tracking")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    @PostMapping("/api/tests/{testId}/execute")
    @Operation(summary = "Record a test execution result for a test case")
    public ResponseEntity<TestExecutionResponse> execute(@PathVariable Long testId,
                                                           @Valid @RequestBody TestExecutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testExecutionService.execute(testId, request));
    }

    @GetMapping("/api/tests/{testId}/executions")
    @Operation(summary = "Get execution history for one test case")
    public ResponseEntity<List<TestExecutionResponse>> getByTestCase(@PathVariable Long testId) {
        return ResponseEntity.ok(testExecutionService.getByTestCase(testId));
    }

    @GetMapping("/api/executions/{id}")
    @Operation(summary = "Get one test execution by id")
    public ResponseEntity<TestExecutionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testExecutionService.getById(id));
    }

    @GetMapping("/api/projects/{projectId}/executions")
    @Operation(summary = "Get execution history across all of a project's test cases")
    public ResponseEntity<List<TestExecutionResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(testExecutionService.getByProject(projectId));
    }
}
