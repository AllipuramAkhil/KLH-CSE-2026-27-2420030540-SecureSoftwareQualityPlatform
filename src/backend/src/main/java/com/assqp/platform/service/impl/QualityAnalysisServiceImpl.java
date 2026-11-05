package com.assqp.platform.service.impl;

import com.assqp.platform.analysis.CodeMetrics;
import com.assqp.platform.analysis.QualityAnalyzer;
import com.assqp.platform.dto.analysis.QualityAnalysisResponse;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.QualityAnalysis;
import com.assqp.platform.entity.SourceCodeFile;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.QualityAnalysisRepository;
import com.assqp.platform.repository.SourceCodeFileRepository;
import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.service.QualityAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QualityAnalysisServiceImpl implements QualityAnalysisService {

    private final QualityAnalysisRepository qualityAnalysisRepository;
    private final SourceCodeFileRepository sourceCodeFileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final QualityAnalyzer qualityAnalyzer;
    private final FeedbackProcessor feedbackProcessor;

    @Override
    @Transactional
    public QualityAnalysisResponse analyze(Long projectId) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        List<SourceCodeFile> files = sourceCodeFileRepository.findByProjectId(projectId);

        List<CodeMetrics> metricsList = files.stream()
                .map(this::analyzeFile)
                .toList();

        CodeMetrics aggregate = qualityAnalyzer.aggregate(metricsList);

        QualityAnalysis analysis = QualityAnalysis.builder()
                .project(project)
                .linesOfCode(aggregate.getLinesOfCode())
                .numberOfMethods(aggregate.getNumberOfMethods())
                .cyclomaticComplexity(aggregate.getCyclomaticComplexity())
                .codeDuplicationPercent(aggregate.getCodeDuplicationPercent())
                .maintainabilityIndex(aggregate.getMaintainabilityIndex())
                .codeSmells(aggregate.getCodeSmells())
                .potentialBugs(aggregate.getPotentialBugs())
                .testCoveragePercent(aggregate.getTestCoveragePercent())
                .technicalDebtHours(aggregate.getTechnicalDebtHours())
                .overallQualityScore(aggregate.getOverallQualityScore())
                .build();

        analysis = qualityAnalysisRepository.save(analysis);

        project.setQualityScore(aggregate.getOverallQualityScore());
        projectRepository.save(project);

        feedbackProcessor.recordCycle(project, "QUALITY_ANALYSIS",
                "Quality analysis completed with score " + aggregate.getOverallQualityScore(),
                aggregate.getOverallQualityScore(), project.getSecurityScore(), project.getRiskScore());

        return toResponse(analysis);
    }

    @Override
    public List<QualityAnalysisResponse> getHistory(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return qualityAnalysisRepository.findByProjectIdOrderByAnalyzedAtDesc(projectId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public QualityAnalysisResponse getLatest(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return qualityAnalysisRepository.findTopByProjectIdOrderByAnalyzedAtDesc(projectId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No quality analysis found for project " + projectId));
    }

    private CodeMetrics analyzeFile(SourceCodeFile file) {
        try {
            String content = Files.readString(Path.of(file.getStoragePath()));
            return qualityAnalyzer.analyze(file.getFileName(), content);
        } catch (IOException ex) {
            return qualityAnalyzer.analyze(file.getFileName(), "");
        }
    }

    @Override
    public QualityAnalysisResponse getById(Long analysisId) {
        QualityAnalysis analysis = qualityAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Quality analysis not found with id: " + analysisId));
        projectAccessGuard.checkAccess(analysis.getProject()); // ownership check - this method takes no projectId
        return toResponse(analysis);
    }

    private QualityAnalysisResponse toResponse(QualityAnalysis a) {
        return QualityAnalysisResponse.builder()
                .id(a.getId())
                .projectId(a.getProject().getId())
                .linesOfCode(a.getLinesOfCode())
                .numberOfMethods(a.getNumberOfMethods())
                .cyclomaticComplexity(a.getCyclomaticComplexity())
                .codeDuplicationPercent(a.getCodeDuplicationPercent())
                .maintainabilityIndex(a.getMaintainabilityIndex())
                .codeSmells(a.getCodeSmells())
                .potentialBugs(a.getPotentialBugs())
                .testCoveragePercent(a.getTestCoveragePercent())
                .technicalDebtHours(a.getTechnicalDebtHours())
                .overallQualityScore(a.getOverallQualityScore())
                .analyzedAt(a.getAnalyzedAt())
                .build();
    }
}
