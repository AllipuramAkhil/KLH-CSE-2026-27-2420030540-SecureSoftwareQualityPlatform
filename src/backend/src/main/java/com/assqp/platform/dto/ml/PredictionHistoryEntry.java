package com.assqp.platform.dto.ml;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionHistoryEntry {
    private Long id;
    private Long projectId;
    private String componentName;
    private String predictionMethod;
    private String ruleBasedRiskLevel;
    private String mlRiskLevel;
    private String finalRiskLevel;
    private Double mlConfidence;
    private String mlModelVersion;
    private LocalDateTime createdAt;
}
