package com.assqp.platform.ai;

import com.assqp.platform.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates test cases from source code / component descriptions.
 * Tries the AI provider first; if unavailable, falls back to a
 * rule-based generator so the platform remains functional offline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseGenerator {

    private final AiClient aiClient;
    private final AiPromptService promptService;

    public GenerationResult generate(String componentName, String sourceSnippet, int count) {
        if (count <= 0) count = 3;

        if (aiClient.isAvailable()) {
            try {
                String response = aiClient.complete(
                        promptService.systemPromptForTestGeneration(),
                        promptService.buildTestGenerationPrompt(componentName, sourceSnippet, count));
                return new GenerationResult(parseAiTests(response, componentName), true, "ai");
            } catch (AiServiceException ex) {
                log.warn("AI test generation failed, using rule-based fallback: {}", ex.getMessage());
            }
        }
        return new GenerationResult(generateRuleBased(componentName, count), false, "rule-based");
    }

    private List<String> parseAiTests(String response, String componentName) {
        List<String> tests = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return generateRuleBased(componentName, 3);
        }
        for (String line : response.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                tests.add(trimmed);
            }
        }
        return tests;
    }

    private List<String> generateRuleBased(String componentName, int count) {
        List<String> tests = new ArrayList<>();
        String[] templates = {
                "Verify that %s behaves correctly with valid input and returns the expected result.",
                "Verify that %s handles null/empty input gracefully without throwing unhandled exceptions.",
                "Verify that %s enforces authorization and rejects unauthorized access attempts.",
                "Verify that %s correctly handles boundary values (min/max/edge cases).",
                "Verify that %s maintains correct behavior under concurrent/repeated invocations.",
                "Verify that %s returns an appropriate error response for invalid input."
        };
        for (int i = 0; i < count; i++) {
            String template = templates[i % templates.length];
            tests.add(String.format(template, componentName));
        }
        return tests;
    }

    public record GenerationResult(List<String> testCaseTitles, boolean aiGenerated, String source) {
    }
}
