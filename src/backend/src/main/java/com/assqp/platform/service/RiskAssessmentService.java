package com.assqp.platform.service;

import com.assqp.platform.dto.risk.RiskAssessmentResponse;
import com.assqp.platform.dto.risk.RiskSummaryResponse;

import java.util.List;

public interface RiskAssessmentService {
    List<RiskAssessmentResponse> analyze(Long projectId);
    List<RiskAssessmentResponse> getByProject(Long projectId);
    RiskAssessmentResponse getById(Long id);
    RiskSummaryResponse getSummary(Long projectId);
}
