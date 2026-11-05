package com.assqp.platform.integration.ml;

import lombok.*;

import java.util.Map;

/** Mirrors app/schemas.py RiskPredictionResponse in ml-service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlRiskResponse {
    private String riskLevel;
    private Double confidence;
    private String modelVersion;
    private Map<String, Double> featureContributions;
}
