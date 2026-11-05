package com.assqp.platform.exception;

/**
 * Thrown when SonarQube is not configured, unreachable, or returns an
 * error. Callers must catch this and fall back to the existing
 * RuleBasedQualityAnalyzer results - never let it crash the application.
 */
public class SonarQubeException extends RuntimeException {
    public SonarQubeException(String message) {
        super(message);
    }

    public SonarQubeException(String message, Throwable cause) {
        super(message, cause);
    }
}
