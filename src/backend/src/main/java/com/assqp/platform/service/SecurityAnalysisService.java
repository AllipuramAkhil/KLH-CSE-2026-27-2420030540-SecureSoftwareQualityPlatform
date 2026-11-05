package com.assqp.platform.service;

import com.assqp.platform.dto.analysis.SecurityVulnerabilityResponse;

import java.util.List;

public interface SecurityAnalysisService {
    List<SecurityVulnerabilityResponse> analyze(Long projectId);
    List<SecurityVulnerabilityResponse> getByProject(Long projectId);
    List<SecurityVulnerabilityResponse> getByProjectFiltered(Long projectId, String severity, String status);
    List<SecurityVulnerabilityResponse> getLatest(Long projectId);
    SecurityVulnerabilityResponse getById(Long id);
    SecurityVulnerabilityResponse updateStatus(Long id, String status);
}
