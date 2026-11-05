package com.assqp.platform.integration.zap;

import com.assqp.platform.exception.ZapException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ZapClientImpl implements ZapClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${zap.api.url:}")
    private String apiUrl;

    @Value("${zap.api.key:}")
    private String apiKey;

    @Value("${zap.allowed-targets:}")
    private String allowedTargetsRaw;

    @Value("${zap.scan.timeout-seconds:120}")
    private int timeoutSeconds;

    public ZapClientImpl(WebClient zapWebClient) {
        this.webClient = zapWebClient;
    }

    @Override
    public boolean isAvailable() {
        return apiUrl != null && !apiUrl.isBlank();
    }

    @Override
    public List<String> getAllowedTargets() {
        List<String> targets = new ArrayList<>();
        if (allowedTargetsRaw == null || allowedTargetsRaw.isBlank()) {
            return targets;
        }
        for (String t : allowedTargetsRaw.split(",")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) {
                targets.add(trimmed);
            }
        }
        return targets;
    }

    @Override
    public boolean isTargetAllowed(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return false;
        }
        String normalizedTarget = targetUrl.trim();
        for (String allowed : getAllowedTargets()) {
            String normalizedAllowed = allowed.endsWith("/") ? allowed : allowed + "/";
            String candidateWithSlash = normalizedTarget.endsWith("/") ? normalizedTarget : normalizedTarget + "/";
            if (normalizedTarget.equals(allowed) || candidateWithSlash.startsWith(normalizedAllowed)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String startSpiderScan(String targetUrl) {
        requireAvailable();
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/JSON/spider/action/scan/")
                            .queryParam("url", targetUrl)
                            .queryParam("recurse", "true")
                            .queryParamIfPresent("apikey", apiKeyParam())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            JsonNode node = objectMapper.readTree(json);
            JsonNode scanNode = node.get("scan");
            if (scanNode == null) {
                throw new ZapException("ZAP spider/action/scan response did not contain a 'scan' id: " + json);
            }
            return scanNode.asText();
        } catch (ZapException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ZAP spider scan trigger failed for target {}: {}", targetUrl, ex.getMessage());
            throw new ZapException("Failed to start ZAP spider scan: " + ex.getMessage(), ex);
        }
    }

    @Override
    public int getSpiderStatus(String scanId) {
        requireAvailable();
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/JSON/spider/view/status/")
                            .queryParam("scanId", scanId)
                            .queryParamIfPresent("apikey", apiKeyParam())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            JsonNode node = objectMapper.readTree(json);
            JsonNode statusNode = node.get("status");
            if (statusNode == null) {
                throw new ZapException("ZAP spider/view/status response did not contain a 'status' field: " + json);
            }
            return Integer.parseInt(statusNode.asText());
        } catch (ZapException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ZAP spider status check failed for scanId {}: {}", scanId, ex.getMessage());
            throw new ZapException("Failed to check ZAP spider status: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String fetchAlertsJson(String baseUrl) {
        requireAvailable();
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/JSON/core/view/alerts/")
                            .queryParam("baseurl", baseUrl)
                            .queryParamIfPresent("apikey", apiKeyParam())
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
            if (json == null || json.isBlank()) {
                throw new ZapException("ZAP returned an empty alerts response for base URL: " + baseUrl);
            }
            return json;
        } catch (ZapException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ZAP alert fetch failed for base URL {}: {}", baseUrl, ex.getMessage());
            throw new ZapException("Failed to fetch ZAP alerts: " + ex.getMessage(), ex);
        }
    }

    private java.util.Optional<String> apiKeyParam() {
        return (apiKey == null || apiKey.isBlank()) ? java.util.Optional.empty() : java.util.Optional.of(apiKey);
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new ZapException("OWASP ZAP is not configured (ZAP_API_URL missing). " +
                    "The rule-based security scanner remains the source of truth - use import-file instead.");
        }
    }
}
