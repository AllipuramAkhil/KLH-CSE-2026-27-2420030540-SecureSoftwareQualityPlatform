package com.assqp.platform.integration.trivy;

import com.assqp.platform.enums.Severity;
import com.assqp.platform.exception.TrivyException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses Trivy's `--format json` output (from `trivy fs`, `trivy image`,
 * or `trivy config`/secret scans - they all share the same top-level
 * Results[] shape). Deliberately reads the JSON as a generic tree rather
 * than binding to a strict POJO schema, since Trivy's JSON schema has
 * evolved across versions and adds new optional fields regularly; we only
 * read the handful of fields the project's vulnerability model needs.
 */
@Slf4j
@Component
public class TrivyReportParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<TrivyFinding> parse(InputStream jsonInputStream) {
        try {
            JsonNode root = objectMapper.readTree(jsonInputStream);
            return parse(root);
        } catch (IOException ex) {
            throw new TrivyException("Failed to read Trivy JSON report: " + ex.getMessage(), ex);
        }
    }

    public List<TrivyFinding> parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return parse(root);
        } catch (IOException ex) {
            throw new TrivyException("Failed to parse Trivy JSON report: " + ex.getMessage(), ex);
        }
    }

    private List<TrivyFinding> parse(JsonNode root) {
        List<TrivyFinding> findings = new ArrayList<>();
        JsonNode results = root.path("Results");
        if (!results.isArray()) {
            // A report with no "Results" (e.g. a completely clean scan) is
            // valid, not an error - just nothing to import.
            return findings;
        }

        for (JsonNode result : results) {
            String target = result.path("Target").asText("unknown");

            for (JsonNode vuln : result.path("Vulnerabilities")) {
                findings.add(TrivyFinding.builder()
                        .vulnerabilityId(vuln.path("VulnerabilityID").asText(null))
                        .packageName(vuln.path("PkgName").asText(null))
                        .installedVersion(vuln.path("InstalledVersion").asText(null))
                        .fixedVersion(emptyToNull(vuln.path("FixedVersion").asText(null)))
                        .severity(mapSeverity(vuln.path("Severity").asText("UNKNOWN")))
                        .title(firstNonBlank(vuln.path("Title").asText(null), vuln.path("VulnerabilityID").asText("Vulnerability")))
                        .description(truncate(vuln.path("Description").asText("")))
                        .target(target)
                        .primaryUrl(vuln.path("PrimaryURL").asText(null))
                        .secretFinding(false)
                        .build());
            }

            for (JsonNode secret : result.path("Secrets")) {
                String ruleId = secret.path("RuleID").asText("secret");
                findings.add(TrivyFinding.builder()
                        .vulnerabilityId("SECRET-" + ruleId)
                        .packageName(null)
                        .installedVersion(null)
                        .fixedVersion(null)
                        .severity(mapSeverity(secret.path("Severity").asText("HIGH")))
                        .title(firstNonBlank(secret.path("Title").asText(null), "Hardcoded secret detected (" + ruleId + ")"))
                        .description("Secret matching rule '" + ruleId + "' detected" +
                                (secret.hasNonNull("Match") ? ": " + truncate(secret.path("Match").asText("")) : "."))
                        .target(target)
                        .primaryUrl(null)
                        .secretFinding(true)
                        .build());
            }
        }
        return findings;
    }

    private Severity mapSeverity(String trivySeverity) {
        if (trivySeverity == null) return Severity.INFO;
        return switch (trivySeverity.toUpperCase()) {
            case "CRITICAL" -> Severity.CRITICAL;
            case "HIGH" -> Severity.HIGH;
            case "MEDIUM" -> Severity.MEDIUM;
            case "LOW" -> Severity.LOW;
            default -> Severity.INFO; // UNKNOWN and anything unrecognized
        };
    }

    private String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 1900 ? s.substring(0, 1900) + "…" : s;
    }
}
