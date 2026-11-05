package com.assqp.platform.analysis;

import java.util.List;

/**
 * Modular analyzer interface. The default implementation is rule-based
 * (RuleBasedQualityAnalyzer). External tools such as SonarQube can later
 * implement this interface to plug directly into the platform without
 * changing any calling code.
 */
public interface QualityAnalyzer {
    CodeMetrics analyze(String fileName, String fileContent);
    CodeMetrics aggregate(List<CodeMetrics> metricsList);
}
