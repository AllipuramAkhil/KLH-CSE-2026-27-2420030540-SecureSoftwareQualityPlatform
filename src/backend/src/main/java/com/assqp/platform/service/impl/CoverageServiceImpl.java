package com.assqp.platform.service.impl;

import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.coverage.JacocoCoverageResult;
import com.assqp.platform.coverage.JacocoReportParser;
import com.assqp.platform.dto.coverage.CoverageReportResponse;
import com.assqp.platform.dto.coverage.CoverageTrendPoint;
import com.assqp.platform.entity.CoverageReport;
import com.assqp.platform.entity.Project;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.CoverageReportRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.CoverageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Imports real test coverage from JaCoCo XML reports and layers it on top
 * of (without replacing) the existing rule-based / execution-ratio
 * coverage figure already surfaced by QualityAnalysis and the dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverageServiceImpl implements CoverageService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CoverageReportRepository coverageReportRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final JacocoReportParser jacocoReportParser;
    private final FeedbackProcessor feedbackProcessor;

    @Value("${coverage.jacoco.report-path:./target/site/jacoco/jacoco.xml}")
    private String defaultReportPath;

    @Override
    @Transactional
    public CoverageReportResponse importFromDefaultPath(Long projectId) {
        Project project = getProject(projectId);
        JacocoCoverageResult result = jacocoReportParser.parse(Path.of(defaultReportPath));
        return persist(project, result);
    }

    @Override
    @Transactional
    public CoverageReportResponse importFromFile(Long projectId, MultipartFile file) {
        Project project = getProject(projectId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded JaCoCo report file must not be empty");
        }
        try {
            JacocoCoverageResult result = jacocoReportParser.parse(file.getInputStream());
            return persist(project, result);
        } catch (java.io.IOException ex) {
            throw new BadRequestException("Could not read uploaded JaCoCo report: " + ex.getMessage());
        }
    }

    @Override
    public CoverageReportResponse getLatest(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return coverageReportRepository.findTopByProjectIdOrderByImportedAtDesc(projectId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No coverage report has been imported yet for project " + projectId));
    }

    @Override
    public List<CoverageReportResponse> getHistory(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return coverageReportRepository.findByProjectIdOrderByImportedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<CoverageTrendPoint> getTrend(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return coverageReportRepository.findByProjectIdOrderByImportedAtDesc(projectId)
                .stream()
                .sorted((a, b) -> a.getImportedAt().compareTo(b.getImportedAt()))
                .map(r -> CoverageTrendPoint.builder()
                        .date(r.getImportedAt().format(FORMATTER))
                        .lineCoveragePercent(r.getLineCoveragePercent())
                        .branchCoveragePercent(r.getBranchCoveragePercent())
                        .overallCoveragePercent(r.getOverallCoveragePercent())
                        .build())
                .toList();
    }

    private CoverageReportResponse persist(Project project, JacocoCoverageResult result) {
        // "Overall" coverage = line coverage, the conventional headline
        // number most coverage tools (and JaCoCo badges) lead with.
        double overall = result.getLineCoveragePercent();

        CoverageReport report = CoverageReport.builder()
                .project(project)
                .lineCoveragePercent(result.getLineCoveragePercent())
                .branchCoveragePercent(result.getBranchCoveragePercent())
                .instructionCoveragePercent(result.getInstructionCoveragePercent())
                .overallCoveragePercent(overall)
                .linesCovered(result.getLinesCovered())
                .linesMissed(result.getLinesMissed())
                .branchesCovered(result.getBranchesCovered())
                .branchesMissed(result.getBranchesMissed())
                .classesCovered(result.getClassesCovered())
                .classesMissed(result.getClassesMissed())
                .reportSource("JACOCO")
                .build();

        report = coverageReportRepository.save(report);

        feedbackProcessor.recordCycle(project, "COVERAGE_IMPORT",
                "JaCoCo coverage imported: " + overall + "% line, " + result.getBranchCoveragePercent() + "% branch",
                project.getQualityScore(), project.getSecurityScore(), project.getRiskScore());

        log.info("Imported JaCoCo coverage for project {}: line={}%, branch={}%",
                project.getId(), result.getLineCoveragePercent(), result.getBranchCoveragePercent());

        return toResponse(report);
    }

    private Project getProject(Long projectId) {
        return projectAccessGuard.getAccessibleProject(projectId);
    }

    private CoverageReportResponse toResponse(CoverageReport r) {
        return CoverageReportResponse.builder()
                .id(r.getId())
                .projectId(r.getProject().getId())
                .lineCoveragePercent(r.getLineCoveragePercent())
                .branchCoveragePercent(r.getBranchCoveragePercent())
                .instructionCoveragePercent(r.getInstructionCoveragePercent())
                .overallCoveragePercent(r.getOverallCoveragePercent())
                .linesCovered(r.getLinesCovered())
                .linesMissed(r.getLinesMissed())
                .branchesCovered(r.getBranchesCovered())
                .branchesMissed(r.getBranchesMissed())
                .classesCovered(r.getClassesCovered())
                .classesMissed(r.getClassesMissed())
                .reportSource(r.getReportSource())
                .importedAt(r.getImportedAt())
                .build();
    }
}
