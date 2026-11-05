package com.assqp.platform.coverage;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Plain result of parsing a jacoco.xml report's top-level (project-wide)
 * &lt;counter&gt; elements. Percentages are covered / (covered + missed) * 100,
 * rounded to 2 decimal places; 0 when a counter type is absent (e.g. a
 * report with no branches at all).
 */
@Getter
@AllArgsConstructor
public class JacocoCoverageResult {
    private final double lineCoveragePercent;
    private final double branchCoveragePercent;
    private final double instructionCoveragePercent;
    private final int linesCovered;
    private final int linesMissed;
    private final int branchesCovered;
    private final int branchesMissed;
    private final int classesCovered;
    private final int classesMissed;
}
