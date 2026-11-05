package com.assqp.platform.dto.risk;

import jakarta.validation.constraints.*;
import lombok.*;

/** Matches the ML service's /predict-risk request shape exactly - see ml-service/README.md. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectRiskPredictionRequest {
    @NotNull @PositiveOrZero
    private Double codeComplexity;
    @NotNull @PositiveOrZero
    private Double codeChurn;
    @NotNull @PositiveOrZero
    private Double historicalDefects;
    @NotNull @PositiveOrZero
    private Double vulnerabilityCount;
    @NotNull @DecimalMin("0") @DecimalMax("10")
    private Double maxVulnerabilitySeverity;
    @NotNull @DecimalMin("0") @DecimalMax("100")
    private Double testCoverage;
    @NotNull @PositiveOrZero
    private Double codeSmells;
    @NotNull @DecimalMin("0") @DecimalMax("100")
    private Double duplication;
    @NotNull @DecimalMin("0") @DecimalMax("1")
    private Double historicalFailureRate;
    @NotNull @PositiveOrZero
    private Double technicalDebt;
}
