package com.assqp.platform.service.impl;

import com.assqp.platform.dto.dashboard.DashboardResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.Report;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.ReportRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.service.DashboardService;
import com.assqp.platform.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reports are generated and stored as JSON today (contentJson). The schema
 * is deliberately kept as a generic Map so a future PDF/CSV exporter can
 * consume the same structure without changes to this service.
 */
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final DashboardService dashboardService;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @SneakyThrows
    public Map<String, Object> generate(Long projectId, String title) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        DashboardResponse dashboard = dashboardService.getDashboard(projectId);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("qualityScore", project.getQualityScore());
        content.put("securityScore", project.getSecurityScore());
        content.put("riskScore", project.getRiskScore());
        content.put("totalTests", dashboard.getTotalTests());
        content.put("passedTests", dashboard.getPassedTests());
        content.put("failedTests", dashboard.getFailedTests());
        content.put("testCoverage", dashboard.getTestCoverage());
        content.put("vulnerabilities", vulnerabilityRepository.findByProjectId(projectId).size());
        content.put("riskAssessments", riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId).size());
        content.put("recentAnalyses", dashboard.getRecentAnalyses());
        content.put("mostRiskyComponents", dashboard.getMostRiskyComponents());

        Report report = Report.builder()
                .project(project)
                .title(title != null && !title.isBlank() ? title : "ASSQP Report - " + project.getName())
                .contentJson(objectMapper.writeValueAsString(content))
                .build();

        report = reportRepository.save(report);
        return toMap(report);
    }

    @Override
    public List<Map<String, Object>> getByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return reportRepository.findByProjectIdOrderByGeneratedAtDesc(projectId)
                .stream().map(this::toMap).toList();
    }

    @Override
    public Map<String, Object> getById(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        projectAccessGuard.checkAccess(report.getProject()); // ownership check
        return toMap(report);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + id));
        projectAccessGuard.checkAccess(report.getProject()); // ownership check
        reportRepository.delete(report);
    }

    @SneakyThrows
    private Map<String, Object> toMap(Report report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", report.getId());
        map.put("projectId", report.getProject().getId());
        map.put("title", report.getTitle());
        map.put("content", objectMapper.readValue(report.getContentJson(), Map.class));
        map.put("generatedAt", report.getGeneratedAt());
        return map;
    }
}
