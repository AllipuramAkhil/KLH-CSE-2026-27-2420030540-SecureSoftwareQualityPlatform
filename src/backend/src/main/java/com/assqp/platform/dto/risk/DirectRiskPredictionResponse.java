package com.assqp.platform.dto.risk;

import lombok.*;

import java.util.Map;

/** Matches the ML service's /predict-risk response shape, plus predictionMethod for consistency with hybrid results. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectRiskPredictionResponse {
    private String riskLevel;
    private Double confidence;
    private String modelVersion;
    private Map<String, Double> featureContributions;
    private String predictionMethod;
}
