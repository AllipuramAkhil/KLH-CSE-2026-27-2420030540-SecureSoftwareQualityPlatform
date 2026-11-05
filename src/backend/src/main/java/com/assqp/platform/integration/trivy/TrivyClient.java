package com.assqp.platform.integration.trivy;

/**
 * Runs the `trivy` CLI locally (filesystem/dependency and container image
 * scans) and returns the raw JSON report for {@link TrivyReportParser} to
 * parse. Entirely optional - {@link #isAvailable()} returns false when
 * the binary isn't on PATH, and TrivyServiceImpl falls back to accepting
 * an uploaded report (e.g. the GitHub Actions pipeline's JSON artifact)
 * instead. Never crashes the application either way.
 */
public interface TrivyClient {

    /** True only if the configured trivy binary responds to `--version`. */
    boolean isAvailable();

    /**
     * Runs `trivy fs --format json --scanners vuln,secret,misconfig <targetPath>`
     * and returns the raw JSON output.
     *
     * @param targetPath directory to scan; if null/blank, the configured default is used
     */
    String scanFilesystem(String targetPath);

    /**
     * Runs `trivy image --format json <imageRef>` and returns the raw JSON output.
     */
    String scanImage(String imageRef);
}
