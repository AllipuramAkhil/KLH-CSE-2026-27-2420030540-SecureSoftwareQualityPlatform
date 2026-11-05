package com.assqp.platform.exception;

/**
 * Thrown when OWASP ZAP is not configured/reachable, a requested scan
 * target isn't on the configured allow-list, or a ZAP report/response
 * can't be parsed. Callers must catch this and let the rule-based
 * security scanner (and any Trivy findings) remain fully available -
 * never let it crash the application.
 */
public class ZapException extends RuntimeException {
    public ZapException(String message) {
        super(message);
    }

    public ZapException(String message, Throwable cause) {
        super(message, cause);
    }
}
