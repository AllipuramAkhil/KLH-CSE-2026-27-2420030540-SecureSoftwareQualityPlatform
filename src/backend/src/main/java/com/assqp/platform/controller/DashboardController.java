package com.assqp.platform.controller;

import com.assqp.platform.dto.dashboard.DashboardResponse;
import com.assqp.platform.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Centralized project quality/security/risk dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get the full centralized dashboard for a project")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable Long projectId) {
        return ResponseEntity.ok(dashboardService.getDashboard(projectId));
    }
}
