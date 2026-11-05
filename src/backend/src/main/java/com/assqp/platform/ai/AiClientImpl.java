package com.assqp.platform.ai;

import com.assqp.platform.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiClientImpl implements AiClient {

    private final WebClient webClient;

    @Value("${ai.api.key:}")
    private String apiKey;

    @Value("${ai.api.url:}")
    private String apiUrl;

    @Value("${ai.api.model:claude-sonnet-4-6}")
    private String model;

    public AiClientImpl(WebClient aiWebClient) {
        this.webClient = aiWebClient;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank() && apiUrl != null && !apiUrl.isBlank();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            throw new AiServiceException("AI provider is not configured (AI_API_KEY / AI_API_URL missing)");
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 1024,
                    "system", systemPrompt,
                    "messages", List.of(Map.of("role", "user", "content", userPrompt))
            );

            Map response = webClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(20))
                    .block();

            if (response == null) {
                throw new AiServiceException("AI provider returned an empty response");
            }

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (content == null || content.isEmpty()) {
                throw new AiServiceException("AI provider returned no content");
            }

            return String.valueOf(content.get(0).get("text"));

        } catch (AiServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("AI provider call failed: {}", ex.getMessage());
            throw new AiServiceException("AI provider call failed: " + ex.getMessage(), ex);
        }
    }
}
