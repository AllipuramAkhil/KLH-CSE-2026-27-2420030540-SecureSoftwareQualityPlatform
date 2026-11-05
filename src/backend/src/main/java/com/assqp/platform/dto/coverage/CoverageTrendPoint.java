package com.assqp.platform.dto.coverage;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageTrendPoint {
    private String date;
    private Double lineCoveragePercent;
    private Double branchCoveragePercent;
    private Double overallCoveragePercent;
}
