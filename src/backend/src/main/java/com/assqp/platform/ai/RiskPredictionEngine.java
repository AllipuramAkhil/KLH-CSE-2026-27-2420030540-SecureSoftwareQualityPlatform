package com.assqp.platform.ai;

import com.assqp.platform.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskPredictionEngine {

    private final AiClient aiClient;
    private final AiPromptService promptService;

    public String explainRisk(String componentName, double riskScore, String riskLevel, String factors) {
        String context = "Component: " + componentName
                + "\nRisk score: " + riskScore
                + "\nRisk level: " + riskLevel
                + "\nContributing factors: " + factors;

        if (aiClient.isAvailable()) {
            try {
                return aiClient.complete(promptService.systemPromptForRiskAnalysis(), context);
            } catch (AiServiceException ex) {
                log.warn("AI risk explanation failed, using rule-based fallback: {}", ex.getMessage());
            }
        }
        return componentName + " has a " + riskLevel + " risk level (score " + riskScore
                + ") driven primarily by: " + factors + ". Prioritize additional test coverage and code review for this component.";
    }
}
