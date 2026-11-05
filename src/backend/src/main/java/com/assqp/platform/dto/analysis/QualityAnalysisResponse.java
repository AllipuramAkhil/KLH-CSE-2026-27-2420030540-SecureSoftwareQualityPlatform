package com.assqp.platform.dto.analysis;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityAnalysisResponse {
    private Long id;
    private Long projectId;
    private Integer linesOfCode;
    private Integer numberOfMethods;
    private Double cyclomaticComplexity;
    private Double codeDuplicationPercent;
    private Double maintainabilityIndex;
    private Integer codeSmells;
    private Integer potentialBugs;
    private Double testCoveragePercent;
    private Double technicalDebtHours;
    private Double overallQualityScore;
    private LocalDateTime analyzedAt;
}
