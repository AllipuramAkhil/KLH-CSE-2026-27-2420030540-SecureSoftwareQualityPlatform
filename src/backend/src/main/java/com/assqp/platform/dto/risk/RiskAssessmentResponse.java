package com.assqp.platform.dto.risk;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessmentResponse {
    private Long id;
    private Long projectId;
    private String componentName;
    private Double probability;
    private Double impact;
    private Double riskScore;
    private String riskLevel;
    private String contributingFactors;
    private LocalDateTime assessedAt;

    // --- Phase 8 additions ---
    private String predictionMethod;
    private String mlRiskLevel;
    private Double mlConfidence;
    private String mlModelVersion;
}
