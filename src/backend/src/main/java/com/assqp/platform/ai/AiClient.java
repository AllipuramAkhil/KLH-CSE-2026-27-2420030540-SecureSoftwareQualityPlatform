package com.assqp.platform.ai;

/**
 * Abstraction over any external AI provider. The concrete implementation
 * (AiClientImpl) is configured entirely through environment variables
 * (ai.api.url, ai.api.key). No API key is ever hardcoded. If the provider
 * is unreachable or unconfigured, callers must fall back to rule-based
 * analysis (see DefectPredictionEngine, VulnerabilityClassificationEngine,
 * RiskPredictionEngine, TestCaseGenerator).
 */
public interface AiClient {
    String complete(String systemPrompt, String userPrompt);
    boolean isAvailable();
}
