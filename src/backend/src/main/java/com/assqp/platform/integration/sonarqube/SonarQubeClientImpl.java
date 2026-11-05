package com.assqp.platform.integration.sonarqube;

import com.assqp.platform.exception.SonarQubeException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the SonarQube (or SonarCloud - same API shape) Web API's
 * /api/measures/component endpoint to retrieve the current quality
 * metrics for a project.
 *
 * Gracefully degrades: {@link #isAvailable()} returns false whenever
 * SONAR_HOST_URL / SONAR_TOKEN aren't both set, and any network or
 * parsing failure is wrapped in {@link SonarQubeException} rather than
 * propagating raw exceptions - the caller (SonarQubeServiceImpl) catches
 * this and the RuleBasedQualityAnalyzer remains fully functional.
 */
@Slf4j
@Component
public class SonarQubeClientImpl implements SonarQubeClient {

    private static final List<String> METRIC_KEYS = List.of(
            "bugs", "vulnerabilities", "code_smells", "sqale_index", "complexity",
            "sqale_rating", "reliability_rating", "security_rating",
            "coverage", "duplicated_lines_density", "ncloc"
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sonar.host.url:}")
    private String hostUrl;

    @Value("${sonar.token:}")
    private String token;

    @Value("${sonar.project.key:}")
    private String defaultProjectKey;

    public SonarQubeClientImpl(WebClient sonarWebClient) {
        this.webClient = sonarWebClient;
    }

    @Override
    public boolean isAvailable() {
        return hostUrl != null && !hostUrl.isBlank() && token != null && !token.isBlank();
    }

    @Override
    public SonarQubeMeasures fetchMeasures(String sonarProjectKey) {
        if (!isAvailable()) {
            throw new SonarQubeException(
                    "SonarQube is not configured (SONAR_HOST_URL / SONAR_TOKEN missing). " +
                            "The rule-based quality analyzer remains the source of truth.");
        }

        String projectKey = (sonarProjectKey != null && !sonarProjectKey.isBlank())
                ? sonarProjectKey : defaultProjectKey;

        if (projectKey == null || projectKey.isBlank()) {
            throw new SonarQubeException(
                    "No Sonar project key provided and SONAR_PROJECT_KEY is not configured.");
        }

        try {
            String metricKeys = String.join(",", METRIC_KEYS);

            String rawJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/measures/component")
                            .queryParam("component", projectKey)
                            .queryParam("metricKeys", metricKeys)
                            .build())
                    .headers(h -> h.setBasicAuth(token, ""))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            if (rawJson == null || rawJson.isBlank()) {
                throw new SonarQubeException("SonarQube returned an empty response for project key: " + projectKey);
            }

            return parse(rawJson);

        } catch (SonarQubeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("SonarQube call failed for project key {}: {}", projectKey, ex.getMessage());
            throw new SonarQubeException("SonarQube call failed: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private SonarQubeMeasures parse(String rawJson) {
        try {
            Map<String, Object> root = objectMapper.readValue(rawJson, Map.class);
            Map<String, Object> component = (Map<String, Object>) root.get("component");
            if (component == null) {
                throw new SonarQubeException("SonarQube response did not contain a 'component' object " +
                        "- check that the project key exists and the token has browse permission.");
            }

            List<Map<String, Object>> measures = (List<Map<String, Object>>) component.get("measures");
            Map<String, String> values = new java.util.HashMap<>();
            if (measures != null) {
                for (Map<String, Object> m : measures) {
                    Object metric = m.get("metric");
                    Object value = m.get("value");
                    if (metric != null && value != null) {
                        values.put(String.valueOf(metric), String.valueOf(value));
                    }
                }
            }

            return SonarQubeMeasures.builder()
                    .bugs(intOrNull(values.get("bugs")))
                    .vulnerabilities(intOrNull(values.get("vulnerabilities")))
                    .codeSmells(intOrNull(values.get("code_smells")))
                    .technicalDebtMinutes(longOrNull(values.get("sqale_index")))
                    .complexity(doubleOrNull(values.get("complexity")))
                    .maintainabilityRating(doubleOrNull(values.get("sqale_rating")))
                    .reliabilityRating(doubleOrNull(values.get("reliability_rating")))
                    .securityRating(doubleOrNull(values.get("security_rating")))
                    .coveragePercent(doubleOrNull(values.get("coverage")))
                    .duplicatedLinesDensity(doubleOrNull(values.get("duplicated_lines_density")))
                    .linesOfCode(longOrNull(values.get("ncloc")))
                    .rawJson(rawJson)
                    .build();

        } catch (SonarQubeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SonarQubeException("Failed to parse SonarQube measures response: " + ex.getMessage(), ex);
        }
    }

    private Integer intOrNull(String v) {
        try {
            return v == null ? null : (int) Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long longOrNull(String v) {
        try {
            return v == null ? null : (long) Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double doubleOrNull(String v) {
        try {
            return v == null ? null : Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
