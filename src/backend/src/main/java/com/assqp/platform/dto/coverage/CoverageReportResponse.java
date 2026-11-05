package com.assqp.platform.dto.coverage;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageReportResponse {
    private Long id;
    private Long projectId;
    private Double lineCoveragePercent;
    private Double branchCoveragePercent;
    private Double instructionCoveragePercent;
    private Double overallCoveragePercent;
    private Integer linesCovered;
    private Integer linesMissed;
    private Integer branchesCovered;
    private Integer branchesMissed;
    private Integer classesCovered;
    private Integer classesMissed;
    private String reportSource;
    private LocalDateTime importedAt;
}
