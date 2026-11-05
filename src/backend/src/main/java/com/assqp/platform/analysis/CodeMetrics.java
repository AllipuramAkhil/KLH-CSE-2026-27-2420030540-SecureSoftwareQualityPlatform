package com.assqp.platform.analysis;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeMetrics {
    private int linesOfCode;
    private int numberOfMethods;
    private double cyclomaticComplexity;
    private double codeDuplicationPercent;
    private double maintainabilityIndex;
    private int codeSmells;
    private int potentialBugs;
    private double testCoveragePercent;
    private double technicalDebtHours;
    private double overallQualityScore;
}
