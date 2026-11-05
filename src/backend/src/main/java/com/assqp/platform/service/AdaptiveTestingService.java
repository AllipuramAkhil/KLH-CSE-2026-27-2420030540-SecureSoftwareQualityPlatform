package com.assqp.platform.service;

import com.assqp.platform.dto.testing.AdaptiveTestRecommendation;
import com.assqp.platform.entity.AnalysisHistory;

import java.util.List;

public interface AdaptiveTestingService {
    List<AdaptiveTestRecommendation> prioritize(Long projectId);
    List<AdaptiveTestRecommendation> getRecommendations(Long projectId);
    void submitFeedback(Long projectId, String summary);
    List<AnalysisHistory> getHistory(Long projectId);
}
