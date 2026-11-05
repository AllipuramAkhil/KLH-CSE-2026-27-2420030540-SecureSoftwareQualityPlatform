package com.assqp.platform.dto.sonarqube;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SonarQubeAnalysisResponse {
    private Long id;
    private Long projectId;
    private String sonarProjectKey;
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
    private LocalDateTime importedAt;
}
