package com.assqp.platform.service;

import com.assqp.platform.dto.sonarqube.SonarQubeAnalysisResponse;
import com.assqp.platform.dto.sonarqube.SonarQubeStatusResponse;

import java.util.List;

public interface SonarQubeService {

    /** Whether SONAR_HOST_URL/SONAR_TOKEN are configured - does not guarantee reachability. */
    SonarQubeStatusResponse getStatus();

    /**
     * Triggers a fetch of the latest measures from SonarQube for the
     * project's configured (or default) Sonar project key and persists
     * them. Throws SonarQubeException if SonarQube is unavailable/misconfigured -
     * the RuleBasedQualityAnalyzer keeps working regardless.
     */
    SonarQubeAnalysisResponse importLatest(Long projectId, String sonarProjectKeyOverride);

    SonarQubeAnalysisResponse getLatest(Long projectId);

    List<SonarQubeAnalysisResponse> getHistory(Long projectId);
}
