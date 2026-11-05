package com.assqp.platform.integration.sonarqube;

import lombok.Builder;
import lombok.Getter;

/**
 * Plain result of fetching + parsing SonarQube's
 * /api/measures/component response for a project.
 */
@Getter
@Builder
public class SonarQubeMeasures {
    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;
    private Long technicalDebtMinutes;
    private Double complexity;
    private Double maintainabilityRating;
    private Double reliabilityRating;
    private Double securityRating;
    private Double coveragePercent;
    private Double duplicatedLinesDensity;
    private Long linesOfCode;
    private String rawJson;
}
