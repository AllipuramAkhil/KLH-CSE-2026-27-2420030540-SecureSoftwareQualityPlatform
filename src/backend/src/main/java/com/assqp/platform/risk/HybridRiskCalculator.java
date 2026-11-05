package com.assqp.platform.risk;

import com.assqp.platform.enums.RiskLevel;
import org.springframework.stereotype.Component;

/**
 * Combines the rule-based RiskLevel (always computed) with the ML
 * service's predicted RiskLevel (when available) into one final level.
 *
 * Strategy: take the MORE SEVERE of the two. This is a deliberately
 * simple, explainable, and conservative choice for a hybrid safety
 * signal - if either the rule-based engine OR the ML model thinks a
 * component is risky, the combined result reflects that, rather than
 * averaging two different levels into something that might understate
 * the risk either one alone flagged.
 */
@Component
public class HybridRiskCalculator {

    public RiskLevel combine(RiskLevel ruleBasedLevel, RiskLevel mlLevel) {
        if (mlLevel == null) {
            return ruleBasedLevel;
        }
        return rank(mlLevel) > rank(ruleBasedLevel) ? mlLevel : ruleBasedLevel;
    }

    private int rank(RiskLevel level) {
        return switch (level) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case CRITICAL -> 3;
        };
    }
}
