package com.assqp.platform.ai;

import com.assqp.platform.exception.AiServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Verifies the platform's core AI-resilience requirement: it must remain
 * fully functional (with rule-based fallback) when the external AI
 * provider is unavailable or unconfigured.
 */
@ExtendWith(MockitoExtension.class)
class TestCaseGeneratorTest {

    @Mock
    private AiClient aiClient;

    private final AiPromptService promptService = new AiPromptService();

    @InjectMocks
    private TestCaseGenerator testCaseGenerator;

    @Test
    void generate_aiAvailable_usesAiResponse() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.complete(anyString(), anyString()))
                .thenReturn("Verify login with valid credentials\nVerify login with invalid credentials");

        var result = testCaseGenerator.generate("LoginService", "public boolean login() {}", 2);

        assertThat(result.aiGenerated()).isTrue();
        assertThat(result.source()).isEqualTo("ai");
        assertThat(result.testCaseTitles()).hasSize(2);
    }

    @Test
    void generate_aiUnavailable_fallsBackToRuleBasedGeneration() {
        when(aiClient.isAvailable()).thenReturn(false);

        var result = testCaseGenerator.generate("PaymentService", "code", 3);

        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.source()).isEqualTo("rule-based");
        assertThat(result.testCaseTitles()).hasSize(3);
        assertThat(result.testCaseTitles().get(0)).contains("PaymentService");
    }

    @Test
    void generate_aiThrowsException_fallsBackGracefully() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.complete(anyString(), anyString()))
                .thenThrow(new AiServiceException("Provider timeout"));

        var result = testCaseGenerator.generate("OrderService", "code", 2);

        assertThat(result.aiGenerated()).isFalse();
        assertThat(result.source()).isEqualTo("rule-based");
        assertThat(result.testCaseTitles()).isNotEmpty();
    }
}
