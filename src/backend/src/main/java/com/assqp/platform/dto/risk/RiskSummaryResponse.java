package com.assqp.platform.dto.risk;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskSummaryResponse {
    private Long projectId;
    private Double overallRiskScore;
    private String overallRiskLevel;
    private Map<String, Long> riskDistribution;
    private String topRiskyComponent;
}
