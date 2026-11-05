package com.assqp.platform.service.impl;

import com.assqp.platform.adaptive.AdaptiveTestingEngine;
import com.assqp.platform.adaptive.FeedbackProcessor;
import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import com.assqp.platform.entity.AnalysisHistory;
import com.assqp.platform.entity.Project;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.AnalysisHistoryRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.service.AdaptiveTestingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdaptiveTestingServiceImpl implements AdaptiveTestingService {

    private final AdaptiveTestingEngine adaptiveTestingEngine;
    private final FeedbackProcessor feedbackProcessor;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final AnalysisHistoryRepository analysisHistoryRepository;

    @Override
    public List<AdaptiveTestRecommendation> prioritize(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return adaptiveTestingEngine.prioritize(projectId);
    }

    @Override
    public List<AdaptiveTestRecommendation> getRecommendations(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return adaptiveTestingEngine.prioritize(projectId);
    }

    @Override
    @Transactional
    public void submitFeedback(Long projectId, String summary) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);
        feedbackProcessor.recordCycle(project, "ADAPTIVE_TESTING_FEEDBACK", summary,
                project.getQualityScore(), project.getSecurityScore(), project.getRiskScore());
    }

    @Override
    public List<AnalysisHistory> getHistory(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return analysisHistoryRepository.findByProjectIdOrderByRecordedAtDesc(projectId);
    }
}
