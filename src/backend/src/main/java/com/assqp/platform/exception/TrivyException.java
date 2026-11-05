package com.assqp.platform.exception;

/**
 * Thrown when Trivy is not available (binary missing / execution failed)
 * or an uploaded/produced report can't be parsed. Callers must catch this
 * and let RuleBasedSecurityScanner findings remain fully available -
 * never let it crash the application.
 */
public class TrivyException extends RuntimeException {
    public TrivyException(String message) {
        super(message);
    }

    public TrivyException(String message, Throwable cause) {
        super(message, cause);
    }
}
