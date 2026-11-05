package com.assqp.platform.integration.ml;

import lombok.*;

/**
 * Exact wire format for POST {ML_SERVICE_URL}/predict-risk - field names
 * must match app/schemas.py RiskPredictionRequest in ml-service, since
 * this is serialized directly to JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MlRiskRequest {
    private Double codeComplexity;
    private Double codeChurn;
    private Double historicalDefects;
    private Double vulnerabilityCount;
    private Double maxVulnerabilitySeverity;
    private Double testCoverage;
    private Double codeSmells;
    private Double duplication;
    private Double historicalFailureRate;
    private Double technicalDebt;
}
