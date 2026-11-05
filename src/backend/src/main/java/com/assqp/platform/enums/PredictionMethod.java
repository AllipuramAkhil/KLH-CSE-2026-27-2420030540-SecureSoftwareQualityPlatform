package com.assqp.platform.enums;

/**
 * How a RiskAssessment's final risk level was determined. HYBRID means
 * both the rule-based engine and the ML service contributed (Phase 8);
 * ML alone is never used without the rule-based engine also running -
 * RULE_BASED is always computed as a baseline/fallback.
 */
public enum PredictionMethod {
    RULE_BASED,
    ML,
    HYBRID
}
