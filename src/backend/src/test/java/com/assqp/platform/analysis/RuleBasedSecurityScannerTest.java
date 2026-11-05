package com.assqp.platform.analysis;

import com.assqp.platform.enums.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedSecurityScannerTest {

    private final RuleBasedSecurityScanner scanner = new RuleBasedSecurityScanner();

    @Test
    void scan_detectsHardcodedSecret() {
        String code = "String apiKey = \"sk-1234567890abcdef\";";

        List<VulnerabilityFinding> findings = scanner.scan("Config.java", code);

        assertThat(findings).isNotEmpty();
        assertThat(findings).anyMatch(f -> f.getCategory().equals("Hardcoded Secrets")
                && f.getSeverity() == Severity.CRITICAL);
    }

    @Test
    void scan_detectsSqlInjectionPattern() {
        String code = "String query = \"SELECT * FROM users WHERE id = \" + userId;";

        List<VulnerabilityFinding> findings = scanner.scan("UserDao.java", code);

        assertThat(findings).anyMatch(f -> f.getCategory().equals("SQL Injection"));
    }

    @Test
    void scan_cleanCode_returnsNoFindings() {
        String code = "public int add(int a, int b) { return a + b; }";

        List<VulnerabilityFinding> findings = scanner.scan("Calculator.java", code);

        assertThat(findings).isEmpty();
    }
}
