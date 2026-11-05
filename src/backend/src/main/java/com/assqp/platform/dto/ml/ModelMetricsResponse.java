package com.assqp.platform.dto.ml;

import lombok.*;

import java.util.Map;

/** Mirrors app/schemas.py ModelMetricsResponse in ml-service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelMetricsResponse {
    private double accuracy;
    private double precisionWeighted;
    private double recallWeighted;
    private double f1Weighted;
    /** Present only if training was run with compareLogreg=true - the LogisticRegression comparison metrics. */
    private Map<String, Object> comparison;
}
