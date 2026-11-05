package com.assqp.platform.service;

import com.assqp.platform.dto.analysis.QualityAnalysisResponse;

import java.util.List;

public interface QualityAnalysisService {
    QualityAnalysisResponse analyze(Long projectId);
    List<QualityAnalysisResponse> getHistory(Long projectId);
    QualityAnalysisResponse getLatest(Long projectId);
    QualityAnalysisResponse getById(Long analysisId);
}
