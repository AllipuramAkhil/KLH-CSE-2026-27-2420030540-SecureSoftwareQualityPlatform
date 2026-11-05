package com.assqp.platform.analysis;

import java.util.List;

/**
 * Modular scanner interface. The default implementation is a custom
 * rule-based scanner (RuleBasedSecurityScanner). It is intentionally
 * decoupled from the service layer so that future integrations with
 * professional SAST/DAST/dependency scanning tools (e.g. Semgrep,
 * OWASP Dependency-Check, Trivy) can implement this same interface.
 */
public interface SecurityScanner {
    List<VulnerabilityFinding> scan(String fileName, String fileContent);
}
