package com.assqp.platform.service;

import com.assqp.platform.dto.ai.AiAnalysisRequest;
import com.assqp.platform.dto.ai.AiAnalysisResponse;
import com.assqp.platform.dto.ai.AiGenerateTestsRequest;

import java.util.List;

public interface AiAnalysisService {
    List<String> generateTests(Long projectId, AiGenerateTestsRequest request);
    AiAnalysisResponse analyze(Long projectId, AiAnalysisRequest request);
    AiAnalysisResponse recommendations(Long projectId);
    AiAnalysisResponse explainDefect(Long projectId, Long qualityAnalysisId);
    AiAnalysisResponse explainVulnerability(Long projectId, Long vulnerabilityId);
}
