package com.assqp.platform.service;

import com.assqp.platform.dto.dashboard.DashboardResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.Report;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.ReportRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.service.impl.ReportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private SecurityVulnerabilityRepository vulnerabilityRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    void generate_validProject_createsAndPersistsReport() {
        Project project = Project.builder().id(1L).name("ASSQP").qualityScore(80.0)
                .securityScore(90.0).riskScore(15.0).build();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        DashboardResponse dashboard = DashboardResponse.builder()
                .projectId(1L).totalTests(10).passedTests(8).failedTests(2).testCoverage(75.0)
                .recentAnalyses(List.of()).mostRiskyComponents(List.of()).build();
        when(dashboardService.getDashboard(1L)).thenReturn(dashboard);
        when(vulnerabilityRepository.findByProjectId(1L)).thenReturn(List.of());
        when(riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(1L)).thenReturn(List.of());

        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(5L);
            return r;
        });

        Map<String, Object> result = reportService.generate(1L, "Monthly Report");

        assertThat(result.get("title")).isEqualTo("Monthly Report");
        assertThat(result.get("id")).isEqualTo(5L);
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(reportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingReport_deletesSuccessfully() {
        Report report = Report.builder().id(5L).project(Project.builder().id(1L).build()).build();
        when(reportRepository.findById(5L)).thenReturn(Optional.of(report));

        reportService.delete(5L);

        verify(reportRepository).delete(report);
    }
}
