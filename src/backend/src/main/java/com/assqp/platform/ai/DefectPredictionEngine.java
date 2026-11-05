package com.assqp.platform.ai;

import com.assqp.platform.analysis.CodeMetrics;
import com.assqp.platform.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Predicts and explains likely defects in a component. Uses AI for
 * natural-language explanation when available, otherwise generates a
 * deterministic rule-based explanation from the computed metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectPredictionEngine {

    private final AiClient aiClient;
    private final AiPromptService promptService;

    public String explainDefectRisk(String componentName, CodeMetrics metrics) {
        String context = "Component: " + componentName
                + "\nCyclomatic complexity: " + metrics.getCyclomaticComplexity()
                + "\nCode smells: " + metrics.getCodeSmells()
                + "\nPotential bugs: " + metrics.getPotentialBugs()
                + "\nDuplication: " + metrics.getCodeDuplicationPercent() + "%"
                + "\nMaintainability index: " + metrics.getMaintainabilityIndex();

        if (aiClient.isAvailable()) {
            try {
                return aiClient.complete(promptService.systemPromptForDefectExplanation(), context);
            } catch (AiServiceException ex) {
                log.warn("AI defect explanation failed, using rule-based fallback: {}", ex.getMessage());
            }
        }
        return ruleBasedExplanation(componentName, metrics);
    }

    private String ruleBasedExplanation(String componentName, CodeMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append(componentName).append(" shows a defect-risk profile driven by ");

        if (metrics.getCyclomaticComplexity() > 10) {
            sb.append("high cyclomatic complexity (").append(metrics.getCyclomaticComplexity()).append("), ");
        }
        if (metrics.getCodeSmells() > 5) {
            sb.append(metrics.getCodeSmells()).append(" code smells, ");
        }
        if (metrics.getPotentialBugs() > 0) {
            sb.append(metrics.getPotentialBugs()).append(" potential bug pattern(s), ");
        }
        if (metrics.getCodeDuplicationPercent() > 15) {
            sb.append(metrics.getCodeDuplicationPercent()).append("% duplication, ");
        }

        sb.append("resulting in a maintainability index of ").append(metrics.getMaintainabilityIndex()).append("/100. ");
        sb.append("Recommended action: refactor high-complexity methods, remove duplicated logic, and add targeted unit tests before the next release.");
        return sb.toString();
    }
}
