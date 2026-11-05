package com.assqp.platform.integration.zap;

import java.util.List;

/**
 * Client for a running OWASP ZAP daemon's REST API (ZAP_API_URL, e.g.
 * http://zap:8090). Runs a baseline-style flow only: spider (crawl) +
 * ZAP's automatic passive scanning - never an active/attack scan, and
 * never against a target outside {@link #getAllowedTargets()}.
 *
 * Must never throw for "not configured" - callers check
 * {@link #isAvailable()} first - and must wrap any network/parse failure
 * in {@link com.assqp.platform.exception.ZapException} so the rule-based
 * scanner (and Trivy) keep working regardless of ZAP's availability.
 */
public interface ZapClient {

    /** True only when a ZAP API URL is configured. Does not guarantee reachability. */
    boolean isAvailable();

    /** The configured allow-list of scannable base URLs. Empty means no live scan is permitted. */
    List<String> getAllowedTargets();

    /** True if targetUrl exactly matches, or is a sub-path of, an allowed base URL. */
    boolean isTargetAllowed(String targetUrl);

    /** Starts a spider (crawl) scan against targetUrl. Returns ZAP's scanId. */
    String startSpiderScan(String targetUrl);

    /** 0-100 crawl completion percentage for the given scanId. */
    int getSpiderStatus(String scanId);

    /**
     * Raw JSON of all alerts ZAP currently holds for the given base URL
     * (accumulated via passive scanning as the spider crawls - safe to
     * call while a scan is still in progress).
     */
    String fetchAlertsJson(String baseUrl);
}
