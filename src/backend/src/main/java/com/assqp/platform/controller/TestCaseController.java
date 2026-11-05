package com.assqp.platform.controller;

import com.assqp.platform.dto.testing.TestCaseRequest;
import com.assqp.platform.dto.testing.TestCaseResponse;
import com.assqp.platform.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Testing", description = "Test case management")
public class TestCaseController {

    private final TestCaseService testCaseService;

    @PostMapping("/api/projects/{projectId}/tests")
    @Operation(summary = "Create a new test case for a project")
    public ResponseEntity<TestCaseResponse> create(@PathVariable Long projectId, @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(testCaseService.create(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/tests")
    @Operation(summary = "List test cases for a project, optionally filtered by type, priority, or status")
    public ResponseEntity<Page<TestCaseResponse>> getByProject(
            @PathVariable Long projectId,
            @Parameter(description = "Filter by test type: FUNCTIONAL, UNIT, INTEGRATION, API, REGRESSION, SECURITY, PERFORMANCE")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by status: ACTIVE, DRAFT, DEPRECATED, UNDER_REVIEW")
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        if (type == null && priority == null && status == null) {
            return ResponseEntity.ok(testCaseService.getByProject(projectId, pageable));
        }
        return ResponseEntity.ok(testCaseService.getByProjectFiltered(projectId, type, priority, status, pageable));
    }

    @GetMapping("/api/tests/{id}")
    @Operation(summary = "Get one test case by id")
    public ResponseEntity<TestCaseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testCaseService.getById(id));
    }

    @PutMapping("/api/tests/{id}")
    @Operation(summary = "Update a test case's details")
    public ResponseEntity<TestCaseResponse> update(@PathVariable Long id, @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.ok(testCaseService.update(id, request));
    }

    @PatchMapping("/api/tests/{id}/status")
    @Operation(summary = "Update a test case's status (ACTIVE, DRAFT, DEPRECATED, UNDER_REVIEW)")
    public ResponseEntity<TestCaseResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(testCaseService.updateStatus(id, body.get("status")));
    }

    @DeleteMapping("/api/tests/{id}")
    @Operation(summary = "Delete a test case (and its execution history)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        testCaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
