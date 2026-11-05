package com.assqp.platform.integration.zap;

import com.assqp.platform.enums.Severity;
import com.assqp.platform.exception.ZapException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses OWASP ZAP JSON output into {@link ZapAlert}s. Accepts three
 * shapes so both the live API and an uploaded report work the same way:
 *
 * 1. Live API response from GET /JSON/core/view/alerts/: {"alerts": [...]}
 * 2. zap-baseline.py -J report.json: {"site": [{"alerts": [...]}, ...]}
 * 3. A bare JSON array of alert objects: [...]
 *
 * Never throws for "no alerts found" - only for genuinely malformed JSON,
 * matching TrivyReportParser's contract.
 */
@Component
public class ZapReportParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ZapAlert> parse(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return parse(root);
        } catch (IOException ex) {
            throw new ZapException("Failed to parse ZAP JSON: " + ex.getMessage(), ex);
        }
    }

    public List<ZapAlert> parse(InputStream inputStream) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);
        return parse(root);
    }

    private List<ZapAlert> parse(JsonNode root) {
        List<ZapAlert> alerts = new ArrayList<>();

        if (root.isArray()) {
            for (JsonNode node : root) {
                alerts.add(toAlert(node));
            }
            return alerts;
        }

        if (root.has("alerts") && root.get("alerts").isArray()) {
            for (JsonNode node : root.get("alerts")) {
                alerts.add(toAlert(node));
            }
            return alerts;
        }

        if (root.has("site") && root.get("site").isArray()) {
            for (JsonNode site : root.get("site")) {
                JsonNode siteAlerts = site.get("alerts");
                if (siteAlerts != null && siteAlerts.isArray()) {
                    for (JsonNode node : siteAlerts) {
                        alerts.add(toAlert(node));
                    }
                }
            }
            return alerts;
        }

        throw new ZapException("Unrecognized ZAP report format - expected an 'alerts' array, " +
                "a 'site[].alerts' array (zap-baseline.py -J output), or a bare JSON array of alerts.");
    }

    private ZapAlert toAlert(JsonNode node) {
        String name = text(node, "alert", text(node, "name", "Unknown ZAP Alert"));
        String riskText = text(node, "risk", text(node, "riskdesc", "Medium"));
        return ZapAlert.builder()
                .pluginId(text(node, "pluginId", text(node, "pluginid", null)))
                .name(name)
                .severity(mapRisk(riskText))
                .confidence(text(node, "confidence", null))
                .description(text(node, "description", null))
                .solution(text(node, "solution", null))
                .url(text(node, "url", text(node, "uri", null)))
                .cweId(text(node, "cweid", text(node, "cwe", null)))
                .build();
    }

    private Severity mapRisk(String risk) {
        if (risk == null) return Severity.MEDIUM;
        String normalized = risk.trim().toLowerCase();
        if (normalized.startsWith("high")) return Severity.HIGH;
        if (normalized.startsWith("critical")) return Severity.CRITICAL;
        if (normalized.startsWith("medium")) return Severity.MEDIUM;
        if (normalized.startsWith("low")) return Severity.LOW;
        if (normalized.startsWith("informational") || normalized.startsWith("info")) return Severity.INFO;
        return Severity.MEDIUM;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return fallback;
        String s = value.asText();
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
