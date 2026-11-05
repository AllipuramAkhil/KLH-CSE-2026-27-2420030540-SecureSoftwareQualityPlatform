package com.assqp.platform.ai;

import org.springframework.stereotype.Service;

/**
 * Centralizes prompt construction so all AI-driven engines produce
 * consistent, well-structured prompts.
 */
@Service
public class AiPromptService {

    public String systemPromptForTestGeneration() {
        return "You are a senior QA engineer. Generate concise, high-value test cases "
                + "(title, steps, expected result) for the given component or source code. "
                + "Respond in plain, structured text.";
    }

    public String buildTestGenerationPrompt(String componentName, String sourceSnippet, int count) {
        return "Component: " + componentName + "\n"
                + "Number of test cases requested: " + count + "\n"
                + "Source code:\n" + truncate(sourceSnippet) + "\n\n"
                + "Generate " + count + " test cases covering functional, edge-case, and negative scenarios.";
    }

    public String systemPromptForDefectExplanation() {
        return "You are a senior software quality engineer. Explain the likely root cause and business impact "
                + "of the given code quality issue in 3-5 sentences, then suggest a concrete fix.";
    }

    public String systemPromptForVulnerabilityExplanation() {
        return "You are an application security expert. Explain the given vulnerability finding, its exploitability, "
                + "and provide a concrete remediation recommendation in 3-5 sentences.";
    }

    public String systemPromptForRiskAnalysis() {
        return "You are a software risk analyst. Given the metrics for a component, identify whether it is high-risk "
                + "and explain the key contributing factors in 2-4 sentences.";
    }

    public String systemPromptForSummary() {
        return "You are a QA lead preparing an executive summary. Summarize the analysis results in 4-6 sentences "
                + "for a non-technical stakeholder, highlighting the most important risks and recommended actions.";
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 3000 ? text.substring(0, 3000) + "... [truncated]" : text;
    }
}
