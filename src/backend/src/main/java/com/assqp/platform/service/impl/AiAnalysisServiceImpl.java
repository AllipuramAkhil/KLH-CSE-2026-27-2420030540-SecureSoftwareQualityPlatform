package com.assqp.platform.service.impl;

import com.assqp.platform.ai.DefectPredictionEngine;
import com.assqp.platform.ai.RiskPredictionEngine;
import com.assqp.platform.ai.TestCaseGenerator;
import com.assqp.platform.ai.VulnerabilityClassificationEngine;
import com.assqp.platform.dto.ai.AiAnalysisRequest;
import com.assqp.platform.dto.ai.AiAnalysisResponse;
import com.assqp.platform.dto.ai.AiGenerateTestsRequest;
import com.assqp.platform.entity.AiRecommendation;
import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.QualityAnalysis;
import com.assqp.platform.entity.RiskAssessment;
import com.assqp.platform.entity.SecurityVulnerability;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.AiRecommendationRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.QualityAnalysisRepository;
import com.assqp.platform.repository.RiskAssessmentRepository;
import com.assqp.platform.repository.SecurityVulnerabilityRepository;
import com.assqp.platform.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {

    private final TestCaseGenerator testCaseGenerator;
    private final DefectPredictionEngine defectPredictionEngine;
    private final VulnerabilityClassificationEngine vulnerabilityClassificationEngine;
    private final RiskPredictionEngine riskPredictionEngine;

    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final QualityAnalysisRepository qualityAnalysisRepository;
    private final SecurityVulnerabilityRepository vulnerabilityRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final AiRecommendationRepository aiRecommendationRepository;

    @Override
    @Transactional
    public List<String> generateTests(Long projectId, AiGenerateTestsRequest request) {
        getProjectOrThrow(projectId);
        int count = request.getNumberOfTests() != null ? request.getNumberOfTests() : 3;
        var result = testCaseGenerator.generate(request.getComponentName(), request.getSourceCodeSnippet(), count);
        return result.testCaseTitles();
    }

    @Override
    @Transactional
    public AiAnalysisResponse analyze(Long projectId, AiAnalysisRequest request) {
        Project project = getProjectOrThrow(projectId);
        List<String> insights = new ArrayList<>();

        QualityAnalysis latestQuality = qualityAnalysisRepository
                .findTopByProjectIdOrderByAnalyzedAtDesc(projectId).orElse(null);
        if (latestQuality != null) {
            insights.add(defectPredictionEngine.explainDefectRisk(project.getName(),
                    com.assqp.platform.analysis.CodeMetrics.builder()
                            .linesOfCode(latestQuality.getLinesOfCode())
                            .cyclomaticComplexity(latestQuality.getCyclomaticComplexity())
                            .codeDuplicationPercent(latestQuality.getCodeDuplicationPercent())
                            .maintainabilityIndex(latestQuality.getMaintainabilityIndex())
                            .codeSmells(latestQuality.getCodeSmells())
                            .potentialBugs(latestQuality.getPotentialBugs())
                            .build()));
        }

        List<SecurityVulnerability> criticalVulns = vulnerabilityRepository.findByProjectId(projectId);
        criticalVulns.stream()
                .filter(v -> v.getSeverity() == com.assqp.platform.enums.Severity.CRITICAL)
                .limit(3)
                .forEach(v -> insights.add(vulnerabilityClassificationEngine.explain(
                        com.assqp.platform.analysis.VulnerabilityFinding.builder()
                                .title(v.getTitle()).category(v.getCategory()).severity(v.getSeverity())
                                .affectedFile(v.getAffectedFile()).lineNumber(v.getLineNumber())
                                .description(v.getDescription()).impact(v.getImpact())
                                .recommendation(v.getRecommendation()).build())));

        boolean aiGenerated = !insights.isEmpty();

        AiRecommendation recommendation = AiRecommendation.builder()
                .project(project)
                .category(request.getAnalysisType() != null ? request.getAnalysisType() : "GENERAL")
                .content(String.join("\n\n", insights))
                .aiGenerated(aiGenerated)
                .build();
        aiRecommendationRepository.save(recommendation);

        return AiAnalysisResponse.builder()
                .summary("Analysis generated " + insights.size() + " insight(s) for project " + project.getName())
                .insights(insights)
                .recommendations(insights)
                .aiGenerated(aiGenerated)
                .source(aiGenerated ? "ai-or-rule-based-hybrid" : "rule-based")
                .build();
    }

    @Override
    public AiAnalysisResponse recommendations(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        List<RiskAssessment> risks = riskAssessmentRepository.findByProjectIdOrderByAssessedAtDesc(projectId);

        List<String> recs = risks.stream().limit(5).map(r ->
                riskPredictionEngine.explainRisk(r.getComponentName(), r.getRiskScore(),
                        r.getRiskLevel().name(), r.getContributingFactors())
        ).toList();

        return AiAnalysisResponse.builder()
                .summary("Top risk-based recommendations for " + project.getName())
                .insights(recs)
                .recommendations(recs)
                .aiGenerated(!recs.isEmpty())
                .source("adaptive-risk-engine")
                .build();
    }

    @Override
    public AiAnalysisResponse explainDefect(Long projectId, Long qualityAnalysisId) {
        Project project = getProjectOrThrow(projectId);
        QualityAnalysis qa = qualityAnalysisRepository.findById(qualityAnalysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Quality analysis not found with id: " + qualityAnalysisId));
        if (!qa.getProject().getId().equals(project.getId())) {
            // Prevents an authorized owner of projectId from reaching another project's
            // quality analysis by guessing/incrementing qualityAnalysisId.
            throw new ResourceNotFoundException("Quality analysis not found with id: " + qualityAnalysisId);
        }

        String explanation = defectPredictionEngine.explainDefectRisk(project.getName(),
                com.assqp.platform.analysis.CodeMetrics.builder()
                        .linesOfCode(qa.getLinesOfCode())
                        .cyclomaticComplexity(qa.getCyclomaticComplexity())
                        .codeDuplicationPercent(qa.getCodeDuplicationPercent())
                        .maintainabilityIndex(qa.getMaintainabilityIndex())
                        .codeSmells(qa.getCodeSmells())
                        .potentialBugs(qa.getPotentialBugs())
                        .build());

        return AiAnalysisResponse.builder()
                .summary("Defect risk explanation for quality analysis #" + qualityAnalysisId)
                .insights(List.of(explanation))
                .recommendations(List.of(explanation))
                .aiGenerated(true)
                .source("defect-prediction-engine")
                .build();
    }

    @Override
    public AiAnalysisResponse explainVulnerability(Long projectId, Long vulnerabilityId) {
        Project project = getProjectOrThrow(projectId);
        SecurityVulnerability vulnerability = vulnerabilityRepository.findById(vulnerabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Vulnerability not found with id: " + vulnerabilityId));
        if (!vulnerability.getProject().getId().equals(project.getId())) {
            throw new ResourceNotFoundException("Vulnerability not found with id: " + vulnerabilityId);
        }

        String explanation = vulnerabilityClassificationEngine.explain(
                com.assqp.platform.analysis.VulnerabilityFinding.builder()
                        .title(vulnerability.getTitle())
                        .category(vulnerability.getCategory())
                        .severity(vulnerability.getSeverity())
                        .affectedFile(vulnerability.getAffectedFile())
                        .lineNumber(vulnerability.getLineNumber())
                        .description(vulnerability.getDescription())
                        .impact(vulnerability.getImpact())
                        .recommendation(vulnerability.getRecommendation())
                        .build());

        return AiAnalysisResponse.builder()
                .summary("Vulnerability explanation for finding #" + vulnerabilityId)
                .insights(List.of(explanation))
                .recommendations(List.of(explanation))
                .aiGenerated(true)
                .source("vulnerability-classification-engine")
                .build();
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectAccessGuard.getAccessibleProject(projectId);
    }
}
