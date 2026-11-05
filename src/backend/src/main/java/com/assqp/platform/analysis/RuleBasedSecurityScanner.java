package com.assqp.platform.analysis;

import com.assqp.platform.enums.Severity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom, dependency-free rule-based vulnerability scanner. This is
 * intentionally kept separate from any future professional SAST/DAST or
 * dependency-scanning integration so the platform remains functional and
 * self-contained without third-party services.
 */
@Component
public class RuleBasedSecurityScanner implements SecurityScanner {

    private record Rule(Pattern pattern, String title, String category, String cwe,
                         String owasp, Severity severity, String impact, String recommendation) {
    }

    private final List<Rule> rules = List.of(
            new Rule(Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE)\\s.+\\+\\s*\\w+"),
                    "Potential SQL Injection", "SQL Injection", "CWE-89", "A03:2021-Injection",
                    Severity.CRITICAL, "Attacker could read, modify, or delete database records.",
                    "Use parameterized queries or JPA/Hibernate named parameters instead of string concatenation."),

            new Rule(Pattern.compile("(?i)innerHTML\\s*="),
                    "Potential Cross-Site Scripting (XSS)", "XSS", "CWE-79", "A03:2021-Injection",
                    Severity.HIGH, "Attacker-controlled input rendered as HTML could execute arbitrary scripts.",
                    "Sanitize and encode all untrusted input before rendering; prefer textContent over innerHTML."),

            new Rule(Pattern.compile("(?i)(Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder)\\s*\\("),
                    "Potential Command Injection", "Command Injection", "CWE-78", "A03:2021-Injection",
                    Severity.CRITICAL, "Attacker input passed to system shell could execute arbitrary OS commands.",
                    "Avoid invoking shell commands with user input; use safe APIs and strict allow-lists."),

            new Rule(Pattern.compile("(?i)(password|secret|api[_-]?key|token)\\s*=\\s*\"[^\"]{4,}\""),
                    "Hardcoded Secret Detected", "Hardcoded Secrets", "CWE-798", "A07:2021-Identification and Authentication Failures",
                    Severity.CRITICAL, "Exposed credentials could allow unauthorized access to systems or data.",
                    "Move secrets to environment variables or a secrets manager; never commit credentials to source control."),

            new Rule(Pattern.compile("(?i)@PreAuthorize\\s*\\(\\s*\"permitAll\"\\s*\\)"),
                    "Potential Broken Access Control", "Broken Access Control", "CWE-284", "A01:2021-Broken Access Control",
                    Severity.HIGH, "Endpoint may be reachable without proper authorization checks.",
                    "Apply role-based or attribute-based access control appropriate to the resource sensitivity."),

            new Rule(Pattern.compile("(?i)MD5|DES/ECB|SHA1\\("),
                    "Weak Cryptographic Algorithm", "Weak Authentication", "CWE-327", "A02:2021-Cryptographic Failures",
                    Severity.MEDIUM, "Weak hashing/encryption algorithms can be broken, exposing sensitive data.",
                    "Use strong, modern algorithms such as BCrypt/Argon2 for passwords and AES-256-GCM for encryption."),

            new Rule(Pattern.compile("(?i)debug\\s*=\\s*true|trustAllCerts|X509TrustManager"),
                    "Insecure Configuration Detected", "Insecure Configuration", "CWE-16", "A05:2021-Security Misconfiguration",
                    Severity.MEDIUM, "Debug mode or disabled certificate validation can leak information or allow MITM attacks.",
                    "Disable debug mode in production and never bypass TLS certificate validation."),

            new Rule(Pattern.compile("(?i)\\.setAttribute\\(\\s*\"ssn|creditCard|cardNumber"),
                    "Potential Sensitive Data Exposure", "Sensitive Data Exposure", "CWE-200", "A02:2021-Cryptographic Failures",
                    Severity.HIGH, "Sensitive personal or financial data may be exposed without proper protection.",
                    "Encrypt sensitive data at rest and in transit; mask sensitive fields in logs and responses.")
    );

    @Override
    public List<VulnerabilityFinding> scan(String fileName, String fileContent) {
        List<VulnerabilityFinding> findings = new ArrayList<>();
        if (fileContent == null || fileContent.isBlank()) {
            return findings;
        }

        String[] lines = fileContent.split("\\r?\\n");

        for (Rule rule : rules) {
            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = rule.pattern().matcher(lines[i]);
                if (matcher.find()) {
                    findings.add(VulnerabilityFinding.builder()
                            .title(rule.title())
                            .category(rule.category())
                            .cwe(rule.cwe())
                            .owaspCategory(rule.owasp())
                            .affectedFile(fileName)
                            .lineNumber(i + 1)
                            .description("Pattern matching '" + rule.category() + "' detected during static analysis of " + fileName + ".")
                            .impact(rule.impact())
                            .recommendation(rule.recommendation())
                            .severity(rule.severity())
                            .build());
                }
            }
        }
        return findings;
    }
}
