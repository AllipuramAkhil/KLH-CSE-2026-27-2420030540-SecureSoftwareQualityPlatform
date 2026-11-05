package com.assqp.platform.exception;

/**
 * Thrown when the ML risk-prediction microservice is not configured/
 * reachable, or its response can't be parsed. Callers must catch this
 * and fall back to RiskAssessmentServiceImpl's rule-based score - never
 * let it crash the application or block risk assessment entirely.
 */
public class MlServiceException extends RuntimeException {
    public MlServiceException(String message) {
        super(message);
    }

    public MlServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
