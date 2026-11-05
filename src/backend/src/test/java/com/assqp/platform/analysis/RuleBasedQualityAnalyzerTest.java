package com.assqp.platform.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedQualityAnalyzerTest {

    private final RuleBasedQualityAnalyzer analyzer = new RuleBasedQualityAnalyzer();

    @Test
    void analyze_cleanCode_returnsHighQualityScore() {
        String cleanCode = """
                public class Calculator {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;

        CodeMetrics metrics = analyzer.analyze("Calculator.java", cleanCode);

        assertThat(metrics.getOverallQualityScore()).isGreaterThan(70);
        assertThat(metrics.getPotentialBugs()).isZero();
    }

    @Test
    void analyze_complexCodeWithSmells_returnsLowerQualityScore() {
        StringBuilder messyCode = new StringBuilder("public class Messy {\n");
        for (int i = 0; i < 20; i++) {
            messyCode.append("    if (x == null || y == null) { } // TODO fix this\n");
        }
        messyCode.append("}\n");

        CodeMetrics metrics = analyzer.analyze("Messy.java", messyCode.toString());

        assertThat(metrics.getCodeSmells()).isGreaterThan(0);
        assertThat(metrics.getOverallQualityScore()).isLessThan(90);
    }

    @Test
    void analyze_emptyFile_doesNotThrow() {
        CodeMetrics metrics = analyzer.analyze("Empty.java", "");
        assertThat(metrics.getLinesOfCode()).isZero();
        assertThat(metrics.getOverallQualityScore()).isBetween(0.0, 100.0);
    }
}
